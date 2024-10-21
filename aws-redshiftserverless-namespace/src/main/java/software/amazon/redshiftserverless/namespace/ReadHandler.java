package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.*;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class ReadHandler extends BaseHandlerStd {
    private final String GET_RESOURCE_POLICY_ERROR = "not authorized to perform: redshift:GetResourcePolicy";
    private final Integer GET_RESOURCE_POLICY_ERR_STATUS_CODE = 403;
    private final String RESOURCE_POLICY_UNSUPPORTED_ERROR = "The resource policy feature isn't supported";
    private final Integer RESOURCE_POLICY_UNSUPPORTED_ERR_STATUS_CODE = 400;
    private boolean containsResourcePolicy = false;
    private boolean containsSnapshotCopyConfigurations = false;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final ProxyClient<RedshiftClient> redshiftProxyClient,
        final ProxyClient<SecretsManagerClient> secretsManagerProxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        /*
        containsResourcePolicy will be true if NamespaceResourcePolicy property is included in the template.
        This attribute will be used to decide if "not authorized to perform: redshift:GetResourcePolicy" errors
        in Read handler should be suppressed or not.
         */
        containsResourcePolicy = model.getNamespaceResourcePolicy() != null;
        containsSnapshotCopyConfigurations = model.getSnapshotCopyConfigurations() != null;

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> {
                    progress = proxy.initiate("AWS-RedshiftServerless-Namespace::Read", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .makeServiceCall(this::getNamespace)
                        .handleError(this::defaultErrorHandler)
                        .done(awsResponse -> {
                            callbackContext.setNamespaceArn(awsResponse.namespace().namespaceArn());
                            return ProgressEvent.progress(Translator.translateFromReadResponse(awsResponse), callbackContext);
                        });
                    return progress;
                })
                .then(progress -> {
                    progress = proxy.initiate("AWS-RedshiftServerless-Namespace::Read::ReadTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToReadTagsRequest)
                        .makeServiceCall(this::readTags)
                        .handleError(this::defaultErrorHandler)
                        .done((_request, _response, _client, _model, _context) -> {
                            return ProgressEvent.progress(Translator.translateFromReadTagsResponse(_response, _model), _context);
                        });
                    return progress;
                })
                .then(progress -> {
                    return proxy.initiate("AWS-Redshift-ResourcePolicy::Get", redshiftProxyClient, progress.getResourceModel(), callbackContext)
                        .translateToServiceRequest(resourceModelRequest -> Translator.translateToGetResourcePolicy(resourceModelRequest, callbackContext.getNamespaceArn()))
                        .makeServiceCall(this::getNamespaceResourcePolicy)
                        .done((_request, _response, _client, _model, _context) -> {
                            _model.setNamespaceResourcePolicy(Translator.convertStringToJson(_response.resourcePolicy().policy(), logger));
                            return ProgressEvent.progress(_model, _context);
                        });
                })
                .then(progress -> {
                    return proxy.initiate("AWS-RedshiftServerless-Namespace::SnapshotCopyConfigurations::List", proxyClient, progress.getResourceModel(), callbackContext)
                            .translateToServiceRequest(Translator::translateToListSnapshotCopyConfigurationsRequest)
                            .makeServiceCall(this::listSnapshotCopyConfigurations)
                            .handleError(this::listSnapshotCopyConfigurationErrorHandler)
                            .done((_request, _response, _client, _model, _context) -> {
                                _model.setSnapshotCopyConfigurations(Translator.translateToSnapshotCopyConfigurations(_response.snapshotCopyConfigurations()));
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

        /**
         * Customers are still seeing 403 w/ different error message that we checked earlier, as a result we didn't
         * catch the exception for these customers.
         * This commit catches all the RedshiftException if resource policy is not in the CFN template,
         * which might catch more exceptions than intended, we're doing this to unblock existing customers who
         * don't specify the resource policy at all.
         *
         * We'll see if this all-inclusive catch makes sense, then make changes if needed
         */
          if(!containsResourcePolicy) {
              logger.log(String.format("Template does not have resource policy, " +
                      "InvalidPolicyException or UnsupportedOpoerationException: %s", e.getMessage()));
              return noOpNamespaceResourcePoliy(awsRequest);
          } else {
              throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
          }
        } catch (RedshiftException e) {
            /* This error handling is required for backward compatibility. Without this exception handling, existing customers creating
            or updating their namespace will see an error with permission issues - "is not authorized to perform: redshift:GetResourcePolicy",
            as Read handler is trying to hit getResourcePolicy APIs to get namespaceResourcePolicy details.*/

            /**
             * Customers are still seeing 403 w/ different error message that we checked earlier, as a result we didn't
             * catch the exception for these customers.
             * This commit catches all the RedshiftException if resource policy is not in the CFN template,
             * which might catch more exceptions than intended, we're doing this to unblock existing customers who
             * don't specify the resource policy at all.
             *
             * We'll see if this all-inclusive catch makes sense, then make changes if needed
             */
            if(!containsResourcePolicy) {
                logger.log(String.format("Template does not have resource policy, RedshiftException: %s", e.getMessage()));
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

    private ProgressEvent<ResourceModel, CallbackContext> listSnapshotCopyConfigurationErrorHandler(final ListSnapshotCopyConfigurationsRequest request,
                                                                                                    final Exception exception,
                                                                                                    final ProxyClient<RedshiftServerlessClient> client,
                                                                                                    final ResourceModel model,
                                                                                                    final CallbackContext context) {
        if (exception instanceof ValidationException) {
            // ValidationException is thrown when the feature is not enabled in a region
            logger.log(String.format("CRC feature is not enabled for this region: %s", exception.getMessage()));
            return ProgressEvent.defaultSuccessHandler(model);
        } else if (!containsSnapshotCopyConfigurations) {
            // This error handling is required for backward compatibility. Without this exception handling,
            // existing customers creating or updating their namespace will see an error with permission issues
            logger.log(String.format("Template does not have snapshot copy configurations, RedshiftServerlessException: %s", exception.getMessage()));
            return ProgressEvent.defaultSuccessHandler(model);
        }

        // Otherwise perform the standard error handling
        return errorHandler(exception);
    }

    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            final GetNamespaceResponse getNamespaceResponse) {
        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(getNamespaceResponse));
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
