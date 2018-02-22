package com.netflix.spinnaker.clouddriver.model;

import java.util.List;

public class NoopFunctionProvider implements FunctionProvider<Function> {

  @Override
  public List<Function> listAll(final String account, final String region) {
    return null;
  }

  @Override
  public Function byName(final String account, final String region, final String name) {
    return null;
  }
}
