package software.amazon.redshiftserverless.namespace;


import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.net.URI;

public class ClientBuilder {
  public static final String CLIENT_ENDPOINT_DEVO = "https://devo.us-east-1.serverless.redshift.aws.a2z.com";
  public static RedshiftServerlessClient getClient() {
    return RedshiftServerlessClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create(CLIENT_ENDPOINT_DEVO))
            .build();
  }
}
