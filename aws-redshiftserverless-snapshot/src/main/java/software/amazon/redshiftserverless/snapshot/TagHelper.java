package software.amazon.redshiftserverless.snapshot;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkClient;
// TODO: Critical! Please replace the CloudFormation Tag model below with your service's own SDK Tag model
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class TagHelper {
    /**
     * convertToMap
     *
     * Converts a collection of Tag objects to a tag-name -> tag-value map.
     *
     * Note: Tag objects with null tag values will not be included in the output
     * map.
     *
     * @param tags Collection of tags to convert
     * @return Converted Map of tags
     */
    public static Map<String, String> convertToMap(final Collection<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyMap();
        }
        return tags.stream()
            .filter(tag -> tag.value() != null)
            .collect(Collectors.toMap(
                Tag::key,
                Tag::value,
                (oldValue, newValue) -> newValue));
    }

    /**
     * convertToSet
     *
     * Converts a tag map to a set of Tag objects.
     *
     * Note: Like convertToMap, convertToSet filters out value-less tag entries.
     *
     * @param tagMap Map of tags to convert
     * @return Set of Tag objects
     */
    public static Set<Tag> convertToSet(final Map<String, String> tagMap) {
        if (MapUtils.isEmpty(tagMap)) {
            return Collections.emptySet();
        }
        return tagMap.entrySet().stream()
            .filter(tag -> tag.getValue() != null)
            .map(tag -> Tag.builder()
                .key(tag.getKey())
                .value(tag.getValue())
                .build())
            .collect(Collectors.toSet());
    }

    /**
     * shouldUpdateTags
     *
     * Determines whether user defined tags have been changed during update.
     */
    public final boolean shouldUpdateTags(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> previousTags = getPreviouslyAttachedTags(handlerRequest);
        final Map<String, String> desiredTags = getNewDesiredTags(handlerRequest);
        return ObjectUtils.notEqual(previousTags, desiredTags);
    }

    /**
     * getPreviouslyAttachedTags
     *
     * If stack tags and resource tags are not merged together in Configuration class,
     * we will get previously attached system (with `aws:cloudformation` prefix) and user defined tags from
     * handlerRequest.getPreviousSystemTags() (system tags),
     * handlerRequest.getPreviousResourceTags() (stack tags),
     * handlerRequest.getPreviousResourceState().getTags() (resource tags).
     *
     * System tags are an optional feature. Merge them to your tags if you have enabled them for your resource.
     * System tags can change on resource update if the resource is imported to the stack.
     */
    public Map<String, String> getPreviouslyAttachedTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> previousTags = new HashMap<>();

        // TODO: get previous system tags if your service supports CloudFormation system tags
        // if (handlerRequest.getPreviousSystemTags() != null) {
        //     previousTags.putAll(handlerRequest.getPreviousSystemTags());
        // }

        // get previous stack level tags from handlerRequest
        if (handlerRequest.getPreviousResourceTags() != null) {
            previousTags.putAll(handlerRequest.getPreviousResourceTags());
        }

        // TODO: get resource level tags from previous resource state based on your tag property name
        // TODO: previousTags.putAll(handlerRequest.getPreviousResourceState().getTags()); // if tags are not null
        return previousTags;
    }

    /**
     * getNewDesiredTags
     *
     * If stack tags and resource tags are not merged together in Configuration class,
     * we will get new desired system (with `aws:cloudformation` prefix) and user defined tags from
     * handlerRequest.getSystemTags() (system tags),
     * handlerRequest.getDesiredResourceTags() (stack tags),
     * handlerRequest.getDesiredResourceState().getTags() (resource tags).
     *
     * System tags are an optional feature. Merge them to your tags if you have enabled them for your resource.
     * System tags can change on resource update if the resource is imported to the stack.
     */
    public Map<String, String> getNewDesiredTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> desiredTags = new HashMap<>();

        // TODO: merge system tags with desired resource tags if your service supports CloudFormation system tags
        // if (handlerRequest.getSystemTags() != null) {
        //     desiredTags.putAll(handlerRequest.getSystemTags());
        // }

        // get desired stack level tags from handlerRequest
        if (handlerRequest.getDesiredResourceTags() != null) {
            desiredTags.putAll(handlerRequest.getDesiredResourceTags());
        }

        // TODO: get resource level tags from resource model based on your tag property name
        // TODO: desiredTags.putAll(convertToMap(handlerRequest.getDesiredResourceState().getTags())); // if tags are not null
        return desiredTags;
    }

    /**
     * generateTagsToAdd
     *
     * Determines the tags the customer desired to define or redefine.
     */
    public Map<String, String> generateTagsToAdd(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
        return desiredTags.entrySet().stream()
            .filter(e -> !previousTags.containsKey(e.getKey()) || !Objects.equals(previousTags.get(e.getKey()), e.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
    }

    /**
     * getTagsToRemove
     *
     * Determines the tags the customer desired to remove from the function.
     */
    public Set<String> generateTagsToRemove(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
        final Set<String> desiredTagNames = desiredTags.keySet();

        return previousTags.keySet().stream()
            .filter(tagName -> !desiredTagNames.contains(tagName))
            .collect(Collectors.toSet());
    }

    /**
     * generateTagsToAdd
     *
     * Determines the tags the customer desired to define or redefine.
     */
    public Set<Tag> generateTagsToAdd(final Set<Tag> previousTags, final Set<Tag> desiredTags) {
        return Sets.difference(new HashSet<>(desiredTags), new HashSet<>(previousTags));
    }

    /**
     * getTagsToRemove
     *
     * Determines the tags the customer desired to remove from the function.
     */
    public Set<Tag> generateTagsToRemove(final Set<Tag> previousTags, final Set<Tag> desiredTags) {
        return Sets.difference(new HashSet<>(previousTags), new HashSet<>(desiredTags));
    }


    /**
     * tagResource during update
     *
     * Calls the service:TagResource API.
     */
    private ProgressEvent<ResourceModel, CallbackContext>
    tagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<SdkClient> serviceClient, final ResourceModel resourceModel,
                final ResourceHandlerRequest<ResourceModel> handlerRequest, final CallbackContext callbackContext, final Map<String, String> addedTags, final Logger logger) {
        // TODO: add log for adding tags to resources during update
        // e.g. logger.log(String.format("[UPDATE][IN PROGRESS] Going to add tags for ... resource: %s with AccountId: %s",
        // resourceModel.getResourceName(), handlerRequest.getAwsAccountId()));

        // TODO: change untagResource in the method to your service API according to your SDK
        return proxy.initiate("AWS-RedshiftServerless-Snapshot::TagOps", serviceClient, resourceModel, callbackContext)
            .translateToServiceRequest(model ->
                Translator.tagResourceRequest(model, addedTags))
            .makeServiceCall((request, client) -> {
                return (AwsResponse) null;
                // TODO: replace the return null with your invoke log to call tagResource API to add tags
                // e.g. proxy.injectCredentialsAndInvokeV2(request, client.client()::tagResource))
            })
            .progress();
    }

    /**
     * untagResource during update
     *
     * Calls the service:UntagResource API.
     */
    private ProgressEvent<ResourceModel, CallbackContext>
    untagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<SdkClient> serviceClient, final ResourceModel resourceModel,
                  final ResourceHandlerRequest<ResourceModel> handlerRequest, final CallbackContext callbackContext, final Set<String> removedTags, final Logger logger) {
        // TODO: add log for removing tags from resources during update
        // e.g. logger.log(String.format("[UPDATE][IN PROGRESS] Going to remove tags for ... resource: %s with AccountId: %s",
        // resourceModel.getResourceName(), handlerRequest.getAwsAccountId()));

        // TODO: change untagResource in the method to your service API according to your SDK
        return proxy.initiate("AWS-RedshiftServerless-Snapshot::TagOps", serviceClient, resourceModel, callbackContext)
            .translateToServiceRequest(model ->
                Translator.untagResourceRequest(model, removedTags))
            .makeServiceCall((request, client) -> {
                return (AwsResponse) null;
                // TODO: replace the return null with your invoke log to call untag API to remove tags
                // e.g. proxy.injectCredentialsAndInvokeV2(request, client.client()::untagResource)
            })
            .progress();
    }

}
