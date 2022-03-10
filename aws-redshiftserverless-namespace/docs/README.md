# AWS::RedshiftServerless::Namespace

Definition of AWS::RedshiftServerless::Namespace Resource Type

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::RedshiftServerless::Namespace",
    "Properties" : {
        "<a href="#adminuserpassword" title="AdminUserPassword">AdminUserPassword</a>" : <i>String</i>,
        "<a href="#adminusername" title="AdminUsername">AdminUsername</a>" : <i>String</i>,
        "<a href="#dbname" title="DbName">DbName</a>" : <i>String</i>,
        "<a href="#defaultiamrolearn" title="DefaultIamRoleArn">DefaultIamRoleArn</a>" : <i>String</i>,
        "<a href="#iamroles" title="IamRoles">IamRoles</a>" : <i>[ String, ... ]</i>,
        "<a href="#kmskeyid" title="KmsKeyId">KmsKeyId</a>" : <i>String</i>,
        "<a href="#logexports" title="LogExports">LogExports</a>" : <i>[ String, ... ]</i>,
        "<a href="#namespacename" title="NamespaceName">NamespaceName</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::RedshiftServerless::Namespace
Properties:
    <a href="#adminuserpassword" title="AdminUserPassword">AdminUserPassword</a>: <i>String</i>
    <a href="#adminusername" title="AdminUsername">AdminUsername</a>: <i>String</i>
    <a href="#dbname" title="DbName">DbName</a>: <i>String</i>
    <a href="#defaultiamrolearn" title="DefaultIamRoleArn">DefaultIamRoleArn</a>: <i>String</i>
    <a href="#iamroles" title="IamRoles">IamRoles</a>: <i>
      - String</i>
    <a href="#kmskeyid" title="KmsKeyId">KmsKeyId</a>: <i>String</i>
    <a href="#logexports" title="LogExports">LogExports</a>: <i>
      - String</i>
    <a href="#namespacename" title="NamespaceName">NamespaceName</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
</pre>

## Properties

#### AdminUserPassword

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AdminUsername

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DbName

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DefaultIamRoleArn

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### IamRoles

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### KmsKeyId

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### LogExports

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NamespaceName

_Required_: Yes

_Type_: String

_Minimum_: <code>3</code>

_Maximum_: <code>64</code>

_Pattern_: <code>^[a-z0-9]+$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the NamespaceName.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Namespace

Returns the <code>Namespace</code> value.
