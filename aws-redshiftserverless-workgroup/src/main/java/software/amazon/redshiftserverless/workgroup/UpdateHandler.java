package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.*;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftArcadiaCoralClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-RedshiftServerless-Workgroup::Update::first", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToFirstUpdateRequest)
                        .backoffDelay(UPDATE_BACKOFF_STRATEGY)
                        .makeServiceCall(this::updateWorkgroup)
                        .stabilize((_request, _response, _client, _model, _context) -> isWorkgroupActive(_client, _model, _context))
                        .handleError(this::updateWorkflowErrorHandler)
                        .progress())
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateWorkgroupResponse updateWorkgroup(final UpdateWorkgroupRequest awsRequest,
                                                          final ProxyClient<RedshiftArcadiaCoralClient> proxyClient) {
        UpdateWorkgroupResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::updateWorkgroup);

        logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateWorkflowErrorHandler(final UpdateWorkgroupRequest awsRequest,
                                                                                             final Exception exception,
                                                                                             final ProxyClient<RedshiftArcadiaCoralClient> client,
                                                                                             final ResourceModel model,
                                                                                             final CallbackContext context) {
        if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
        } else if (exception instanceof ValidationException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
        } else if (exception instanceof InternalServerException || exception instanceof InsufficientCapacityException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);
        } else if (exception instanceof ConflictException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ResourceConflict);
        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }
}
