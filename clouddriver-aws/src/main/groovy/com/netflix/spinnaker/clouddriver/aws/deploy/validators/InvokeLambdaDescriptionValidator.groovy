package com.netflix.spinnaker.clouddriver.aws.deploy.validators

import com.netflix.spinnaker.clouddriver.aws.AmazonOperation
import com.netflix.spinnaker.clouddriver.aws.deploy.description.InvokeLambdaDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import org.springframework.stereotype.Component
import org.springframework.validation.Errors


//TODO what other validation do we need?
@AmazonOperation(AtomicOperations.INVOKE_FUNCTION)
@Component("invokeLambdaDescriptionValidator")
class InvokeLambdaDescriptionValidator extends AmazonDescriptionValidationSupport<InvokeLambdaDescription> {
  @Override
  void validate(final List priorDescriptions, final InvokeLambdaDescription description, final Errors errors) {
    def key = InvokeLambdaDescription.class.simpleName
    if (!description.functionName) {
      errors.rejectValue("functionName", "${key}.functionName.empty")
    }

    validateRegion(description, description.region, key, errors)
  }
}
