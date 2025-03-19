# AWS::RedshiftServerless::Workgroup Workgroup

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#basecapacity" title="BaseCapacity">BaseCapacity</a>" : <i>Integer</i>,
    "<a href="#maxcapacity" title="MaxCapacity">MaxCapacity</a>" : <i>Integer</i>,
    "<a href="#configparameters" title="ConfigParameters">ConfigParameters</a>" : <i>[ <a href="configparameter.md">ConfigParameter</a>, ... ]</i>,
    "<a href="#endpoint" title="Endpoint">Endpoint</a>" : <i><a href="endpoint.md">Endpoint</a></i>,
    "<a href="#priceperformancetarget" title="PricePerformanceTarget">PricePerformanceTarget</a>" : <i><a href="performancetarget.md">PerformanceTarget</a></i>,
}
</pre>

### YAML

<pre>
<a href="#basecapacity" title="BaseCapacity">BaseCapacity</a>: <i>Integer</i>
<a href="#maxcapacity" title="MaxCapacity">MaxCapacity</a>: <i>Integer</i>
<a href="#configparameters" title="ConfigParameters">ConfigParameters</a>: <i>
      - <a href="configparameter.md">ConfigParameter</a></i>
<a href="#endpoint" title="Endpoint">Endpoint</a>: <i><a href="endpoint.md">Endpoint</a></i>
<a href="#priceperformancetarget" title="PricePerformanceTarget">PricePerformanceTarget</a>: <i><a href="performancetarget.md">PerformanceTarget</a></i>
</pre>

## Properties

#### BaseCapacity

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### MaxCapacity

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ConfigParameters

_Required_: No

_Type_: List of <a href="configparameter.md">ConfigParameter</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Endpoint

_Required_: No

_Type_: <a href="endpoint.md">Endpoint</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PricePerformanceTarget

_Required_: No

_Type_: <a href="performancetarget.md">PerformanceTarget</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
