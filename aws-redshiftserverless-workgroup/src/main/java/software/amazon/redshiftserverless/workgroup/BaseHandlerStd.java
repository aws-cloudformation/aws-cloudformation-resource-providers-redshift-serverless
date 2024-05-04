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
import software.amazon.awssdk.services.redshiftserverless.model.NamespaceStatus;
import software.amazon.awssdk.services.redshiftserverless.model.WorkgroupStatus;
import software.amazon.awssdk.services.redshiftserverless.model.RedshiftServerlessResponse;
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
    protected Logger logger;

    public static final String BUSY_WORKGROUP_RETRY_EXCEPTION_MESSAGE =
            "There is an operation running on the existing workgroup";

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

        GetNamespaceResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                getNamespaceRequest, proxyClient.client()::getNamespace);

        logger.log(String.format("Namespace : %s has successfully been read.", awsResponse.namespace().namespaceName()));
        logger.log(awsResponse.toString());

        return awsResponse;
    }

    protected GetWorkgroupResponse readWorkgroup(final GetWorkgroupRequest awsRequest,
                                                 final ProxyClient<RedshiftServerlessClient> proxyClient) {

        GetWorkgroupResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                awsRequest, proxyClient.client()::getWorkgroup);

        logger.log(String.format("Workgroup : %s has successfully been read.", awsResponse.workgroup().workgroupName()));
        logger.log(awsResponse.toString());

        return awsResponse;
    }

    protected CreateWorkgroupResponse createWorkgroup(final CreateWorkgroupRequest awsRequest,
                                                      final ProxyClient<RedshiftServerlessClient> proxyClient) {

        CreateWorkgroupResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                awsRequest, proxyClient.client()::createWorkgroup);

        logger.log(String.format("Workgroup : %s has successfully been created.", awsResponse.workgroup().workgroupName()));
        logger.log(awsResponse.toString());

        return awsResponse;
    }

    protected UpdateWorkgroupResponse updateWorkgroup(final UpdateWorkgroupRequest awsRequest,
                                                      final ProxyClient<RedshiftServerlessClient> proxyClient) {

        UpdateWorkgroupResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                awsRequest, proxyClient.client()::updateWorkgroup);

        logger.log(String.format("Workgroup : %s has successfully been updated.", awsResponse.workgroup().workgroupName()));
        logger.log(awsResponse.toString());

        return awsResponse;

    }

    protected DeleteWorkgroupResponse deleteWorkgroup(final DeleteWorkgroupRequest awsRequest,
                                                      final ProxyClient<RedshiftServerlessClient> proxyClient) {

        DeleteWorkgroupResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                awsRequest, proxyClient.client()::deleteWorkgroup);

        logger.log(String.format("Workgroup : %s has successfully been deleted.", awsResponse.workgroup().workgroupName()));
        logger.log(awsResponse.toString());

        return awsResponse;
    }

    protected boolean isWorkgroupStable(final Object awsRequest,
                                        final RedshiftServerlessResponse awsResponse,
                                        final ProxyClient<RedshiftServerlessClient> proxyClient,
                                        final ResourceModel model,
                                        final CallbackContext context) {

        GetWorkgroupRequest getWorkgroupStatusRequest = GetWorkgroupRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .build();

        GetWorkgroupResponse getWorkgroupResponse = this.readWorkgroup(getWorkgroupStatusRequest, proxyClient);

        logger.log(String.format("Workgroup: %s has successfully been read.", getWorkgroupResponse.workgroup().workgroupName()));

        logger.log(getWorkgroupResponse.toString());

        return getWorkgroupResponse.workgroup().status().equals(WorkgroupStatus.AVAILABLE);
    }

    protected boolean isNamespaceStable(final Object awsRequest,
                                        final RedshiftServerlessResponse awsResponse,
                                        final ProxyClient<RedshiftServerlessClient> proxyClient,
                                        final ResourceModel model,
                                        final CallbackContext context) {

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .namespaceName(model.getNamespaceName())
                .build();

        GetNamespaceResponse getNamespaceResponse = this.readNamespace(getNamespaceRequest, proxyClient);

        logger.log(String.format("%s : Namespace: %s has successfully been read.",
                ResourceModel.TYPE_NAME, getNamespaceResponse.namespace().namespaceName()));

        logger.log(getNamespaceResponse.toString());

        return getNamespaceResponse.namespace().status().equals(NamespaceStatus.AVAILABLE);
    }

    protected boolean isWorkgroupDeleted(final Object awsRequest,
                                         final RedshiftServerlessResponse awsResponse,
                                         final ProxyClient<RedshiftServerlessClient> proxyClient,
                                         final ResourceModel model,
                                         final CallbackContext context) {

        GetWorkgroupRequest getWorkgroupStatusRequest = GetWorkgroupRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .build();

        try {
            GetWorkgroupResponse getWorkgroupResponse = this.readWorkgroup(getWorkgroupStatusRequest, proxyClient);

            logger.log(String.format("%s : Workgroup: %s has successfully been read.",
                    ResourceModel.TYPE_NAME, getWorkgroupResponse.workgroup().workgroupName()));

            logger.log(getWorkgroupResponse.toString());
        } catch (ResourceNotFoundException e) {
            return true;
        }

        return false;
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
