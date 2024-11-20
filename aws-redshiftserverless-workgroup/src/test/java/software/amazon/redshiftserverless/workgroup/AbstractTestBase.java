package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListWorkgroupsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.NamespaceStatus;
import software.amazon.awssdk.services.redshiftserverless.model.WorkgroupStatus;
import software.amazon.awssdk.services.redshiftserverless.model.PerformanceTargetStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AbstractTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;
    protected static final String AWS_REGION;
    private static final String WORKGROUP_NAME;
    private static final String NAMESPACE_NAME;
    private static final String WORKGROUP_ARN;
    private static final String PERFORMANCE_TARGET_STATUS;
    private static final int BASE_CAPACITY;
    private static final int MAX_CAPACITY;
    private static final int UPDATED_BASE_CAPACITY;
    private static final int UPDATED_MAX_CAPACITY;
    private static final int PERFORMANCE_TARGET_LEVEL;
    private static final WorkgroupStatus STATUS;
    private static final NamespaceStatus NAMESPACE_STATUS;
    private static final List<String> SUBNET_IDS;
    private static final List<String> SECURITY_GROUP_IDS;
    private static final Set<ConfigParameter> CONFIG_PARAMETERS;
    private static final int DEFAULT_PORT;
    private static final PerformanceTarget PERFORMANCE_TARGET;

    private static final List<software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter> RESPONSE_CONFIG_PARAMS;

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
        AWS_REGION = "us-east-1";

        WORKGROUP_NAME = "DUMMY_WORKGROUP";
        NAMESPACE_NAME = "DUMMY_NAMESPACE";
        NAMESPACE_STATUS = NamespaceStatus.AVAILABLE;
        WORKGROUP_ARN = "DUMMY_WORKGROUP_ARN";
        BASE_CAPACITY = 0;
        UPDATED_BASE_CAPACITY = 0;
        MAX_CAPACITY = 512;
        UPDATED_MAX_CAPACITY = 1024;
        STATUS = WorkgroupStatus.AVAILABLE;
        SUBNET_IDS = Collections.emptyList();
        SECURITY_GROUP_IDS = Collections.emptyList();
        CONFIG_PARAMETERS = Collections.emptySet();
        RESPONSE_CONFIG_PARAMS = Collections.emptyList();
        DEFAULT_PORT = 5439;
        PERFORMANCE_TARGET_LEVEL = 1;
        PERFORMANCE_TARGET_STATUS = PerformanceTargetStatus.ENABLED.toString();
        PERFORMANCE_TARGET = PerformanceTarget.builder()
                .status(PERFORMANCE_TARGET_STATUS)
                .level(PERFORMANCE_TARGET_LEVEL)
                .build();
    }

    static ProxyClient<RedshiftServerlessClient> MOCK_PROXY(
            final AmazonWebServicesClientProxy proxy,
            final RedshiftServerlessClient sdkClient) {
        return new ProxyClient<RedshiftServerlessClient>() {
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
            public RedshiftServerlessClient client() {
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
                .namespaceName(NAMESPACE_NAME)
                .baseCapacity(BASE_CAPACITY)
                .maxCapacity(MAX_CAPACITY)
                .securityGroupIds(SECURITY_GROUP_IDS)
                .subnetIds(SUBNET_IDS)
                .configParameters(CONFIG_PARAMETERS)
                .port(DEFAULT_PORT)
                .publiclyAccessible(true)
                .tags(List.of())
                .pricePerformanceTarget(PERFORMANCE_TARGET)
                .workgroup(Workgroup.builder()
                        .workgroupName(WORKGROUP_NAME)
                        .namespaceName(NAMESPACE_NAME)
                        .workgroupArn(WORKGROUP_ARN)
                        .status(STATUS.toString())
                        .baseCapacity(BASE_CAPACITY)
                        .maxCapacity(MAX_CAPACITY)
                        .securityGroupIds(SECURITY_GROUP_IDS)
                        .subnetIds(SUBNET_IDS)
                        .configParameters(CONFIG_PARAMETERS)
                        .creationDate("null")
                        .pricePerformanceTarget(PERFORMANCE_TARGET)
                        .publiclyAccessible(true)
                        .endpoint(Endpoint.builder().port(DEFAULT_PORT).vpcEndpoints(Collections.emptyList()).build())
                        .build())
                .build();
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

    public static GetWorkgroupResponse getReadResponseSdk() {
        return GetWorkgroupResponse.builder()
                .workgroup(software.amazon.awssdk.services.redshiftserverless.model.Workgroup.builder()
                        .workgroupName(WORKGROUP_NAME)
                        .namespaceName(NAMESPACE_NAME)
                        .workgroupArn(WORKGROUP_ARN)
                        .status(STATUS)
                        .baseCapacity(BASE_CAPACITY)
                        .maxCapacity(MAX_CAPACITY)
                        .securityGroupIds(SECURITY_GROUP_IDS)
                        .subnetIds(SUBNET_IDS)
                        .configParameters(RESPONSE_CONFIG_PARAMS)
                        .publiclyAccessible(true)
                        .pricePerformanceTarget(software.amazon.awssdk.services.redshiftserverless.model.PerformanceTarget.builder()
                                .status(PERFORMANCE_TARGET_STATUS)
                                .level(PERFORMANCE_TARGET_LEVEL)
                                .build())
                        .endpoint(software.amazon.awssdk.services.redshiftserverless.model.Endpoint.builder()
                                .port(DEFAULT_PORT)
                                .vpcEndpoints(Collections.emptyList())
                                .build())
                        .build())
                .build();
    }

    public static ResourceModel deleteRequestResourceModel() {
        return ResourceModel.builder().
                workgroupName(WORKGROUP_NAME).
                build();
    }

    public static DeleteWorkgroupResponse deleteResponseSdk() {
        return DeleteWorkgroupResponse.builder()
                .workgroup(software.amazon.awssdk.services.redshiftserverless.model.Workgroup.builder()
                        .workgroupName(WORKGROUP_NAME)
                        .build()).build();
    }

    public static ResourceModel listRequestResourceModel() {
        return ResourceModel.builder().build();
    }

    public static ListWorkgroupsResponse getListResponsesSdk() {
        return ListWorkgroupsResponse.builder()
                .workgroups(software.amazon.awssdk.services.redshiftserverless.model.Workgroup.builder()
                        .workgroupName(WORKGROUP_NAME)
                        .build())
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
                .maxCapacity(MAX_CAPACITY)
                .securityGroupIds(SECURITY_GROUP_IDS)
                .subnetIds(SUBNET_IDS)
                .configParameters(CONFIG_PARAMETERS)
                .pricePerformanceTarget(PERFORMANCE_TARGET)
                .publiclyAccessible(true)
                .port(DEFAULT_PORT)
                .build();

    }

    public static CreateWorkgroupResponse createResponseSdk() {
        return CreateWorkgroupResponse.builder()
                .workgroup(software.amazon.awssdk.services.redshiftserverless.model.Workgroup.builder()
                        .workgroupName(WORKGROUP_NAME)
                        .status(STATUS)
                        .build())
                .build();
    }

    public static ResourceModel updateRequestResourceModel() {
        return createRequestResourceModel();
    }

    public static UpdateWorkgroupResponse updateResponseSdk() {
        return UpdateWorkgroupResponse.builder()
                .workgroup(software.amazon.awssdk.services.redshiftserverless.model.Workgroup.builder()
                        .workgroupName(WORKGROUP_NAME)
                        .status(STATUS)
                        .build())
                .build();
    }

    public static ResourceModel updateResponseResourceModel() {
        return getReadResponseResourceModel();
    }

}
