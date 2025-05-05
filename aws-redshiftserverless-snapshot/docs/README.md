# AWS::RedshiftServerless::Snapshot

Resource Type definition for AWS::RedshiftServerless::Snapshot Resource Type.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::RedshiftServerless::Snapshot",
    "Properties" : {
        "<a href="#snapshotname" title="SnapshotName">SnapshotName</a>" : <i>String</i>,
        "<a href="#namespacename" title="NamespaceName">NamespaceName</a>" : <i>String</i>,
        "<a href="#namespacearn" title="NamespaceArn">NamespaceArn</a>" : <i>String</i>,
        "<a href="#snapshotarn" title="SnapshotArn">SnapshotArn</a>" : <i>String</i>,
        "<a href="#snapshotidentifier" title="SnapshotIdentifier">SnapshotIdentifier</a>" : <i>String</i>,
        "<a href="#snapshotcreatetime" title="SnapshotCreateTime">SnapshotCreateTime</a>" : <i>String</i>,
        "<a href="#adminusername" title="AdminUsername">AdminUsername</a>" : <i>String</i>,
        "<a href="#kmskeyid" title="KmsKeyId">KmsKeyId</a>" : <i>String</i>,
        "<a href="#owneraccount" title="OwnerAccount">OwnerAccount</a>" : <i>String</i>,
        "<a href="#retentionperiod" title="RetentionPeriod">RetentionPeriod</a>" : <i>Integer</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#snapshot" title="Snapshot">Snapshot</a>" : <i><a href="snapshot.md">Snapshot</a></i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::RedshiftServerless::Snapshot
Properties:
    <a href="#snapshotname" title="SnapshotName">SnapshotName</a>: <i>String</i>
    <a href="#namespacename" title="NamespaceName">NamespaceName</a>: <i>String</i>
    <a href="#namespacearn" title="NamespaceArn">NamespaceArn</a>: <i>String</i>
    <a href="#snapshotarn" title="SnapshotArn">SnapshotArn</a>: <i>String</i>
    <a href="#snapshotidentifier" title="SnapshotIdentifier">SnapshotIdentifier</a>: <i>String</i>
    <a href="#snapshotcreatetime" title="SnapshotCreateTime">SnapshotCreateTime</a>: <i>String</i>
    <a href="#adminusername" title="AdminUsername">AdminUsername</a>: <i>String</i>
    <a href="#kmskeyid" title="KmsKeyId">KmsKeyId</a>: <i>String</i>
    <a href="#owneraccount" title="OwnerAccount">OwnerAccount</a>: <i>String</i>
    <a href="#retentionperiod" title="RetentionPeriod">RetentionPeriod</a>: <i>Integer</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#snapshot" title="Snapshot">Snapshot</a>: <i><a href="snapshot.md">Snapshot</a></i>
</pre>

## Properties

#### SnapshotName

The name of the snapshot.

_Required_: Yes

_Type_: String

_Minimum Length_: <code>3</code>

_Maximum Length_: <code>64</code>

_Pattern_: <code>^(?=^[a-z0-9-]+$).{3,64}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### NamespaceName

The namespace the snapshot is associated with.

_Required_: No

_Type_: String

_Minimum Length_: <code>3</code>

_Maximum Length_: <code>64</code>

_Pattern_: <code>^(?=^[a-z0-9-]+$).{3,64}$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### NamespaceArn

The ARN for the Redshift namespace that the snapshot is associated with.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SnapshotArn

The ARN for the snapshot.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SnapshotIdentifier

The identifier for the snapshot.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SnapshotCreateTime

The creation time of the snapshot.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AdminUsername

The username of the database user that owns the snapshot.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### KmsKeyId

The KMS key ID used to encrypt the snapshot.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### OwnerAccount

The owner account of the snapshot.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RetentionPeriod

The retention period of the snapshot.

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

An array of key-value pairs to apply to this resource.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Snapshot

_Required_: No

_Type_: <a href="snapshot.md">Snapshot</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the SnapshotName.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### SnapshotName

Returns the <code>SnapshotName</code> value.

#### NamespaceName

Returns the <code>NamespaceName</code> value.

#### NamespaceArn

Returns the <code>NamespaceArn</code> value.

#### SnapshotArn

Returns the <code>SnapshotArn</code> value.

#### SnapshotCreateTime

Returns the <code>SnapshotCreateTime</code> value.

#### Status

Returns the <code>Status</code> value.

#### AdminUsername

Returns the <code>AdminUsername</code> value.

#### KmsKeyId

Returns the <code>KmsKeyId</code> value.

#### OwnerAccount

Returns the <code>OwnerAccount</code> value.

#### RetentionPeriod

Returns the <code>RetentionPeriod</code> value.

