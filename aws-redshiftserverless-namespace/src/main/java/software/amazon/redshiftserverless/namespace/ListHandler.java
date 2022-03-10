package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.InternalServerException;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ListNamespacesResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ListNamespacesRequest;
import java.util.List;

public class ListHandler extends BaseHandler<CallbackContext> {
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        this.logger = logger;
        ListNamespacesRequest listNamespacesRequest = Translator.translateToListRequest(request.getNextToken());
        ListNamespacesResponse listNamespacesResponse = listNamespaces(listNamespacesRequest, proxy);
        final List<ResourceModel> models = Translator.translateFromListRequest(listNamespacesResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .nextToken(listNamespacesResponse.nextToken())
            .status(OperationStatus.SUCCESS)
            .build();
    }

    private ListNamespacesResponse listNamespaces(final ListNamespacesRequest listNamespacesRequest,
                                                  final AmazonWebServicesClientProxy proxy) {
        ListNamespacesResponse listNamespacesResponse;
        try {
            listNamespacesResponse = proxy.injectCredentialsAndInvokeV2(listNamespacesRequest, ClientBuilder.getClient()::listNamespaces);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (final ValidationException e){
            throw new CfnInvalidRequestException(e);
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(e);
        }
        logger.log(String.format("%s has successfully been listed.", ResourceModel.TYPE_NAME));
        return listNamespacesResponse;
    }
}
