package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.UpdateNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.UpdateNamespaceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.List;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftArcadiaCoralClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel currModel = request.getDesiredResourceState();
        final ResourceModel prevModel = request.getPreviousResourceState();

        // Generate the resourceModel for update request that only contains updated params
        ResourceModel tempUpdateRequestModel = currModel.toBuilder()
                .adminUserPassword(compareStringParamsEqualOrNot(prevModel.getAdminUserPassword(), currModel.getAdminUserPassword()) ? null : currModel.getAdminUserPassword())
                .adminUsername(compareStringParamsEqualOrNot(prevModel.getAdminUsername(), currModel.getAdminUsername()) ? null : currModel.getAdminUsername())
                .dbName(compareStringParamsEqualOrNot(prevModel.getDbName(), currModel.getDbName()) ? null : currModel.getDbName())
                .kmsKeyId(compareStringParamsEqualOrNot(prevModel.getKmsKeyId(), currModel.getKmsKeyId()) ? null : currModel.getKmsKeyId())
                .defaultIamRoleArn(compareStringParamsEqualOrNot(prevModel.getDefaultIamRoleArn(), currModel.getDefaultIamRoleArn()) ? null : currModel.getDefaultIamRoleArn())
                .iamRoles(compareListParamsEqualOrNot(prevModel.getIamRoles(), currModel.getIamRoles()) ? null : currModel.getIamRoles())
                .logExports(compareListParamsEqualOrNot(prevModel.getLogExports(), currModel.getLogExports()) ? null : currModel.getLogExports())
                .build();

        // To update the adminUserPassword or adminUserName, we need to specify both username and password in update request
        if (!compareStringParamsEqualOrNot(prevModel.getAdminUserPassword(), currModel.getAdminUserPassword()) || !compareStringParamsEqualOrNot(prevModel.getAdminUsername(), currModel.getAdminUsername())) {
            tempUpdateRequestModel = tempUpdateRequestModel.toBuilder()
                    .adminUsername(currModel.getAdminUsername())
                    .adminUserPassword(currModel.getAdminUserPassword())
                    .build();
        }

        // To update the defaultIamRole, need to specify the iam roles, we need to specify both defaultIamRole and iamRoles in update request
        if (!compareStringParamsEqualOrNot(prevModel.getDefaultIamRoleArn(), currModel.getDefaultIamRoleArn())) {
            tempUpdateRequestModel = tempUpdateRequestModel.toBuilder()
                    .defaultIamRoleArn(currModel.getDefaultIamRoleArn())
                    .iamRoles(currModel.getIamRoles())
                    .build();
        }

        final ResourceModel updateRequestModel = tempUpdateRequestModel;
        return ProgressEvent.progress(currModel, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Namespace::Update::first", proxyClient, updateRequestModel, progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .backoffDelay(UPDATE_BACKOFF_STRATEGY)
                                .makeServiceCall(this::updateNamespace)
                                .stabilize((_awsRequest, _awsResponse, _client, _model, _context) -> isNamespaceActive(_client, _model, _context))
                                .handleError(this::updateNamespaceErrorHandler)
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateNamespaceResponse updateNamespace(final UpdateNamespaceRequest updateNamespaceRequest,
                                                    final ProxyClient<RedshiftArcadiaCoralClient> proxyClient) {
        UpdateNamespaceResponse updateNamespaceResponse = null;

        logger.log(String.format("%s %s updateNamespace.", ResourceModel.TYPE_NAME, updateNamespaceRequest.namespaceName()));
        updateNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(updateNamespaceRequest, proxyClient.client()::updateNamespace);
        logger.log(String.format("%s %s update namespace issued.", ResourceModel.TYPE_NAME,
                updateNamespaceRequest.namespaceName()));
        return updateNamespaceResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateNamespaceErrorHandler(final UpdateNamespaceRequest updateNamespaceRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftArcadiaCoralClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        return errorHandler(exception);
    }

    private boolean compareStringParamsEqualOrNot(String prevParam, String currParam) {
        if (prevParam == null && currParam == null) {
            return true;
        } else if (prevParam == null) {
            return false;
        } else {
            return prevParam.equals(currParam);
        }
    }

    private boolean compareListParamsEqualOrNot(List<String> prevParam, List<String> currParam) {
        if (prevParam == null && currParam == null) {
            return true;
        } else if (prevParam != null && currParam != null) {
            Collections.sort(prevParam);
            Collections.sort(currParam);
            return prevParam.equals(currParam);
        } else if ((prevParam == null || prevParam.isEmpty()) && (currParam == null || currParam.isEmpty())) {
            return true;
        }
        return false;
    }
}
