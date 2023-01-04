# AWS::RedshiftServerless::Workgroup Endpoint

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#address" title="Address">Address</a>" : <i>String</i>,
    "<a href="#port" title="Port">Port</a>" : <i>Integer</i>,
    "<a href="#vpcendpoints" title="VpcEndpoints">VpcEndpoints</a>" : <i>[ <a href="vpcendpoint.md">VpcEndpoint</a>, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#address" title="Address">Address</a>: <i>String</i>
<a href="#port" title="Port">Port</a>: <i>Integer</i>
<a href="#vpcendpoints" title="VpcEndpoints">VpcEndpoints</a>: <i>
      - <a href="vpcendpoint.md">VpcEndpoint</a></i>
</pre>

## Properties

#### Address

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Port

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VpcEndpoints

_Required_: No

_Type_: List of <a href="vpcendpoint.md">VpcEndpoint</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

