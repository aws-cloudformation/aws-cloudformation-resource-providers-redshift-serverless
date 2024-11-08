package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.NamespaceStatus;
import software.amazon.awssdk.services.redshiftserverless.model.RedshiftServerlessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request,
                                                                          final CallbackContext callbackContext,
                                                                          final ProxyClient<RedshiftServerlessClient> proxyClient,
                                                                          final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Delete::ReadWorkgroup", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToReadRequest)
                                .backoffDelay(PREOPERATION_BACKOFF_STRATEGY)// We wait for max of 5mins here
                                .makeServiceCall(this::readWorkgroup)
                                .stabilize(this::isWorkgroupStable) // This basically checks for workgroup to be in stable state before we delete workgroup
                                .handleError(this::deleteWorkgroupErrorHandler)
                                .done( awsResponse -> {
                                    return ProgressEvent.progress(Translator.translateFromReadResponse(awsResponse), callbackContext);
                                })
                )
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Delete::ReadNamespaceBeforeDelete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToReadNamespaceRequest)
                                .backoffDelay(PREOPERATION_BACKOFF_STRATEGY)// We wait for max of 5mins here
                                .makeServiceCall(this::readNamespace)
                                .stabilize(this::isNamespaceStable) // This basically checks for namespace to be in stable state before we delete workgroup
                                .handleError(this::deleteWorkgroupErrorHandler)
                                .done( awsResponse -> {
                                    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                                })
                )
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::deleteWorkgroup)
                                .stabilize(this::isWorkgroupDeleted)
                                .handleError(this::deleteWorkgroupErrorHandler)
                                .done(awsResponse -> {
                                    return ProgressEvent.progress(Translator.translateFromDeleteResponse(awsResponse), callbackContext);
                                })
                )
                .then(progress -> {
                    if (progress.getCallbackContext().isPropagationDelay()) {
                        logger.log("Propagation delay completed");
                        return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
                    }
                    progress.getCallbackContext().setPropagationDelay(true);
                    logger.log("Setting propagation delay");
                    return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(),
                            EVENTUAL_CONSISTENCY_DELAY_SECONDS, progress.getResourceModel());
                })
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::ReadNameSpaceAfterDelete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToReadNamespaceRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::readNamespace)
                                .stabilize(this::isNamespaceStable)
                                .handleError(this::deleteWorkgroupErrorHandler)
                                .progress()
                )
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteWorkgroupErrorHandler(final Object awsRequest,
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
