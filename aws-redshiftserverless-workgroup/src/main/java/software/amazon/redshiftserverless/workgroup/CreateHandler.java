package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.redshiftserverless.model.*;

import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        logger.log(String.format("ResourceModel of the request " + request.toString()));
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                proxy.initiate("AWS-RedshiftServerless-Workgroup::Create", proxyClient,progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToCreateRequest)
                        .makeServiceCall(this::createWorkgroup)
                        .stabilize((_request, _response, _client, _model, _context) -> isWorkgroupActive(_client, _model, _context))
                        .handleError(this::createWorkgroupErrorHandler)
                        .progress()
                )
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private CreateWorkgroupResponse createWorkgroup(final CreateWorkgroupRequest awsRequest,
                                                          final ProxyClient<RedshiftServerlessClient> proxyClient) {

        CreateWorkgroupResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createWorkgroup);

        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> createWorkgroupErrorHandler(final CreateWorkgroupRequest awsRequest,
                                                                                             final Exception exception,
                                                                                             final ProxyClient<RedshiftServerlessClient> client,
                                                                                             final ResourceModel model,
                                                                                             final CallbackContext context) {
        if (exception instanceof ValidationException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
        } else if (exception instanceof ResourceNotFoundException ){
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
        } else if (exception instanceof InternalServerException || exception instanceof TooManyTagsException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);
        } else if (exception instanceof ConflictException) {
            if (exception.getMessage().contains("already exists")) {
                return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.AlreadyExists);
            }
            return ProgressEvent.defaultFailureHandler(exception,HandlerErrorCode.ResourceConflict);
        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }

}
