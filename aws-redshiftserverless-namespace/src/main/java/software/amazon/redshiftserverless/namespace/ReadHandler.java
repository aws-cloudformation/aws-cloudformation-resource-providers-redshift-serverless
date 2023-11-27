package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.*;
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

import java.lang.UnsupportedOperationException;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final ProxyClient<RedshiftClient> redshiftProxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

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
                    progress = proxy.initiate("AWS-Redshift-ResourcePolicy::Get", redshiftProxyClient, progress.getResourceModel(), callbackContext)
                            .translateToServiceRequest(resourceModelRequest -> Translator.translateToGetResourcePolicy(resourceModelRequest, callbackContext.getNamespaceArn()))
                            .makeServiceCall(this::getNamespaceResourcePolicy)
                            .done((_request, _response, _client, _model, _context) -> {
                                _model.setNamespaceResourcePolicy(Translator.convertStringToJson(_response.resourcePolicy().policy(), logger));
                                return ProgressEvent.defaultSuccessHandler(_model);
                            });
                    return progress;
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
            ResourcePolicy resourcePolicy = ResourcePolicy.builder()
                    .resourceArn(awsRequest.resourceArn())
                    .policy("")
                    .build();
            return GetResourcePolicyResponse.builder().resourcePolicy(resourcePolicy).build();
        } catch (InvalidPolicyException | UnsupportedOperationException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        } catch (SdkClientException | RedshiftException e) {
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
}
