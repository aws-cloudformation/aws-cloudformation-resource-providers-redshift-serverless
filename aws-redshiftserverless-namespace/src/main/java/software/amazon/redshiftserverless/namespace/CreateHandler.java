package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.redshift.model.InvalidPolicyException;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyResponse;
import software.amazon.awssdk.services.redshift.model.RedshiftException;
import software.amazon.awssdk.services.redshift.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
import software.amazon.awssdk.services.redshift.model.ResourcePolicy;
import software.amazon.awssdk.services.redshiftserverless.model.CreateNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final ProxyClient<RedshiftClient> redshiftProxyClient,
        final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> {
                return proxy.initiate("AWS-RedshiftServerless-Namespace::Create", proxyClient, progress.getResourceModel(), callbackContext)
                    .translateToServiceRequest(Translator::translateToCreateRequest)
                    .makeServiceCall(this::createNamespace)
                    .stabilize((_awsRequest, _awsResponse, _client, _model, _context) -> isNamespaceActive(_client, _model, _context))
                    .handleError(this::createNamespaceErrorHandler)
                    .done((_request, _response, _client, _model, _context) -> {
                        callbackContext.setNamespaceArn(_response.namespace().namespaceArn());
                        return ProgressEvent.progress(_model, callbackContext);
                    });
            })
            .then(progress -> {
                if (progress.getResourceModel().getNamespaceResourcePolicy() != null) {
                    return proxy.initiate("AWS-Redshift-ResourcePolicy::Put", redshiftProxyClient, progress.getResourceModel(), callbackContext)
                        .translateToServiceRequest(resourceModelRequest -> Translator.translateToPutResourcePolicy(resourceModelRequest, callbackContext.getNamespaceArn(), logger))
                        .makeServiceCall(this::putNamespaceResourcePolicy)
                        .progress();
                }
                return progress;
            })
            .then(progress ->
                new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, redshiftProxyClient, logger)
            );
    }

    private CreateNamespaceResponse createNamespace(final CreateNamespaceRequest createNamespaceRequest,
                                                    final ProxyClient<RedshiftServerlessClient> proxyClient) {
        CreateNamespaceResponse createNamespaceResponse = null;

        logger.log(String.format("createNamespace for %s", createNamespaceRequest.namespaceName()));
        createNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(createNamespaceRequest, proxyClient.client()::createNamespace);

        logger.log(String.format("%s %s successfully created.", ResourceModel.TYPE_NAME, createNamespaceRequest.namespaceName()));
        return createNamespaceResponse;
    }

    private PutResourcePolicyResponse putNamespaceResourcePolicy(
            final PutResourcePolicyRequest putRequest,
            final ProxyClient<RedshiftClient> proxyClient) {
        PutResourcePolicyResponse putResponse = null;

        try {
            logger.log(String.format("%s putResourcePolicy.", ResourceModel.TYPE_NAME));
            putResponse = proxyClient.injectCredentialsAndInvokeV2(putRequest, proxyClient.client()::putResourcePolicy);
        } catch (ResourceNotFoundException e){
            throw new CfnNotFoundException(e);
        } catch (UnsupportedOperationException e) {
            logger.log(e);
            return noOpNamespaceResourcePoliy(putRequest);
        } catch (InvalidPolicyException | InvalidParameterValueException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        } catch (SdkClientException | RedshiftException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s successfully put resource policy.", putRequest.resourceArn()));
        return putResponse;
    }

    /**
     * No Op method for assigning empty resource policy for Namespace create response.
     * @param awsRequest the aws service request to describe a resource
     * @return GetResourcePolicyResponse
     */
    private PutResourcePolicyResponse noOpNamespaceResourcePoliy(final PutResourcePolicyRequest awsRequest) {
        ResourcePolicy resourcePolicy = ResourcePolicy.builder()
                .resourceArn(awsRequest.resourceArn())
                .policy(null)
                .build();

        return PutResourcePolicyResponse.builder().resourcePolicy(resourcePolicy).build();
    }

    private ProgressEvent<ResourceModel, CallbackContext> createNamespaceErrorHandler(final CreateNamespaceRequest createNamespaceRequest,
                                                                                      final Exception exception,
                                                                                      final ProxyClient<RedshiftServerlessClient> client,
                                                                                      final ResourceModel model,
                                                                                      final CallbackContext context) {
        return errorHandler(exception);
    }
}
