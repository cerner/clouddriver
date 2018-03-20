package com.netflix.spinnaker.clouddriver.aws.deploy.converters

import com.netflix.spinnaker.clouddriver.aws.AmazonOperation
import com.netflix.spinnaker.clouddriver.aws.deploy.description.InvokeLambdaDescription
import com.netflix.spinnaker.clouddriver.aws.deploy.ops.InvokeLambdaAtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import org.springframework.stereotype.Component

@AmazonOperation(AtomicOperations.INVOKE_FUNCTION)
@Component("invokeLambdaDescription")
class InvokeLambdaAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  AtomicOperation convertOperation(final Map input) {
    new InvokeLambdaAtomicOperation(convertDescription(input), objectMapper)
  }

  @Override
  InvokeLambdaDescription convertDescription(final Map input) {
    def converted = objectMapper.convertValue(input, InvokeLambdaDescription)
    converted.credentials = getCredentialsObject(input.credentials as String)
    converted
  }
}
