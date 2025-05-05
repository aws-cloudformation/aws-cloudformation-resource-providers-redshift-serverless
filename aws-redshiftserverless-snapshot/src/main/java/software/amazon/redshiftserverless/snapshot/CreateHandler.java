package software.amazon.redshiftserverless.snapshot;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.redshift.model.InvalidPolicyException;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyResponse;
import software.amazon.awssdk.services.redshift.model.RedshiftException;
import software.amazon.awssdk.services.redshift.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.Optional;


public class CreateHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Snapshot::Create::ReadNamespaceBeforeCreate", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToReadNamespaceRequest)
                                .backoffDelay(PREOPERATION_BACKOFF_STRATEGY)// We wait for max of 5mins here
                                .makeServiceCall(this::readNamespace)
                                .stabilize(this::isNamespaceStable) // This basically checks for namespace to be in stable state before we create snapshot
                                .handleError(this::defaultErrorHandler)
                                .done(awsResponse -> {
                                    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                                })
                )
                .then(progress -> {
                    return proxy.initiate("AWS-RedshiftServerless-Snapshot::Create", proxyClient, model, callbackContext)
                            .translateToServiceRequest(Translator::translateToCreateRequest)
                            .backoffDelay(getBackOffStrategy())
                            .makeServiceCall(this::createSnapshot)
                            .stabilize(this::isNamespaceStable)
                            .handleError(this::defaultErrorHandler)
                            .done((_request, _response, _client, _model, _context) -> {
                                return ProgressEvent.progress(_model, callbackContext);
                            });
                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private CreateSnapshotResponse createSnapshot(final CreateSnapshotRequest createSnapshotRequest,
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        CreateSnapshotResponse createSnapshotResponse = null;

        logger.log(String.format("createSnapshot for %s", createSnapshotRequest.snapshotName()));
        createSnapshotResponse = proxyClient.injectCredentialsAndInvokeV2(createSnapshotRequest, proxyClient.client()::createSnapshot);

        logger.log(String.format("%s %s successfully created.", ResourceModel.TYPE_NAME, createSnapshotRequest.snapshotName()));
        return createSnapshotResponse;
    }
}
