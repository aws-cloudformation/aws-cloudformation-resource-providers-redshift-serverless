package software.amazon.redshiftserverless.endpointaccess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.redshiftserverless.model.CreateEndpointAccessRequest;
import software.amazon.awssdk.services.redshiftserverless.model.DeleteEndpointAccessRequest;
import software.amazon.awssdk.services.redshiftserverless.model.EndpointAccess;
import software.amazon.awssdk.services.redshiftserverless.model.GetEndpointAccessRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListEndpointAccessRequest;
import software.amazon.awssdk.services.redshiftserverless.model.ListEndpointAccessResponse;
import software.amazon.awssdk.services.redshiftserverless.model.UpdateEndpointAccessRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {
  private static final Gson GSON = new GsonBuilder().create();

  /**
   * Request to create endpoint access
   *
   * @param model resource model
   * @return awsRequest the aws service request to create endpoint access
   */
  static CreateEndpointAccessRequest translateToCreateEndpointAccessRequest(final ResourceModel model) {
    return CreateEndpointAccessRequest.builder()
            .endpointName(model.getEndpointName())
            .ownerAccount(model.getOwnerAccount())
            .vpcSecurityGroupIds(model.getVpcSecurityGroupIds())
            .subnetIds(model.getSubnetIds())
            .workgroupName(model.getWorkgroupName())
            .build();
  }

  /**
   * Translates EndpointAccess resource object from sdk into a resource model
   *
   * @param endpointAccess the aws services resource response
   * @return model resource model
   */
  static ResourceModel translateFromEndpointAccessResponse(final EndpointAccess endpointAccess) {
    return ResourceModel.builder()
            .endpointName(endpointAccess.endpointName())
            .endpointStatus(endpointAccess.endpointStatus())
            .workgroupName(endpointAccess.workgroupName())
            .endpointCreateTime(endpointAccess.endpointCreateTime().toString())
            .port(endpointAccess.port())
            .address(endpointAccess.address())
            .subnetIds(endpointAccess.subnetIds())
            .vpcSecurityGroups(translateToModelVpcSecurityGroupMemberships(endpointAccess.vpcSecurityGroups()))
            .vpcEndpoint(translateToModelVpcEndpoint(endpointAccess.vpcEndpoint()))
            .endpointArn(endpointAccess.endpointArn())
            .build();
  }

  /**
   * Request to get endpoint access
   *
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static GetEndpointAccessRequest translateToGetEndpointAccessRequest(final ResourceModel model) {
    return GetEndpointAccessRequest.builder()
            .endpointName(model.getEndpointName())
            .build();
  }

  /**
   * Request to delete endpoint access
   *
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteEndpointAccessRequest translateToDeleteEndpointAccessRequest(final ResourceModel model) {
    return DeleteEndpointAccessRequest.builder()
            .endpointName(model.getEndpointName())
            .build();
  }

  /**
   * Request to update properties of a previously created endpoint access
   *
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateEndpointAccessRequest translateToUpdateEndpointAccessRequest(final ResourceModel model) {
    return UpdateEndpointAccessRequest.builder()
            .endpointName(model.getEndpointName())
            .vpcSecurityGroupIds(model.getVpcSecurityGroupIds())
            .build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListEndpointAccessRequest translateToListEndpointAccessRequest(final ResourceModel resourceModel, final String nextToken) {
    return ListEndpointAccessRequest.builder()
            .workgroupName(resourceModel.getWorkgroupName())
            .ownerAccount(resourceModel.getOwnerAccount())
            .vpcId(resourceModel.getVpcId())
            .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   *
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListEndpointAccessResponse(final ListEndpointAccessResponse awsResponse) {
    return awsResponse.endpoints()
            .stream()
            .map(endpointAccess -> translateFromEndpointAccessResponse(endpointAccess))
            .collect(Collectors.toList());
  }
  

  private static VpcEndpoint translateToModelVpcEndpoint(software.amazon.awssdk.services.redshiftserverless.model.VpcEndpoint vpcEndpoint) {
    return GSON.fromJson(GSON.toJson(vpcEndpoint), VpcEndpoint.class);
  }

  private static VpcSecurityGroupMembership translateToModelVpcSecurityGroupMembership(software.amazon.awssdk.services.redshiftserverless.model.VpcSecurityGroupMembership vpcSecurityGroupMembership) {
    return GSON.fromJson(GSON.toJson(vpcSecurityGroupMembership), VpcSecurityGroupMembership.class);
  }

  private static List<VpcSecurityGroupMembership> translateToModelVpcSecurityGroupMemberships(Collection<software.amazon.awssdk.services.redshiftserverless.model.VpcSecurityGroupMembership> vpcSecurityGroupMemberships) {
    return vpcSecurityGroupMemberships == null ? null : vpcSecurityGroupMemberships
            .stream()
            .map(Translator::translateToModelVpcSecurityGroupMembership)
            .collect(Collectors.toList());
  }
}
