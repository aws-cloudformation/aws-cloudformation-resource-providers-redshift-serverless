# AWS::RedshiftServerless::Namespace
The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.

## PreRequisite environment setup
1. Install samcli
   https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html
2. Install pythonenv & dependencies
   ```bash
      python3 --version && pip3 --version && python3 -m venv <virtualenvfolder>
      source <virtualenvfolder>/bin/activate && pip3 install install pip --upgrade && pip3 install cloudformation-cli --upgrade && pip3 install cloudformation-cli-java-plugin --upgrade && pip3 install pre-commit --upgrade && pip3 install cfn-lint --upgrade
   ```

## Update Cloudformation resource version / Register your resource
## Requires: PreRequisite environment setup
## Note: This is required to test your resource in your account. This is a prerequisite for handler tests, contract tests and integration tests
## These steps below will build your code locally and cfn would use the build artifacts to deploy the resource type in your account.
## To verify if the resource is deployed in your account go to Cloudformation -> Activated Extensions -> Privately Registered
1. Build package artifacts, activate python virtualenv, submit new version
   ```bash
   cd <package_folder> && bb && source <virtualenvfolder>/bin/activate && cd <resource_folder> && cfn submit --region <region>
   ```
2. Update the resource version with latest version after cfn submit
   ```bash
   aws cloudformation list-type-versions --type "RESOURCE" --type-name "AWS::RedshiftServerless::Namespace" --region <region>
   aws cloudformation set-type-default-version --type "RESOURCE" --type-name "AWS::RedshiftServerless::Namespace" --region <region> --version-id <max_version_from_previous_step>
   ```
   Verify if the resource has the latest version with your schema changes by calling list-type-versions again

## Testing resource handlers locally
## Requires: PreRequisite environment setup, Set the default aws region ($ aws configure)
## Note: This should be tested in the same region as your deployed resource.
## This would just test the resource handler changes and deploy resource in your account. This uses your local aws credentials
## The permissions provided here doesn't truly represent the necessary permission defined in the schema and need to be updated before you execute contract tests.
## References: 
##    - https://w.amazon.com/bin/view/AWS21/Design/Uluru/Onboarding_Guide/Uluru_OpenSource_And_Developing_In_Amazon/IntegrationTests/

1. cd <resource_folder> && source <virtualenvfolder>/bin/activate && sam local start-lambda
2. cp <resource_folder>/local-test-artifacts/create-<resource>.json cp local-test-artifacts/create-<resource>-<feature>.json
3. Modify feature json with your parameter changes and test handlers are running as expected in separate terminal.
   ```bash
   cfn invoke resource CREATE local-test-artifacts/create-<resource>-<feature>.json
   ```
4. Verify if you have a received a successful response on cfn invoke
5. Verify if the resource is successfully deployed in the account in the region

## Testing if changes work with aws cloudformation create-stack
## This would test if your resource deploys correctly in your account if invoke using aws cloudformation create-stack
## This uses your local aws credentials. ## The permissions provided here doesn't truly represent the necessary permission defined in the schema and need to be updated before you execute contract tests.
## Requires: PreRequisite environment setup, Update Cloudformation resource version
## Note: This should be tested in the same region as your deployed resource.
## References:
##    - https://docs.aws.amazon.com/cli/latest/reference/cloudformation/create-stack.html

1. cp <resource_folder>/local-test-artifacts/create-<resource>.yaml cp local-test-artifacts/create-<resource>-<feature>.json 
2. cd <resource_folder> && aws cloudformation create-stack --stack-name <stack-name> --template-body file://local-test-artifacts/create-<resource>-<feature>.yaml> --region <region>
3. Verify if the cloudformation template is deployed successfully into your account.

## Testing if changes work with contract tests
## CTV2 are a bunch of tests run by Cloudformation team to test if resource action perform as expected.
## This would create a Step function which executes contract tests in your account for the deployed resource
## Attention: Cloudformation would not publish a new version in a prod region if contract test fail
## Requires: PreRequisite environment setup, Update Cloudformation resource version
## Note: This should be tested in the same region as your deployed resource.
## This uses schema properties and permissions defined in the schema and need to be updated before you execute contract tests.
## References:
## - https://w.amazon.com/bin/view/AWS/CloudFormation/Teams/ProviderEx/RP-Framework/Projects/UluruContractTests/
1. Build artifacts and copy to your bucket
   ```bash
   bb && cp <Packagefolder>/build/packaging_additional_published_artifacts/
   ```
2. Local Contract Tests expect credentials to be present in terminal before execution.
   ```bash
   export ISENGARD_PRODUCTION_ACCOUNT=false
   export AWS_ACCESS_KEY_ID=<AWS_ACCESS_KEY_ID>
   export AWS_SECRET_ACCESS_KEY=<AWS_SECRET_ACCESS_KEY>
   export AWS_SESSION_TOKEN=<AWS_SESSION_TOKEN>
   ```
3. Run a docker image that deploys resources to run CTV2 in your account
   ```bash
   docker run --env AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
   --env AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
   --env AWS_SESSION_TOKEN=$AWS_SESSION_TOKEN \
   --env AWS_DEFAULT_REGION=us-west-2 \
   -p 9000:8080 public.ecr.aws/j9c3o4f9/contract-tests:latest
   ```
4. In a separate terminal window, invoke CTV2
   ```bash
   curl -XPOST "http://localhost:9000/2015-03-31/functions/function/invocations" -d '{ "TypeName": "<ResourceType>", "Bucket": "<S3_Artifact_Bucket>", "Key": "<artifact_zip_file>" }'
   ```
5. Go to Step function and verify if state machine is created and the execution for the above invocation is successful for that resource

## Testing if changes work with Integration tests
## Integration Tests are bunch of cloudformation deployments to check if stack actions are working as expected
## This would create and delete cloudformation stacks in your accounts against the latest version of your resource
## Requires: PreRequisite environment setup, Update Cloudformation resource version
## Note: This should be tested in the same region as your deployed resource.
## References:
## - https://w.amazon.com/bin/view/AWS21/Design/Uluru/Onboarding_Guide/Integration_Test_Guide

1. Running whole test suite:
   ```bash
      cd <integration_test_package> && bb release && bb integ-local
   ```
2. Running test suite for Namespace Resource:
   ```bash
      cd <integration_test_package> && bb integ-local --tests com.aws.redshiftserverless.cfnregistry.integration.resources.Namespace.NamespaceIntegrationTests
   ```
3. Running individual test
   ```bash
      cd <integration_test_package> && bb release && bb integ-local --tests com.aws.redshiftserverless.cfnregistry.integration.resources.Namespace.NamespaceIntegrationTests.<testName>
   ```
