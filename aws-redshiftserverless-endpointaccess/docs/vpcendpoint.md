# AWS::RedshiftServerless::EndpointAccess VpcEndpoint

The connection endpoint for connecting to Amazon Redshift Serverless.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#vpcendpointid" title="VpcEndpointId">VpcEndpointId</a>" : <i>String</i>,
    "<a href="#vpcid" title="VpcId">VpcId</a>" : <i>String</i>,
    "<a href="#networkinterfaces" title="NetworkInterfaces">NetworkInterfaces</a>" : <i>[ <a href="networkinterface.md">NetworkInterface</a>, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#vpcendpointid" title="VpcEndpointId">VpcEndpointId</a>: <i>String</i>
<a href="#vpcid" title="VpcId">VpcId</a>: <i>String</i>
<a href="#networkinterfaces" title="NetworkInterfaces">NetworkInterfaces</a>: <i>
      - <a href="networkinterface.md">NetworkInterface</a></i>
</pre>

## Properties

#### VpcEndpointId

The connection endpoint ID for connecting to Amazon Redshift Serverless.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### VpcId

The VPC identifier that the endpoint is associated with.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NetworkInterfaces

One or more network interfaces of the endpoint. Also known as an interface endpoint.

_Required_: No

_Type_: List of <a href="networkinterface.md">NetworkInterface</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

