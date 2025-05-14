package software.amazon.redshiftserverless.namespace;

import java.time.Duration;
import java.util.Collections;
import java.util.ArrayList;

import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.*;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotCopyConfigurationResponse;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotCopyConfigurationResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotCopyConfigurationResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<RedshiftServerlessClient> proxyClient;

    @Mock
    RedshiftServerlessClient sdkClient;

    @Mock
    private ProxyClient<RedshiftClient> redshiftProxyClient;

    @Mock
    private ProxyClient<SecretsManagerClient> secretsManagerProxyClient;

    @Mock
    RedshiftClient redshiftSdkClient;


    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RedshiftServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);

        redshiftSdkClient = mock(RedshiftClient.class);
        redshiftProxyClient = MOCK_PROXY(proxy, redshiftSdkClient);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel requestResourceModel = getUpdateRequestResourceModel();
        final ResourceModel responseResourceModel = getUpdateResponseResourceModel();
        ResourceModel prevModel = ResourceModel.builder()
                .namespaceName(NAMESPACE_NAME)
                .tags(new ArrayList<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(prevModel)
            .desiredResourceState(requestResourceModel)
            .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);
        verify(proxyClient.client()).updateNamespace(any(UpdateNamespaceRequest.class));
        verify(proxyClient.client(), times(2)).listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testDeleteNamespaceResourcePolicy() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel requestResourceModel = getUpdateRequestResourceModel();
        final ResourceModel responseResourceModel = getUpdateResponseResourceModel();
        requestResourceModel.setNamespaceResourcePolicy(Translator.convertStringToJson(NAMESPACE_RESOURCE_POLICY_DOCUMENT_EMPTY, logger));
        ResourceModel prevModel = ResourceModel.builder()
                .namespaceName(NAMESPACE_NAME)
                .namespaceResourcePolicy(Translator.convertStringToJson(NAMESPACE_RESOURCE_POLICY_DOCUMENT, logger))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .previousResourceState(prevModel)
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());
        when(redshiftProxyClient.client().deleteResourcePolicy(any(DeleteResourcePolicyRequest.class))).thenReturn(null);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testModifyNamespaceResourcePolicy() {
        final UpdateHandler handler = new UpdateHandler();
        final String NEW_NAMESPACE_RESOURCE_POLICY = "{\"Version\":\"2012-10-17\"}";

        final ResourceModel requestResourceModel = getUpdateRequestResourceModel();
        final ResourceModel responseResourceModel = getUpdateResponseResourceModel();
        ResourceModel prevModel = ResourceModel.builder()
                .namespaceName(NAMESPACE_NAME)
                .namespaceResourcePolicy(Translator.convertStringToJson(NAMESPACE_RESOURCE_POLICY_DOCUMENT, logger))
                .build();

        requestResourceModel.setNamespaceResourcePolicy(Translator.convertStringToJson(NEW_NAMESPACE_RESOURCE_POLICY, logger));
        responseResourceModel.setNamespaceResourcePolicy(Translator.convertStringToJson(NEW_NAMESPACE_RESOURCE_POLICY, logger));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .previousResourceState(prevModel)
                .build();

        ResourcePolicy newResourcePolicy = ResourcePolicy.builder()
                .resourceArn("DummyNamespaceArn")
                .policy(NEW_NAMESPACE_RESOURCE_POLICY)
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().putResourcePolicy(any(PutResourcePolicyRequest.class))).thenReturn(PutResourcePolicyResponse.builder()
                .resourcePolicy(newResourcePolicy)
                .build());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(GetResourcePolicyResponse.builder()
                .resourcePolicy(newResourcePolicy)
                .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testUpdate_OptInManagedAdminPassword() {
        final UpdateHandler handler = new UpdateHandler();

        ResourceModel requestResourceModel = getUpdateRequestResourceModelWithManagedAdminPassword();
        ResourceModel responseResourceModel = getUpdateResponseResourceModelWithManagedAdminPassword();
        ResourceModel prevModel = ResourceModel.builder()
                .namespaceName(NAMESPACE_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(prevModel)
            .desiredResourceState(requestResourceModel)
            .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdkForManagedAdminPasswords());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdkForManagedAdminPasswords());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);
        verify(proxyClient.client()).updateNamespace(any(UpdateNamespaceRequest.class));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getManageAdminPassword()).isTrue();
        assertThat(response.getResourceModel().getNamespace().getAdminPasswordSecretArn()).isEqualTo(SECRET_ARN);
        assertThat(response.getResourceModel().getAdminPasswordSecretKmsKeyId()).isEqualTo(SECRET_KMS_KEY_ID);
        assertThat(response.getResourceModel().getAdminUserPassword()).isNull();
    }

    @Test
    public void testUpdate_OptOutManagedAdminPassword() {
        final UpdateHandler handler = new UpdateHandler();

        ResourceModel requestResourceModel = getUpdateRequestResourceModel();
        ResourceModel responseResourceModel = getUpdateResponseResourceModel();
        ResourceModel prevModel = ResourceModel.builder()
                .namespaceName(NAMESPACE_NAME)
                .manageAdminPassword(true)
                .adminPasswordSecretKmsKeyId(SECRET_KMS_KEY_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(prevModel)
            .desiredResourceState(requestResourceModel)
            .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);
        verify(proxyClient.client()).updateNamespace(any(UpdateNamespaceRequest.class));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getManageAdminPassword()).isNull();
        assertThat(response.getResourceModel().getNamespace().getAdminPasswordSecretArn()).isNull();
        assertThat(response.getResourceModel().getAdminPasswordSecretKmsKeyId()).isNull();
    }

    @Test
    public void testUpdate_UpdateAdminPasswordSecretKmsKeyId() {
        final UpdateHandler handler = new UpdateHandler();

        ResourceModel requestResourceModel = getUpdateRequestResourceModelWithManagedAdminPassword();

        ResourceModel responseResourceModel = getUpdateResponseResourceModelWithManagedAdminPassword();

        String oldSecretKmsKeyId = "old" + SECRET_KMS_KEY_ID;
        ResourceModel prevModel = ResourceModel.builder()
                .namespaceName(NAMESPACE_NAME)
                .manageAdminPassword(true)
                .adminPasswordSecretKmsKeyId(oldSecretKmsKeyId)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(prevModel)
            .desiredResourceState(requestResourceModel)
            .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdkForManagedAdminPasswords());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdkForManagedAdminPasswords());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);
        verify(proxyClient.client()).updateNamespace(any(UpdateNamespaceRequest.class));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getManageAdminPassword()).isTrue();
        assertThat(response.getResourceModel().getNamespace().getAdminPasswordSecretArn()).isEqualTo(SECRET_ARN);
        assertThat(response.getResourceModel().getAdminPasswordSecretKmsKeyId()).isEqualTo(SECRET_KMS_KEY_ID);
        assertThat(response.getResourceModel().getAdminUserPassword()).isNull();
    }

    @Test
    public void handleRequest_SnapshotCopyConfiguration_DeleteAndReCreate() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel requestResourceModel = getUpdateRequestResourceModel().toBuilder()
                .snapshotCopyConfigurations(Collections.singletonList(software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder()
                        .destinationRegion("us-west-1")
                        .build()))
                .build();
        final ResourceModel responseResourceModel = getCreateResponseResourceModel().toBuilder()
                .snapshotCopyConfigurations(Collections.singletonList(software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder()
                        .destinationRegion("us-west-1")
                        .destinationKmsKeyId("AWS_OWNED_KMS_KEY")
                        .snapshotRetentionPeriod(-1)
                        .build()))
                .build();

        ResourceModel prevModel = ResourceModel.builder()
                .namespaceName(NAMESPACE_NAME)
                .snapshotCopyConfigurations(Collections.singletonList(software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder()
                        .destinationRegion("us-west-2")
                        .build()))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(prevModel)
                .desiredResourceState(requestResourceModel)
                .build();

        final SnapshotCopyConfiguration oldSnapshotCopyConfiguration = SnapshotCopyConfiguration.builder()
                .snapshotCopyConfigurationId("snap-id-1234")
                .destinationRegion("us-west-2")
                .namespaceName(requestResourceModel.getNamespaceName())
                .destinationKmsKeyId("AWS_OWNED_KMS_KEY")
                .snapshotRetentionPeriod(-1)
                .build();
        final SnapshotCopyConfiguration newSnapshotCopyConfiguration = SnapshotCopyConfiguration.builder()
                .snapshotCopyConfigurationId("snap-id-5678")
                .destinationRegion("us-west-1")
                .namespaceName(requestResourceModel.getNamespaceName())
                .destinationKmsKeyId("AWS_OWNED_KMS_KEY")
                .snapshotRetentionPeriod(-1)
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().createSnapshotCopyConfiguration(any(CreateSnapshotCopyConfigurationRequest.class)))
                .thenReturn(CreateSnapshotCopyConfigurationResponse.builder()
                        .snapshotCopyConfiguration(newSnapshotCopyConfiguration)
                        .build());
        when(proxyClient.client().deleteSnapshotCopyConfiguration(any(DeleteSnapshotCopyConfigurationRequest.class)))
                .thenReturn(DeleteSnapshotCopyConfigurationResponse.builder()
                        .snapshotCopyConfiguration(oldSnapshotCopyConfiguration)
                        .build());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class)))
                .thenReturn(ListSnapshotCopyConfigurationsResponse.builder()
                        .snapshotCopyConfigurations(Collections.singletonList(oldSnapshotCopyConfiguration))
                        .build())
                .thenReturn(ListSnapshotCopyConfigurationsResponse.builder()
                        .snapshotCopyConfigurations(Collections.singletonList(newSnapshotCopyConfiguration))
                        .build());
        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);
        verify(proxyClient.client()).updateNamespace(any(UpdateNamespaceRequest.class));
        verify(proxyClient.client(), times(1)).deleteSnapshotCopyConfiguration(any(DeleteSnapshotCopyConfigurationRequest.class));
        verify(proxyClient.client(), times(1)).createSnapshotCopyConfiguration(any(CreateSnapshotCopyConfigurationRequest.class));
        verify(proxyClient.client(), times(2)).listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SnapshotCopyConfiguration_Update() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel requestResourceModel = getUpdateRequestResourceModel().toBuilder()
                .snapshotCopyConfigurations(Collections.singletonList(software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder()
                        .destinationRegion("us-west-2")
                        .snapshotRetentionPeriod(2)
                        .build()))
                .build();
        final ResourceModel responseResourceModel = getCreateResponseResourceModel().toBuilder()
                .snapshotCopyConfigurations(Collections.singletonList(software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder()
                        .destinationRegion("us-west-2")
                        .destinationKmsKeyId("AWS_OWNED_KMS_KEY")
                        .snapshotRetentionPeriod(2)
                        .build()))
                .build();

        ResourceModel prevModel = ResourceModel.builder()
                .namespaceName(NAMESPACE_NAME)
                .snapshotCopyConfigurations(Collections.singletonList(software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder()
                        .destinationRegion("us-west-2")
                        .build()))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(prevModel)
                .desiredResourceState(requestResourceModel)
                .build();

        final SnapshotCopyConfiguration snapshotCopyConfiguration = SnapshotCopyConfiguration.builder()
                .snapshotCopyConfigurationId("snap-id-1234")
                .destinationRegion("us-west-2")
                .namespaceName(requestResourceModel.getNamespaceName())
                .destinationKmsKeyId("AWS_OWNED_KMS_KEY")
                .snapshotRetentionPeriod(-1)
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().updateSnapshotCopyConfiguration(any(UpdateSnapshotCopyConfigurationRequest.class)))
                .thenReturn(UpdateSnapshotCopyConfigurationResponse.builder()
                        .snapshotCopyConfiguration(snapshotCopyConfiguration.toBuilder().snapshotRetentionPeriod(2).build())
                        .build());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class)))
                .thenReturn(ListSnapshotCopyConfigurationsResponse.builder()
                        .snapshotCopyConfigurations(Collections.singletonList(snapshotCopyConfiguration))
                        .build())
                .thenReturn(ListSnapshotCopyConfigurationsResponse.builder()
                        .snapshotCopyConfigurations(Collections.singletonList(snapshotCopyConfiguration.toBuilder().snapshotRetentionPeriod(2).build()))
                        .build());
        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);
        verify(proxyClient.client()).updateNamespace(any(UpdateNamespaceRequest.class));
        verify(proxyClient.client(), times(1)).updateSnapshotCopyConfiguration(any(UpdateSnapshotCopyConfigurationRequest.class));
        verify(proxyClient.client(), times(2)).listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
