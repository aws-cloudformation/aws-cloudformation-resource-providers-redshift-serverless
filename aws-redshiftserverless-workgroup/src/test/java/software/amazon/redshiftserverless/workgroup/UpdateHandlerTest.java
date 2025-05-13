package software.amazon.redshiftserverless.workgroup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromRecoveryPointRequest;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromRecoveryPointResponse;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
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
public class UpdateHandlerTest extends AbstractTestBase {

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
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel responseResourceModel = updateResponseResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createRequestResourceModel())
                .desiredResourceState(updateRequestResourceModel())
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().updateWorkgroup(any(UpdateWorkgroupRequest.class))).thenReturn(updateResponseSdk());
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
    public void handleRequest_NoReadTagsPermissions() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel responseResourceModel = updateResponseResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createRequestResourceModel())
                .desiredResourceState(updateRequestResourceModel())
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenThrow(ValidationException.builder().build());
        when(proxyClient.client().getWorkgroup(any(GetWorkgroupRequest.class))).thenReturn(getReadResponseSdk());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void handleRequest_RestoreFromSnapshot_Success() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel responseResourceModel = updateResponseResourceModel();

        final ResourceModel requestModel = updateRequestResourceModel();
        requestModel.setSnapshotArn("arn:aws:redshift-serverless:us-west-2:123456789012:snapshot/example-snapshot");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createRequestResourceModel())
                .desiredResourceState(requestModel)
                .build();

        when(proxyClient.client().updateWorkgroup(any(UpdateWorkgroupRequest.class))).thenReturn(updateResponseSdk());
        when(proxyClient.client().getWorkgroup(any(GetWorkgroupRequest.class))).thenReturn(getReadResponseSdk());
        when(proxyClient.client().restoreFromSnapshot(any(RestoreFromSnapshotRequest.class))).thenReturn(
                RestoreFromSnapshotResponse.builder().snapshotName("snapshotName").build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).restoreFromSnapshot(any(RestoreFromSnapshotRequest.class));
    }

    @Test
    public void handleRequest_RestoreFromSnapshot_Failed() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel requestModel = updateRequestResourceModel();
        requestModel.setSnapshotArn("arn:aws:redshift-serverless:us-west-2:123456789012:snapshot/example-snapshot");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createRequestResourceModel())
                .desiredResourceState(requestModel)
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().updateWorkgroup(any(UpdateWorkgroupRequest.class))).thenReturn(updateResponseSdk());
        when(proxyClient.client().getWorkgroup(any(GetWorkgroupRequest.class))).thenReturn(getReadResponseSdk());
        when(proxyClient.client().restoreFromSnapshot(any(RestoreFromSnapshotRequest.class))).thenThrow(ValidationException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

        verify(proxyClient.client()).restoreFromSnapshot(any(RestoreFromSnapshotRequest.class));
    }

    @Test
    public void handleRequest_RestoreFromRecoveryPoint_Success() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel requestModel = updateRequestResourceModel();
        requestModel.setRecoveryPointId("testRecoveryPointId");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createRequestResourceModel())
                .desiredResourceState(requestModel)
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().updateWorkgroup(any(UpdateWorkgroupRequest.class))).thenReturn(updateResponseSdk());
        when(proxyClient.client().getWorkgroup(any(GetWorkgroupRequest.class))).thenReturn(getReadResponseSdk());
        when(proxyClient.client().restoreFromRecoveryPoint(any(RestoreFromRecoveryPointRequest.class)))
                .thenReturn(RestoreFromRecoveryPointResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void handleRequest_RestoreFromRecoveryPoint_Failed() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel requestModel = updateRequestResourceModel();
        requestModel.setRecoveryPointId("testRecoveryPointId");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createRequestResourceModel())
                .desiredResourceState(requestModel)
                .build();

        when(proxyClient.client().updateWorkgroup(any(UpdateWorkgroupRequest.class))).thenReturn(updateResponseSdk());
        when(proxyClient.client().getWorkgroup(any(GetWorkgroupRequest.class))).thenReturn(getReadResponseSdk());
        when(proxyClient.client().restoreFromRecoveryPoint(any(RestoreFromRecoveryPointRequest.class)))
                .thenThrow(ValidationException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

        verify(proxyClient.client()).restoreFromRecoveryPoint(any(RestoreFromRecoveryPointRequest.class));
    }
}
