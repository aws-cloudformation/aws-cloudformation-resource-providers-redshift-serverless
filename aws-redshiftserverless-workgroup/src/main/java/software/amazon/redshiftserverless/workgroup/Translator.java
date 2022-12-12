package software.amazon.redshiftserverless.workgroup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListWorkgroupsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListWorkgroupsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.TagResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UntagResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Request to create a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static CreateWorkgroupRequest translateToCreateRequest(final ResourceModel model) {
        return CreateWorkgroupRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .namespaceName(model.getNamespaceName())
                .baseCapacity(model.getBaseCapacity())
                .enhancedVpcRouting(model.getEnhancedVpcRouting())
                .configParameters(translateToSdkConfigParameters(model.getConfigParameters()))
                .securityGroupIds(model.getSecurityGroupIds())
                .subnetIds(model.getSubnetIds())
                .publiclyAccessible(model.getPubliclyAccessible())
                .tags(translateToSdkTags(model.getTags()))
                .port(model.getPort())
                .build();
    }

    /**
     * Request to read a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static GetWorkgroupRequest translateToReadRequest(final ResourceModel model) {
        return GetWorkgroupRequest.builder().
                workgroupName(model.getWorkgroupName()).
                build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final GetWorkgroupResponse awsResponse) {
        return ResourceModel.builder()
                .workgroupName(awsResponse.workgroup().workgroupName())
                .namespaceName(awsResponse.workgroup().namespaceName())
                .baseCapacity(awsResponse.workgroup().baseCapacity())
                .enhancedVpcRouting(awsResponse.workgroup().enhancedVpcRouting())
                .configParameters(translateToModelConfigParameters(awsResponse.workgroup().configParameters()))
                .securityGroupIds(awsResponse.workgroup().securityGroupIds())
                .subnetIds(awsResponse.workgroup().subnetIds())
                .publiclyAccessible(awsResponse.workgroup().publiclyAccessible())
                .workgroup(Workgroup.builder()
                        .workgroupId(awsResponse.workgroup().workgroupId())
                        .workgroupArn(awsResponse.workgroup().workgroupArn())
                        .workgroupName(awsResponse.workgroup().workgroupName())
                        .namespaceName(awsResponse.workgroup().namespaceName())
                        .baseCapacity(awsResponse.workgroup().baseCapacity())
                        .enhancedVpcRouting(awsResponse.workgroup().enhancedVpcRouting())
                        .configParameters(translateToModelConfigParameters(awsResponse.workgroup().configParameters()))
                        .securityGroupIds(awsResponse.workgroup().securityGroupIds())
                        .subnetIds(awsResponse.workgroup().subnetIds())
                        .status(awsResponse.workgroup().statusAsString())
                        .endpoint(translateToModelEndpoint(awsResponse.workgroup().endpoint()))
                        .publiclyAccessible(awsResponse.workgroup().publiclyAccessible())
                        .creationDate(Objects.toString(awsResponse.workgroup().creationDate()))
                        .port(awsResponse.workgroup().port())
                        .build())
                .build();
    }

    /**
     * Request to delete a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static DeleteWorkgroupRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteWorkgroupRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .build();
    }

    /**
     * Request to update properties of a previously created resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to modify a resource
     */
    static UpdateWorkgroupRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdateWorkgroupRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .baseCapacity(model.getBaseCapacity())
                .enhancedVpcRouting(model.getEnhancedVpcRouting())
                .configParameters(translateToSdkConfigParameters(model.getConfigParameters()))
                .publiclyAccessible(model.getPubliclyAccessible())
                .subnetIds(model.getSubnetIds())
                .securityGroupIds(model.getSecurityGroupIds())
                .port(model.getPort())
                .build();
    }

    /**
     * Request to list resources
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListWorkgroupsRequest translateToListRequest(final String nextToken) {
        return ListWorkgroupsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param awsResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListResponse(final ListWorkgroupsResponse awsResponse) {
        return awsResponse.workgroups()
                .stream()
                .map(workgroup -> ResourceModel.builder()
                        .workgroupName(workgroup.workgroupName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Request to read tags for a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to update tags of a resource
     */
    static ListTagsForResourceRequest translateToReadTagsRequest(final ResourceModel model) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(model.getWorkgroup().getWorkgroupArn())
                .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @param model       the resource model contained the current resource info
     * @return awsRequest the aws service request to update tags of a resource
     */
    static ResourceModel translateFromReadTagsResponse(final ListTagsForResourceResponse awsResponse,
                                                       final ResourceModel model) {
        return model.toBuilder()
                .tags(translateToModelTags(awsResponse.tags()))
                .build();
    }

    /**
     * Request to update tags for a resource
     *
     * @param desiredResourceState the resource model request to update tags
     * @param currentResourceState the resource model request to delete tags
     * @return awsRequest the aws service request to update tags of a resource
     */
    static UpdateTagsRequest translateToUpdateTagsRequest(final ResourceModel desiredResourceState,
                                                          final ResourceModel currentResourceState) {
        String resourceArn = currentResourceState.getWorkgroup().getWorkgroupArn();

        List<Tag> toBeCreatedTags = desiredResourceState.getTags() == null ? Collections.emptyList() : desiredResourceState.getTags()
                .stream()
                .filter(tag -> currentResourceState.getTags() == null || !currentResourceState.getTags().contains(tag))
                .collect(Collectors.toList());

        List<Tag> toBeDeletedTags = currentResourceState.getTags() == null ? Collections.emptyList() : currentResourceState.getTags()
                .stream()
                .filter(tag -> desiredResourceState.getTags() == null || !desiredResourceState.getTags().contains(tag))
                .collect(Collectors.toList());

        return UpdateTagsRequest.builder()
                .createNewTagsRequest(TagResourceRequest.builder()
                        .tags(translateToSdkTags(toBeCreatedTags))
                        .resourceArn(resourceArn)
                        .build())
                .deleteOldTagsRequest(UntagResourceRequest.builder()
                        .tagKeys(toBeDeletedTags
                                .stream()
                                .map(Tag::getKey)
                                .collect(Collectors.toList()))
                        .resourceArn(resourceArn)
                        .build())
                .build();
    }

    private static software.amazon.awssdk.services.redshiftserverless.model.Tag translateToSdkTag(Tag tag) {
        return GSON.fromJson(GSON.toJson(tag), software.amazon.awssdk.services.redshiftserverless.model.Tag.class);
    }

    private static List<software.amazon.awssdk.services.redshiftserverless.model.Tag> translateToSdkTags(final List<Tag> tags) {
        return tags == null ? null : tags
                .stream()
                .map(Translator::translateToSdkTag)
                .collect(Collectors.toList());
    }

    private static Tag translateToModelTag(software.amazon.awssdk.services.redshiftserverless.model.Tag tag) {
        return GSON.fromJson(GSON.toJson(tag), Tag.class);
    }

    private static List<Tag> translateToModelTags(Collection<software.amazon.awssdk.services.redshiftserverless.model.Tag> tags) {
        return tags == null ? null : tags
                .stream()
                .map(Translator::translateToModelTag)
                .collect(Collectors.toList());
    }

    private static software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter translateToSdkConfigParameter(ConfigParameter configParameter) {
        return GSON.fromJson(GSON.toJson(configParameter), software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter.class);
    }

    private static List<software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter> translateToSdkConfigParameters(Collection<ConfigParameter> configParameters) {
        return configParameters == null ? null : configParameters
                .stream()
                .map(Translator::translateToSdkConfigParameter)
                .collect(Collectors.toList());
    }

    private static ConfigParameter translateToModelConfigParameter(software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter configParameter) {
        return GSON.fromJson(GSON.toJson(configParameter), ConfigParameter.class);
    }

    private static Set<ConfigParameter> translateToModelConfigParameters(Collection<software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter> configParameters) {
        return configParameters == null ? null : configParameters
                .stream()
                .map(Translator::translateToModelConfigParameter)
                .collect(Collectors.toSet());
    }

    private static Endpoint translateToModelEndpoint(software.amazon.awssdk.services.redshiftserverless.model.Endpoint endpoint) {
        return GSON.fromJson(GSON.toJson(endpoint), Endpoint.class);
    }
}
