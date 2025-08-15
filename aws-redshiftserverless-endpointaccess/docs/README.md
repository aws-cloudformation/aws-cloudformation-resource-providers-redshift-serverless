# AWS::RedshiftServerless::EndpointAccess

Resource schema for a Redshift Serverless managed VPC endpoint.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::RedshiftServerless::EndpointAccess",
    "Properties" : {
        "<a href="#endpointname" title="EndpointName">EndpointName</a>" : <i>String</i>,
        "<a href="#workgroupname" title="WorkgroupName">WorkgroupName</a>" : <i>String</i>,
        "<a href="#subnetids" title="SubnetIds">SubnetIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#owneraccount" title="OwnerAccount">OwnerAccount</a>" : <i>String</i>,
        "<a href="#vpcsecuritygroupids" title="VpcSecurityGroupIds">VpcSecurityGroupIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#vpcid" title="VpcId">VpcId</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::RedshiftServerless::EndpointAccess
Properties:
    <a href="#endpointname" title="EndpointName">EndpointName</a>: <i>String</i>
    <a href="#workgroupname" title="WorkgroupName">WorkgroupName</a>: <i>String</i>
    <a href="#subnetids" title="SubnetIds">SubnetIds</a>: <i>
      - String</i>
    <a href="#owneraccount" title="OwnerAccount">OwnerAccount</a>: <i>String</i>
    <a href="#vpcsecuritygroupids" title="VpcSecurityGroupIds">VpcSecurityGroupIds</a>: <i>
      - String</i>
    <a href="#vpcid" title="VpcId">VpcId</a>: <i>String</i>
</pre>

## Properties

#### EndpointName

The name of the endpoint.

_Required_: Yes

_Type_: String

_Pattern_: <code>^(?=^[a-z][a-z0-9]*(-[a-z0-9]+)*$).{1,30}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### WorkgroupName

The name of the workgroup.

_Required_: No

_Type_: String

_Minimum Length_: <code>3</code>

_Maximum Length_: <code>64</code>

_Pattern_: <code>^(?=^[a-z0-9-]+$).{3,64}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SubnetIds

The unique identifier of subnets where Amazon Redshift Serverless choose to deploy the VPC endpoint.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### OwnerAccount

Account Id of the resource owner

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VpcSecurityGroupIds

A list of VPC security group IDs to associate with the workgroup.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VpcId

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the EndpointName.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Address

The DNS address of the endpoint.

#### EndpointStatus

The status of the VPC endpoint.

#### EndpointCreateTime

The time (UTC) that the endpoint was created.

#### Port

The port number on which Amazon Redshift Serverless accepts incoming connections.

#### VpcSecurityGroups

A list of Virtual Private Cloud (VPC) security groups to be associated with the endpoint.

#### VpcEndpoint

The connection endpoint for connecting to Amazon Redshift Serverless.

#### EndpointArn

The Amazon Resource Name (ARN) of the VPC endpoint.

