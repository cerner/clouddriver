package com.netflix.spinnaker.clouddriver.aws.deploy.ops;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.clouddriver.aws.deploy.description.InvokeLambdaDescription;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import com.netflix.spinnaker.clouddriver.data.task.Task;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;

public class InvokeLambdaAtomicOperation implements AtomicOperation<InvokeLambdaResponse> {

  private static final String PHASE = "INVOKE_LAMBDA";
  private final InvokeLambdaDescription description;
  private final ObjectMapper objectMapper;

  @Autowired
  AmazonClientProvider amazonClientProvider;

  public InvokeLambdaAtomicOperation(final InvokeLambdaDescription description, final ObjectMapper objectMapper) {
    this.description = description;
    this.objectMapper = objectMapper;
  }

  @Override
  public InvokeLambdaResponse operate(final List priorOutputs) {
    final AWSLambda lambda = amazonClientProvider.getAmazonLambda(description.getAccount(),
                                                                  description.getCredentials().getCredentialsProvider(),
                                                                  description.getRegion());

    final InvokeRequest request = new InvokeRequest();
    try {
      request.withFunctionName(description.getFunctionName())
             .withPayload(objectMapper.writeValueAsString(description.getPayload()));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to convert payload to string", e);
    }

    task().updateStatus(PHASE, "invoking function " + description.getFunctionName());
    final InvokeResult result = lambda.invoke(request);

    task().updateStatus(PHASE, description.getFunctionName() +
      " returned status " + result.getStatusCode() +
      " request Id: " + lambda.getCachedResponseMetadata(request).getRequestId());

    final String error = result.getFunctionError();
    try {
      final String response = objectMapper.readValue(result.getPayload().array(), String.class);

      return InvokeLambdaResponse.builder()
                                 .error(error)
                                 .response(response)
                                 .build();
    } catch (final IOException e) {
      throw new RuntimeException("Could not deserialize Lambda result", e);
    }
  }

  private static Task task() {
    return TaskRepository.threadLocalTask.get();
  }
}
