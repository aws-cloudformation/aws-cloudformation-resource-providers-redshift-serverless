package software.amazon.redshiftserverless.snapshot;

import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;

public class DeleteHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        // Set the namespace Name in the callback context. We will use this in the stabilize operation of this handler
        if (callbackContext.getNamespaceName() == null) {
            String namespaceName = getNamespaceName(proxyClient, model.getSnapshotName());
            callbackContext.setNamespaceName(namespaceName);
            logger.log(String.format("Set namespace namein callback context: %s", namespaceName));
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Snapshot::Delete", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .backoffDelay(getBackOffStrategy())
                                .makeServiceCall(this::deleteSnapshot)
                                .stabilize((_awsRequest, _awsResponse, _client, _model, _context) -> isSnapshotActiveAfterDelete(_client, model, _context) &&
                                        isNamespaceActive(_client, _context))
                                .handleError(this::defaultErrorHandler)
                                .done(deleteSnapshotResponse -> {
                                    logger.log(String.format("%s %s deleted.",ResourceModel.TYPE_NAME, model.getSnapshotName()));
                                    return ProgressEvent.defaultSuccessHandler(null);
                                })
                );
    }

    private DeleteSnapshotResponse deleteSnapshot(final DeleteSnapshotRequest deleteSnapshotRequest,
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        DeleteSnapshotResponse deleteSnapshotResponse = null;

        logger.log(String.format("%s %s deleteSnapshot", ResourceModel.TYPE_NAME, deleteSnapshotRequest.snapshotName()));
        deleteSnapshotResponse = proxyClient.injectCredentialsAndInvokeV2(deleteSnapshotRequest, proxyClient.client()::deleteSnapshot);
        logger.log(String.format("%s %s successfully deleted.", ResourceModel.TYPE_NAME, deleteSnapshotRequest.snapshotName()));
        return deleteSnapshotResponse;
    }
}
