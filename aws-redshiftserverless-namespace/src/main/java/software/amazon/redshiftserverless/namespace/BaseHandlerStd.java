package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ConflictException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.InternalServerException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.Namespace;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.TooManyTagsException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
  protected final String NAMESPACE_STATUS_AVAILABLE = "available";
  protected final int CALLBACK_DELAY_SECONDS = 30;

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
    final ProxyClient<RedshiftArcadiaCoralClient> proxyClient,
    final Logger logger);

  protected boolean isNamespaceActive (final ProxyClient<RedshiftArcadiaCoralClient> proxyClient, ResourceModel resourceModel, CallbackContext context) {
    GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder().namespaceName(resourceModel.getNamespaceName()).build();
    GetNamespaceResponse getNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(getNamespaceRequest, proxyClient.client()::getNamespace);
    Namespace namespace = getNamespaceResponse.namespace();
    if (namespace == null) {
      return false;
    }

    return NAMESPACE_STATUS_AVAILABLE.equalsIgnoreCase(getNamespaceResponse.namespace().statusAsString());
  }

  protected boolean isNamespaceActiveAfterDelete (final ProxyClient<RedshiftArcadiaCoralClient> proxyClient, ResourceModel resourceModel, CallbackContext context) {
    GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder().namespaceName(resourceModel.getNamespaceName()).build();
    try {
      proxyClient.injectCredentialsAndInvokeV2(getNamespaceRequest, proxyClient.client()::getNamespace);
    } catch (final ResourceNotFoundException e) {
      return true;
    }
    return false;
  }

  protected ProgressEvent<ResourceModel, CallbackContext> errorhandler(final Exception exception) {
    if (exception instanceof ValidationException) {
      return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
    } else if (exception instanceof InternalServerException) {
      return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ServiceInternalError);
    } else if (exception instanceof ResourceNotFoundException) {
      return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
    } else if (exception instanceof TooManyTagsException) {
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
