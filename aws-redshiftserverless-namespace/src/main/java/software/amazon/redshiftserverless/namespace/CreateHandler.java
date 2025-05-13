package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.redshift.model.InvalidPolicyException;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyResponse;
import software.amazon.awssdk.services.redshift.model.RedshiftException;
import software.amazon.awssdk.services.redshift.model.ResourceNotFoundException;
import software.amazon.awssdk.services.redshift.model.UnsupportedOperationException;
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
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.Collections;
import java.util.Optional;
import java.util.List;


public class CreateHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<RedshiftServerlessClient> proxyClient,
        final ProxyClient<RedshiftClient> redshiftProxyClient,
        final ProxyClient<SecretsManagerClient> secretsManagerProxyClient,
        final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> {
                List<Tag>mergedTags = TagHelper.convertToTagList(
                        TagHelper.mergeTags(
                                request,
                                TagHelper.convertToMap(request.getDesiredResourceState().getTags()),
                                request.getSystemTags(),
                                TagHelper.convertToMap(progress.getResourceModel().getTags())
                        ));
                return proxy.initiate("AWS-RedshiftServerless-Namespace::Create", proxyClient, progress.getResourceModel(), callbackContext)
                        .translateToServiceRequest((model) -> Translator.translateToCreateRequest(model, mergedTags))
                        .makeServiceCall(this::createNamespace)
                        .stabilize(this::isNamespaceActive)
                        .handleError(this::defaultErrorHandler)
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
            .then(progress -> {
                for (SnapshotCopyConfiguration snapshotCopyConfiguration : Optional.ofNullable(progress.getResourceModel().getSnapshotCopyConfigurations()).orElse(Collections.emptyList())) {
                    progress = progress.then(nextProgress -> proxy.initiate(String.format("AWS-RedshiftServerless-Namespace::CreateSnapshotCopyConfiguration::%s", snapshotCopyConfiguration.getDestinationRegion()), proxyClient, nextProgress.getResourceModel(), callbackContext)
                            .translateToServiceRequest((model) -> Translator.translateToCreateSnapshotCopyConfigurationRequest(model, snapshotCopyConfiguration))
                            .makeServiceCall(this::createSnapshotCopyConfiguration)
                            .handleError(this::defaultErrorHandler)
                            .progress());
                }
               return progress;
            })
            .then(progress ->
                new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, redshiftProxyClient, secretsManagerProxyClient, logger)
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
        } catch (InvalidPolicyException | UnsupportedOperationException | InvalidParameterValueException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        } catch (SdkClientException | RedshiftException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s successfully put resource policy.", putRequest.resourceArn()));
        return putResponse;
    }
}
