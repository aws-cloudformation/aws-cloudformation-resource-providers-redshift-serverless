package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.InsufficientCapacityException;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.RedshiftServerlessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromRecoveryPointRequest;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromRecoveryPointResponse;
import software.amazon.awssdk.services.redshiftserverless.model.TooManyTagsException;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;
import java.util.List;

public class CreateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Create::ReadNamespaceBeforeCreate", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToReadNamespaceRequest)
                                .backoffDelay(PREOPERATION_BACKOFF_STRATEGY)// We wait for max of 5mins here
                                .makeServiceCall(this::readNamespace)
                                .stabilize(this::isNamespaceStable) // This basically checks for namespace to be in stable state before we create workgroup
                                .handleError(this::createWorkgroupErrorHandler)
                                .done(awsResponse -> {
                                    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                                })
                )
                .then(progress -> {
                        List<Tag> mergedTags = TagHelper.convertToTagList(
                                TagHelper.mergeTags(
                                        request,
                                        TagHelper.convertToMap(request.getDesiredResourceState().getTags()),
                                        request.getSystemTags(),
                                        TagHelper.convertToMap(progress.getResourceModel().getTags())
                                ));
                        return proxy.initiate("AWS-RedshiftServerless-Workgroup::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest((model) -> Translator.translateToCreateRequest(model, mergedTags))
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::createWorkgroup)
                                .stabilize(this::isWorkgroupStable)
                                .handleError(this::createWorkgroupErrorHandler)
                                .done(awsResponse -> {
                                    return ProgressEvent.progress(Translator.translateFromCreateResponse(awsResponse), callbackContext);
                                });
                    }
                )
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::ReadNameSpaceAfterCreate", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToReadNamespaceRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::readNamespace)
                                .stabilize(this::isNamespaceStable)
                                .handleError(this::createWorkgroupErrorHandler)
                                .progress()
                )
                .then(progress -> {
                    if (!StringUtils.isEmpty(request.getDesiredResourceState().getSnapshotArn()) ||
                            !StringUtils.isEmpty(request.getDesiredResourceState().getSnapshotName())) {
                        return proxy.initiate("AWS-RedshiftServerless-Workgroup::Create::RestoreFromSnapshot", proxyClient, request.getDesiredResourceState(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToRestoreFromSnapshotRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall((awsRequest, client) -> {
                                    RestoreFromSnapshotResponse response = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::restoreFromSnapshot);
                                    logger.log(String.format("%s : %s has been restored from snapshot.", ResourceModel.TYPE_NAME, awsRequest.workgroupName()));
                                    logger.log(String.format("%s : %s restore from snapshot response: %s", ResourceModel.TYPE_NAME, awsRequest.workgroupName(), response));
                                    return response;
                                })
                                .stabilize(this::isWorkgroupStable)
                                .handleError(this::createWorkgroupErrorHandler)
                                .progress();
                    }
                    return progress;
                })
                .then(progress -> {
                    if (!StringUtils.isEmpty(request.getDesiredResourceState().getRecoveryPointId())) {
                        return proxy.initiate("AWS-RedshiftServerless-Workgroup::Create::RestoreFromRecoveryPoint", proxyClient, request.getDesiredResourceState(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToRestoreFromRecoveryPointRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall((awsRequest, client) -> {
                                    RestoreFromRecoveryPointResponse response = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::restoreFromRecoveryPoint);
                                    logger.log(String.format("%s : %s has been restored from recovery point.", ResourceModel.TYPE_NAME, awsRequest.workgroupName()));
                                    logger.log(String.format("%s : %s restore from recovery point response: %s", ResourceModel.TYPE_NAME, awsRequest.workgroupName(), response));
                                    return response;
                                })
                                .stabilize(this::isWorkgroupStable)
                                .handleError(this::createWorkgroupErrorHandler)
                                .progress();
                    }
                    return progress;
                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> createWorkgroupErrorHandler(final Object awsRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {

        logger.log(String.format("Operation: %s : encountered exception for model: %s",
                awsRequest.getClass().getName(), ResourceModel.TYPE_NAME));
        logger.log(awsRequest.toString());
        Pattern pattern = Pattern.compile(".*is not authorized to perform: redshift-serverless:TagResource.*", Pattern.CASE_INSENSITIVE);
        if(pattern.matcher(exception.getMessage()).matches()){
            return ProgressEvent.failed(model, context, HandlerErrorCode.UnauthorizedTaggingOperation, exception.getMessage());
        }

        return this.defaultWorkgroupErrorHandler(awsRequest, exception, client, model, context);
    }
}
