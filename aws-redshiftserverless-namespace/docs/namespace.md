# AWS::RedshiftServerless::Namespace Namespace

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#namespacearn" title="NamespaceArn">NamespaceArn</a>" : <i>String</i>,
    "<a href="#namespaceid" title="NamespaceId">NamespaceId</a>" : <i>String</i>,
    "<a href="#namespacename" title="NamespaceName">NamespaceName</a>" : <i>String</i>,
    "<a href="#adminusername" title="AdminUsername">AdminUsername</a>" : <i>String</i>,
    "<a href="#dbname" title="DbName">DbName</a>" : <i>String</i>,
    "<a href="#kmskeyid" title="KmsKeyId">KmsKeyId</a>" : <i>String</i>,
    "<a href="#defaultiamrolearn" title="DefaultIamRoleArn">DefaultIamRoleArn</a>" : <i>String</i>,
    "<a href="#iamroles" title="IamRoles">IamRoles</a>" : <i>[ String, ... ]</i>,
    "<a href="#logexports" title="LogExports">LogExports</a>" : <i>[ String, ... ]</i>,
    "<a href="#status" title="Status">Status</a>" : <i>String</i>,
    "<a href="#creationdate" title="CreationDate">CreationDate</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#namespacearn" title="NamespaceArn">NamespaceArn</a>: <i>String</i>
<a href="#namespaceid" title="NamespaceId">NamespaceId</a>: <i>String</i>
<a href="#namespacename" title="NamespaceName">NamespaceName</a>: <i>String</i>
<a href="#adminusername" title="AdminUsername">AdminUsername</a>: <i>String</i>
<a href="#dbname" title="DbName">DbName</a>: <i>String</i>
<a href="#kmskeyid" title="KmsKeyId">KmsKeyId</a>: <i>String</i>
<a href="#defaultiamrolearn" title="DefaultIamRoleArn">DefaultIamRoleArn</a>: <i>String</i>
<a href="#iamroles" title="IamRoles">IamRoles</a>: <i>
      - String</i>
<a href="#logexports" title="LogExports">LogExports</a>: <i>
      - String</i>
<a href="#status" title="Status">Status</a>: <i>String</i>
<a href="#creationdate" title="CreationDate">CreationDate</a>: <i>String</i>
</pre>

## Properties

#### NamespaceArn

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NamespaceId

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NamespaceName

_Required_: No

_Type_: String

_Minimum Length_: <code>3</code>

_Maximum Length_: <code>64</code>

_Pattern_: <code>^[a-z0-9-]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AdminUsername

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DbName

_Required_: No

_Type_: String

_Pattern_: <code>[a-zA-Z][a-zA-Z_0-9+.@-]*</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### KmsKeyId

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

#### LogExports

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Status

_Required_: No

_Type_: String

_Allowed Values_: <code>AVAILABLE</code> | <code>MODIFYING</code> | <code>DELETING</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### CreationDate

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
