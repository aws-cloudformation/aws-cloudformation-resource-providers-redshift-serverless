package software.amazon.redshiftserverless.namespace;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.redshift.model.DeleteResourcePolicyRequest;
import software.amazon.awssdk.services.redshift.model.GetResourcePolicyRequest;
import software.amazon.awssdk.services.redshift.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.GetNamespaceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListNamespacesRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListNamespacesResponse;
import software.amazon.awssdk.services.redshiftserverless.model.ListSnapshotCopyConfigurationsRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateNamespaceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.CreateSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteSnapshotCopyConfigurationRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.redshiftserverless.model.TagResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UntagResourceRequest;
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
  static CreateNamespaceRequest translateToCreateRequest(final ResourceModel model) {
    return CreateNamespaceRequest.builder()
            .namespaceName(model.getNamespaceName())
            .adminUsername(model.getAdminUsername())
            .adminUserPassword(model.getAdminUserPassword())
            .dbName(model.getDbName())
            .kmsKeyId(model.getKmsKeyId())
            .defaultIamRoleArn(model.getDefaultIamRoleArn())
            .iamRoles(model.getIamRoles())
            .logExportsWithStrings(model.getLogExports())
            .tags(translateToSdkTags(model.getTags()))
            .manageAdminPassword(model.getManageAdminPassword())
            .adminPasswordSecretKmsKeyId(model.getAdminPasswordSecretKmsKeyId())
            .redshiftIdcApplicationArn(model.getRedshiftIdcApplicationArn())
            .build();
  }

  /*
    This function is to return the iam role in the same format as input iam roles.
    Instead of modifying the schema for backward compatibitlity we use regex to extract the iam role.
    'IamRole(applyStatus=null, iamRoleArn=arn:aws:iam::254260483320:role/contracttest9nfdg-redshif-RedshiftServerlessNamespa-nAxEywsQousB)'
   */
  static List<String> translateIamRoles(final List<String> iamRoles) {
    return Optional.ofNullable(iamRoles).orElse(Collections.emptyList())
            .stream()
            .map(iamRole -> {
              Pattern iamPattern = Pattern.compile("(arn:aws.*?:iam::[0-9]{12}?:role/[a-zA-Z0-9_+=,.@-]{1,64})");
              Matcher matcher = iamPattern.matcher(iamRole);
              if (matcher.find()){
                return matcher.group(0);
              }
              // Case for invalid arn format provided. This is added as a precaution
              // Service API call to RACS will throw error in case user provides invalid ARN and this wouldnt be reachable.
              return null;
            }).filter (iamRole->iamRole!=null)
            .collect(Collectors.toList());
  }

  /**
   * Request to list snapshot copy configurations for a namespace
   * @param model resource model
   * @return awsRequest the aws service request to list the snapshot copy configurations
   */
  static ListSnapshotCopyConfigurationsRequest translateToListSnapshotCopyConfigurationsRequest(final ResourceModel model) {
    return ListSnapshotCopyConfigurationsRequest.builder()
            .namespaceName(model.getNamespaceName())
            .build();
  }

  /**
   * Request to create a snapshot copy configuration for a namespace
   * @param model resource model
   * @param snapshotCopyConfiguration the snapshot copy configuration to create
   * @return awsRequest the aws service request to create a snapshot copy configuration
   */
  static CreateSnapshotCopyConfigurationRequest translateToCreateSnapshotCopyConfigurationRequest(final ResourceModel model, final SnapshotCopyConfiguration snapshotCopyConfiguration) {
    return CreateSnapshotCopyConfigurationRequest.builder()
            .namespaceName(model.getNamespaceName())
            .destinationRegion(snapshotCopyConfiguration.getDestinationRegion())
            .destinationKmsKeyId(snapshotCopyConfiguration.getDestinationKmsKeyId())
            .snapshotRetentionPeriod(snapshotCopyConfiguration.getSnapshotRetentionPeriod())
            .build();
  }

  /**
   * Request to update a snapshot copy configuration for a namespace
   * @param model resource model
   * @param snapshotCopyConfiguration the snapshot copy configuration to update
   * @return awsRequest the aws service request to update a snapshot copy configuration
   */
  static UpdateSnapshotCopyConfigurationRequest translateToUpdateSnapshotCopyConfigurationRequest(final ResourceModel model, final String snapshotCopyConfigurationId, final SnapshotCopyConfiguration snapshotCopyConfiguration) {
    return UpdateSnapshotCopyConfigurationRequest.builder()
            .snapshotCopyConfigurationId(snapshotCopyConfigurationId)
            .snapshotRetentionPeriod(snapshotCopyConfiguration.getSnapshotRetentionPeriod())
            .build();
  }

  /**
   * Request to delete a snapshot copy configuration for a namespace
   * @param model resource model
   * @param snapshotCopyConfigurationId the snapshot copy configuration id to delete
   * @return awsRequest the aws service request to delete a snapshot copy configuration
   */
  static DeleteSnapshotCopyConfigurationRequest translateToDeleteSnapshotCopyConfigurationRequest(final ResourceModel model, final String snapshotCopyConfigurationId) {
    return DeleteSnapshotCopyConfigurationRequest.builder()
            .snapshotCopyConfigurationId(snapshotCopyConfigurationId)
            .build();
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
            .iamRoles(translateIamRoles(awsResponse.namespace().iamRoles()))
            .kmsKeyId(awsResponse.namespace().kmsKeyId())
            .logExports(awsResponse.namespace().logExportsAsStrings())
            .namespaceName(awsResponse.namespace().namespaceName())
            .namespace(translateToModelNamespace(awsResponse.namespace()))
            .manageAdminPassword(StringUtils.isNullOrEmpty(awsResponse.namespace().adminPasswordSecretArn()) ? null : true)
            .adminPasswordSecretKmsKeyId(awsResponse.namespace().adminPasswordSecretKmsKeyId())
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
            .logExportsWithStrings(model.getLogExports())
            .adminUsername(model.getAdminUsername())
            //TODO: we only support updating db-name after GA
//            .dbName(model.getDbName())
            .defaultIamRoleArn(model.getDefaultIamRoleArn())
            .manageAdminPassword(model.getManageAdminPassword())
            .adminPasswordSecretKmsKeyId(model.getAdminPasswordSecretKmsKeyId())
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

  /**
   * Request to read tags for a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to update tags of a resource
   */
  static ListTagsForResourceRequest translateToReadTagsRequest(final ResourceModel model) {
    return ListTagsForResourceRequest.builder()
            .resourceArn(model.getNamespace().getNamespaceArn())
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
    String resourceArn = currentResourceState.getNamespace().getNamespaceArn();

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

  private static Namespace translateToModelNamespace(
          software.amazon.awssdk.services.redshiftserverless.model.Namespace namespace) {

    return Namespace.builder()
            .namespaceArn(namespace.namespaceArn())
            .namespaceId(namespace.namespaceId())
            .namespaceName(namespace.namespaceName())
            .adminUsername(namespace.adminUsername())
            .dbName(namespace.dbName())
            .kmsKeyId(namespace.kmsKeyId())
            .defaultIamRoleArn(namespace.defaultIamRoleArn())
            .iamRoles(translateIamRoles(namespace.iamRoles()))
            .logExports(namespace.logExportsAsStrings())
            .status(namespace.statusAsString())
            .creationDate(namespace.creationDate() == null ? null : namespace.creationDate().toString())
            .adminPasswordSecretArn(namespace.adminPasswordSecretArn())
            .adminPasswordSecretKmsKeyId(namespace.adminPasswordSecretKmsKeyId())
            .build();
  }

  /**
   * Request to put a policy on resource
   * @param model resource model
   * @return putResourcePolicyRequest the service request to put a policy on resource
   */
  static PutResourcePolicyRequest translateToPutResourcePolicy(final ResourceModel model, final String namespaceArn, Logger logger) {
    return PutResourcePolicyRequest.builder()
            .resourceArn(namespaceArn)
            .policy(convertJsonToString(model.getNamespaceResourcePolicy(), logger))
            .build();
  }

  static GetResourcePolicyRequest translateToGetResourcePolicy(final ResourceModel model, final String namespaceArn) {
    return GetResourcePolicyRequest.builder()
            .resourceArn(namespaceArn)
            .build();
  }

  static DeleteResourcePolicyRequest translateToDeleteResourcePolicyRequest(final ResourceModel model, final String namespaceArn) {
    return DeleteResourcePolicyRequest.builder()
            .resourceArn(namespaceArn)
            .build();
  }

  /**
   * Json to String converter
   * @param policy Policy Document Map
   * @param logger Logger to log Json processing error
   * @return Json converted String
   */
  static String convertJsonToString(Map<String, Object> policy, Logger logger) {
    ObjectMapper mapper = new ObjectMapper();
    String json = "";
    try {
      json = mapper.writeValueAsString(policy);
    } catch (JsonProcessingException e) {
      logger.log("Error parsing Policy Json to String");
    }
    return json;
  }

  /**
   *
   * @param policy Policy Document String
   * @param logger Logger to log Json processing error
   * @return Json object Map
   */
  static Map<String, Object> convertStringToJson(String policy, Logger logger) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> json = null;
    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
    };
    try {
      if (policy != null) {
        if (policy.isEmpty()) {
          logger.log("Empty NamespaceResourcePolicy");
        } else {
          json = mapper.readValue(URLDecoder.decode(policy, StandardCharsets.UTF_8.toString()), typeRef);
        }
      }
    } catch (IOException e) {
      logger.log("Error parsing Policy String to Json");
    }
    return json;
  }

  static SnapshotCopyConfiguration translateToSnapshotCopyConfiguration(software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration config) {
    return SnapshotCopyConfiguration.builder()
            .destinationRegion(config.destinationRegion())
            .destinationKmsKeyId(config.destinationKmsKeyId())
            .snapshotRetentionPeriod(config.snapshotRetentionPeriod())
            .build();
  }

  static List<SnapshotCopyConfiguration> translateToSnapshotCopyConfigurations(List<software.amazon.awssdk.services.redshiftserverless.model.SnapshotCopyConfiguration> configs) {
    return configs.stream()
            .map(Translator::translateToSnapshotCopyConfiguration)
            .collect(Collectors.toList());
  }
}
