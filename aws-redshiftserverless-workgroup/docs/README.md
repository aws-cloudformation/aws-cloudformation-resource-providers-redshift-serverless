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
        "<a href="#enhancedvpcrouting" title="EnhancedVpcRouting">EnhancedVpcRouting</a>" : <i>Boolean</i>,
        "<a href="#configparameters" title="ConfigParameters">ConfigParameters</a>" : <i>[ <a href="configparameter.md">ConfigParameter</a>, ... ]</i>,
        "<a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#subnetids" title="SubnetIds">SubnetIds</a>" : <i>[ String, ... ]</i>,
        "<a href="#publiclyaccessible" title="PubliclyAccessible">PubliclyAccessible</a>" : <i>Boolean</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
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
    <a href="#enhancedvpcrouting" title="EnhancedVpcRouting">EnhancedVpcRouting</a>: <i>Boolean</i>
    <a href="#configparameters" title="ConfigParameters">ConfigParameters</a>: <i>
      - <a href="configparameter.md">ConfigParameter</a></i>
    <a href="#securitygroupids" title="SecurityGroupIds">SecurityGroupIds</a>: <i>
      - String</i>
    <a href="#subnetids" title="SubnetIds">SubnetIds</a>: <i>
      - String</i>
    <a href="#publiclyaccessible" title="PubliclyAccessible">PubliclyAccessible</a>: <i>Boolean</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#workgroup" title="Workgroup">Workgroup</a>: <i><a href="workgroup.md">Workgroup</a></i>
</pre>

## Properties

#### WorkgroupName

_Required_: Yes

_Type_: String

_Minimum Length_: <code>3</code>

_Maximum Length_: <code>64</code>

_Pattern_: <code>^(?=^[a-z0-9-]+$).{3,64}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### NamespaceName

_Required_: No

_Type_: String

_Minimum Length_: <code>3</code>

_Maximum Length_: <code>64</code>

_Pattern_: <code>^(?=^[a-z0-9-]+$).{3,64}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### BaseCapacity

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EnhancedVpcRouting

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ConfigParameters

_Required_: No

_Type_: List of <a href="configparameter.md">ConfigParameter</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SecurityGroupIds

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SubnetIds

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PubliclyAccessible

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

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

#### BaseCapacity

Returns the <code>BaseCapacity</code> value.

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
