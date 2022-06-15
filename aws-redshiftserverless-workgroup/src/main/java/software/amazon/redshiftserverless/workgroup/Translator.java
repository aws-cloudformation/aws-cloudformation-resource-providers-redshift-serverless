package software.amazon.redshiftserverless.workgroup;

import com.google.common.collect.Lists;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.*;
import software.amazon.cloudformation.proxy.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

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
  static CreateWorkgroupRequest translateToCreateRequest(final ResourceModel model) {
    return CreateWorkgroupRequest.builder()
            .workgroupName(model.getWorkgroupName())
            .namespaceName(model.getNamespaceName())
            .baseCapacity(model.getBaseCapacity())
            .enhancedVpcRouting(model.getEnhancedVpcRouting())
            .securityGroupIds(model.getSecurityGroupIds())
            .subnetIds(model.getSubnetIds())
            .configParameters(convertConfigParametersToRequest(model.getConfigParameters()))
            .publiclyAccessible(model.getPubliclyAccessible())
            .tags(translateTagsToSdk(model.getTags()))
            .build();

  }

  static List<software.amazon.awssdk.services.redshiftserverless.model.Tag> translateTagsToSdk(final List<software.amazon.redshiftserverless.workgroup.Tag> tags) {
    return Optional.ofNullable(tags).orElse(Collections.emptyList())
            .stream()
            .map(tag -> software.amazon.awssdk.services.redshiftserverless.model.Tag.builder()
                    .key(tag.getKey())
                    .value(tag.getValue()).build())
            .collect(Collectors.toList());
  }
  /**
   * Request to read a resource
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
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetWorkgroupResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L58-L73
    return ResourceModel.builder()
            .workgroupName(awsResponse.workgroup().workgroupName())
            .workgroup(Workgroup.builder()
                    .workgroupName(awsResponse.workgroup().workgroupName())
                    .namespaceName(awsResponse.workgroup().namespaceName())
                    .baseCapacity(awsResponse.workgroup().baseCapacity())
                    .subnetIds(awsResponse.workgroup().subnetIds())
                    .securityGroupIds(awsResponse.workgroup().securityGroupIds())
                    .status(awsResponse.workgroup().statusAsString())
                    .configParameters(convertConfigParametersFromResponse(awsResponse.workgroup().configParameters()))
                    .build())
            .build();

  }

  /**
   * Request to delete a resource
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
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateWorkgroupRequest translateToFirstUpdateRequest(final ResourceModel model) {
    return UpdateWorkgroupRequest.builder()
            .workgroupName(model.getWorkgroupName())
            .baseCapacity(model.getBaseCapacity())
            .build();
  }


  /**
   * Request to list resources
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
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListWorkgroupsResponse awsResponse) {

    return awsResponse.workgroups().stream()
        .map(workgroup -> ResourceModel.builder()
                         .workgroupName(workgroup.workgroupName())
                         .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  private static Collection<software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter> convertConfigParametersToRequest(List<software.amazon.redshiftserverless.workgroup.ConfigParameter> configParameters) {
    Collection<software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter> newConfigParameters = new ArrayList<>();
    if (configParameters!=null) {
      for (software.amazon.redshiftserverless.workgroup.ConfigParameter configParameter : configParameters) {
        newConfigParameters.add(software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter.builder()
                .parameterKey(configParameter.getParameterKey())
                .parameterValue(configParameter.getParameterValue())
                .build());
      }
    }
    return newConfigParameters;
  }

  private static List<software.amazon.redshiftserverless.workgroup.ConfigParameter> convertConfigParametersFromResponse(Collection<software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter> configParameters) {
    ArrayList<ConfigParameter> newConfigParameters = new ArrayList<>();
    for(software.amazon.awssdk.services.redshiftserverless.model.ConfigParameter cp: configParameters) {
      newConfigParameters.add(ConfigParameter.builder()
              .parameterKey(cp.parameterKey())
              .parameterValue(cp.parameterValue())
              .build());
    }
    return newConfigParameters;
  }
}
