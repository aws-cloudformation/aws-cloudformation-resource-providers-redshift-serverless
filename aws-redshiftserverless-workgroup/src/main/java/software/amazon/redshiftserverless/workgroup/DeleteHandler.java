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
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                          final ResourceHandlerRequest<ResourceModel> request,
                                                                          final CallbackContext callbackContext,
                                                                          final ProxyClient<RedshiftServerlessClient> proxyClient,
                                                                          final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall((awsRequest, sdkProxyClient) -> {
                                    DeleteWorkgroupResponse awsResponse = this.deleteWorkgroup(awsRequest, sdkProxyClient);

                                    logger.log(String.format("%s : %s has successfully been deleted.",
                                            ResourceModel.TYPE_NAME, awsResponse.workgroup().workgroupName()));
                                    logger.log(awsResponse.toString());

                                    return awsResponse;
                                })
                                .stabilize(this::isWorkgroupDeleted)
                                .handleError(this::deleteWorkgroupErrorHandler)
                                .done(awsResponse -> {
                                    return ProgressEvent.progress(Translator.translateFromDeleteResponse(awsResponse), callbackContext);
                                })
                )
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::ReadNameSpace", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToReadNamespaceRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall((awsRequest, sdkProxyClient) -> {
                                    GetNamespaceResponse awsResponse = this.readNamespace(awsRequest, sdkProxyClient);

                                    logger.log(String.format("%s : %s has successfully been created.",
                                            ResourceModel.TYPE_NAME, awsResponse.namespace().namespaceName()));
                                    logger.log(awsResponse.toString());

                                    return awsResponse;
                                })
                                .stabilize(this::isNamespaceStable)
                                .handleError(this::deleteWorkgroupErrorHandler)
                                .progress()
                )
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private boolean isWorkgroupDeleted(final DeleteWorkgroupRequest awsRequest,
                                       final RedshiftServerlessResponse awsResponse,
                                       final ProxyClient<RedshiftServerlessClient> proxyClient,
                                       final ResourceModel model,
                                       final CallbackContext context) {

        GetWorkgroupRequest getWorkgroupStatusRequest = GetWorkgroupRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .build();

        try {
            GetWorkgroupResponse getWorkgroupResponse = this.readWorkgroup(getWorkgroupStatusRequest, proxyClient);

            logger.log(String.format("%s : Workgroup: %s has successfully been read.",
                    ResourceModel.TYPE_NAME, getWorkgroupResponse.workgroup().workgroupName()));

            logger.log(getWorkgroupResponse.toString());
        } catch (ResourceNotFoundException e) {
            return true;
        }

        return false;
    }

    private boolean isNamespaceStable(final Object awsRequest,
                                      final RedshiftServerlessResponse awsResponse,
                                      final ProxyClient<RedshiftServerlessClient> proxyClient,
                                      final ResourceModel model,
                                      final CallbackContext context) {

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .namespaceName(model.getNamespaceName())
                .build();

        GetNamespaceResponse getNamespaceResponse = this.readNamespace(getNamespaceRequest, proxyClient);

        logger.log(String.format("%s : Namespace: %s has successfully been read.",
                ResourceModel.TYPE_NAME, getNamespaceResponse.namespace().namespaceName()));

        logger.log(getNamespaceResponse.toString());

        return NAMESPACE_STATUS_AVAILABLE.equalsIgnoreCase(getNamespaceResponse.namespace().statusAsString());
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
