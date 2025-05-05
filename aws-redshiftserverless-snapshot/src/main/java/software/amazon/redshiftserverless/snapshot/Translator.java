package software.amazon.redshiftserverless.snapshot;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetSnapshotResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.TagResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UntagResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotResponse;
import software.amazon.cloudformation.proxy.Logger;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {
  private static final Gson GSON = new GsonBuilder().create();

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateSnapshotRequest translateToCreateRequest(final ResourceModel model) {
    return CreateSnapshotRequest.builder()
            .namespaceName(model.getNamespaceName())
            .snapshotName(model.getSnapshotName())
            .retentionPeriod(model.getRetentionPeriod())
            .tags(translateToSdkTags(model.getTags()))
            .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static GetSnapshotRequest translateToReadRequest(final ResourceModel model) {
    return GetSnapshotRequest.builder()
            .snapshotName(model.getSnapshotName())
            .ownerAccount(model.getOwnerAccount())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  // TO DO
  static ResourceModel translateFromReadResponse(final GetSnapshotResponse awsResponse) {
    return ResourceModel.builder()
            .namespaceName(awsResponse.snapshot().namespaceName())
            .namespaceArn(awsResponse.snapshot().namespaceArn())
            .snapshotName(awsResponse.snapshot().snapshotName())
            .snapshotArn(awsResponse.snapshot().snapshotArn())
            .snapshotCreateTime(Objects.toString(awsResponse.snapshot().snapshotCreateTime()))
            .adminUsername(awsResponse.snapshot().adminUsername())
            .kmsKeyId(awsResponse.snapshot().kmsKeyId())
            .ownerAccount(awsResponse.snapshot().ownerAccount())
            .retentionPeriod(awsResponse.snapshot().snapshotRetentionPeriod())
            .snapshot(Snapshot.builder()
                    .namespaceName(awsResponse.snapshot().namespaceName())
                    .namespaceArn(awsResponse.snapshot().namespaceArn())
                    .snapshotName(awsResponse.snapshot().snapshotName())
                    .snapshotArn(awsResponse.snapshot().snapshotArn())
                    .snapshotCreateTime(Objects.toString(awsResponse.snapshot().snapshotCreateTime()))
                    .status(awsResponse.snapshot().statusAsString())
                    .adminUsername(awsResponse.snapshot().adminUsername())
                    .kmsKeyId(awsResponse.snapshot().kmsKeyId())
                    .ownerAccount(awsResponse.snapshot().ownerAccount())
                    .retentionPeriod(awsResponse.snapshot().snapshotRetentionPeriod())
                    .build())
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteSnapshotRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteSnapshotRequest.builder()
            .snapshotName(model.getSnapshotName())
            .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateSnapshotRequest translateToUpdateRequest(final ResourceModel model) {
    return UpdateSnapshotRequest.builder()
            .snapshotName(model.getSnapshotName())
            .retentionPeriod(model.getRetentionPeriod())
            .build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListSnapshotsRequest translateToListRequest(final String nextToken) {
    return ListSnapshotsRequest.builder()
            .nextToken(nextToken)
            .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListSnapshotsResponse awsResponse) {
    return awsResponse.snapshots()
            .stream()
            .map(snapshot -> ResourceModel.builder()
                    .snapshotName(snapshot.snapshotName())
                    .build())
            .collect(Collectors.toList());
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

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  /**
   * Request to add tags to a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AwsRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    return awsRequest;
  }

  /**
   * Request to add tags to a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AwsRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    return awsRequest;
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
}
