package software.amazon.redshiftserverless.namespace;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.InternalServerException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.UpdateNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.UpdateNamespaceResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftArcadiaCoralClient> proxyClient,
        final Logger logger) {

        this.logger = logger;


        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                    proxy.initiate("AWS-RedshiftServerless-Namespace::Update::first", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToUpdateRequest)
                        .makeServiceCall(this::updateNamespace)
                        .stabilize((_awsRequest, _awsResponse, _client, _model, _context) -> isNamespaceActive(_client, _model, _context))
                        .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateNamespaceResponse updateNamespace(final UpdateNamespaceRequest updateNamespaceRequest,
                                                    final ProxyClient<RedshiftArcadiaCoralClient> proxyClient) {
        UpdateNamespaceResponse updateNamespaceResponse = null;
        try {
            logger.log(String.format("%s %s updateNamespace.", ResourceModel.TYPE_NAME, updateNamespaceRequest.namespaceName()));
            updateNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(updateNamespaceRequest, proxyClient.client()::updateNamespace);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(e);
        } catch (SdkClientException | AwsServiceException e) {
            throw new CfnGeneralServiceException(e);
        }
        logger.log(String.format("%s %s update namespace issued.", ResourceModel.TYPE_NAME,
                updateNamespaceRequest.namespaceName()));
        return updateNamespaceResponse;
    }
}
