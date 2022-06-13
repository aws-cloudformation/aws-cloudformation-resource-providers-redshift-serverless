package software.amazon.redshiftserverless.namespace;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

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
                .adminUserPassword(StringUtils.equals(prevModel.getAdminUserPassword(), currentModel.getAdminUserPassword()) ? null : currentModel.getAdminUserPassword())
                .adminUsername(StringUtils.equals(prevModel.getAdminUsername(), currentModel.getAdminUsername()) ? null : currentModel.getAdminUsername())
                //TODO: we only support updating db-name after GA
//                .dbName(StringUtils.equals(prevModel.getDbName(), currentModel.getDbName()) ? null : currentModel.getDbName())
                .kmsKeyId(StringUtils.equals(prevModel.getKmsKeyId(), currentModel.getKmsKeyId()) ? null : currentModel.getKmsKeyId())
                .defaultIamRoleArn(StringUtils.equals(prevModel.getDefaultIamRoleArn(), currentModel.getDefaultIamRoleArn()) ? null : currentModel.getDefaultIamRoleArn())
                .iamRoles(compareListParamsEqualOrNot(prevModel.getIamRoles(), currentModel.getIamRoles()) ? null : currentModel.getIamRoles())
                .logExports(compareListParamsEqualOrNot(prevModel.getLogExports(), currentModel.getLogExports()) ? null : currentModel.getLogExports())
                .build();

        // To update the adminUserPassword or adminUserName, we need to specify both username and password in update request
        if (!StringUtils.equals(prevModel.getAdminUserPassword(), currentModel.getAdminUserPassword()) || !StringUtils.equals(prevModel.getAdminUsername(), currentModel.getAdminUsername())) {
            tempUpdateRequestModel = tempUpdateRequestModel.toBuilder()
                    .adminUsername(currentModel.getAdminUsername())
                    .adminUserPassword(currentModel.getAdminUserPassword())
                    .build();
        }

        // To update the defaultIamRole, need to specify the iam roles, we need to specify both defaultIamRole and iamRoles in update request
        if (!StringUtils.equals(prevModel.getDefaultIamRoleArn(), currentModel.getDefaultIamRoleArn())) {
            tempUpdateRequestModel = tempUpdateRequestModel.toBuilder()
                    .defaultIamRoleArn(currentModel.getDefaultIamRoleArn())
                    .iamRoles(currentModel.getIamRoles())
                    .build();
        }

        final ResourceModel updateRequestModel = tempUpdateRequestModel;
        return ProgressEvent.progress(currentModel, callbackContext)
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
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        UpdateNamespaceResponse updateNamespaceResponse = null;

        logger.log(String.format("%s %s updateNamespace.", ResourceModel.TYPE_NAME, updateNamespaceRequest.namespaceName()));
        updateNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(updateNamespaceRequest, proxyClient.client()::updateNamespace);
        logger.log(String.format("%s %s update namespace issued.", ResourceModel.TYPE_NAME,
                updateNamespaceRequest.namespaceName()));
        return updateNamespaceResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateNamespaceErrorHandler(final UpdateNamespaceRequest updateNamespaceRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        return errorHandler(exception);
    }

    private boolean compareListParamsEqualOrNot(List<String> prevParam, List<String> currParam) {
        if ((prevParam == null || prevParam.isEmpty()) && (currParam == null || currParam.isEmpty())) {
            return true;
        } else if (prevParam != null && currParam != null) {
            return CollectionUtils.isEqualCollection(prevParam, currParam);
        }
        return false;
    }
}
