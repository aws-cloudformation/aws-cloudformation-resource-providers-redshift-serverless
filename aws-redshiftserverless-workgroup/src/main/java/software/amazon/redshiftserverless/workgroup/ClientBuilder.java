package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.net.URI;

public class ClientBuilder {

    // TODO: change the endpoint when it goes to prod
    public static RedshiftServerlessClient getClient() {
        return RedshiftServerlessClient.builder()
                .endpointOverride(URI.create("https://qa.us-east-1.serverless.redshift.aws.a2z.com"))
                .region(Region.US_EAST_1)
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
