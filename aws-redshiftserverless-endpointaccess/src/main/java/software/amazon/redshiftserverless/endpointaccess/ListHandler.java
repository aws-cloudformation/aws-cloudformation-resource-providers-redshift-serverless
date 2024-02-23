package software.amazon.redshiftserverless.endpointaccess;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.ConflictException;
import software.amazon.awssdk.services.redshiftserverless.model.ListEndpointAccessRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
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
        final ResourceModel resourceModel = request.getDesiredResourceState();
        
        ListEndpointAccessRequest awsRequest = Translator.translateToListEndpointAccessRequest(resourceModel, request.getNextToken());
        ListEndpointAccessResponse awsResponse = listEndpointAccess(awsRequest, proxy);
        List<ResourceModel> models = Translator.translateFromListEndpointAccessResponse(awsResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(awsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private ListEndpointAccessResponse listEndpointAccess(final ListEndpointAccessRequest awsRequest,
                                                  final AmazonWebServicesClientProxy proxy) {
        ListEndpointAccessResponse awsResponse;
        try {
            awsResponse = proxy.injectCredentialsAndInvokeV2(awsRequest, ClientBuilder.getClient()::listEndpointAccess);

        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(e);

        } catch (final InternalServerException e) {
            throw new CfnInternalFailureException(e);

        } catch(final ConflictException e) {
            throw new CfnResourceConflictException(e);

        } catch(final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);

        }

        logger.log(String.format("%s has successfully been listed.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }
}
