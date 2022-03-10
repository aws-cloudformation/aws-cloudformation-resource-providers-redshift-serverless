package software.amazon.redshiftserverless.namespace;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import com.amazonaws.SdkClientException;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.RedshiftArcadiaCoralClient;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.InternalServerException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ResourceNotFoundException;
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

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftArcadiaCoralClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return proxy.initiate("AWS-RedshiftServerless-Namespace::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::getNamespace)
                .done(this::constructResourceModelFromResponse);
    }

    private GetNamespaceResponse getNamespace(final GetNamespaceRequest getNamespaceRequest,
                                               final ProxyClient<RedshiftArcadiaCoralClient> proxyClient) {
        GetNamespaceResponse getNamespaceResponse = null;
        try {
            logger.log(String.format("%s %s getNamespaces.", ResourceModel.TYPE_NAME,
                    getNamespaceRequest.namespaceName()));
            getNamespaceResponse = proxyClient.injectCredentialsAndInvokeV2(getNamespaceRequest, proxyClient.client()::getNamespace);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(e);
        } catch (SdkClientException | AwsServiceException e) {
            throw new CfnGeneralServiceException(e);
        }
        logger.log(String.format("%s %s has successfully been read.", ResourceModel.TYPE_NAME, getNamespaceRequest.namespaceName()));
        return getNamespaceResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            final GetNamespaceResponse getNamespaceResponse) {
        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(getNamespaceResponse));
    }
}
