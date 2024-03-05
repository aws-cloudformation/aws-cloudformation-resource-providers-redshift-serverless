package software.amazon.redshiftserverless.namespace;

import java.time.Duration;

import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.*;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
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
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .previousResourceState(prevModel)
            .desiredResourceState(requestResourceModel)
            .build();

        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, logger);
        verify(proxyClient.client()).updateNamespace(any(UpdateNamespaceRequest.class));
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

        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());
        when(redshiftProxyClient.client().deleteResourcePolicy(any(DeleteResourcePolicyRequest.class))).thenReturn(null);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, logger);

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

        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().putResourcePolicy(any(PutResourcePolicyRequest.class))).thenReturn(PutResourcePolicyResponse.builder()
                .resourcePolicy(newResourcePolicy)
                .build());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(GetResourcePolicyResponse.builder()
                .resourcePolicy(newResourcePolicy)
                .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, logger);

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

        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdkForManagedAdminPasswords());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdkForManagedAdminPasswords());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, logger);
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

        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, logger);
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

        when(proxyClient.client().updateNamespace(any(UpdateNamespaceRequest.class))).thenReturn(getUpdateResponseSdkForManagedAdminPasswords());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdkForManagedAdminPasswords());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, logger);
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
}
