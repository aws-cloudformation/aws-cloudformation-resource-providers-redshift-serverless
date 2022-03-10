package software.amazon.redshiftserverless.namespace;

import com.amazonaws.SdkClientException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.DeleteNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.DeleteNamespaceResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.InternalServerException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftArcadiaCoralClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> {
                    if (!callbackContext.getCallBackForDelete()) {
                        callbackContext.setCallBackForDelete(true);
                        logger.log ("In Delete, Initiate a CallBack Delay of "+CALLBACK_DELAY_SECONDS+" seconds");
                        progress = ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);

                    }
                    return progress;
                })
                .then(progress ->
                    proxy.initiate("AWS-RedshiftServerless-Namespace::Delete", proxyClient, model, callbackContext)
                            .translateToServiceRequest(Translator::translateToDeleteRequest)
                            .makeServiceCall(this::deleteNamespace)
                            .stabilize((_awsRequest, _awsResponse, _client, _model, _context) -> isNamespaceActiveAfterDelete(_client, _model, _context))
                            .done(deleteNamespaceResponse -> {
                                logger.log(String.format("%s %s deleted.",ResourceModel.TYPE_NAME, model.getNamespaceName()));
                                return ProgressEvent.defaultSuccessHandler(null);
                            })
                );
    }

    private DeleteNamespaceResponse deleteNamespace(final DeleteNamespaceRequest deleteNamespaceRequest,
                                                    final ProxyClient<RedshiftArcadiaCoralClient> proxyClient) {
        DeleteNamespaceResponse deleteNamespaceResponse = null;
        try {
            logger.log(String.format("%s %s deleteNamespace", ResourceModel.TYPE_NAME, deleteNamespaceRequest.namespaceName()));
            logger.log("The endpoint before calling is: " + proxyClient.client().toString());
            deleteNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(deleteNamespaceRequest, proxyClient.client()::deleteNamespace);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(e);
        } catch (SdkClientException | AwsServiceException e) {
            throw new CfnGeneralServiceException(e);
        }

        logger.log(String.format("%s %s successfully deleted.", ResourceModel.TYPE_NAME, deleteNamespaceRequest.namespaceName()));
        return deleteNamespaceResponse;
    }
}
