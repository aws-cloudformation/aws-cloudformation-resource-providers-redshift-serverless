package software.amazon.redshiftserverless.workgroup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    RedshiftServerlessClient sdkClient;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<RedshiftServerlessClient> proxyClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(RedshiftServerlessClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel requestResourceModel = createRequestResourceModel();
        final ResourceModel responseResourceModel = getReadResponseResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .build();

        when(proxyClient.client().createWorkgroup(any(CreateWorkgroupRequest.class))).thenReturn(createResponseSdk());
        when(proxyClient.client().getWorkgroup(any(GetWorkgroupRequest.class))).thenReturn(getReadResponseSdk());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_retryOnConflictException() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel requestResourceModel = createRequestResourceModel();
        final ResourceModel responseResourceModel = getReadResponseResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .build();

        ConflictException exception = ConflictException.builder()
                .message("There is an operation running on the existing workgroup. Try again later.")
                .build();

        /**
         * The Thread.sleep() is actually called here, making unit longer.
         * MockStatic easily will need a Java version upgrade, then the Mockito upgrade,
         * I'll leave the upgrade in another commit.
         */
        when(proxyClient.client().createWorkgroup(any(CreateWorkgroupRequest.class)))
                .thenThrow(exception)
                .thenReturn(createResponseSdk());
        when(proxyClient.client().getWorkgroup(any(GetWorkgroupRequest.class))).thenReturn(getReadResponseSdk());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_noRetryOnOtherException() throws InterruptedException {
        final CreateHandler handler = new CreateHandler();
        final ResourceModel requestResourceModel = createRequestResourceModel();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(requestResourceModel)
                .build();

        /**
         * The Thread.sleep() is actually called here, making unit longer.
         * MockStatic easily will need a Java version upgrade, then the Mockito upgrade,
         * I'll leave the upgrade in another commit.
         */
        when(proxyClient.client().createWorkgroup(any(CreateWorkgroupRequest.class)))
                .thenThrow(InternalServerException.builder()
                        .message("test")
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("test");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}
