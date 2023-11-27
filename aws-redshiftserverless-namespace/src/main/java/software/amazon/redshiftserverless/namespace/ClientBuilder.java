package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.util.function.Supplier;

public class ClientBuilder {

    public static RedshiftServerlessClient getClient() {
    return RedshiftServerlessClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .build();
  }
    public static RedshiftClient redshiftClient() {
        return RedshiftClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
