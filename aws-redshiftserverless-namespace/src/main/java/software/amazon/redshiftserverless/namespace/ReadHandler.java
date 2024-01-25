package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.*;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;
    private final String GET_RESOURCE_POLICY_ERROR = "not authorized to perform: redshift:GetResourcePolicy";
    private final Integer GET_RESOURCE_POLICY_ERR_STATUS_CODE = 403;
    private final String RESOURCE_POLICY_UNSUPPORTED_ERROR = "The resource policy feature isn't supported";
    private final Integer RESOURCE_POLICY_UNSUPPORTED_ERR_STATUS_CODE = 400;
    private boolean containsResourcePolicy = false;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final ProxyClient<RedshiftClient> redshiftProxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        /*
        containsResourcePolicy will be true if NamespaceResourcePolicy property is included in the template.
        This attribute will be used to decide if "not authorized to perform: redshift:GetResourcePolicy" errors
        in Read handler should be suppressed or not.
         */
        containsResourcePolicy = model.getNamespaceResourcePolicy() != null;

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> {
                    progress = proxy.initiate("AWS-RedshiftServerless-Namespace::Read", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .makeServiceCall(this::getNamespace)
                        .handleError(this::getNamespaceErrorHandler)
                        .done(awsResponse -> {
                            callbackContext.setNamespaceArn(awsResponse.namespace().namespaceArn());
                            return ProgressEvent.progress(Translator.translateFromReadResponse(awsResponse), callbackContext);
                        });
                    return progress;
                })
                .then(progress -> {
                    return proxy.initiate("AWS-Redshift-ResourcePolicy::Get", redshiftProxyClient, progress.getResourceModel(), callbackContext)
                        .translateToServiceRequest(resourceModelRequest -> Translator.translateToGetResourcePolicy(resourceModelRequest, callbackContext.getNamespaceArn()))
                        .makeServiceCall(this::getNamespaceResourcePolicy)
                        .done((_request, _response, _client, _model, _context) -> {
                            _model.setNamespaceResourcePolicy(Translator.convertStringToJson(_response.resourcePolicy().policy(), logger));
                            return ProgressEvent.defaultSuccessHandler(_model);
                        });
                });
    }

    private GetNamespaceResponse getNamespace(final GetNamespaceRequest getNamespaceRequest,
                                               final ProxyClient<RedshiftServerlessClient> proxyClient) {
        GetNamespaceResponse getNamespaceResponse = null;

        logger.log(String.format("%s %s getNamespaces.", ResourceModel.TYPE_NAME, getNamespaceRequest.namespaceName()));
        getNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(getNamespaceRequest, proxyClient.client()::getNamespace);
        logger.log(String.format("%s %s has successfully been read.", ResourceModel.TYPE_NAME, getNamespaceRequest.namespaceName()));
        return getNamespaceResponse;
    }

    /**
     * Gets resource policy for Cluster
     * @param awsRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return getResponse resource response
     */
    private GetResourcePolicyResponse getNamespaceResourcePolicy(
            final GetResourcePolicyRequest awsRequest,
            final ProxyClient<RedshiftClient> proxyClient) {
        GetResourcePolicyResponse getResponse = null;

        try {
            logger.log(awsRequest.resourceArn());
            getResponse = proxyClient.injectCredentialsAndInvokeV2(
                    awsRequest, proxyClient.client()::getResourcePolicy);
        } catch (ResourceNotFoundException e){
            logger.log(String.format("NamespaceResourcePolicy not found for namespace %s", awsRequest.resourceArn()));
            return noOpNamespaceResourcePoliy(awsRequest);
        } catch (InvalidPolicyException | UnsupportedOperationException e) {
          /* ResourcePolicy is not enabled in all regions, we should handle unsupported operation exception
          if NamespaceResourcePolicy is not added as a property while creating Namespace resource. */
          if(!containsResourcePolicy && e.statusCode() == RESOURCE_POLICY_UNSUPPORTED_ERR_STATUS_CODE &&
                  e.awsErrorDetails().errorMessage().contains(RESOURCE_POLICY_UNSUPPORTED_ERROR)) {
              logger.log(e.getMessage());
              return noOpNamespaceResourcePoliy(awsRequest);
          } else {
              throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
          }
        } catch ( RedshiftException e) {
            /* This error handling is required for backward compatibility. Without this exception handling, existing customers creating
            or updating their namespace will see an error with permission issues - "is not authorized to perform: redshift:GetResourcePolicy",
            as Read handler is trying to hit getResourcePolicy APIs to get namespaceResourcePolicy details.*/
            if(!containsResourcePolicy && e.statusCode() == GET_RESOURCE_POLICY_ERR_STATUS_CODE &&
                    e.awsErrorDetails().errorMessage().contains(GET_RESOURCE_POLICY_ERROR)) {
                logger.log(String.format("RedshiftException:  %s", e.getMessage()));
                return noOpNamespaceResourcePoliy(awsRequest);
            } else {
                throw new CfnGeneralServiceException(e);
            }
        } catch (SdkClientException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        logger.log(String.format("%s  resource policy has successfully been read.", ResourceModel.TYPE_NAME));
        return getResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            final GetNamespaceResponse getNamespaceResponse) {
        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(getNamespaceResponse));
    }

    private ProgressEvent<ResourceModel, CallbackContext> getNamespaceErrorHandler(final GetNamespaceRequest getNamespaceRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        return errorHandler(exception);
    }

    /**
     * No Op method for assigning empty resource policy for Namespace create response.
     * @param awsRequest the aws service request to describe a resource
     * @return GetResourcePolicyResponse
     */
    private GetResourcePolicyResponse noOpNamespaceResourcePoliy(final GetResourcePolicyRequest awsRequest) {
        ResourcePolicy resourcePolicy = ResourcePolicy.builder()
                .resourceArn(awsRequest.resourceArn())
                .policy("")
                .build();
        return GetResourcePolicyResponse.builder().resourcePolicy(resourcePolicy).build();
    }
}
