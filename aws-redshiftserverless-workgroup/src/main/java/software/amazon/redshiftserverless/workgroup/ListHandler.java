package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ListWorkgroupsRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ListWorkgroupsResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
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
        ListWorkgroupsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());
        ListWorkgroupsResponse awsResponse =  listWorkgroups(awsRequest, proxy);
        List<ResourceModel> models = Translator.translateFromListRequest(awsResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .nextToken(awsResponse.nextToken())
            .status(OperationStatus.SUCCESS)
            .build();
    }

    private ListWorkgroupsResponse listWorkgroups(final ListWorkgroupsRequest awsRequest,
                                                                  final AmazonWebServicesClientProxy proxy) {
        ListWorkgroupsResponse awsResponse;
        try {
            awsResponse = proxy.injectCredentialsAndInvokeV2(awsRequest, ClientBuilder.getClient()::listWorkgroups);
        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(e);
        } catch (final CfnGeneralServiceException e) {
            throw new CfnGeneralServiceException(e);
        }

        logger.log(String.format("%s has successfully been listed.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }
}
