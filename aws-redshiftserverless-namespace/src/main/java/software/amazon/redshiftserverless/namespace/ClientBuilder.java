package software.amazon.redshiftserverless.namespace;


import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.net.URI;

public class ClientBuilder {
  public static RedshiftServerlessClient getClient() {
    return RedshiftServerlessClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }
}
