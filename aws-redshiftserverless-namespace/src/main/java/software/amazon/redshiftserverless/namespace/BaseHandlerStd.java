package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.AccessDeniedException;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotCopyConfigurationResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.redshiftserverless.model.Namespace;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.TooManyTagsException;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse;
import com.amazonaws.util.StringUtils;

import java.time.Duration;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
  protected Logger logger;
  protected final String NAMESPACE_STATUS_AVAILABLE = "available";
  protected static final Constant BACKOFF_STRATEGY = Constant.of().
          timeout(Duration.ofMinutes(30L)).delay(Duration.ofSeconds(10L)).build();

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
      proxy.newProxy(ClientBuilder::redshiftClient),
      proxy.newProxy(ClientBuilder::secretsManagerClient),
      logger
    );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final ProxyClient<RedshiftServerlessClient> proxyClient,
    final ProxyClient<RedshiftClient> redshiftProxyClient,
    final ProxyClient<SecretsManagerClient> secretsManagerProxyClient,
    final Logger logger);

  protected boolean isNamespaceActive (final ProxyClient<RedshiftServerlessClient> proxyClient, ResourceModel resourceModel, CallbackContext context) {
    GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder().namespaceName(resourceModel.getNamespaceName()).build();
    GetNamespaceResponse getNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(getNamespaceRequest, proxyClient.client()::getNamespace);
    Namespace namespace = getNamespaceResponse.namespace();
    if (namespace == null) {
      return false;
    }

    return NAMESPACE_STATUS_AVAILABLE.equalsIgnoreCase(getNamespaceResponse.namespace().statusAsString());
  }

  protected boolean isNamespaceActiveAfterDelete (final ProxyClient<RedshiftServerlessClient> proxyClient, ResourceModel resourceModel, CallbackContext context) {
    GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder().namespaceName(resourceModel.getNamespaceName()).build();
    try {
      proxyClient.injectCredentialsAndInvokeV2(getNamespaceRequest, proxyClient.client()::getNamespace);
    } catch (final ResourceNotFoundException e) {
      return true;
    }
    return false;
  }

  protected boolean isNamespaceSecretDeleted (final ProxyClient<SecretsManagerClient> secretsManagerProxyClient, CallbackContext context) {
    String namespaceSecretArn = context.getAdminPasswordSecretArn();

    // For namespaces that aren't opted in to Redshift Managed Passwords, AdminPasswordSecretArn is null
    if (StringUtils.isNullOrEmpty(namespaceSecretArn)) {
      return true;
    }

    DescribeSecretRequest describeSecretRequest = DescribeSecretRequest.builder().secretId(namespaceSecretArn).build();
    try {
      secretsManagerProxyClient.injectCredentialsAndInvokeV2(describeSecretRequest, secretsManagerProxyClient.client()::describeSecret);
    } catch (final software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException e) {
      logger.log(String.format("Secret %s has successfully been deleted.", namespaceSecretArn));
      return true;
    }
    return false;
  }

  protected ListSnapshotCopyConfigurationsResponse listSnapshotCopyConfigurations(final ListSnapshotCopyConfigurationsRequest listRequest,
                                                                                  final ProxyClient<RedshiftServerlessClient> proxyClient) {
    ListSnapshotCopyConfigurationsResponse listResponse = proxyClient.injectCredentialsAndInvokeV2(listRequest, proxyClient.client()::listSnapshotCopyConfigurations);
    logger.log(String.format("%s %s snapshot configurations has successfully been read.", ResourceModel.TYPE_NAME, listRequest.namespaceName()));
    return listResponse;
  }

  protected CreateSnapshotCopyConfigurationResponse createSnapshotCopyConfiguration(final CreateSnapshotCopyConfigurationRequest createRequest,
                                                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
    CreateSnapshotCopyConfigurationResponse createResponse = proxyClient.injectCredentialsAndInvokeV2(createRequest, proxyClient.client()::createSnapshotCopyConfiguration);
    logger.log(String.format("Created snapshot copy configuration for %s %s in destination region %s.", ResourceModel.TYPE_NAME,
            createResponse.snapshotCopyConfiguration().namespaceName(), createResponse.snapshotCopyConfiguration().destinationRegion()));
    return createResponse;
  }

  protected String getNamespaceSecretArn(final ProxyClient<RedshiftServerlessClient> proxyClient, final String namespaceName) {
    String namespaceSecretArn = null;
    GetNamespaceResponse getNamespaceResponse = null;

    GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder().namespaceName(namespaceName).build();

    try {
      getNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(getNamespaceRequest, proxyClient.client()::getNamespace);
      namespaceSecretArn = getNamespaceResponse.namespace().adminPasswordSecretArn();
    } catch (final ResourceNotFoundException e) {
      // do nothing here, we will handle this in .handleError part of the handler instead
    }
    return namespaceSecretArn;
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
