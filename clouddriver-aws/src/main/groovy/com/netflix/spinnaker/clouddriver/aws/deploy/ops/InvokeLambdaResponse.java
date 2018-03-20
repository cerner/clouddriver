package com.netflix.spinnaker.clouddriver.aws.deploy.ops;

import lombok.Builder;
import lombok.Data;

import com.amazonaws.services.lambda.model.InvokeResult;

@Data
@Builder
class InvokeLambdaResponse {

  /**
   * Response returned from the Lambda; Deserialized value of {@link InvokeResult#getPayload()}
   */
  private final String response;

  /**
   * Error details, if any, from the lambda invocation; Deserialized value of {@link InvokeResult#getFunctionError()}
   */
  private final String error;
}
