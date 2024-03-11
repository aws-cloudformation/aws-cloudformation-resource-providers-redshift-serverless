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
import software.amazon.awssdk.services.redshiftserverless.model.TagResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ThrottlingException;
import software.amazon.awssdk.services.redshiftserverless.model.TooManyTagsException;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

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
                                .makeServiceCall(this::readWorkgroup)
                                .handleError(this::operateTagsErrorHandler)
                                .done((readRequest, readResponse, client, model, context) -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .callbackContext(context)
                                        .callbackDelaySeconds(0)
                                        .resourceModel(getUpdatableResourceModel(model, Translator.translateFromReadResponse(readResponse)))
                                        .status(OperationStatus.IN_PROGRESS)
                                        .build()))

                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Update::ReadTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToReadTagsRequest)
                                .makeServiceCall(this::readTags)
                                .handleError(this::operateTagsErrorHandler)
                                .done((tagsRequest, tagsResponse, client, model, context) -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .callbackContext(context)
                                        .callbackDelaySeconds(0)
                                        .resourceModel(Translator.translateFromReadTagsResponse(tagsResponse, model))
                                        .status(OperationStatus.IN_PROGRESS)
                                        .build()))

                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Update::UpdateTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(resourceModel -> Translator.translateToUpdateTagsRequest(request.getDesiredResourceState(), resourceModel))
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::updateTags)
                                .stabilize(this::isWorkgroupStable)
                                .handleError(this::operateTagsErrorHandler)
                                .progress())

                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Update::UpdateInstance", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::updateWorkgroup)
                                .stabilize(this::isWorkgroupStable)
                                .handleError(this::updateWorkgroupErrorHandler)
                                .progress())

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
                .port((Integer) getDelta.apply(desiredModel.getPort(), previousModel.getPort()))
                .workgroup(previousModel.getWorkgroup())
                .build();
    }

    private GetWorkgroupResponse readWorkgroup(final GetWorkgroupRequest awsRequest,
                                               final ProxyClient<RedshiftServerlessClient> proxyClient) {
        GetWorkgroupResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::getWorkgroup);

        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ListTagsForResourceResponse readTags(final ListTagsForResourceRequest awsRequest,
                                                 final ProxyClient<RedshiftServerlessClient> proxyClient) {
        ListTagsForResourceResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listTagsForResource);

        logger.log(String.format("%s's tags have successfully been read.", ResourceModel.TYPE_NAME));
        return awsResponse;
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

    private ProgressEvent<ResourceModel, CallbackContext> operateTagsErrorHandler(final Object awsRequest,
                                                                                  final Exception exception,
                                                                                  final ProxyClient<RedshiftServerlessClient> client,
                                                                                  final ResourceModel model,
                                                                                  final CallbackContext context) {
        if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);

        } else if (exception instanceof ValidationException ||
                exception instanceof TooManyTagsException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

        } else if (exception instanceof ThrottlingException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.Throttling);

        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);

        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }

    private UpdateWorkgroupResponse updateWorkgroup(final UpdateWorkgroupRequest awsRequest,
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        final int MAX_RETRIES = 4;
        int retryCount = 0;

        while (true) {
            try {
                UpdateWorkgroupResponse awsResponse =
                        proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::updateWorkgroup);

                logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));

                return awsResponse;
            } catch (ConflictException ex) {
                if (retryCount >= MAX_RETRIES || !isRetriableWorkgroupException(ex)) {
                    throw ex;
                }

                logger.log(String.format("Retrying UpdateWorkgroup due to expected ConflictException: " +
                        "%s. Attempt %d/%d", ex.getMessage(), retryCount + 1, MAX_RETRIES));
                retryCount++;
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new RuntimeException("Interrupted during retry wait", ie);
            }
        }

    }

    private ProgressEvent<ResourceModel, CallbackContext> updateWorkgroupErrorHandler(final UpdateWorkgroupRequest awsRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);

        } else if (exception instanceof ValidationException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);

        } else if (exception instanceof ConflictException ||
                exception instanceof InsufficientCapacityException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ResourceConflict);

        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }
}
