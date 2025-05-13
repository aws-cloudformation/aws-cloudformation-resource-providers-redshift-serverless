package software.amazon.redshiftserverless.namespace;

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
import software.amazon.awssdk.services.redshift.model.GetResourcePolicyResponse;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyResponse;
import software.amazon.awssdk.services.redshift.model.ResourcePolicy;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.CreateNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListNamespacesResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;
  protected static final String NAMESPACE_ARN;
  protected static final String NAMESPACE_ID;
  protected static final String NAMESPACE_NAME;
  private static final String ADMIN_USERNAME;
  private static final String ADMIN_USERPASSWORD;
  private static final String DB_NAME;
  private static final String KMS_KEY_ID;
  private static final String DEFAULT_IAM_ROLE_ARN;
  private static final List<String> IAM_ROLES;
  private static final List<String> LOG_EXPORTS;
  private static final String STATUS;
  private static final Instant CREATION_DATE;
  private static final software.amazon.awssdk.services.redshiftserverless.model.Namespace NAMESPACE;
  private static final software.amazon.awssdk.services.redshiftserverless.model.Namespace NAMESPACE_WITH_MANAGED_ADMIN_PASSWORD;
  private static final String FINAL_SNAPSHOT_NAME;
  private static final int FINAL_SNAPSHOT_RETENTION_PERIOD;
  protected static final String AWS_REGION = "us-east-1";
  protected static final String NAMESPACE_RESOURCE_POLICY_DOCUMENT;
  protected static final String NAMESPACE_RESOURCE_POLICY_DOCUMENT_EMPTY;
  protected static final ResourcePolicy NAMESPACE_RESOURCE_POLICY;
  protected static final ResourcePolicy NAMESPACE_RESOURCE_POLICY_EMPTY;
  protected static final String SECRET_ARN;
  protected static final String SECRET_KMS_KEY_ID;
  protected static final String REDSHIFT_IDC_APPLICATION_ARN;

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    logger = new LoggerProxy();

    NAMESPACE_ARN = "DummyNamespaceArn";
    NAMESPACE_ID = "DummyNamespaceId";
    NAMESPACE_NAME = "dummyNamespacename";
    ADMIN_USERNAME = "DummyAdminUsername";
    ADMIN_USERPASSWORD = "DummyAdminUserpassword";
    DB_NAME = "DummyDBName";
    KMS_KEY_ID = "DummyKmsKeyId";
    DEFAULT_IAM_ROLE_ARN = "DummyDefaultIAMRoleArn";
    IAM_ROLES = Arrays.asList("arn:aws-abc:iam::123456789012:role/12345678-newrole");
    LOG_EXPORTS = Arrays.asList("DummyLogExports");
    STATUS = "available";
    CREATION_DATE = Instant.parse("9999-01-01T00:00:00Z");
    FINAL_SNAPSHOT_NAME = "testFinalSnapshot";
    FINAL_SNAPSHOT_RETENTION_PERIOD = 1;
    NAMESPACE_RESOURCE_POLICY_DOCUMENT = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Resource\": \"*\",\"Action\":\"test:test\"}]}";
    NAMESPACE_RESOURCE_POLICY_DOCUMENT_EMPTY = "{}";
    SECRET_ARN = "DummyClusterSecretArn";
    SECRET_KMS_KEY_ID = "DummySecretKmsKeyId";
    REDSHIFT_IDC_APPLICATION_ARN = "DummyRedshiftIdcApplicationArn";

    NAMESPACE = software.amazon.awssdk.services.redshiftserverless.model.Namespace.builder()
            .namespaceName(NAMESPACE_NAME)
            .namespaceArn("DummyNamespaceArn")
            .namespaceId(NAMESPACE_ID)
            .adminUsername(ADMIN_USERNAME)
            .dbName(DB_NAME)
            .kmsKeyId(KMS_KEY_ID)
            .defaultIamRoleArn(DEFAULT_IAM_ROLE_ARN)
            .iamRoles(IAM_ROLES)
            .logExportsWithStrings(LOG_EXPORTS)
            .status(STATUS)
            .creationDate(CREATION_DATE)
            .build();

    NAMESPACE_WITH_MANAGED_ADMIN_PASSWORD = NAMESPACE.toBuilder()
                                        .adminPasswordSecretArn(SECRET_ARN)
                                        .adminPasswordSecretKmsKeyId(SECRET_KMS_KEY_ID)
                                        .build();

    NAMESPACE_RESOURCE_POLICY = ResourcePolicy.builder()
            .resourceArn(NAMESPACE.namespaceArn())
            .policy(NAMESPACE_RESOURCE_POLICY_DOCUMENT)
            .build();

    NAMESPACE_RESOURCE_POLICY_EMPTY = ResourcePolicy.builder()
            .resourceArn(NAMESPACE.namespaceArn())
            .policy(null)
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
              .namespaceName(NAMESPACE_NAME)
              .adminUsername(ADMIN_USERNAME)
              .adminUserPassword(ADMIN_USERPASSWORD)
              .dbName(DB_NAME)
              .kmsKeyId(KMS_KEY_ID)
              .defaultIamRoleArn(DEFAULT_IAM_ROLE_ARN)
              .iamRoles(IAM_ROLES)
              .logExports(LOG_EXPORTS)
              .tags(new ArrayList<software.amazon.redshiftserverless.namespace.Tag>())
              .redshiftIdcApplicationArn(REDSHIFT_IDC_APPLICATION_ARN)
              .build();
  }

  public static ResourceModel getCreateRequestResourceModelWithManagedAdminPassword() {
      return getCreateRequestResourceModel().toBuilder()
              .adminUserPassword(null)
              .manageAdminPassword(true)
              .adminPasswordSecretKmsKeyId(SECRET_KMS_KEY_ID)
              .build();
  }

  public static ResourceModel getCreateResponseResourceModel() {
    return ResourceModel.builder()
            .adminUsername(ADMIN_USERNAME)
            .dbName(DB_NAME)
            .defaultIamRoleArn(DEFAULT_IAM_ROLE_ARN)
            .iamRoles(IAM_ROLES)
            .kmsKeyId(KMS_KEY_ID)
            .logExports(LOG_EXPORTS)
            .namespaceName(NAMESPACE_NAME)
            .namespace(translateToModelNamespace(NAMESPACE))
            .tags(Collections.emptyList())
            .snapshotCopyConfigurations(Collections.emptyList())
            .build();
  }

  public static ResourceModel getCreateResponseResourceModelWithManagedAdminPassword() {
    return getCreateResponseResourceModel().toBuilder()
            .adminPasswordSecretKmsKeyId(SECRET_KMS_KEY_ID)
            .manageAdminPassword(true)
            .namespace(translateToModelNamespace(NAMESPACE_WITH_MANAGED_ADMIN_PASSWORD))
            .build();
  }


  private static Namespace translateToModelNamespace(
          software.amazon.awssdk.services.redshiftserverless.model.Namespace namespace) {
    return Namespace.builder()
            .namespaceArn(namespace.namespaceArn())
            .namespaceId(namespace.namespaceId())
            .namespaceName(namespace.namespaceName())
            .adminUsername(namespace.adminUsername())
            .dbName(namespace.dbName())
            .kmsKeyId(namespace.kmsKeyId())
            .defaultIamRoleArn(namespace.defaultIamRoleArn())
            .iamRoles(namespace.iamRoles())
            .logExports(namespace.logExportsAsStrings())
            .status(namespace.statusAsString())
            .creationDate(namespace.creationDate().toString())
            .adminPasswordSecretArn(namespace.adminPasswordSecretArn())
            .adminPasswordSecretKmsKeyId(namespace.adminPasswordSecretKmsKeyId())
            .build();
  }

  public static CreateNamespaceResponse getCreateResponseSdk() {
    return CreateNamespaceResponse.builder()
            .namespace(NAMESPACE)
            .build();
  }

  public static CreateNamespaceResponse getCreateResponseSdkForManagedAdminPasswords() {
    return CreateNamespaceResponse.builder()
            .namespace(NAMESPACE_WITH_MANAGED_ADMIN_PASSWORD)
            .build();
  }

  public static GetNamespaceResponse getNamespaceResponseSdk() {
    return GetNamespaceResponse.builder()
            .namespace(NAMESPACE)
            .build();
  }

  public static GetNamespaceResponse getNamespaceResponseSdkForManagedAdminPasswords() {
    return GetNamespaceResponse.builder()
            .namespace(NAMESPACE_WITH_MANAGED_ADMIN_PASSWORD)
            .build();
  }

  public static ResourceModel getDeleteRequestResourceModel() {
    return ResourceModel.builder()
            .namespaceName(NAMESPACE_NAME)
            .finalSnapshotName(FINAL_SNAPSHOT_NAME)
            .finalSnapshotRetentionPeriod(FINAL_SNAPSHOT_RETENTION_PERIOD)
            .build();
  }

  public static DeleteNamespaceResponse getDeleteResponseSdk() {
    return DeleteNamespaceResponse.builder().build();
  }

  public static ResourceModel getListRequestResourceModel() {
    return ResourceModel.builder().build();
  }

  public static List<ResourceModel> getListResponsesResourceModel() {
    return Collections.singletonList(ResourceModel.builder()
            .namespaceName(NAMESPACE_NAME)
            .build());
  }

  public static ListNamespacesResponse getListResponsesSdk() {
    return ListNamespacesResponse.builder()
            .namespaces(NAMESPACE)
            .build();
  }

  public static ResourceModel getNamespaceRequestResourceModel() {
    return ResourceModel.builder()
            .namespaceName(NAMESPACE_NAME)
            .build();
  }

  public static ResourceModel getNamespaceResponseResourceModel() {
    return ResourceModel.builder()
            .adminUsername(ADMIN_USERNAME)
            .dbName(DB_NAME)
            .defaultIamRoleArn(DEFAULT_IAM_ROLE_ARN)
            .iamRoles(IAM_ROLES)
            .kmsKeyId(KMS_KEY_ID)
            .logExports(LOG_EXPORTS)
            .tags(Collections.emptyList())
            .namespaceName(NAMESPACE_NAME)
            .namespace(translateToModelNamespace(NAMESPACE))
            .snapshotCopyConfigurations(Collections.emptyList())
            .build();
  }

  public static ResourceModel getUpdateRequestResourceModel() {
    return  ResourceModel.builder()
            .namespaceName(NAMESPACE_NAME)
            .adminUsername(ADMIN_USERNAME)
            .adminUserPassword(ADMIN_USERPASSWORD)
            .dbName(DB_NAME)
            .kmsKeyId(KMS_KEY_ID)
            .defaultIamRoleArn(DEFAULT_IAM_ROLE_ARN)
            .iamRoles(IAM_ROLES)
            .logExports(LOG_EXPORTS)
            .tags(new ArrayList<>())
            .build();
  }

  public static ResourceModel getUpdateRequestResourceModelWithManagedAdminPassword() {
    return  getUpdateRequestResourceModel().toBuilder()
            .adminUserPassword(null)
            .manageAdminPassword(true)
            .adminPasswordSecretKmsKeyId(SECRET_KMS_KEY_ID)
            .build();
  }

  public static ResourceModel getUpdateResponseResourceModel() {
    return getCreateResponseResourceModel();
  }

  public static ResourceModel getUpdateResponseResourceModelWithManagedAdminPassword() {
    return getCreateResponseResourceModelWithManagedAdminPassword();
  }

  public static UpdateNamespaceResponse getUpdateResponseSdk() {
    return UpdateNamespaceResponse.builder()
            .namespace(NAMESPACE)
            .build();
  }

  public static UpdateNamespaceResponse getUpdateResponseSdkForManagedAdminPasswords() {
    return UpdateNamespaceResponse.builder()
            .namespace(NAMESPACE_WITH_MANAGED_ADMIN_PASSWORD)
            .build();
  }

  public static PutResourcePolicyResponse putResourcePolicyResponseSdk() {
    return PutResourcePolicyResponse.builder()
            .resourcePolicy(NAMESPACE_RESOURCE_POLICY)
            .build();
  }

  public static GetResourcePolicyResponse getResourcePolicyResponseSdk() {
    return GetResourcePolicyResponse.builder()
            .resourcePolicy(NAMESPACE_RESOURCE_POLICY)
            .build();
  }

  public static GetResourcePolicyResponse getEmptyResourcePolicyResponseSdk() {
    return GetResourcePolicyResponse.builder()
            .resourcePolicy(NAMESPACE_RESOURCE_POLICY_EMPTY)
            .build();
  }

  public static ListSnapshotCopyConfigurationsResponse getSnapshotCopyConfigurationsResponseSdk() {
    return ListSnapshotCopyConfigurationsResponse.builder()
            .snapshotCopyConfigurations(Collections.emptyList())
            .build();
  }
}
