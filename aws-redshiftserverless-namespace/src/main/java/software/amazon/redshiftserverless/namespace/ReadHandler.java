package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.GetNamespaceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftArcadiaCoralClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return proxy.initiate("AWS-RedshiftServerless-Namespace::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::getNamespace)
                .handleError(this::getNamespaceErrorHandler)
                .done(this::constructResourceModelFromResponse);
    }

    private GetNamespaceResponse getNamespace(final GetNamespaceRequest getNamespaceRequest,
                                               final ProxyClient<RedshiftArcadiaCoralClient> proxyClient) {
        GetNamespaceResponse getNamespaceResponse = null;

        logger.log(String.format("%s %s getNamespaces.", ResourceModel.TYPE_NAME, getNamespaceRequest.namespaceName()));
        getNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(getNamespaceRequest, proxyClient.client()::getNamespace);
        logger.log(String.format("%s %s has successfully been read.", ResourceModel.TYPE_NAME, getNamespaceRequest.namespaceName()));
        return getNamespaceResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            final GetNamespaceResponse getNamespaceResponse) {
        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(getNamespaceResponse));
    }

    private ProgressEvent<ResourceModel, CallbackContext> getNamespaceErrorHandler(final GetNamespaceRequest getNamespaceRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftArcadiaCoralClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        return errorhandler(exception);
    }
}
