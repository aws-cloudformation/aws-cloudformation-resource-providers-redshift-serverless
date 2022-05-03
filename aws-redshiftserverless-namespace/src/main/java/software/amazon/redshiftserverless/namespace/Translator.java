package software.amazon.redshiftserverless.namespace;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.CreateNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.DeleteNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ListNamespacesRequest;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.ListNamespacesResponse;
import software.amazon.awssdk.services.redshiftarcadiacoral.model.UpdateNamespaceRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateNamespaceRequest translateToCreateRequest(final ResourceModel model) {
    return CreateNamespaceRequest.builder()
            .namespaceName(model.getNamespaceName())
            .adminUsername(model.getAdminUsername())
            .adminUserPassword(model.getAdminUserPassword())
            .dbName(model.getDbName())
            .kmsKeyId(model.getKmsKeyId())
            .defaultIamRoleArn(model.getDefaultIamRoleArn())
            .iamRoles(model.getIamRoles())
            .logExports(model.getLogExports())
            .tags(translateTagsToSdk(model.getTags()))
            .build();
  }

  static List<software.amazon.awssdk.services.redshiftarcadiacoral.model.Tag> translateTagsToSdk(final List<software.amazon.redshiftserverless.namespace.Tag> tags) {
    return Optional.ofNullable(tags).orElse(Collections.emptyList())
            .stream()
            .map(tag -> software.amazon.awssdk.services.redshiftarcadiacoral.model.Tag.builder()
            .key(tag.getKey())
            .value(tag.getValue()).build())
            .collect(Collectors.toList());
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static GetNamespaceRequest translateToReadRequest(final ResourceModel model) {
    return GetNamespaceRequest.builder()
            .namespaceName(model.getNamespaceName())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetNamespaceResponse awsResponse) {

    return ResourceModel.builder()
            .adminUsername(awsResponse.namespace().adminUsername())
            .dbName(awsResponse.namespace().dbName())
            .defaultIamRoleArn(awsResponse.namespace().defaultIamRoleArn())
            .iamRoles(awsResponse.namespace().iamRoles())
            .kmsKeyId(awsResponse.namespace().kmsKeyId())
            .logExports(awsResponse.namespace().logExports())
            .namespaceName(awsResponse.namespace().namespaceName())
            .namespace(translateToModelNamespace(awsResponse.namespace()))
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteNamespaceRequest translateToDeleteRequest(final ResourceModel model) {

    return DeleteNamespaceRequest.builder()
            .namespaceName(model.getNamespaceName())
            .finalSnapshotName(model.getFinalSnapshotName())
            .finalSnapshotRetentionPeriod(model.getFinalSnapshotRetentionPeriod())
            .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateNamespaceRequest translateToUpdateRequest(final ResourceModel model) {
    return UpdateNamespaceRequest.builder()
            .namespaceName(model.getNamespaceName())
            .adminUserPassword(model.getAdminUserPassword())
            .kmsKeyId(model.getKmsKeyId())
            .iamRoles(model.getIamRoles())
            .logExports(model.getLogExports())
            .adminUsername(model.getAdminUsername())
            .dbName(model.getDbName())
            .defaultIamRoleArn(model.getDefaultIamRoleArn())
            .build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListNamespacesRequest translateToListRequest(final String nextToken) {
    return ListNamespacesRequest.builder()
            .nextToken(nextToken)
            .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListNamespacesResponse awsResponse) {
    return awsResponse.namespaces()
            .stream()
            .map(namespace -> ResourceModel.builder()
            .namespaceName(namespace.namespaceName())
            .build())
            .collect(Collectors.toList());
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

  private static Namespace translateToModelNamespace(
          software.amazon.awssdk.services.redshiftarcadiacoral.model.Namespace namespace) {

    return Namespace.builder()
            .namespaceArn(namespace.namespaceArn())
            .namespaceId(namespace.namespaceId())
            .namespaceName(namespace.namespaceName())
            .adminUsername(namespace.adminUsername())
            .dbName(namespace.dbName())
            .kmsKeyId(namespace.kmsKeyId())
            .defaultIamRoleArn(namespace.defaultIamRoleArn())
            .iamRoles(namespace.iamRoles())
            .logExports(namespace.logExports())
            .status(namespace.statusAsString())
            .creationDate(namespace.creationDate() == null ? null : namespace.creationDate().toString())
            .build();
  }
}
