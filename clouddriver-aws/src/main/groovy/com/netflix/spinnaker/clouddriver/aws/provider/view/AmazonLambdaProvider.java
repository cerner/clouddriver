package com.netflix.spinnaker.clouddriver.aws.provider.view;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.netflix.awsobjectmapper.AmazonObjectMapperConfigurer;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.aws.cache.Keys;
import com.netflix.spinnaker.clouddriver.aws.model.AmazonLambda;
import com.netflix.spinnaker.clouddriver.model.FunctionProvider;

@Component
public class AmazonLambdaProvider implements FunctionProvider<AmazonLambda> {

  private final Cache cacheView;
  private final ObjectMapper objectMapper;

  @Autowired
  public AmazonLambdaProvider(final Cache cacheView,
                              final ObjectMapper objectMapper) {
    this.cacheView = cacheView;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<AmazonLambda> listAll(final String account, final String region) {
    return loadResults(cacheView.filterIdentifiers(Keys.Namespace.FUNCTION.getNs(),
                                                   Keys.getFunctionKey("*", region, account)));
  }

  @Override
  public AmazonLambda byName(final String account, final String region, final String name) {
    return Iterables.getFirst(
      loadResults(cacheView.filterIdentifiers(Keys.Namespace.FUNCTION.getNs(),
                                              Keys.getFunctionKey(name, region, account))), null);
  }

  List<AmazonLambda> loadResults(final Collection<String> identifiers) {
    Collection<CacheData> data = cacheView.getAll(Keys.Namespace.FUNCTION.getNs(), identifiers, RelationshipCacheFilter.none());
    return data.isEmpty()? ImmutableList.of() : data.stream()
                                                    .map(this::fromCache)
                                                    .collect(Collectors.toList());
  }

  AmazonLambda fromCache(final CacheData cacheData) {
    final Map<String, String> parts = Keys.parse(cacheData.getId());
    final FunctionConfiguration functionConfiguration = objectMapper.convertValue(cacheData.getAttributes(), FunctionConfiguration.class);
    return AmazonLambda.builder()
                       .description(functionConfiguration.getDescription())
                       .handler(functionConfiguration.getHandler())
                       .name(functionConfiguration.getFunctionName())
                       .region(parts.get("region"))
                       .runtime(functionConfiguration.getRuntime())
                       .version(functionConfiguration.getVersion())
                       .build();
  }
}
