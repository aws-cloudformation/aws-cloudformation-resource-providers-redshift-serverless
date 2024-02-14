# AWS::RedshiftServerless::Workgroup VpcEndpointAccess

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#endpointname" title="EndpointName">EndpointName</a>" : <i>String</i>,
    "<a href="#endpointstatus" title="EndpointStatus">EndpointStatus</a>" : <i>String</i>,
    "<a href="#workgroupname" title="WorkgroupName">WorkgroupName</a>" : <i>String</i>,
    "<a href="#endpointcreatetime" title="EndpointCreateTime">EndpointCreateTime</a>" : <i>String</i>,
    "<a href="#port" title="Port">Port</a>" : <i>Integer</i>,
    "<a href="#address" title="Address">Address</a>" : <i>String</i>,
    "<a href="#subnetids" title="SubnetIds">SubnetIds</a>" : <i>[ String, ... ]</i>,
    "<a href="#vpcsecuritygroups" title="VpcSecurityGroups">VpcSecurityGroups</a>" : <i>[ <a href="vpcsecuritygroupmembership.md">VpcSecurityGroupMembership</a>, ... ]</i>,
    "<a href="#vpcendpoint" title="VpcEndpoint">VpcEndpoint</a>" : <i><a href="vpcendpoint.md">VpcEndpoint</a></i>,
    "<a href="#endpointarn" title="EndpointArn">EndpointArn</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#endpointname" title="EndpointName">EndpointName</a>: <i>String</i>
<a href="#endpointstatus" title="EndpointStatus">EndpointStatus</a>: <i>String</i>
<a href="#workgroupname" title="WorkgroupName">WorkgroupName</a>: <i>String</i>
<a href="#endpointcreatetime" title="EndpointCreateTime">EndpointCreateTime</a>: <i>String</i>
<a href="#port" title="Port">Port</a>: <i>Integer</i>
<a href="#address" title="Address">Address</a>: <i>String</i>
<a href="#subnetids" title="SubnetIds">SubnetIds</a>: <i>
      - String</i>
<a href="#vpcsecuritygroups" title="VpcSecurityGroups">VpcSecurityGroups</a>: <i>
      - <a href="vpcsecuritygroupmembership.md">VpcSecurityGroupMembership</a></i>
<a href="#vpcendpoint" title="VpcEndpoint">VpcEndpoint</a>: <i><a href="vpcendpoint.md">VpcEndpoint</a></i>
<a href="#endpointarn" title="EndpointArn">EndpointArn</a>: <i>String</i>
</pre>

## Properties

#### EndpointName

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EndpointStatus

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### WorkgroupName

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EndpointCreateTime

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Port

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Address

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SubnetIds

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VpcSecurityGroups

_Required_: No

_Type_: List of <a href="vpcsecuritygroupmembership.md">VpcSecurityGroupMembership</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VpcEndpoint

_Required_: No

_Type_: <a href="vpcendpoint.md">VpcEndpoint</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EndpointArn

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

