package software.amazon.redshiftserverless.workgroup;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.*;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.Workgroup;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.util.List;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;

  private static final String WORKGROUP_NAME;
  private static final String NAMESPACE_NAME;
  private static final int BASE_CAPACITY;
  private static final int UPDATED_BASE_CAPACITY;
  private static final String STATUS;
  private static final List<String> SUBNET_IDS;
  private static final List<String> SECURITY_GROUP_IDS;
  private static final List<ConfigParameter> CONFIG_PARAMETERS;
  private static final Collection<software.amazon.awssdk.services.redshiftarcadiacoral.model.ConfigParameter> RESPONSE_CONFIG_PARAMS;

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    logger = new LoggerProxy();

    WORKGROUP_NAME = "DUMMY_WORKGROUP";
    NAMESPACE_NAME = "DUMMY_NAMESPACE";
    BASE_CAPACITY = 0;
    UPDATED_BASE_CAPACITY = 0;
    STATUS = "available";
    SUBNET_IDS = Collections.emptyList();
    SECURITY_GROUP_IDS = Collections.emptyList();
    CONFIG_PARAMETERS = Collections.emptyList();
    RESPONSE_CONFIG_PARAMS = Collections.emptyList();

  }
  static ProxyClient<RedshiftArcadiaCoralClient> MOCK_PROXY(
    final AmazonWebServicesClientProxy proxy,
    final RedshiftArcadiaCoralClient sdkClient) {
    return new ProxyClient<RedshiftArcadiaCoralClient>() {
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
      public RedshiftArcadiaCoralClient client() {
        return sdkClient;
      }
    };
  }

  public static ResourceModel getReadRequestResourceModel() {
    return ResourceModel.builder().
            workgroupName(WORKGROUP_NAME).
            build();
  }

  public static ResourceModel getReadResponseResourceModel() {
    return ResourceModel.builder()
            .workgroupName(WORKGROUP_NAME)
            .workgroup(software.amazon.redshiftserverless.workgroup.Workgroup.builder()
                    .workgroupName(WORKGROUP_NAME)
                    .namespaceName(NAMESPACE_NAME)
                    .status(STATUS)
                    .baseCapacity(BASE_CAPACITY)
                    .securityGroupIds(SECURITY_GROUP_IDS)
                    .subnetIds(SUBNET_IDS)
                    .configParameters(CONFIG_PARAMETERS)
                    .build())
            .build();
  }

  public static GetWorkgroupResponse getReadResponseSdk() {
    return GetWorkgroupResponse.builder()
            .workgroup(Workgroup.builder()
                    .workgroupName(WORKGROUP_NAME)
                    .namespaceName(NAMESPACE_NAME)
                    .status(STATUS)
                    .baseCapacity(BASE_CAPACITY)
                    .securityGroupIds(SECURITY_GROUP_IDS)
                    .subnetIds(SUBNET_IDS)
                    .configParameters(RESPONSE_CONFIG_PARAMS)
                    .build())
            .build();
  }

  public static ResourceModel deleteRequestResourceModel() {
    return ResourceModel.builder().
            workgroupName(WORKGROUP_NAME).
            build();
  }

  public static DeleteWorkgroupResponse deleteResponseSdk() {
    return DeleteWorkgroupResponse.builder().build();
  }

  public static ResourceModel listRequestResourceModel() {
    return ResourceModel.builder().build();
  }

  public static ListWorkgroupsResponse getListResponsesSdk() {
    return ListWorkgroupsResponse.builder()
            .workgroups(Workgroup.builder().workgroupName(WORKGROUP_NAME).build())
            .build();
  }

  public static List<ResourceModel> getListResponsesResourceModel() {
    return Collections.singletonList(ResourceModel.builder()
            .workgroupName(WORKGROUP_NAME)
            .build());
  }

  public static ResourceModel createRequestResourceModel() {
    return ResourceModel.builder()
            .workgroupName(WORKGROUP_NAME)
            .namespaceName(NAMESPACE_NAME)
            .baseCapacity(BASE_CAPACITY)
            .securityGroupIds(SECURITY_GROUP_IDS)
            .subnetIds(SUBNET_IDS)
            .configParameters(CONFIG_PARAMETERS)
            .publiclyAccessible(true)
            .build();

  }

  public static CreateWorkgroupResponse createResponseSdk() {
    return CreateWorkgroupResponse.builder()
            .workgroup(Workgroup.builder()
                    .workgroupName(WORKGROUP_NAME)
                    .build())
            .build();
  }

  public static ResourceModel updateRequestResourceModel() {
    return createRequestResourceModel();
  }

  public static UpdateWorkgroupResponse updateResponseSdk() {
    return UpdateWorkgroupResponse.builder()
            .workgroup(Workgroup.builder()
                    .workgroupName(WORKGROUP_NAME)
                    .build())
            .build();
  }

  public static ResourceModel updateResponseResourceModel() {
    return getReadResponseResourceModel();
  }

}
