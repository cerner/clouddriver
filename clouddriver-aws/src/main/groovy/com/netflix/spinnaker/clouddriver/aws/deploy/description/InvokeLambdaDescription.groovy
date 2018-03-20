package com.netflix.spinnaker.clouddriver.aws.deploy.description

class InvokeLambdaDescription extends AbstractAmazonCredentialsDescription {
  String account
  String region
  String functionName
  Map payload
}
