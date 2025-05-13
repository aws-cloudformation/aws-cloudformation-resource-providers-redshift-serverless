package software.amazon.redshiftserverless.workgroup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetWorkgroupResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListWorkgroupsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListWorkgroupsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.RestoreFromRecoveryPointRequest;
import software.amazon.awssdk.services.redshiftserverless.model.TagResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UntagResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateWorkgroupRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


import static software.amazon.redshiftserverless.workgroup.TagHelper.convertToTagList;
import static software.amazon.redshiftserverless.workgroup.TagHelper.generateTagsToRemove;
import static software.amazon.redshiftserverless.workgroup.TagHelper.generateTagsToAdd;

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
    static CreateWorkgroupRequest translateToCreateRequest(final ResourceModel model,  final List<Tag>mergedTags) {
        return CreateWorkgroupRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .namespaceName(model.getNamespaceName())
                .baseCapacity(model.getBaseCapacity())
                .maxCapacity(model.getMaxCapacity())
                .enhancedVpcRouting(model.getEnhancedVpcRouting())
                .configParameters(translateToSdkConfigParameters(model.getConfigParameters()))
                .securityGroupIds(model.getSecurityGroupIds())
                .subnetIds(model.getSubnetIds())
                .pricePerformanceTarget(translateToSdkPerformanceTarget(model.getPricePerformanceTarget()))
                .publiclyAccessible(model.getPubliclyAccessible())
                .tags(translateToSdkTags(mergedTags))
                .port(model.getPort())
                .trackName(model.getTrackName())
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
                .baseCapacity(awsResponse.workgroup().baseCapacity() != null ? awsResponse.workgroup().baseCapacity() : -1)
                .maxCapacity(awsResponse.workgroup().maxCapacity())
                .enhancedVpcRouting(awsResponse.workgroup().enhancedVpcRouting())
                .configParameters(translateToModelConfigParameters(awsResponse.workgroup().configParameters()))
                .securityGroupIds(awsResponse.workgroup().securityGroupIds())
                .subnetIds(awsResponse.workgroup().subnetIds())
                .pricePerformanceTarget(translateToModelPerformanceTarget(awsResponse.workgroup().pricePerformanceTarget()))
                .publiclyAccessible(awsResponse.workgroup().publiclyAccessible())
                .port(awsResponse.workgroup().endpoint().port())
                .trackName(awsResponse.workgroup().trackName())
                .workgroup(Workgroup.builder()
                        .workgroupId(awsResponse.workgroup().workgroupId())
                        .workgroupArn(awsResponse.workgroup().workgroupArn())
                        .workgroupName(awsResponse.workgroup().workgroupName())
                        .namespaceName(awsResponse.workgroup().namespaceName())
                        .baseCapacity(awsResponse.workgroup().baseCapacity() != null ? awsResponse.workgroup().baseCapacity() : -1)
                        .maxCapacity(awsResponse.workgroup().maxCapacity())
                        .enhancedVpcRouting(awsResponse.workgroup().enhancedVpcRouting())
                        .configParameters(translateToModelConfigParameters(awsResponse.workgroup().configParameters()))
                        .securityGroupIds(awsResponse.workgroup().securityGroupIds())
                        .subnetIds(awsResponse.workgroup().subnetIds())
                        .status(awsResponse.workgroup().statusAsString())
                        .endpoint(translateToModelEndpoint(awsResponse.workgroup().endpoint()))
                        .pricePerformanceTarget(translateToModelPerformanceTarget(awsResponse.workgroup().pricePerformanceTarget()))
                        .publiclyAccessible(awsResponse.workgroup().publiclyAccessible())
                        .creationDate(Objects.toString(awsResponse.workgroup().creationDate()))
                        .trackName(awsResponse.workgroup().trackName())
                        .build())
                .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromCreateResponse(final CreateWorkgroupResponse awsResponse) {
        return ResourceModel.builder()
                .workgroupName(awsResponse.workgroup().workgroupName())
                .namespaceName(awsResponse.workgroup().namespaceName())
                .workgroup(Workgroup.builder()
                        .workgroupName(awsResponse.workgroup().workgroupName())
                        .namespaceName(awsResponse.workgroup().namespaceName())
                        .build())
                .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromDeleteResponse(final DeleteWorkgroupResponse awsResponse) {
        return ResourceModel.builder()
                .workgroupName(awsResponse.workgroup().workgroupName())
                .namespaceName(awsResponse.workgroup().namespaceName())
                .workgroup(Workgroup.builder()
                        .workgroupName(awsResponse.workgroup().workgroupName())
                        .namespaceName(awsResponse.workgroup().namespaceName())
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
                .maxCapacity(model.getMaxCapacity())
                .enhancedVpcRouting(model.getEnhancedVpcRouting())
                .configParameters(translateToSdkConfigParameters(model.getConfigParameters()))
                .pricePerformanceTarget(translateToSdkPerformanceTarget(model.getPricePerformanceTarget()))
                .publiclyAccessible(model.getPubliclyAccessible())
                .subnetIds(model.getSubnetIds())
                .securityGroupIds(model.getSecurityGroupIds())
                .port(model.getPort())
                .trackName(model.getTrackName())
                .build();
    }

    /**
     * Request to restore workgroup from a snapshot
     *
     * @param model resource model
     * @return awsRequest the aws service request to modify a resource
     */
    static RestoreFromSnapshotRequest translateToRestoreFromSnapshotRequest(final ResourceModel model) {
        return RestoreFromSnapshotRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .namespaceName(model.getNamespaceName())
                .snapshotArn(model.getSnapshotArn())
                .snapshotName(model.getSnapshotName())
                .ownerAccount(model.getSnapshotOwnerAccount())
                .build();
    }

    /**
     * Request to restore workgroup from a recovery point
     *
     * @param model resource model
     * @return awsRequest the aws service request to modify a resource
     */
    static RestoreFromRecoveryPointRequest translateToRestoreFromRecoveryPointRequest(final ResourceModel model) {
        return RestoreFromRecoveryPointRequest.builder()
                .workgroupName(model.getWorkgroupName())
                .namespaceName(model.getNamespaceName())
                .recoveryPointId(model.getRecoveryPointId())
                .build();
    }

    /**
     * Request to read a resource
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static GetNamespaceRequest translateToReadNamespaceRequest(final ResourceModel model) {
        return GetNamespaceRequest.builder()
                .namespaceName(model.getNamespaceName())
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
                        .trackName(workgroup.trackName())
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

        // If desiredResourceState.getTags() is null, we should preserve existing tags
        // This ensures that when CloudFormation doesn't specify tags in the update,
        // we don't remove existing tags
        final List<Tag> effectiveDesiredTags;
        if (desiredResourceState.getTags() == null && currentResourceState.getTags() != null) {
            // Preserve existing tags by using them as the desired tags
            effectiveDesiredTags = currentResourceState.getTags();
        } else {
            effectiveDesiredTags = desiredResourceState.getTags();
        }

        List<Tag> toBeCreatedTags = effectiveDesiredTags == null ? Collections.emptyList() : effectiveDesiredTags
                .stream()
                .filter(tag -> currentResourceState.getTags() == null || !currentResourceState.getTags().contains(tag))
                .collect(Collectors.toList());

        List<Tag> toBeDeletedTags = currentResourceState.getTags() == null ? Collections.emptyList() : currentResourceState.getTags()
                .stream()
                .filter(tag -> effectiveDesiredTags == null || !effectiveDesiredTags.contains(tag))
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

    static UpdateTagsRequest translateToUpdateTagsRequest(Map<String, String> previousTags, Map<String, String>desiredTags, String resourceArn) {
        List<Tag> toBeCreatedTags = convertToTagList(generateTagsToAdd(previousTags, desiredTags));
        List<String> tagKeysToBeDeleted = generateTagsToRemove(previousTags, desiredTags);

        return UpdateTagsRequest.builder()
                .createNewTagsRequest(TagResourceRequest.builder()
                        .tags(translateToSdkTags(toBeCreatedTags))
                        .resourceArn(resourceArn)
                        .build())
                .deleteOldTagsRequest(UntagResourceRequest.builder()
                        .tagKeys(tagKeysToBeDeleted)
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

    private static PerformanceTarget translateToModelPerformanceTarget(software.amazon.awssdk.services.redshiftserverless.model.PerformanceTarget performanceTarget) {
        return GSON.fromJson(GSON.toJson(performanceTarget), PerformanceTarget.class);
    }

    private static software.amazon.awssdk.services.redshiftserverless.model.PerformanceTarget translateToSdkPerformanceTarget(PerformanceTarget performanceTarget) {
        return GSON.fromJson(GSON.toJson(performanceTarget), software.amazon.awssdk.services.redshiftserverless.model.PerformanceTarget.class);
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
