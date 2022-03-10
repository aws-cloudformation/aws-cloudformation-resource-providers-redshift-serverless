package software.amazon.redshiftserverless.namespace;

import com.amazonaws.SdkClientException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.CreateNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.CreateNamespaceResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.InternalServerException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
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
        final ProxyClient<RedshiftArcadiaCoralClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-RedshiftServerless-Namespace::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                        .translateToServiceRequest(Translator::translateToCreateRequest)
                        .makeServiceCall(this::createNamespace)
                        .progress()
            )
            .then(progress ->
                new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger)
            );
    }

    private CreateNamespaceResponse createNamespace(final CreateNamespaceRequest createNamespaceRequest,
                                                    final ProxyClient<RedshiftArcadiaCoralClient> proxyClient) {
        CreateNamespaceResponse createNamespaceResponse = null;
        try {
            logger.log(String.format("createNamespace for %s", createNamespaceRequest.namespaceName()));
            logger.log("The endpoint before calling is: " + proxyClient.client().toString());
            createNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(createNamespaceRequest, proxyClient.client()::createNamespace);

        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(e);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (SdkClientException | AwsServiceException e) {
            throw new CfnGeneralServiceException(e);
        }

        logger.log(String.format("%s %s successfully created.", ResourceModel.TYPE_NAME, createNamespaceRequest.namespaceName()));
        return createNamespaceResponse;
    }
}
