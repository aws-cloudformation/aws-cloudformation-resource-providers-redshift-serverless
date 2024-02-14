package software.amazon.redshiftserverless.workgroup;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.AccessDeniedException;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.CreateEndpointAccessRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.InsufficientCapacityException;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.redshiftserverless.model.TooManyTagsException;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.regex.Pattern;

public class CreateHandler extends BaseHandlerStd {
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
                    if (!StringUtils.isNullOrEmpty(resourceModel.getEndpointName())) {
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::CreateEndpointAccess", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToCreateEndpointAccessRequest)
                                .makeServiceCall(this::createEndpointAccess)
                                .stabilize(this::isEndpointAccessRequest)
                                .handleError(this::createEndpointAccessErrorHandler)
                                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromEndpointAccessResponse(awsResponse.endpoint())));
                    }
                    return progress;
                })

                .then(progress -> {
                    if (StringUtils.isNullOrEmpty(resourceModel.getEndpointName())) {
                        proxy.initiate("AWS-RedshiftServerless-Workgroup::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall(this::createWorkgroup)
                                .stabilize(this::isWorkgroupStable)
                                .handleError(this::createWorkgroupErrorHandler)
                                .progress();
                    }
                    return progress;
                })

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
        if (exception instanceof ValidationException ||
                exception instanceof TooManyTagsException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

        } else if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);

        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);

        } else if (exception instanceof ConflictException ||
                exception instanceof InsufficientCapacityException) {
            Pattern pattern = Pattern.compile(".*already exists.*", Pattern.CASE_INSENSITIVE);
            HandlerErrorCode handlerErrorCode = pattern.matcher(exception.getMessage()).matches() ?
                    HandlerErrorCode.AlreadyExists :
                    HandlerErrorCode.ResourceConflict;

            return ProgressEvent.defaultFailureHandler(exception, handlerErrorCode);

        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }

    private CreateEndpointAccessResponse createEndpointAccess(final CreateEndpointAccessRequest awsRequest,
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        CreateEndpointAccessResponse awsResponse;
        awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createEndpointAccess);

        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> createEndpointAccessErrorHandler(final CreateEndpointAccessRequest awsRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        if (exception instanceof ValidationException ||
                exception instanceof AccessDeniedException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

        } else if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);

        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);

        } else if (exception instanceof ConflictException ||
                exception instanceof ServiceQuotaExceededException) {
            Pattern pattern = Pattern.compile(".*already exists.*", Pattern.CASE_INSENSITIVE);
            HandlerErrorCode handlerErrorCode = pattern.matcher(exception.getMessage()).matches() ?
                    HandlerErrorCode.AlreadyExists :
                    HandlerErrorCode.ResourceConflict;

            return ProgressEvent.defaultFailureHandler(exception, handlerErrorCode);

        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }
}
