package software.amazon.redshiftserverless.workgroup;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.CollectionUtils;
import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.*;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.Workgroup;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.Locale;
import software.amazon.cloudformation.proxy.delay.Constant;
import java.time.Duration;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  protected static final Constant DELETE_BACKOFF_STRATEGY = Constant.of().
          timeout(Duration.ofMinutes(5L)).delay(Duration.ofSeconds(5L)).build();

  protected static final Constant UPDATE_BACKOFF_STRATEGY = Constant.of().
          timeout(Duration.ofMinutes(20L)).delay(Duration.ofSeconds(5L)).build();

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

  private Logger logger;
  public boolean isWorkgroupActive(final ProxyClient<RedshiftArcadiaCoralClient> proxyClient, ResourceModel model, CallbackContext cxt) {
    GetWorkgroupRequest awsRequest =
            GetWorkgroupRequest.builder().workgroupName(model.getWorkgroupName()).build();
    GetWorkgroupResponse awsResponse =
            proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::getWorkgroup);

    Workgroup workgroup =  awsResponse.workgroup();
    return awsResponse.workgroup().statusAsString().equalsIgnoreCase(WorkgroupStatus.AVAILABLE.toString());
  }

  protected boolean isWorkgroupActiveAfterDelete (final ProxyClient<RedshiftArcadiaCoralClient> proxyClient, ResourceModel model, CallbackContext cxt) {
    GetWorkgroupRequest awsRequest =
            GetWorkgroupRequest.builder().workgroupName(model.getWorkgroupName()).build();
    try {
      GetWorkgroupResponse awsResponse =
              proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::getWorkgroup);
    } catch (final ResourceNotFoundException e) {
      return true;
    }
    return false;
  }


}
