package software.amazon.redshiftserverless.snapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.redshift.model.ResourcePolicy;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.NamespaceStatus;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;
  protected static final String SNAPSHOT_NAME;
  protected static final String SNAPSHOT_ARN;
  protected static final String NAMESPACE_NAME;
  protected static final String NAMESPACE_ARN;
  protected static final String OWNER_ACCOUNT;
  private static final String ADMIN_USERNAME;
  private static final String KMS_KEY_ID;
  private static final String STATUS;
  private static final Instant CREATION_DATE;
  private static final software.amazon.awssdk.services.redshiftserverless.model.Snapshot SNAPSHOT;
  private static final int SNAPSHOT_RETENTION_PERIOD;
  private static final NamespaceStatus NAMESPACE_STATUS;
  protected static final String AWS_REGION = "us-east-1";

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    logger = new LoggerProxy();

    SNAPSHOT_NAME = "DummySnapshotName";
    SNAPSHOT_ARN = "DummySnapshotArn";
    NAMESPACE_NAME = "dummyNamespacename";
    NAMESPACE_ARN = "DummyNamespaceArn";
    ADMIN_USERNAME = "DummyAdminUsername";
    OWNER_ACCOUNT = "DummyOwnerAccount";
    KMS_KEY_ID = "DummyKmsKeyId";
    STATUS = "AVAILABLE";
    CREATION_DATE = Instant.parse("9999-01-01T00:00:00Z");
    SNAPSHOT_RETENTION_PERIOD = 1;
    NAMESPACE_STATUS = NamespaceStatus.AVAILABLE;

    SNAPSHOT = software.amazon.awssdk.services.redshiftserverless.model.Snapshot.builder()
            .snapshotName(SNAPSHOT_NAME)
            .snapshotArn(SNAPSHOT_ARN)
            .namespaceName(NAMESPACE_NAME)
            .namespaceArn(NAMESPACE_ARN)
            .adminUsername(ADMIN_USERNAME)
            .ownerAccount(OWNER_ACCOUNT)
            .kmsKeyId(KMS_KEY_ID)
            .status(STATUS)
            .snapshotCreateTime(CREATION_DATE)
            .snapshotRetentionPeriod(SNAPSHOT_RETENTION_PERIOD)
            .build();
  }
  static <T> ProxyClient<T> MOCK_PROXY(
          final AmazonWebServicesClientProxy proxy,
          final T sdkClient) {
    return new ProxyClient<T>() {
      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
      injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
        return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
      CompletableFuture<ResponseT>
      injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
      IterableT
      injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
      injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
      injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public T client() {
        return sdkClient;
      }
    };
  }

  public static ResourceModel getCreateRequestResourceModel() {
    return ResourceModel.builder()
            .snapshotName(SNAPSHOT_NAME)
            .snapshotArn(SNAPSHOT_ARN)
            .namespaceName(NAMESPACE_NAME)
            .namespaceArn(NAMESPACE_ARN)
            .adminUsername(ADMIN_USERNAME)
            .ownerAccount(OWNER_ACCOUNT)
            .kmsKeyId(KMS_KEY_ID)
            .snapshotCreateTime(CREATION_DATE.toString())
            .retentionPeriod(SNAPSHOT_RETENTION_PERIOD)
            .tags(new ArrayList<software.amazon.redshiftserverless.snapshot.Tag>())
            .build();
  }

  public static ResourceModel getCreateResponseResourceModel() {
    return ResourceModel.builder()
            .snapshotName(SNAPSHOT_NAME)
            .snapshotArn(SNAPSHOT_ARN)
            .namespaceName(NAMESPACE_NAME)
            .namespaceArn(NAMESPACE_ARN)
            .adminUsername(ADMIN_USERNAME)
            .ownerAccount(OWNER_ACCOUNT)
            .kmsKeyId(KMS_KEY_ID)
            .snapshotCreateTime(CREATION_DATE.toString())
            .retentionPeriod(SNAPSHOT_RETENTION_PERIOD)
            .snapshot(translateToModelSnapshot(SNAPSHOT))
            .build();
  }


  private static Snapshot translateToModelSnapshot(
          software.amazon.awssdk.services.redshiftserverless.model.Snapshot snapshot) {
    return Snapshot.builder()
            .snapshotName(snapshot.snapshotName())
            .snapshotArn(snapshot.snapshotArn())
            .namespaceName(snapshot.namespaceName())
            .namespaceArn(snapshot.namespaceArn())
            .adminUsername(snapshot.adminUsername())
            .ownerAccount(snapshot.ownerAccount())
            .kmsKeyId(snapshot.kmsKeyId())
            .status(snapshot.statusAsString())
            .snapshotCreateTime(snapshot.snapshotCreateTime().toString())
            .retentionPeriod(snapshot.snapshotRetentionPeriod())
            .build();
  }

  public static CreateSnapshotResponse getCreateResponseSdk() {
    return CreateSnapshotResponse.builder()
            .snapshot(SNAPSHOT)
            .build();
  }

  public static GetSnapshotResponse getSnapshotResponseSdk() {
    return GetSnapshotResponse.builder()
            .snapshot(SNAPSHOT)
            .build();
  }

  public static ResourceModel getSnapshotRequestResourceModel() {
    return ResourceModel.builder()
            .snapshotName(SNAPSHOT_NAME)
            .build();
  }

  public static ResourceModel getSnapshotResponseResourceModel() {
    return ResourceModel.builder()
            .snapshotName(SNAPSHOT_NAME)
            .snapshotArn(SNAPSHOT_ARN)
            .namespaceName(NAMESPACE_NAME)
            .namespaceArn(NAMESPACE_ARN)
            .adminUsername(ADMIN_USERNAME)
            .ownerAccount(OWNER_ACCOUNT)
            .kmsKeyId(KMS_KEY_ID)
            .snapshotCreateTime(CREATION_DATE.toString())
            .retentionPeriod(SNAPSHOT_RETENTION_PERIOD)
            .snapshot(translateToModelSnapshot(SNAPSHOT))
            .build();
  }

  public static ResourceModel getDeleteRequestResourceModel() {
    return ResourceModel.builder()
            .snapshotName(SNAPSHOT_NAME)
            .build();
  }

  public static DeleteSnapshotResponse getDeleteResponseSdk() {
    return DeleteSnapshotResponse.builder().build();
  }

  public static GetNamespaceResponse getNamespaceResponseSdk() {
    return GetNamespaceResponse.builder()
            .namespace(
                    software.amazon.awssdk.services.redshiftserverless.model.Namespace.builder()
                            .status(NAMESPACE_STATUS)
                            .namespaceName(NAMESPACE_NAME)
                            .build())
            .build();
  }

  public static ResourceModel getListRequestResourceModel() {
    return ResourceModel.builder().build();
  }

  public static List<ResourceModel> getListResponsesResourceModel() {
    return Collections.singletonList(ResourceModel.builder()
            .snapshotName(SNAPSHOT_NAME)
            .build());
  }

  public static ListSnapshotsResponse getListResponsesSdk() {
    return ListSnapshotsResponse.builder()
            .snapshots(SNAPSHOT)
            .build();
  }

  public static ResourceModel getUpdateRequestResourceModel() {
    return  ResourceModel.builder()
            .snapshotName(SNAPSHOT_NAME)
            .retentionPeriod(SNAPSHOT_RETENTION_PERIOD)
            .build();
  }

  public static ResourceModel getUpdateResponseResourceModel() {
    return getCreateResponseResourceModel();
  }

  public static UpdateSnapshotResponse getUpdateResponseSdk() {
    return UpdateSnapshotResponse.builder()
            .snapshot(SNAPSHOT)
            .build();
  }
}
