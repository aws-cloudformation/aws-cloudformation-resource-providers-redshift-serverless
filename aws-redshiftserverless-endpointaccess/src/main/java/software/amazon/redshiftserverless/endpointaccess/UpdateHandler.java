package software.amazon.redshiftserverless.endpointaccess;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.AccessDeniedException;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.InsufficientCapacityException;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateEndpointAccessRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        
        return ProgressEvent.progress(model, callbackContext)
            .then(progress ->
                    proxy.initiate("AWS-RedshiftServerless-EndpointAccess::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                            .translateToServiceRequest(Translator::translateToUpdateEndpointAccessRequest)
                            .makeServiceCall(this::updateEndpointAccess)
                            .stabilize(this::isEndpointAccessRequest)
                            .handleError(this::updateEndpointAccessErrorHandler)
                            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromEndpointAccessResponse(awsResponse.endpoint()))));
    }

    private UpdateEndpointAccessResponse updateEndpointAccess(final UpdateEndpointAccessRequest awsRequest,
                                                              final ProxyClient<RedshiftServerlessClient> proxyClient) {
        UpdateEndpointAccessResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::updateEndpointAccess);

        logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateEndpointAccessErrorHandler(final UpdateEndpointAccessRequest awsRequest,
                                                                                           final Exception exception,
                                                                                           final ProxyClient<RedshiftServerlessClient> client,
                                                                                           final ResourceModel model,
                                                                                           final CallbackContext context) {
        if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);

        } else if (exception instanceof ValidationException ||
                exception instanceof AccessDeniedException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);

        } else if (exception instanceof ConflictException ||
                exception instanceof InsufficientCapacityException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ResourceConflict);

        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }
}
