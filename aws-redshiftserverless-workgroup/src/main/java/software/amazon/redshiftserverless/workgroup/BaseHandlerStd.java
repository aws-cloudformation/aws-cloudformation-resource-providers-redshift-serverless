package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.Namespace;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.InsufficientCapacityException;
import software.amazon.awssdk.services.redshiftserverless.model.TooManyTagsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.util.regex.Pattern;
import java.time.Duration;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    public static final String BUSY_WORKGROUP_RETRY_EXCEPTION_MESSAGE =
            "There is an operation running on the existing workgroup";

    protected static final String NAMESPACE_STATUS_AVAILABLE = "available";

    protected static boolean isRetriableWorkgroupException(ConflictException exception) {
        return exception.getMessage().contains(BUSY_WORKGROUP_RETRY_EXCEPTION_MESSAGE);
    }

    protected static final Constant BACKOFF_STRATEGY = Constant.of()
            .timeout(Duration.ofMinutes(120L))
            .delay(Duration.ofSeconds(5L))
            .build();

    protected static final Constant PREOPERATION_BACKOFF_STRATEGY = Constant.of()
            .timeout(Duration.ofMinutes(5L))
            .delay(Duration.ofSeconds(5L))
            .build();

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger);

    protected GetNamespaceResponse readNamespace(final GetNamespaceRequest getNamespaceRequest,
                                                 final ProxyClient<RedshiftServerlessClient> proxyClient) {

        GetNamespaceResponse getNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(
                getNamespaceRequest, proxyClient.client()::getNamespace);

        return getNamespaceResponse;
    }

    protected GetWorkgroupResponse readWorkgroup(final GetWorkgroupRequest awsRequest,
                                                 final ProxyClient<RedshiftServerlessClient> proxyClient) {

        GetWorkgroupResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                awsRequest, proxyClient.client()::getWorkgroup);

        return awsResponse;
    }

    protected CreateWorkgroupResponse createWorkgroup(final CreateWorkgroupRequest awsRequest,
                                                      final ProxyClient<RedshiftServerlessClient> proxyClient) {

        CreateWorkgroupResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                awsRequest, proxyClient.client()::createWorkgroup);

        return awsResponse;
    }

    protected UpdateWorkgroupResponse updateWorkgroup(final UpdateWorkgroupRequest awsRequest,
                                                      final ProxyClient<RedshiftServerlessClient> proxyClient) {

        UpdateWorkgroupResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                awsRequest, proxyClient.client()::updateWorkgroup);

        return awsResponse;

    }

    protected DeleteWorkgroupResponse deleteWorkgroup(final DeleteWorkgroupRequest awsRequest,
                                                      final ProxyClient<RedshiftServerlessClient> proxyClient) {

        DeleteWorkgroupResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                awsRequest, proxyClient.client()::deleteWorkgroup);

        return awsResponse;
    }

    protected ProgressEvent<ResourceModel, CallbackContext> defaultWorkgroupErrorHandler(final Object awsRequest,
                                                                                         final Exception exception,
                                                                                         final ProxyClient<RedshiftServerlessClient> client,
                                                                                         final ResourceModel model,
                                                                                         final CallbackContext context) {

        if (exception instanceof ValidationException || exception instanceof TooManyTagsException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

        } else if (exception instanceof ConflictException || exception instanceof InsufficientCapacityException) {
            Pattern pattern = Pattern.compile(".*already exists.*", Pattern.CASE_INSENSITIVE);
            HandlerErrorCode handlerErrorCode = pattern.matcher(exception.getMessage()).matches() ?
                    HandlerErrorCode.AlreadyExists :
                    HandlerErrorCode.ResourceConflict;

            return ProgressEvent.defaultFailureHandler(exception, handlerErrorCode);

        } else if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);

        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);

        } else if (exception instanceof ValidationException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);

        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }
}
