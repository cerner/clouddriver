package com.netflix.spinnaker.clouddriver.model;

/**
 * Represents a function, a serverless computation
 */
public interface Function {

  String getName();
  String getDescription();
  String getVersion();
}
