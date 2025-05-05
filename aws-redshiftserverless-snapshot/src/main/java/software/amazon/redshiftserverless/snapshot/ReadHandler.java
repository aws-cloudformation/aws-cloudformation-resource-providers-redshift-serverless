package software.amazon.redshiftserverless.snapshot;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.redshift.model.*;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
import software.amazon.awssdk.services.redshiftserverless.model.GetSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> {
                    progress = proxy.initiate("AWS-RedshiftServerless-Snapshot::Read", proxyClient, model, callbackContext)
                            .translateToServiceRequest(Translator::translateToReadRequest)
                            .makeServiceCall(this::getSnapshot)
                            .handleError(this::defaultErrorHandler)
                            .done(awsResponse -> {
                                return ProgressEvent.success(Translator.translateFromReadResponse(awsResponse), callbackContext);
                            });
                    return progress;
                });
    }

    private GetSnapshotResponse getSnapshot(final GetSnapshotRequest getSnapshotRequest,
                                              final ProxyClient<RedshiftServerlessClient> proxyClient) {
        GetSnapshotResponse getSnapshotResponse = null;

        logger.log(String.format("%s %s getSnapshot.", ResourceModel.TYPE_NAME, getSnapshotRequest.snapshotName()));
        getSnapshotResponse = proxyClient.injectCredentialsAndInvokeV2(getSnapshotRequest, proxyClient.client()::getSnapshot);
        logger.log(String.format("%s %s has successfully been read.", ResourceModel.TYPE_NAME, getSnapshotRequest.snapshotName()));
        return getSnapshotResponse;
    }
}
