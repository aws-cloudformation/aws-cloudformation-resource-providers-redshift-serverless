package software.amazon.redshiftserverless.workgroup;

import software.amazon.awssdk.services.redshiftserverless.RedshiftServerlessClient;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.InternalServerException;
import software.amazon.awssdk.services.redshiftserverless.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshiftserverless.model.ValidationException;
import software.amazon.awssdk.services.redshiftserverless.model.WorkgroupStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;

import static software.amazon.cloudformation.proxy.ProgressEvent.progress;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<RedshiftServerlessClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        return proxy.initiate("AWS-RedshiftServerless-Workgroup::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::readWorkgroup)
                .handleError(this::readWorkgroupErrorHandler)
                .done(awsResponse -> getProgressEventFromReadWorkgroupResponse(awsResponse, callbackContext));
    }

    private GetWorkgroupResponse readWorkgroup(final GetWorkgroupRequest awsRequest,
                                               final ProxyClient<RedshiftServerlessClient> proxyClient) {
        GetWorkgroupResponse awsResponse;
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::getWorkgroup);

            logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
            return awsResponse;
    }

    /**
     * We used to return operationStatus.SUCCESS for all workgroup statuses,
     * including creating, deleting, modifying.
     *
     * When CFN contract test checks if the resource has been deleted by calling ReadHandler,
     * and when the workgroup == DELETING, we should have returned in_progress to indicate the
     * deletion is not finished, and contract test can't start creating the same resource again.
     *
     * Same scenario would cause customer issues too.
     *
     * @param getWorkgroupResponse
     * @param ctx
     * @return
     */
    private static ProgressEvent<ResourceModel, CallbackContext> getProgressEventFromReadWorkgroupResponse(GetWorkgroupResponse getWorkgroupResponse,
                                                                                                    CallbackContext ctx) {
        ResourceModel workgroupModel = Translator.translateFromReadResponse(getWorkgroupResponse);
        List<WorkgroupStatus> inProgressWorkgroupStatuses = Arrays.asList(
                WorkgroupStatus.CREATING,
                WorkgroupStatus.DELETING,
                WorkgroupStatus.MODIFYING
        );
        boolean isInProgress = inProgressWorkgroupStatuses.contains(getWorkgroupResponse.workgroup().status());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent = progress(workgroupModel, ctx);
        progressEvent.setStatus(isInProgress ? OperationStatus.IN_PROGRESS : OperationStatus.SUCCESS);
        return progressEvent;
    }

    private ProgressEvent<ResourceModel, CallbackContext> readWorkgroupErrorHandler(final GetWorkgroupRequest awsRequest,
                                                                                    final Exception exception,
                                                                                    final ProxyClient<RedshiftServerlessClient> client,
                                                                                    final ResourceModel model,
                                                                                    final CallbackContext context) {
        if (exception instanceof ResourceNotFoundException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
        } else if (exception instanceof ValidationException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
        } else if (exception instanceof InternalServerException) {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InternalFailure);
        } else {
            return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
        }
    }
}
