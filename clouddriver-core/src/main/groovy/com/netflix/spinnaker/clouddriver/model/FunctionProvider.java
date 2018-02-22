package com.netflix.spinnaker.clouddriver.model;

import java.util.List;

public interface FunctionProvider<T extends Function> {

  List<T> listAll(String account, String region);
  T byName(String account, String region, String name);
}
