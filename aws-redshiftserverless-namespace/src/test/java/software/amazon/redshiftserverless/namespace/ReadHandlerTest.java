package software.amazon.redshiftserverless.namespace;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.stream.Stream;
import java.util.Collections;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.GetResourcePolicyRequest;
import software.amazon.awssdk.services.redshift.model.InvalidPolicyException;
import software.amazon.awssdk.services.redshift.model.RedshiftException;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.RedshiftServerlessException;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

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
        final ReadHandler handler = new ReadHandler();

        final ResourceModel requestResourceModel = getNamespaceRequestResourceModel();
        final ResourceModel responseResourceModel = getNamespaceResponseResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .build();

        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
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

    static Stream<Arguments> provideResourcePolicyExceptionParams() {
        return Stream.of(
                Arguments.of(InvalidPolicyException.class, false, null),
                Arguments.of(UnsupportedOperationException.class, false, null),
                Arguments.of(RedshiftException.class, false, null),
                Arguments.of(InvalidPolicyException.class, true, CfnInvalidRequestException.class),
                Arguments.of(RedshiftException.class, true, CfnGeneralServiceException.class)
        );
    }

    static Object createExceptionWithBuilder(Class<?> exceptionClass) throws Exception {
        // Dynamically invoke the builder method
        Object builderInstance = exceptionClass.getDeclaredMethod("builder").invoke(null);
        // Build and return the exception object
        Method buildMethod = builderInstance.getClass().getMethod("build");
        buildMethod.setAccessible(true);
        return buildMethod.invoke(builderInstance);
    }

    @ParameterizedTest
    @MethodSource("provideResourcePolicyExceptionParams")
    public void catchResourcePolicyExceptionAsExpected(
            Class<Exception> exceptionClass,
            boolean containsResourcePolicy,
            Class<? extends Exception> expectedException
    ) throws Exception {
        final ReadHandler handler = new ReadHandler();

        ResourceModel requestResourceModel = getNamespaceRequestResourceModel();
        if (containsResourcePolicy) {
            requestResourceModel.setNamespaceResourcePolicy(
                    Translator.convertStringToJson(NAMESPACE_RESOURCE_POLICY_DOCUMENT, logger)
            );
        }
        final ResourceModel responseResourceModel = getNamespaceResponseResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(requestResourceModel)
            .build();

        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class)))
                .thenThrow((Throwable) createExceptionWithBuilder(exceptionClass));

        if (expectedException == null) {
            when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
            final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        } else {
            assertThrows(expectedException, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger));
        }
    }

    @Test
    public void testGetNamespaceResourcePolicy() {
        final ReadHandler handler = new ReadHandler();

        final ResourceModel requestResourceModel = getNamespaceRequestResourceModel();
        final ResourceModel responseResourceModel = getNamespaceResponseResourceModel();

        requestResourceModel.setNamespaceResourcePolicy(Translator.convertStringToJson(NAMESPACE_RESOURCE_POLICY_DOCUMENT, logger));
        responseResourceModel.setNamespaceResourcePolicy(Translator.convertStringToJson(NAMESPACE_RESOURCE_POLICY_DOCUMENT, logger));

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .build();

        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class))).thenReturn(getSnapshotCopyConfigurationsResponseSdk());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
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
    public void handleRequest_ListSnapshotCopyConfigurations() {
        final ReadHandler handler = new ReadHandler();

        final ResourceModel requestResourceModel = getNamespaceRequestResourceModel();
        final ResourceModel responseResourceModel = getNamespaceResponseResourceModel().toBuilder()
                .snapshotCopyConfigurations(Collections.singletonList(software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder()
                        .destinationRegion("us-west-2")
                        .destinationKmsKeyId("AWS_OWNED_KMS_KEY")
                        .snapshotRetentionPeriod(-1)
                        .build()))
                .build();


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .build();

        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class)))
                .thenReturn(ListSnapshotCopyConfigurationsResponse.builder()
                        .snapshotCopyConfigurations(Collections.singletonList(SnapshotCopyConfiguration.builder()
                                .snapshotCopyConfigurationId("snap-id-1234")
                                .destinationRegion("us-west-2")
                                .namespaceName(requestResourceModel.getNamespaceName())
                                .destinationKmsKeyId("AWS_OWNED_KMS_KEY")
                                .snapshotRetentionPeriod(-1)
                                .build()))
                        .build());
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);

        verify(proxyClient.client(), times(1)).listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    static Stream<Arguments> provideListSnapshotCopyConfigurationsExceptionParams() {
        return Stream.of(
                Arguments.of(ValidationException.class, false, null),
                Arguments.of(ResourceNotFoundException.class, false, null),
                Arguments.of(ResourceNotFoundException.class, true, HandlerErrorCode.NotFound),
                Arguments.of(RedshiftServerlessException.class, true, HandlerErrorCode.GeneralServiceException)
        );
    }

    @ParameterizedTest
    @MethodSource("provideListSnapshotCopyConfigurationsExceptionParams")
    public void catchListSnapshotCopyConfigurationsExceptionAsExpected(
            Class<Exception> exceptionClass,
            boolean containsSnapshotCopyConfigurations,
            HandlerErrorCode expectedErrorCode
    ) throws Exception {
        final ReadHandler handler = new ReadHandler();

        ResourceModel requestResourceModel = getNamespaceRequestResourceModel();
        if (containsSnapshotCopyConfigurations) {
            requestResourceModel.setSnapshotCopyConfigurations(Collections.singletonList(
                    software.amazon.redshiftserverless.namespace.SnapshotCopyConfiguration.builder()
                            .destinationRegion("us-west-2")
                            .build()));
        }
        final ResourceModel responseResourceModel = getNamespaceResponseResourceModel().toBuilder()
                .snapshotCopyConfigurations(null)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .build();

        when(proxyClient.client().listSnapshotCopyConfigurations(any(ListSnapshotCopyConfigurationsRequest.class)))
                .thenThrow((Throwable) createExceptionWithBuilder(exceptionClass));
        when(proxyClient.client().getNamespace(any(GetNamespaceRequest.class))).thenReturn(getNamespaceResponseSdk());
        when(redshiftProxyClient.client().getResourcePolicy(any(GetResourcePolicyRequest.class))).thenReturn(getEmptyResourcePolicyResponseSdk());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger);
        assertThat(response).isNotNull();
        if (expectedErrorCode == null) {
            assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        } else {
            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            assertThat(response.getErrorCode()).isEqualTo(expectedErrorCode);
        }
    }
}
