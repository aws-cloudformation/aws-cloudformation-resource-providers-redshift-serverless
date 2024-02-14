package software.amazon.redshiftserverless.workgroup;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.GetEndpointAccessRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.proxy.*;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel resourceModel = request.getDesiredResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> {
                    if (resourceModel.getEndpointName() != null) {
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::GetEndpointAccess", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToGetEndpointAccessRequest)
                                .makeServiceCall(this::getEndpointAccess)
                                .stabilize(this::isEndpointAccessRequest)
                                .handleError(this::getEndpointAccessErrorHandler)
                                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromEndpointAccessResponse(awsResponse.endpoint())));
                    }
                    return progress;
                })

                .then(progress ->
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Read", proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToReadRequest)
                                .makeServiceCall(this::readWorkgroup)
                                .handleError(this::readWorkgroupErrorHandler)
                                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse))));

    }

    private GetWorkgroupResponse readWorkgroup(final GetWorkgroupRequest awsRequest,
                                               final ProxyClient<RedshiftServerlessClient> proxyClient) {
        GetWorkgroupResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::getWorkgroup);

        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> readWorkgroupErrorHandler(final GetWorkgroupRequest awsRequest,
                                                                                    final Exception exception,
                                                                                    final ProxyClient<RedshiftServerlessClient> client,
                                                                                    final ResourceModel model,
                                                                                    final CallbackContext context) {
        if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);

        } else if (exception instanceof ValidationException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);

        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }

    private GetEndpointAccessResponse getEndpointAccess(final GetEndpointAccessRequest awsRequest,
                                               final ProxyClient<RedshiftServerlessClient> proxyClient) {
        GetEndpointAccessResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::getEndpointAccess);

        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> getEndpointAccessErrorHandler(final GetEndpointAccessRequest awsRequest,
                                                                                    final Exception exception,
                                                                                    final ProxyClient<RedshiftServerlessClient> client,
                                                                                    final ResourceModel model,
                                                                                    final CallbackContext context) {
        if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);

        } else if (exception instanceof ValidationException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);

        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }
}
