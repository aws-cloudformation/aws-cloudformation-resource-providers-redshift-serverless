package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.services.redshiftserverless.model.CreateNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
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
                proxy.initiate("AWS-RedshiftServerless-Namespace::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToCreateRequest)
                        .makeServiceCall(this::createNamespace)
                        .stabilize((_awsRequest, _awsResponse, _client, _model, _context) -> isNamespaceActive(_client, _model, _context))
                        .handleError(this::createNamespaceErrorHandler)
                        .progress()
            )
            .then(progress ->
                new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger)
            );
    }

    private CreateNamespaceResponse createNamespace(final CreateNamespaceRequest createNamespaceRequest,
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        CreateNamespaceResponse createNamespaceResponse = null;

        logger.log(String.format("createNamespace for %s", createNamespaceRequest.namespaceName()));
        createNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(createNamespaceRequest, proxyClient.client()::createNamespace);

        logger.log(String.format("%s %s successfully created.", ResourceModel.TYPE_NAME, createNamespaceRequest.namespaceName()));
        return createNamespaceResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> createNamespaceErrorHandler(final CreateNamespaceRequest createNamespaceRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        return errorHandler(exception);
    }
}
