package software.amazon.redshiftserverless.snapshot;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.AccessDeniedException;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.Namespace;
import software.amazon.awssdk.services.redshiftserverless.model.NamespaceStatus;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.RedshiftServerlessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.redshiftserverless.model.Snapshot;
import software.amazon.awssdk.services.redshiftserverless.model.SnapshotStatus;
import software.amazon.awssdk.services.redshiftserverless.model.TooManyTagsException;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;
import software.amazon.cloudformation.proxy.delay.Exponential;
import com.amazonaws.util.StringUtils;

import java.time.Duration;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    protected Logger logger;

    // This is needed to make sure that the operation on the backend succeeds
    public static final int EVENTUAL_CONSISTENCY_DELAY_SECONDS = 300;

    Exponential getBackOffStrategy() {
        return Exponential.of().minDelay(Duration.ofSeconds(240)).timeout(Duration.ofSeconds(500)).build();
    }

    protected static final Constant PREOPERATION_BACKOFF_STRATEGY = Constant.of()
            .timeout(Duration.ofMinutes(60L))
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

    protected boolean isSnapshotAvailable(final Object awsRequest,
                                          final RedshiftServerlessResponse awsResponse,
                                          final ProxyClient<RedshiftServerlessClient> proxyClient,
                                          final ResourceModel resourceModel,
                                          final CallbackContext context) {
      GetSnapshotRequest getSnapshotRequest = GetSnapshotRequest.builder().snapshotName(resourceModel.getSnapshotName()).ownerAccount(resourceModel.getOwnerAccount()).build();
      GetSnapshotResponse getSnapshotResponse = proxyClient.injectCredentialsAndInvokeV2(getSnapshotRequest, proxyClient.client()::getSnapshot);
      Snapshot snapshot = getSnapshotResponse.snapshot();
      if (snapshot == null) {
        return false;
      }

      return snapshot.status().equals(SnapshotStatus.AVAILABLE);
    }

    protected GetNamespaceResponse readNamespace(final GetNamespaceRequest getNamespaceRequest,
                                                 final ProxyClient<RedshiftServerlessClient> proxyClient) {

        GetNamespaceResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(
                getNamespaceRequest, proxyClient.client()::getNamespace);

        logger.log(String.format("Namespace : %s has successfully been read.", awsResponse.namespace().namespaceName()));
        logger.log(awsResponse.toString());

        return awsResponse;
    }

    protected boolean isNamespaceActive (final ProxyClient<RedshiftServerlessClient> proxyClient, CallbackContext context) {
        String namespaceName = context.getNamespaceName();

        if (StringUtils.isNullOrEmpty(namespaceName)) {
            return false;
        }

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .namespaceName(namespaceName)
                .build();

        GetNamespaceResponse getNamespaceResponse = this.readNamespace(getNamespaceRequest, proxyClient);

        logger.log(String.format("%s : Namespace: %s has successfully been read.",
                ResourceModel.TYPE_NAME, getNamespaceResponse.namespace().namespaceName()));

        logger.log(getNamespaceResponse.toString());

        return getNamespaceResponse.namespace().status().equals(NamespaceStatus.AVAILABLE);
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

    protected String getNamespaceName(final ProxyClient<RedshiftServerlessClient> proxyClient, final String snapshotName) {
        String namespaceName = null;
        GetSnapshotResponse getSnapshotResponse = null;

        GetSnapshotRequest getSnapshotRequest = GetSnapshotRequest.builder().snapshotName(snapshotName).build();

        try {
            getSnapshotResponse = proxyClient.injectCredentialsAndInvokeV2(getSnapshotRequest, proxyClient.client()::getSnapshot);
            namespaceName = getSnapshotResponse.snapshot().namespaceName();
        } catch (final ResourceNotFoundException e) {
            // do nothing here, we will handle this in .handleError part of the handler instead
        }
        return namespaceName;
    }

    protected boolean isSnapshotActiveAfterDelete (final ProxyClient<RedshiftServerlessClient> proxyClient, ResourceModel resourceModel, CallbackContext context) {
        GetSnapshotRequest getSnapshotRequest = GetSnapshotRequest.builder().snapshotName(resourceModel.getSnapshotName()).build();
        try {
            proxyClient.injectCredentialsAndInvokeV2(getSnapshotRequest, proxyClient.client()::getSnapshot);
        } catch (final ResourceNotFoundException e) {
            return true;
        }
        return false;
    }

    protected ListTagsForResourceResponse readTags(final ListTagsForResourceRequest awsRequest,
                                                   final ProxyClient<RedshiftServerlessClient> proxyClient) {

        ListTagsForResourceResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listTagsForResource);

        logger.log(String.format("%s's tags have successfully been read.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    protected <T> ProgressEvent<ResourceModel, CallbackContext> defaultErrorHandler(final T request,
                                                                                    final Exception exception,
                                                                                    final ProxyClient<RedshiftServerlessClient> client,
                                                                                    final ResourceModel model,
                                                                                    final CallbackContext context) {
      return errorHandler(exception);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> errorHandler(final Exception exception) {
      if (exception instanceof ValidationException) {
        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
      } else if (exception instanceof AccessDeniedException) {
        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.AccessDenied);
      } else if (exception instanceof InternalServerException) {
        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ServiceInternalError);
      } else if (exception instanceof ResourceNotFoundException) {
        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
      } else if (exception instanceof TooManyTagsException || exception instanceof ServiceQuotaExceededException) {
        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ServiceLimitExceeded);
      } else if (exception instanceof ConflictException) {
        if (exception.getMessage().contains("already exists")) {
          return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.AlreadyExists);
        }
        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ResourceConflict);
      } else {
        return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
      }
    }
}
