package software.amazon.redshiftserverless.snapshot;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
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
        ListSnapshotsRequest listSnapshotsRequest = Translator.translateToListRequest(request.getNextToken());
        ListSnapshotsResponse listSnapshotsResponse = listSnapshots(listSnapshotsRequest, proxy);
        final List<ResourceModel> models = Translator.translateFromListRequest(listSnapshotsResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(listSnapshotsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private ListSnapshotsResponse listSnapshots(final ListSnapshotsRequest listSnapshotsRequest,
                                                  final AmazonWebServicesClientProxy proxy) {
        ListSnapshotsResponse listSnapshotsResponse;
        try {
            listSnapshotsResponse = proxy.injectCredentialsAndInvokeV2(listSnapshotsRequest, ClientBuilder.getClient()::listSnapshots);
        } catch (final ResourceNotFoundException e){
            throw new CfnNotFoundException(e);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(e);
        } catch (final ValidationException e){
            throw new CfnInvalidRequestException(e);
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(e);
        }
        logger.log(String.format("%s has successfully been listed.", ResourceModel.TYPE_NAME));
        return listSnapshotsResponse;
    }
}
