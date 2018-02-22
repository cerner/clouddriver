package com.netflix.spinnaker.clouddriver.aws.model;

import lombok.Builder;
import lombok.Data;

import com.netflix.spinnaker.clouddriver.model.Function;

@Data
@Builder
public class AmazonLambda implements Function {
  final String name;
  final String description;
  final String version;
  final String region;
  final String handler;
  final String runtime;
}
