package software.amazon.redshiftserverless.snapshot;

import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.redshift.model.*;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.GetSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UpdateHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel currentModel = request.getDesiredResourceState();
        final ResourceModel prevModel = request.getPreviousResourceState();

        // Generate the resourceModel for update request that only contains updated params
        ResourceModel tempUpdateRequestModel = currentModel.toBuilder()
                .retentionPeriod((prevModel.getRetentionPeriod() == currentModel.getRetentionPeriod()) ? null : currentModel.getRetentionPeriod())
                .build();

        // To update the retention period of the snapshot, we need to specify snapshotName in update request
        if (prevModel.getRetentionPeriod() != currentModel.getRetentionPeriod()) {
            tempUpdateRequestModel = tempUpdateRequestModel.toBuilder()
                    .snapshotName(currentModel.getSnapshotName())
                    .build();
        }

        // Set the namespace Name in the callback context. We will use this in the stabilize operation of this handler
        if (callbackContext.getNamespaceName() == null) {
            String namespaceName = getNamespaceName(proxyClient, tempUpdateRequestModel.getSnapshotName());
            callbackContext.setNamespaceName(namespaceName);
            logger.log(String.format("Set namespace namein callback context: %s", namespaceName));
        }


        final ResourceModel updateRequestModel = tempUpdateRequestModel;
        return ProgressEvent.progress(currentModel, callbackContext)
                .then(progress -> {
                    progress = proxy.initiate("AWS-RedshiftServerless-Snapshot::UpdateSnapshot", proxyClient, updateRequestModel, progress.getCallbackContext())
                            .translateToServiceRequest(Translator::translateToUpdateRequest)
                            .backoffDelay(getBackOffStrategy())
                            .makeServiceCall(this::updateSnapshot)
                            .stabilize((_awsRequest, _awsResponse, _client, _model, _context) -> isNamespaceActive(_client, _context))
                            .handleError(this::defaultErrorHandler)
                            .progress();
                    return progress;
                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateSnapshotResponse updateSnapshot(final UpdateSnapshotRequest updateSnapshotRequest,
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        UpdateSnapshotResponse updateSnapshotResponse = null;

        logger.log(String.format("%s %s updateSnapshot.", ResourceModel.TYPE_NAME, updateSnapshotRequest.snapshotName()));
        updateSnapshotResponse = proxyClient.injectCredentialsAndInvokeV2(updateSnapshotRequest, proxyClient.client()::updateSnapshot);
        logger.log(String.format("%s %s update snapshot issued.", ResourceModel.TYPE_NAME,
                updateSnapshotRequest.snapshotName()));
        return updateSnapshotResponse;
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
