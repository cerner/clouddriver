package com.netflix.spinnaker.clouddriver.controllers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.spinnaker.clouddriver.model.Function;
import com.netflix.spinnaker.clouddriver.model.FunctionProvider;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;

@RestController
@RequestMapping("/functions/{account}/{region}")
public class FunctionController<T extends Function> {

  @Autowired
  private List<FunctionProvider<T>> functionProviders;

  @RequestMapping(method = RequestMethod.GET)
  List<T> list(@PathVariable final String account,
               @PathVariable final String region) {
    return functionProviders.stream()
                            .flatMap(functionProvider -> functionProvider.listAll(account, region).stream())
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
  }

  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  T byName(@PathVariable final String account,
           @PathVariable final String region,
           @PathVariable final String name) {
    return functionProviders.stream()
                            .map(functionProvider -> functionProvider.byName(account, region, name))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElseThrow(() -> new NotFoundException("Function cannot be found; name: " + name));
  }
}
