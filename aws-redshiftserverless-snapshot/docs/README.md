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
        "<a href="#retentionperiod" title="RetentionPeriod">RetentionPeriod</a>" : <i>Integer</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
    }
}
</pre>

### YAML

<pre>
Type: AWS::RedshiftServerless::Snapshot
Properties:
    <a href="#snapshotname" title="SnapshotName">SnapshotName</a>: <i>String</i>
    <a href="#namespacename" title="NamespaceName">NamespaceName</a>: <i>String</i>
    <a href="#retentionperiod" title="RetentionPeriod">RetentionPeriod</a>: <i>Integer</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
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

#### RetentionPeriod

The retention period of the snapshot.

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

An array of key-value pairs to apply to this resource.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the SnapshotName.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Snapshot

Returns the <code>Snapshot</code> value.

#### OwnerAccount

The owner account of the snapshot.

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
