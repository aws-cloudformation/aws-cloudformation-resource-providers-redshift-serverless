package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;

import static software.amazon.cloudformation.proxy.ProgressEvent.progress;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        return proxy.initiate("AWS-RedshiftServerless-Workgroup::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, sdkProxyClient) -> {
                    GetWorkgroupResponse awsResponse = this.readWorkgroup(awsRequest, sdkProxyClient);

                    logger.log(String.format("%s : %s has successfully been read.", ResourceModel.TYPE_NAME, awsRequest.workgroupName()));
                    logger.log(awsResponse.toString());

                    return awsResponse;
                })
                .handleError((awsRequest, exception, client, resourceModel, cxt) -> {
                    logger.log(String.format("Operation: %s : encountered exception for model: %s", awsRequest.getClass().getName(), ResourceModel.TYPE_NAME));
                    logger.log(awsRequest.toString());
                    return this.defaultWorkgroupErrorHandler(awsRequest, exception, client, resourceModel, cxt);
                })
                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }
}
