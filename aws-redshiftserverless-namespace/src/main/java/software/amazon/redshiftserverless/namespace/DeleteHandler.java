package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final ProxyClient<RedshiftClient> redshiftProxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                    proxy.initiate("AWS-RedshiftServerless-Namespace::Delete", proxyClient, model, callbackContext)
                            .translateToServiceRequest(Translator::translateToDeleteRequest)
                            .backoffDelay(BACKOFF_STRATEGY)
                            .makeServiceCall(this::deleteNamespace)
                            .stabilize((_awsRequest, _awsResponse, _client, _model, _context) -> isNamespaceActiveAfterDelete(_client, _model, _context))
                            .handleError(this::defaultErrorHandler)
                            .done(deleteNamespaceResponse -> {
                                logger.log(String.format("%s %s deleted.",ResourceModel.TYPE_NAME, model.getNamespaceName()));
                                // TODO: Need to add a stabilize operation to verify if secret is deleted
                                // This is a temporary fix to handle deletion of secrets for managed passwords
                                // Since deletion of secret is handled async CTv2 is failing even in SingleTestMode
                                try {
                                    Thread.sleep(30000);
                                } catch(InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                }
                                return ProgressEvent.defaultSuccessHandler(null);
                            })
                );
    }

    private DeleteNamespaceResponse deleteNamespace(final DeleteNamespaceRequest deleteNamespaceRequest,
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        DeleteNamespaceResponse deleteNamespaceResponse = null;

        logger.log(String.format("%s %s deleteNamespace", ResourceModel.TYPE_NAME, deleteNamespaceRequest.namespaceName()));
        deleteNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(deleteNamespaceRequest, proxyClient.client()::deleteNamespace);
        logger.log(String.format("%s %s successfully deleted.", ResourceModel.TYPE_NAME, deleteNamespaceRequest.namespaceName()));
        return deleteNamespaceResponse;
    }
}
