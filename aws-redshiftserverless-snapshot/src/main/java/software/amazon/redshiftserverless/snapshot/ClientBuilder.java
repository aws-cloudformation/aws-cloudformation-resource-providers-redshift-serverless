package software.amazon.redshiftserverless.snapshot;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    public static RedshiftServerlessClient getClient() {
        return RedshiftServerlessClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
