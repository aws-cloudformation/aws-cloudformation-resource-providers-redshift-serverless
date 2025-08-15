package software.amazon.redshiftserverless.endpointaccess;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.redshiftserverless.model.CreateEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.EndpointAccess;
import software.amazon.awssdk.services.redshiftserverless.model.VpcEndpoint;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;
    protected static final String AWS_REGION;
    private static final String ENDPOINT_NAME;
    private static final String ENDPOINT_STATUS;
    private static final String WORKGROUP_NAME;
    private static final int DEFAULT_PORT;
    private static final String ADDRESS;
    private static final List<String> SUBNET_IDS;
    private static final List<software.amazon.awssdk.services.redshiftserverless.model.VpcSecurityGroupMembership> VPC_SECURITY_GROUPS;
    private static final String ENDPOINT_ARN;
    private static final String VPC_ENDPOINT_ID;
    private static final String VPC_ID;
    private static final Instant CREATION_DATE;
    private static final String OWNER_ACCOUNT;

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
        AWS_REGION = "us-east-1";
        ENDPOINT_NAME = "testendpoint";
        ENDPOINT_STATUS = "available";
        WORKGROUP_NAME = "testworkgroup";
        DEFAULT_PORT = 5439;
        ADDRESS = "xyz";
        SUBNET_IDS = Collections.emptyList();
        VPC_SECURITY_GROUPS = Collections.emptyList();
        ENDPOINT_ARN = "abc";
        VPC_ENDPOINT_ID = "def";
        VPC_ID = "ghi";
        CREATION_DATE = Instant.now();
        OWNER_ACCOUNT = "abcd";
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

    public static ResourceModel createEndpointAccessResourceModel() {
        return ResourceModel.builder()
                .endpointName(ENDPOINT_NAME)
                .subnetIds(SUBNET_IDS)
                .workgroupName(WORKGROUP_NAME)
                .build();
    }

    public static CreateEndpointAccessResponse createEndpointAccessResponseSdk() {
        return CreateEndpointAccessResponse.builder()
              .endpoint(EndpointAccess.builder()
                      .endpointName(ENDPOINT_NAME)
                      .endpointArn(ENDPOINT_ARN)
                      .endpointCreateTime(CREATION_DATE)
                      .endpointStatus(ENDPOINT_STATUS)
                      .address(ADDRESS)
                      .port(DEFAULT_PORT)
                      .subnetIds(SUBNET_IDS)
                      .workgroupName(WORKGROUP_NAME)
                      .vpcSecurityGroups(VPC_SECURITY_GROUPS)
                      .vpcEndpoint(VpcEndpoint.builder().vpcEndpointId(VPC_ENDPOINT_ID).vpcId(VPC_ID).networkInterfaces(Collections.emptyList()).build())
                      .build())
              .build();
    }

    public static ResourceModel deleteEndpointAccessResourceModel() {
        return ResourceModel.builder()
                .endpointName(ENDPOINT_NAME)
                .build();
    }

     public static DeleteEndpointAccessResponse deleteEndpointAccessResponseSdk() {
        return DeleteEndpointAccessResponse.builder()
                .endpoint(EndpointAccess.builder()
                        .endpointName(ENDPOINT_NAME)
                        .endpointArn(ENDPOINT_ARN)
                        .endpointCreateTime(CREATION_DATE)
                        .endpointStatus(ENDPOINT_STATUS)
                        .address(ADDRESS)
                        .port(DEFAULT_PORT)
                        .subnetIds(SUBNET_IDS)
                        .workgroupName(WORKGROUP_NAME)
                        .vpcSecurityGroups(VPC_SECURITY_GROUPS)
                        .vpcEndpoint(VpcEndpoint.builder().vpcEndpointId(VPC_ENDPOINT_ID).vpcId(VPC_ID).networkInterfaces(Collections.emptyList()).build())
                        .build())
                .build();
    }

    public static ResourceModel getEndpointAccessResourceModel() {
        return ResourceModel.builder()
                .endpointName(ENDPOINT_NAME)
                .build();
    }

    public static GetEndpointAccessResponse getEndpointAccessResponseSdk() {
        return GetEndpointAccessResponse.builder()
                .endpoint(EndpointAccess.builder()
                        .endpointName(ENDPOINT_NAME)
                        .endpointArn(ENDPOINT_ARN)
                        .endpointCreateTime(CREATION_DATE)
                        .endpointStatus(ENDPOINT_STATUS)
                        .address(ADDRESS)
                        .port(DEFAULT_PORT)
                        .subnetIds(SUBNET_IDS)
                        .workgroupName(WORKGROUP_NAME)
                        .vpcSecurityGroups(VPC_SECURITY_GROUPS)
                        .vpcEndpoint(VpcEndpoint.builder().vpcEndpointId(VPC_ENDPOINT_ID).vpcId(VPC_ID).networkInterfaces(Collections.emptyList()).build())
                        .build())
                .build();
    }

    public static ResourceModel listEndpointAccessResourceModel() {
        return ResourceModel.builder()
                .workgroupName(WORKGROUP_NAME)
                .vpcId(VPC_ID)
                .ownerAccount(OWNER_ACCOUNT)
                .build();
    }

    public static ListEndpointAccessResponse getListEndpointAccessResponsesSdk() {
        return ListEndpointAccessResponse.builder()
                .endpoints(software.amazon.awssdk.services.redshiftserverless.model.EndpointAccess.builder()
                        .endpointName(ENDPOINT_NAME)
                        .endpointArn(ENDPOINT_ARN)
                        .endpointCreateTime(CREATION_DATE)
                        .endpointStatus(ENDPOINT_STATUS)
                        .address(ADDRESS)
                        .port(DEFAULT_PORT)
                        .subnetIds(SUBNET_IDS)
                        .workgroupName(WORKGROUP_NAME)
                        .vpcSecurityGroups(VPC_SECURITY_GROUPS)
                        .vpcEndpoint(VpcEndpoint.builder().vpcEndpointId(VPC_ENDPOINT_ID).vpcId(VPC_ID).networkInterfaces(Collections.emptyList()).build())
                        .build())
                .build();
    }

    public static List<ResourceModel> getListResponsesResourceModel() {
        return Collections.singletonList(ResourceModel.builder()
                .endpointName(ENDPOINT_NAME)
                .endpointArn(ENDPOINT_ARN)
                .endpointCreateTime(CREATION_DATE.toString())
                .endpointStatus(ENDPOINT_STATUS)
                .address(ADDRESS)
                .port(DEFAULT_PORT)
                .subnetIds(SUBNET_IDS)
                .workgroupName(WORKGROUP_NAME)
                .vpcSecurityGroups(Collections.emptyList())
                .vpcEndpoint(software.amazon.redshiftserverless.endpointaccess.VpcEndpoint.builder().vpcEndpointId(VPC_ENDPOINT_ID).vpcId(VPC_ID).networkInterfaces(Collections.emptyList()).build())
                .build());
    }

    public static ResourceModel updateEndpointAccessResourceModel() {
        return ResourceModel.builder()
                .endpointName(ENDPOINT_NAME)
                .vpcSecurityGroups(Collections.emptyList())
                .build();
    }

    public static UpdateEndpointAccessResponse updateEndpointAccessResponseSdk() {
        return UpdateEndpointAccessResponse.builder()
                .endpoint(EndpointAccess.builder()
                        .endpointName(ENDPOINT_NAME)
                        .endpointArn(ENDPOINT_ARN)
                        .endpointCreateTime(CREATION_DATE)
                        .endpointStatus(ENDPOINT_STATUS)
                        .address(ADDRESS)
                        .port(DEFAULT_PORT)
                        .subnetIds(SUBNET_IDS)
                        .workgroupName(WORKGROUP_NAME)
                        .vpcSecurityGroups(VPC_SECURITY_GROUPS)
                        .vpcEndpoint(VpcEndpoint.builder().vpcEndpointId(VPC_ENDPOINT_ID).vpcId(VPC_ID).networkInterfaces(Collections.emptyList()).build())
                        .build())
                .build();
    }

    public static ResourceModel getEndpointAccessResponseResourceModel() {
        return ResourceModel.builder()
                .endpointName(ENDPOINT_NAME)
                .endpointArn(ENDPOINT_ARN)
                .endpointCreateTime(CREATION_DATE.toString())
                .endpointStatus(ENDPOINT_STATUS)
                .address(ADDRESS)
                .port(DEFAULT_PORT)
                .subnetIds(SUBNET_IDS)
                .workgroupName(WORKGROUP_NAME)
                .vpcSecurityGroups(Collections.emptyList())
                .vpcEndpoint(software.amazon.redshiftserverless.endpointaccess.VpcEndpoint.builder().vpcEndpointId(VPC_ENDPOINT_ID).vpcId(VPC_ID).networkInterfaces(Collections.emptyList()).build())
              .build();
    }
}
