# AWS::RedshiftServerless::Workgroup

Definition of AWS::RedshiftServerless::Workgroup Resource Type

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::RedshiftServerless::Workgroup",
    "Properties" : {
        "<a href="#workgroupname" title="WorkgroupName">WorkgroupName</a>" : <i>String</i>,
        "<a href="#namespacename" title="NamespaceName">NamespaceName</a>" : <i>String</i>,
        "<a href="#basecapacity" title="BaseCapacity">BaseCapacity</a>" : <i>Integer</i>,
        "<a href="#maxcapacity" title="MaxCapacity">MaxCapacity</a>" : <i>Integer</i>,
        "<a href="#enhancedvpcrouting" title="EnhancedVpcRouting">EnhancedVpcRouting</a>" : <i>Boolean</i>,
        "<a href="#configparameters" title="ConfigParameters">ConfigParameters</a>" : <i>[ <a href="configparameter.md">ConfigParameter</a>, ... ]</i>,
        "<a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#subnetids" title="SubnetIds">SubnetIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#publiclyaccessible" title="PubliclyAccessible">PubliclyAccessible</a>" : <i>Boolean</i>,
        "<a href="#port" title="Port">Port</a>" : <i>Integer</i>,
        "<a href="#priceperformancetarget" title="PricePerformanceTarget">PricePerformanceTarget</a>" : <i><a href="performancetarget.md">PerformanceTarget</a></i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#trackname" title="TrackName">TrackName</a>" : <i>String</i>,
        "<a href="#workgroup" title="Workgroup">Workgroup</a>" : <i><a href="workgroup.md">Workgroup</a></i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::RedshiftServerless::Workgroup
Properties:
    <a href="#workgroupname" title="WorkgroupName">WorkgroupName</a>: <i>String</i>
    <a href="#namespacename" title="NamespaceName">NamespaceName</a>: <i>String</i>
    <a href="#basecapacity" title="BaseCapacity">BaseCapacity</a>: <i>Integer</i>
    <a href="#maxcapacity" title="MaxCapacity">MaxCapacity</a>: <i>Integer</i>
    <a href="#enhancedvpcrouting" title="EnhancedVpcRouting">EnhancedVpcRouting</a>: <i>Boolean</i>
    <a href="#configparameters" title="ConfigParameters">ConfigParameters</a>: <i>
      - <a href="configparameter.md">ConfigParameter</a></i>
    <a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>: <i>
      - String</i>
    <a href="#subnetids" title="SubnetIds">SubnetIds</a>: <i>
      - String</i>
    <a href="#publiclyaccessible" title="PubliclyAccessible">PubliclyAccessible</a>: <i>Boolean</i>
    <a href="#port" title="Port">Port</a>: <i>Integer</i>
    <a href="#priceperformancetarget" title="PricePerformanceTarget">PricePerformanceTarget</a>: <i><a href="performancetarget.md">PerformanceTarget</a></i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#trackname" title="TrackName">TrackName</a>: <i>String</i>
    <a href="#workgroup" title="Workgroup">Workgroup</a>: <i><a href="workgroup.md">Workgroup</a></i>
</pre>

## Properties

#### WorkgroupName

The name of the workgroup.

_Required_: Yes

_Type_: String

_Minimum Length_: <code>3</code>

_Maximum Length_: <code>64</code>

_Pattern_: <code>^(?=^[a-z0-9-]+$).{3,64}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### NamespaceName

The namespace the workgroup is associated with.

_Required_: No

_Type_: String

_Minimum Length_: <code>3</code>

_Maximum Length_: <code>64</code>

_Pattern_: <code>^(?=^[a-z0-9-]+$).{3,64}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### BaseCapacity

The base compute capacity of the workgroup in Redshift Processing Units (RPUs).

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### MaxCapacity

The max compute capacity of the workgroup in Redshift Processing Units (RPUs).

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EnhancedVpcRouting

The value that specifies whether to enable enhanced virtual private cloud (VPC) routing, which forces Amazon Redshift Serverless to route traffic through your VPC.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ConfigParameters

A list of parameters to set for finer control over a database. Available options are datestyle, enable_user_activity_logging, query_group, search_path, max_query_execution_time, and require_ssl.

_Required_: No

_Type_: List of <a href="configparameter.md">ConfigParameter</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SecurityGroupIds

A list of security group IDs to associate with the workgroup.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SubnetIds

A list of subnet IDs the workgroup is associated with.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PubliclyAccessible

A value that specifies whether the workgroup can be accessible from a public network.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Port

The custom port to use when connecting to a workgroup. Valid port ranges are 5431-5455 and 8191-8215. The default is 5439.

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PricePerformanceTarget

_Required_: No

_Type_: <a href="performancetarget.md">PerformanceTarget</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

The map of the key-value pairs used to tag the workgroup.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TrackName

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>256</code>

_Pattern_: <code>^[a-zA-Z0-9_]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Workgroup

_Required_: No

_Type_: <a href="workgroup.md">Workgroup</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the WorkgroupName.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### WorkgroupId

Returns the <code>WorkgroupId</code> value.

#### WorkgroupArn

Returns the <code>WorkgroupArn</code> value.

#### WorkgroupName

Returns the <code>WorkgroupName</code> value.

#### NamespaceName

Returns the <code>NamespaceName</code> value.

#### EnhancedVpcRouting

Returns the <code>EnhancedVpcRouting</code> value.

#### ParameterKey

Returns the <code>ParameterKey</code> value.

#### ParameterValue

Returns the <code>ParameterValue</code> value.

#### SecurityGroupIds

Returns the <code>SecurityGroupIds</code> value.

#### SubnetIds

Returns the <code>SubnetIds</code> value.

#### TrackName

Returns the <code>TrackName</code> value.

#### Status

Returns the <code>Status</code> value.

#### Address

Returns the <code>Address</code> value.

#### Port

Returns the <code>Port</code> value.

#### VpcEndpointId

Returns the <code>VpcEndpointId</code> value.

#### VpcId

Returns the <code>VpcId</code> value.

#### NetworkInterfaceId

Returns the <code>NetworkInterfaceId</code> value.

#### SubnetId

Returns the <code>SubnetId</code> value.

#### PrivateIpAddress

Returns the <code>PrivateIpAddress</code> value.

#### AvailabilityZone

Returns the <code>AvailabilityZone</code> value.

#### PubliclyAccessible

Returns the <code>PubliclyAccessible</code> value.

#### CreationDate

Returns the <code>CreationDate</code> value.
