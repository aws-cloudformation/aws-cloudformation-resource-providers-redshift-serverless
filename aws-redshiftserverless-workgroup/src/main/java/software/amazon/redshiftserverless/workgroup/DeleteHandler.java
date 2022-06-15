package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.*;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-RedshiftServerless-Workgroup::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToDeleteRequest)
                        .backoffDelay(DELETE_BACKOFF_STRATEGY)
                        .makeServiceCall(this::deleteWorkgroup)
                        .stabilize((_request, _response, _client, _model, _context) -> isWorkgroupActiveAfterDelete(_client, _model, _context))
                        .handleError(this::deleteWorkgroupErrorHandler)
                        .progress()
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private DeleteWorkgroupResponse deleteWorkgroup(final DeleteWorkgroupRequest awsRequest,
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        DeleteWorkgroupResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::deleteWorkgroup);

        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteWorkgroupErrorHandler(final DeleteWorkgroupRequest awsRequest,
                                                                                            final Exception exception,
                                                                                            final ProxyClient<RedshiftServerlessClient> client,
                                                                                            final ResourceModel model,
                                                                                            final CallbackContext context) {
        if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);
        } else if (exception instanceof ConflictException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ResourceConflict);
        } else if (exception instanceof ValidationException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }

}
