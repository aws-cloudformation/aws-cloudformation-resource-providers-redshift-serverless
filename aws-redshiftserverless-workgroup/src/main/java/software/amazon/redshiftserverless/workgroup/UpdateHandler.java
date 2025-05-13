package software.amazon.redshiftserverless.workgroup;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.InsufficientCapacityException;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.RedshiftServerlessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromRecoveryPointRequest;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromRecoveryPointResponse;
import software.amazon.awssdk.services.redshiftserverless.model.TagResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ThrottlingException;
import software.amazon.awssdk.services.redshiftserverless.model.TooManyTagsException;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.awssdk.services.redshiftserverless.model.WorkgroupStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

public class UpdateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Update::ReadInstance", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToReadRequest)
                                .backoffDelay(PREOPERATION_BACKOFF_STRATEGY)// We wait for max of 5mins here
                                .makeServiceCall(this::readWorkgroup)
                                .stabilize(this::isWorkgroupStable) // This basically checks for workgroup to be stabilized before we perform the update operation
                                .handleError(this::updateWorkgroupErrorHandler)
                                .done((readRequest, readResponse, client, model, context) -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .callbackContext(context)
                                        .callbackDelaySeconds(0)
                                        .resourceModel(getUpdatableResourceModel(model, Translator.translateFromReadResponse(readResponse)))
                                        .status(OperationStatus.IN_PROGRESS)
                                        .build()))
                .then(progress -> {
                    Map<String, String> previousTags = TagHelper.mergeTags(
                            request,
                            TagHelper.convertToMap(request.getPreviousResourceState().getTags()),
                            request.getPreviousSystemTags(),
                            request.getPreviousResourceTags()
                    );
                    Map<String, String>desiredTags = TagHelper.mergeTags(
                            request,
                            TagHelper.convertToMap(request.getDesiredResourceState().getTags()),
                            request.getSystemTags(),
                            request.getDesiredResourceTags()
                    );
                    if (ObjectUtils.notEqual(previousTags, desiredTags)) {
                        progress = proxy.initiate("AWS-RedshiftServerless-Workgroup::Update::UpdateTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(resourceModel -> Translator.translateToUpdateTagsRequest(previousTags, desiredTags, resourceModel.getWorkgroup().getWorkgroupArn()))//Translator.translateToUpdateTagsRequest(request.getDesiredResourceState(), resourceModel))
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::updateTags)
                                .stabilize(this::isWorkgroupStable)
                                .handleError((_awsRequest, _sdkEx, _client, _model, _callbackContext) ->
                                        ProgressEvent.failed(_model, _callbackContext, HandlerErrorCode.UnauthorizedTaggingOperation, _sdkEx.getMessage())
                                )
                                .progress();
                    }
                    return progress;
                })
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Update::UpdateInstance", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall((awsRequest, sdkProxyClient) -> {
                                    UpdateWorkgroupResponse awsResponse = this.updateWorkgroup(awsRequest, sdkProxyClient);

                                    logger.log(String.format("%s : %s has successfully been updated.", ResourceModel.TYPE_NAME, awsRequest.workgroupName()));
                                    logger.log(awsResponse.toString());

                                    return awsResponse;
                                })
                                .stabilize(this::isWorkgroupStable)
                                .handleError(this::updateWorkgroupErrorHandler)
                                .progress())

                .then(progress -> {
                    if (!StringUtils.isEmpty(request.getDesiredResourceState().getSnapshotArn()) ||
                            !StringUtils.isEmpty(request.getDesiredResourceState().getSnapshotName())) {
                        return proxy.initiate("AWS-RedshiftServerless-Workgroup::Update::RestoreFromSnapshot", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToRestoreFromSnapshotRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall((awsRequest, client) -> {
                                    RestoreFromSnapshotResponse response = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::restoreFromSnapshot);
                                    logger.log(String.format("%s : %s has been restored from snapshot.", ResourceModel.TYPE_NAME, awsRequest.workgroupName()));
                                    logger.log(String.format("%s : %s restore response: %s", ResourceModel.TYPE_NAME, awsRequest.workgroupName(), response));
                                    return response;
                                })
                                .stabilize(this::isWorkgroupStable)
                                .handleError(this::updateWorkgroupErrorHandler)
                                .progress();
                    }
                    return progress;
                })
                .then(progress -> {
                    if (!StringUtils.isEmpty(request.getDesiredResourceState().getRecoveryPointId())) {
                        return proxy.initiate("AWS-RedshiftServerless-Workgroup::Update::RestoreFromRecoveryPoint", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToRestoreFromRecoveryPointRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall((awsRequest, client) -> {
                                    RestoreFromRecoveryPointResponse response = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::restoreFromRecoveryPoint);
                                    logger.log(String.format("%s : %s has been restored from recovery point.", ResourceModel.TYPE_NAME, awsRequest.workgroupName()));
                                    logger.log(String.format("%s : %s restore from recovery point response: %s", ResourceModel.TYPE_NAME, awsRequest.workgroupName(), response));
                                    return response;
                                })
                                .stabilize(this::isWorkgroupStable)
                                .handleError(this::updateWorkgroupErrorHandler)
                                .progress();
                    }
                    return progress;
                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    @SuppressWarnings("unchecked")
    private ResourceModel getUpdatableResourceModel(ResourceModel desiredModel, ResourceModel previousModel) {
        BiFunction<Object, Object, Object> getDelta = (desired, previous) -> {
            if (desired instanceof Set && previous instanceof Set) {
                return ((Set<Object>) previous).containsAll((Set<Object>) desired) ? null : desired;
            } else if (desired instanceof Collection && previous instanceof Collection) {
                return CollectionUtils.isEqualCollection((Collection<?>) desired, (Collection<?>) previous) ? null : desired;
            } else {
                return Objects.equals(desired, previous) ? null : desired;
            }
        };

        return desiredModel.toBuilder()
                .baseCapacity((Integer) getDelta.apply(desiredModel.getBaseCapacity(), previousModel.getBaseCapacity()))
                .maxCapacity((Integer) getDelta.apply(desiredModel.getMaxCapacity(), previousModel.getMaxCapacity()))
                .enhancedVpcRouting((Boolean) getDelta.apply(desiredModel.getEnhancedVpcRouting(), previousModel.getEnhancedVpcRouting()))
                .configParameters((Set<ConfigParameter>) getDelta.apply(desiredModel.getConfigParameters(), previousModel.getConfigParameters()))
                .publiclyAccessible((Boolean) getDelta.apply(desiredModel.getPubliclyAccessible(), previousModel.getPubliclyAccessible()))
                .subnetIds((List<String>) getDelta.apply(desiredModel.getSubnetIds(), previousModel.getSubnetIds()))
                .securityGroupIds((List<String>) getDelta.apply(desiredModel.getSecurityGroupIds(), previousModel.getSecurityGroupIds()))
                .pricePerformanceTarget((PerformanceTarget) getDelta.apply(desiredModel.getPricePerformanceTarget(), previousModel.getPricePerformanceTarget()))
                .port((Integer) getDelta.apply(desiredModel.getPort(), previousModel.getPort()))
                .trackName((String) getDelta.apply(desiredModel.getTrackName(), previousModel.getTrackName()))
                .workgroup(previousModel.getWorkgroup())
                .build();
    }

    private TagResourceResponse updateTags(final UpdateTagsRequest awsRequest,
                                           final ProxyClient<RedshiftServerlessClient> proxyClient) {
        TagResourceResponse awsResponse = null;

        if (awsRequest.getDeleteOldTagsRequest().tagKeys().isEmpty()) {
            logger.log(String.format("No tags would be deleted for the resource: %s.", ResourceModel.TYPE_NAME));

        } else {
            proxyClient.injectCredentialsAndInvokeV2(awsRequest.getDeleteOldTagsRequest(), proxyClient.client()::untagResource);
            logger.log(String.format("Delete tags for the resource: %s.", ResourceModel.TYPE_NAME));
        }

        if (awsRequest.getCreateNewTagsRequest().tags().isEmpty()) {
            logger.log(String.format("No tags would be created for the resource: %s.", ResourceModel.TYPE_NAME));

        } else {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest.getCreateNewTagsRequest(), proxyClient.client()::tagResource);
            logger.log(String.format("Create tags for the resource: %s.", ResourceModel.TYPE_NAME));
        }

        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateWorkgroupErrorHandler(final Object awsRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        logger.log(String.format("Operation: %s : encountered exception for model : %s",
                awsRequest.getClass().getName(), ResourceModel.TYPE_NAME));
        logger.log(awsRequest.toString());
        return this.defaultWorkgroupErrorHandler(awsRequest, exception, client, model, context);
    }
}
