package software.amazon.redshiftserverless.namespace;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.*;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyResponse;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
import software.amazon.awssdk.services.redshift.model.DeleteResourcePolicyRequest;
import software.amazon.awssdk.services.redshift.model.DeleteResourcePolicyResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
        final ProxyClient<RedshiftClient> redshiftProxyClient,
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
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::updateNamespace)
                                .stabilize((_awsRequest, _awsResponse, _client, _model, _context) -> isNamespaceActive(_client, _model, _context))
                                .handleError(this::updateNamespaceErrorHandler)
                                .progress())
                .then(progress -> {
                    progress = proxy.initiate("AWS-RedshiftServerless-Namespace::Read", proxyClient, updateRequestModel, callbackContext)
                            .translateToServiceRequest(Translator::translateToReadRequest)
                            .makeServiceCall(this::getNamespace)
                            .handleError(this::getNamespaceErrorHandler)
                            .done(awsResponse -> {
                                callbackContext.setNamespaceArn(awsResponse.namespace().namespaceArn());
                                return ProgressEvent.progress(Translator.translateFromReadResponse(awsResponse), callbackContext);
                            });
                    return progress;
                })
                .then(progress -> {
                    if (callbackContext.getNamespaceArn() != null && currentModel.getNamespaceResourcePolicy() != null)  {
                        if (currentModel.getNamespaceResourcePolicy().isEmpty()) {
                            if (request.getPreviousResourceState().getNamespaceResourcePolicy() != null) {
                                return proxy.initiate("AWS-Redshift-ResourcePolicy::Delete", redshiftProxyClient, updateRequestModel, callbackContext)
                                        .translateToServiceRequest(resourceModelRequest -> Translator.translateToDeleteResourcePolicyRequest(resourceModelRequest, callbackContext.getNamespaceArn()))
                                        .makeServiceCall(this::deleteNamespaceResourcePolicy)
                                        .progress();
                            }
                        }
                        else {
                            return proxy.initiate("AWS-Redshift-ResourcePolicy::Update", redshiftProxyClient, updateRequestModel, callbackContext)
                                    .translateToServiceRequest(resourceModel -> Translator.translateToPutResourcePolicy(resourceModel, callbackContext.getNamespaceArn(), logger))
                                    .makeServiceCall(this::putNamespaceResourcePolicy)
                                    .progress();
                        }
                    }
                    return progress;
                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, redshiftProxyClient, logger));
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

    private GetNamespaceResponse getNamespace(final GetNamespaceRequest getNamespaceRequest,
                                              final ProxyClient<RedshiftServerlessClient> proxyClient) {
        GetNamespaceResponse getNamespaceResponse = null;

        logger.log(String.format("%s %s getNamespaces.", ResourceModel.TYPE_NAME, getNamespaceRequest.namespaceName()));
        getNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(getNamespaceRequest, proxyClient.client()::getNamespace);
        logger.log(String.format("%s %s has successfully been read.", ResourceModel.TYPE_NAME, getNamespaceRequest.namespaceName()));
        return getNamespaceResponse;
    }

    private PutResourcePolicyResponse putNamespaceResourcePolicy(
            final PutResourcePolicyRequest putRequest,
            final ProxyClient<RedshiftClient> proxyClient) {
        PutResourcePolicyResponse putResponse = null;

        try {
            putResponse = proxyClient.injectCredentialsAndInvokeV2(putRequest, proxyClient.client()::putResourcePolicy);
        } catch (ResourceNotFoundException e){
            throw new CfnNotFoundException(e);
        } catch (InvalidPolicyException | UnsupportedOperationException | InvalidParameterValueException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        } catch (SdkClientException | RedshiftException  e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s successfully put resource policy.", ResourceModel.TYPE_NAME));
        return putResponse;
    }

    private DeleteResourcePolicyResponse deleteNamespaceResourcePolicy(
            final DeleteResourcePolicyRequest deleteRequest,
            final ProxyClient<RedshiftClient> proxyClient) {
        DeleteResourcePolicyResponse deleteResponse = null;

        try{
            deleteResponse = proxyClient.injectCredentialsAndInvokeV2(deleteRequest, proxyClient.client()::deleteResourcePolicy);
        } catch (ResourceNotFoundException e){
            throw new CfnNotFoundException(e);
        } catch ( UnsupportedOperationException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        } catch (SdkClientException | RedshiftException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s successfully deleted resource policy.", ResourceModel.TYPE_NAME));
        return deleteResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateNamespaceErrorHandler(final UpdateNamespaceRequest updateNamespaceRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        return errorHandler(exception);
    }

    private ProgressEvent<ResourceModel, CallbackContext> getNamespaceErrorHandler(final GetNamespaceRequest getNamespaceRequest,
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
