package software.amazon.redshiftserverless.namespace;

import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.*;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotCopyConfigurationResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.TagResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotCopyConfigurationResponse;
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
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static software.amazon.redshiftserverless.namespace.TagHelper.shouldUpdateTags;

public class UpdateHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final ProxyClient<RedshiftClient> redshiftProxyClient,
        final ProxyClient<SecretsManagerClient> secretsManagerProxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel currentModel = request.getDesiredResourceState();
        final ResourceModel prevModel = request.getPreviousResourceState();

        // Generate the resourceModel for update request that only contains updated params
        ResourceModel tempUpdateRequestModel = currentModel.toBuilder()
                .adminUserPassword(StringUtils.equals(prevModel.getAdminUserPassword(), currentModel.getAdminUserPassword()) ? null : currentModel.getAdminUserPassword())
                .adminUsername(StringUtils.equals(prevModel.getAdminUsername(), currentModel.getAdminUsername()) ? null : currentModel.getAdminUsername())
                .dbName(StringUtils.equals(prevModel.getDbName(), currentModel.getDbName()) ? null : currentModel.getDbName())
                .kmsKeyId(StringUtils.equals(prevModel.getKmsKeyId(), currentModel.getKmsKeyId()) ? null : currentModel.getKmsKeyId())
                .defaultIamRoleArn(StringUtils.equals(prevModel.getDefaultIamRoleArn(), currentModel.getDefaultIamRoleArn()) ? null : currentModel.getDefaultIamRoleArn())
                .iamRoles(compareListParamsEqualOrNot(prevModel.getIamRoles(), currentModel.getIamRoles()) ? null : currentModel.getIamRoles())
                .logExports(compareListParamsEqualOrNot(prevModel.getLogExports(), currentModel.getLogExports()) ? null : currentModel.getLogExports())
                .manageAdminPassword((prevModel.getManageAdminPassword() == currentModel.getManageAdminPassword()) ? null : currentModel.getManageAdminPassword())
                .adminPasswordSecretKmsKeyId(StringUtils.equals(prevModel.getAdminPasswordSecretKmsKeyId(), currentModel.getAdminPasswordSecretKmsKeyId()) ? null : currentModel.getAdminPasswordSecretKmsKeyId())
                .tags(currentModel.getTags())
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

        // To update the adminPasswordSecretKmsKeyId, we need to specify both manageAdminPassword and adminPasswordSecretKmsKeyId in update request
        if (!StringUtils.equals(prevModel.getAdminPasswordSecretKmsKeyId(), currentModel.getAdminPasswordSecretKmsKeyId())) {
            tempUpdateRequestModel = tempUpdateRequestModel.toBuilder()
                    .manageAdminPassword(currentModel.getManageAdminPassword())
                    .adminPasswordSecretKmsKeyId(currentModel.getAdminPasswordSecretKmsKeyId())
                    .build();
        }

        final ResourceModel updateRequestModel = tempUpdateRequestModel;
        return ProgressEvent.progress(currentModel, callbackContext)
                .then(progress -> {
                    progress = proxy.initiate("AWS-RedshiftServerless-Namespace::Update::ReadInstanceBeforeUpdate", proxyClient, updateRequestModel, callbackContext)
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .backoffDelay(PREOPERATION_BACKOFF_STRATEGY)// We wait for max of 5mins here
                        .makeServiceCall(this::getNamespace)
                        .stabilize(this::isNamespaceActive) // This basically checks for namespace to be stabilized before we perform the update operation
                        .handleError(this::defaultErrorHandler)
                        .done(awsResponse -> {
                            callbackContext.setNamespaceArn(awsResponse.namespace().namespaceArn());
                            return ProgressEvent.progress(Translator.translateFromReadResponse(awsResponse), callbackContext);
                        });
                    return progress;
                })
                .then(progress -> {
                    Map<String, String>previousTags = TagHelper.mergeTags(
                            request,
                            TagHelper.convertToMap(request.getPreviousResourceState().getTags()),
                            request.getPreviousSystemTags(),
                            request.getPreviousResourceTags()
                    );

                    Map<String, String>desiredTags = TagHelper.mergeTags(
                            request,
                            TagHelper.convertToMap(request.getDesiredResourceState().getTags()),
                            request.getSystemTags(),
                            request.getDesiredResourceTags()
                    );
                    if (ObjectUtils.notEqual(previousTags, desiredTags)) {
                        progress = proxy.initiate("AWS-RedshiftServerless-Namespace::Update::UpdateTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(resourceModel -> Translator.translateToUpdateTagsRequest(previousTags, desiredTags, resourceModel.getNamespace().getNamespaceArn()))
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::updateTags)
                                .stabilize(this::isNamespaceActive)
                                .handleError(this::defaultErrorHandler)
                                .progress();
                    }
                    return progress;
                })
                .then(progress -> {
                        progress = proxy.initiate("AWS-RedshiftServerless-Namespace::UpdateInstance", proxyClient, updateRequestModel, progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall(this::updateNamespace)
                                .stabilize(this::isNamespaceActive)
                                .handleError(this::defaultErrorHandler)
                                .progress();
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
                .then(progress -> {
                    // We currently only support CRC for 1 destination region per namespace
                    if (currentModel.getSnapshotCopyConfigurations() != null && currentModel.getSnapshotCopyConfigurations().size() > 1) {
                        return ProgressEvent.failed(currentModel, callbackContext, HandlerErrorCode.InvalidRequest,
                                String.format("You can only have one snapshot copy configuration per namespace %s", currentModel.getNamespace().getNamespaceName()));
                    }

                    SnapshotCopyConfigurationDiff diff = getSnapshotCopyConfigurationDiff(
                            Optional.ofNullable(currentModel.getSnapshotCopyConfigurations())
                                    .orElse(Collections.emptyList())
                                    .stream()
                                    .collect(Collectors.toMap(SnapshotCopyConfiguration::getDestinationRegion, Function.identity())),
                            Optional.ofNullable(request.getPreviousResourceState().getSnapshotCopyConfigurations())
                                    .orElse(Collections.emptyList())
                                    .stream()
                                    .collect(Collectors.toMap(SnapshotCopyConfiguration::getDestinationRegion, Function.identity())),
                            getSnapshotCopyConfigurations(proxyClient, currentModel));

                    // 1. Delete snapshot copy configurations
                    for (software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration snapshotCopyConfiguration : diff.getToDelete()) {
                        progress = progress.then(__ -> proxy.initiate(String.format("AWS-RedshiftServerless-Namespace::DeleteSnapshotCopyConfiguration::%s", snapshotCopyConfiguration.destinationRegion()), proxyClient, currentModel, callbackContext)
                                .translateToServiceRequest((model) -> Translator.translateToDeleteSnapshotCopyConfigurationRequest(model, snapshotCopyConfiguration.snapshotCopyConfigurationId()))
                                .makeServiceCall(this::deleteSnapshotCopyConfiguration)
                                .handleError(this::deleteSnapshotCopyConfigurationErrorHandler)
                                .progress());
                    }

                    // 2. Update snapshot copy configurations
                    for (Map.Entry<String, SnapshotCopyConfiguration> entry : diff.getToUpdate().entrySet()) {
                        progress = progress.then(__ -> proxy.initiate(String.format("AWS-RedshiftServerless-Namespace::UpdateSnapshotCopyConfiguration::%s", entry.getValue().getDestinationRegion()), proxyClient, currentModel, callbackContext)
                                .translateToServiceRequest((model) -> Translator.translateToUpdateSnapshotCopyConfigurationRequest(model, entry.getKey(), entry.getValue()))
                                .makeServiceCall(this::updateSnapshotCopyConfiguration)
                                .handleError(this::defaultErrorHandler)
                                .progress());
                    }

                    // 3. Create snapshot copy configurations
                    for (SnapshotCopyConfiguration snapshotCopyConfiguration : diff.getToCreate()) {
                        progress = progress.then(__ -> proxy.initiate(String.format("AWS-RedshiftServerless-Namespace::CreateSnapshotCopyConfiguration::%s", snapshotCopyConfiguration.getDestinationRegion()), proxyClient, currentModel, callbackContext)
                                .translateToServiceRequest((model) -> Translator.translateToCreateSnapshotCopyConfigurationRequest(model, snapshotCopyConfiguration))
                                .makeServiceCall(this::createSnapshotCopyConfiguration)
                                .handleError(this::defaultErrorHandler)
                                .progress());
                    }

                    return progress;
                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger));
    }

    private TagResourceResponse updateTags(final UpdateTagsRequest awsRequest,
                                           final ProxyClient<RedshiftServerlessClient> proxyClient) {
        TagResourceResponse awsResponse = null;

        if (awsRequest.getDeleteOldTagsRequest().tagKeys().isEmpty()) {
            logger.log(String.format("No tags would be deleted for the resource: %s.", ResourceModel.TYPE_NAME));

        } else {
            proxyClient.injectCredentialsAndInvokeV2(awsRequest.getDeleteOldTagsRequest(), proxyClient.client()::untagResource);
            logger.log(String.format("Delete tags for the resource: %s.", ResourceModel.TYPE_NAME));
        }

        if (awsRequest.getCreateNewTagsRequest().tags().isEmpty()) {
            logger.log(String.format("No tags would be created for the resource: %s.", ResourceModel.TYPE_NAME));

        } else {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest.getCreateNewTagsRequest(), proxyClient.client()::tagResource);
            logger.log(String.format("Create tags for the resource: %s.", ResourceModel.TYPE_NAME));
        }

        return awsResponse;
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

    private Map<String, software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration> getSnapshotCopyConfigurations(final ProxyClient<RedshiftServerlessClient> proxyClient, ResourceModel model) {
        try {
            ListSnapshotCopyConfigurationsResponse listResponse = proxyClient.injectCredentialsAndInvokeV2(Translator.translateToListSnapshotCopyConfigurationsRequest(model),
                    proxyClient.client()::listSnapshotCopyConfigurations);
            return listResponse.snapshotCopyConfigurations().stream()
                    .collect(Collectors.toMap(software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration::destinationRegion, Function.identity()));
        } catch (Exception ex) {
            if (ex instanceof ValidationException) {
                logger.log(String.format("CRC feature is not enabled for this region: %s", ex.getMessage()));
                return Collections.emptyMap();
            }
            throw ex;
        }
    }

    private UpdateSnapshotCopyConfigurationResponse updateSnapshotCopyConfiguration(final UpdateSnapshotCopyConfigurationRequest updateRequest,
                                                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        UpdateSnapshotCopyConfigurationResponse updateResponse = proxyClient.injectCredentialsAndInvokeV2(updateRequest, proxyClient.client()::updateSnapshotCopyConfiguration);
        logger.log(String.format("Updated snapshot copy configuration for %s %s in destination region %s.", ResourceModel.TYPE_NAME,
                updateResponse.snapshotCopyConfiguration().namespaceName(), updateResponse.snapshotCopyConfiguration().destinationRegion()));
        return updateResponse;
    }

    private DeleteSnapshotCopyConfigurationResponse deleteSnapshotCopyConfiguration(final DeleteSnapshotCopyConfigurationRequest deleteRequest,
                                                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        DeleteSnapshotCopyConfigurationResponse deleteResponse = proxyClient.injectCredentialsAndInvokeV2(deleteRequest, proxyClient.client()::deleteSnapshotCopyConfiguration);
        logger.log(String.format("Deleted snapshot copy configuration for %s %s in destination region %s.", ResourceModel.TYPE_NAME,
                deleteResponse.snapshotCopyConfiguration().namespaceName(), deleteResponse.snapshotCopyConfiguration().destinationRegion()));
        return deleteResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteSnapshotCopyConfigurationErrorHandler(final DeleteSnapshotCopyConfigurationRequest request,
                                                                                                      final Exception exception,
                                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                                      final ResourceModel model,
                                                                                                      final CallbackContext context) {
        if (exception instanceof ResourceNotFoundException) {
            logger.log(String.format("Snapshot copy configuration id %s does not exist.", request.snapshotCopyConfigurationId()));
            return ProgressEvent.defaultInProgressHandler(context, 0, model);
        }
        return errorHandler(exception);
    }

    /**
     * Determine snapshot copy configurations to delete, update & create
     * We only determine the diffs with the previous model state, this ensures that snapshot copy configurations defined in CFN to only be managed.
     * Therefore, if snapshot copy configurations are created manually but not specified in CFN, these will not be touched by CFN!
     */
    private SnapshotCopyConfigurationDiff getSnapshotCopyConfigurationDiff(final Map<String, SnapshotCopyConfiguration> desiredSnapshotCopyConfigurations,
                                                                           final Map<String, SnapshotCopyConfiguration> previousSnapshotCopyConfigurations,
                                                                           final Map<String, software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration> existingSnapshotCopyConfigurations) {
        // Include snapshot copy configurations found through the API which are not in the previous model state
        SetUtils.intersection(desiredSnapshotCopyConfigurations.keySet(), existingSnapshotCopyConfigurations.keySet())
                .stream()
                .filter(destinationRegion -> !previousSnapshotCopyConfigurations.containsKey(destinationRegion))
                .forEach(destinationRegion -> {
                    software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration existing = existingSnapshotCopyConfigurations.get(destinationRegion);
                    previousSnapshotCopyConfigurations.put(destinationRegion, Translator.translateToSnapshotCopyConfiguration(existing));
                });

        List<SnapshotCopyConfiguration> configurationsToCreate = SetUtils.difference(desiredSnapshotCopyConfigurations.keySet(), previousSnapshotCopyConfigurations.keySet())
                .stream()
                .map(desiredSnapshotCopyConfigurations::get)
                .collect(Collectors.toList());

        List<software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration> configurationsToDelete = SetUtils.difference(previousSnapshotCopyConfigurations.keySet(), desiredSnapshotCopyConfigurations.keySet())
                .stream()
                .map(existingSnapshotCopyConfigurations::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, SnapshotCopyConfiguration> configurationsToUpdate = new HashMap<>();
        SetUtils.intersection(desiredSnapshotCopyConfigurations.keySet(), previousSnapshotCopyConfigurations.keySet())
                .forEach(destinationRegion -> {
                    SnapshotCopyConfiguration desired = desiredSnapshotCopyConfigurations.get(destinationRegion);
                    software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration existing = existingSnapshotCopyConfigurations.get(destinationRegion);

                    // If an existing configuration doesn't exist (manually deleted), lets re-create it!
                    if (existing == null) {
                        configurationsToCreate.add(desired);
                    // If destination kms key has changed, then we need to delete and re-create the snapshot copy configuration
                    } else if (ObjectUtils.notEqual(Optional.ofNullable(desired.getDestinationKmsKeyId()).orElse("AWS_OWNED_KMS_KEY"), existing.destinationKmsKeyId())) {
                        configurationsToDelete.add(existing);
                        configurationsToCreate.add(desired);
                    // If retention period has changed, then we update the snapshot copy configuration
                    } else if (ObjectUtils.notEqual(Optional.ofNullable(desired.getSnapshotRetentionPeriod()).orElse(-1), existing.snapshotRetentionPeriod())) {
                        configurationsToUpdate.put(existing.snapshotCopyConfigurationId(), desired);
                    }
                });

        return new SnapshotCopyConfigurationDiff(configurationsToCreate, configurationsToDelete, configurationsToUpdate);
    }

    @Value
    private static class SnapshotCopyConfigurationDiff {
        List<SnapshotCopyConfiguration> toCreate;
        List<software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration> toDelete;
        Map<String, SnapshotCopyConfiguration> toUpdate;
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
