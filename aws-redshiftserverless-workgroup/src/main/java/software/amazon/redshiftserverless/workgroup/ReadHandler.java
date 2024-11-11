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

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then (progress -> {
                    progress = proxy.initiate("AWS-RedshiftServerless-Workgroup::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                            .translateToServiceRequest(Translator::translateToReadRequest)
                            .makeServiceCall(this::readWorkgroup)
                            .handleError((awsRequest, exception, client, resourceModel, cxt) -> {
                                logger.log(String.format("Operation: %s : encountered exception for model: %s", awsRequest.getClass().getName(), ResourceModel.TYPE_NAME));
                                logger.log(awsRequest.toString());
                                return this.defaultWorkgroupErrorHandler(awsRequest, exception, client, resourceModel, cxt);
                            })
                            .done(awsResponse -> ProgressEvent.progress(Translator.translateFromReadResponse(awsResponse), callbackContext));

                    return progress;
                }).then(progress -> {
                    progress = proxy.initiate("AWS-RedshiftServerless-Namespace::Read::ReadTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                            .translateToServiceRequest(Translator::translateToReadTagsRequest)
                            .makeServiceCall(this::readTags)
                            .handleError((_awsRequest, _sdkEx, _client, _model, _callbackContext) ->
                                    ProgressEvent.failed(_model, _callbackContext, HandlerErrorCode.UnauthorizedTaggingOperation, _sdkEx.getMessage())
                            )
                            .done((_request, _response, _client, _model, _context) -> {
                                return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadTagsResponse(_response, _model));
                            });
                    return progress;
                });
    }
}
