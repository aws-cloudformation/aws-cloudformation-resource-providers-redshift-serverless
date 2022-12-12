# AWS::RedshiftServerless::Workgroup Workgroup

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#configparameters" title="ConfigParameters">ConfigParameters</a>" : <i>[ <a href="configparameter.md">ConfigParameter</a>, ... ]</i>,
    "<a href="#endpoint" title="Endpoint">Endpoint</a>" : <i><a href="endpoint.md">Endpoint</a></i>,
    "<a href="#publiclyaccessible" title="PubliclyAccessible">PubliclyAccessible</a>" : <i>Boolean</i>,
    "<a href="#port" title="Port">Port</a>" : <i>Integer</i>,
    "<a href="#creationdate" title="CreationDate">CreationDate</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#configparameters" title="ConfigParameters">ConfigParameters</a>: <i>
      - <a href="configparameter.md">ConfigParameter</a></i>
<a href="#endpoint" title="Endpoint">Endpoint</a>: <i><a href="endpoint.md">Endpoint</a></i>
<a href="#publiclyaccessible" title="PubliclyAccessible">PubliclyAccessible</a>: <i>Boolean</i>
<a href="#port" title="Port">Port</a>: <i>Integer</i>
<a href="#creationdate" title="CreationDate">CreationDate</a>: <i>String</i>
</pre>

## Properties

#### ConfigParameters

_Required_: No

_Type_: List of <a href="configparameter.md">ConfigParameter</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Endpoint

_Required_: No

_Type_: <a href="endpoint.md">Endpoint</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PubliclyAccessible

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Port

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### CreationDate

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
