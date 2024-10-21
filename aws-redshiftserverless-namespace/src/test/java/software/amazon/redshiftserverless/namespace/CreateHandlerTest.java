package software.amazon.redshiftserverless.namespace;

import java.time.Duration;
import java.util.Collections;

import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.GetResourcePolicyRequest;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.CreateNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotCopyConfigurationResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

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
        final CreateHandler handler = new CreateHandler();

        final ResourceModel requestResourceModel = getCreateRequestResourceModel();
        final ResourceModel responseResourceModel = getCreateResponseResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(requestResourceModel)
            .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().createNamespace(any(CreateNamespaceRequest.class))).thenReturn(getCreateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

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
    public void testPutNamespaceResourcePolicy() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel requestResourceModel = getCreateRequestResourceModel();
        final ResourceModel responseResourceModel = getCreateResponseResourceModel();

        requestResourceModel.setNamespaceResourcePolicy(Translator.convertStringToJson(NAMESPACE_RESOURCE_POLICY_DOCUMENT, logger));
        responseResourceModel.setNamespaceResourcePolicy(Translator.convertStringToJson(NAMESPACE_RESOURCE_POLICY_DOCUMENT, logger));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().createNamespace(any(CreateNamespaceRequest.class))).thenReturn(getCreateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(redshiftProxyClient.client().putResourcePolicy(any(PutResourcePolicyRequest.class))).thenReturn(putResourcePolicyResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getResourcePolicyResponseSdk());

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
    public void testCreateNamespace_ManagedAdminPassword() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel requestResourceModel = getCreateRequestResourceModelWithManagedAdminPassword();

        final ResourceModel responseResourceModel = getCreateResponseResourceModelWithManagedAdminPassword();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(requestResourceModel)
            .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().createNamespace(any(CreateNamespaceRequest.class))).thenReturn(getCreateResponseSdkForManagedAdminPasswords());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdkForManagedAdminPasswords());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);

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
    public void testCreateNamespace_SnapshotCopyConfiguration() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel requestResourceModel = getCreateRequestResourceModel().toBuilder()
                .snapshotCopyConfigurations(Collections.singletonList(software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder().destinationRegion("us-west-2").build()))
                .build();
        final ResourceModel responseResourceModel = getCreateResponseResourceModel().toBuilder()
                .snapshotCopyConfigurations(Collections.singletonList(software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder()
                        .destinationRegion("us-west-2")
                        .destinationKmsKeyId("AWS_OWNED_KMS_KEY")
                        .snapshotRetentionPeriod(-1)
                        .build()))
                .build();

        final SnapshotCopyConfiguration snapshotCopyConfiguration = SnapshotCopyConfiguration.builder()
                .snapshotCopyConfigurationId("snap-id-1234")
                .destinationRegion("us-west-2")
                .namespaceName(requestResourceModel.getNamespaceName())
                .destinationKmsKeyId("AWS_OWNED_KMS_KEY")
                .snapshotRetentionPeriod(-1)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().createNamespace(any(CreateNamespaceRequest.class))).thenReturn(getCreateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(proxyClient.client().createSnapshotCopyConfiguration(any(CreateSnapshotCopyConfigurationRequest.class)))
                .thenReturn(CreateSnapshotCopyConfigurationResponse.builder()
                        .snapshotCopyConfiguration(snapshotCopyConfiguration)
                        .build());
        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class)))
                .thenReturn(ListSnapshotCopyConfigurationsResponse.builder()
                        .snapshotCopyConfigurations(Collections.singletonList(snapshotCopyConfiguration))
                        .build());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);

        verify(proxyClient.client(), times(1)).createSnapshotCopyConfiguration(any(CreateSnapshotCopyConfigurationRequest.class));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

    }
}
