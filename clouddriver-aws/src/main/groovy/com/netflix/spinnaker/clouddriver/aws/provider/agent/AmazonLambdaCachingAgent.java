package com.netflix.spinnaker.clouddriver.aws.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.netflix.spinnaker.cats.agent.AccountAware;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.aws.cache.Keys;
import com.netflix.spinnaker.clouddriver.aws.provider.AwsInfrastructureProvider;
import com.netflix.spinnaker.clouddriver.aws.provider.AwsProvider;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials;

public class AmazonLambdaCachingAgent implements CachingAgent, AccountAware {

  static final Set<AgentDataType> types = ImmutableSet.of(AUTHORITATIVE.forType(Keys.Namespace.FUNCTION.getNs()));
  private final String agentType;
  private final AmazonClientProvider amazonClientProvider;
  private final NetflixAmazonCredentials account;
  private final String region;
  private final ObjectMapper objectMapper;

  public AmazonLambdaCachingAgent(final AmazonClientProvider amazonClientProvider,
                                  final NetflixAmazonCredentials account,
                                  final String region,
                                  final ObjectMapper objectMapper) {
    this.amazonClientProvider = amazonClientProvider;
    this.account = account;
    this.region = region;
    this.objectMapper = objectMapper;
    this.agentType = getAccountName() + region + AmazonLambdaCachingAgent.class.getSimpleName();
  }

  @Override
  public String getAgentType() {
    return agentType;
  }

  @Override
  public String getProviderName() {
    return AwsProvider.PROVIDER_NAME;
  }

  @Override
  public String getAccountName() {
    return account.getName();
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }

  @Override
  public CacheResult loadData(final ProviderCache providerCache) {
    AWSLambda lambda = amazonClientProvider.getAmazonLambda(account.getName(), account.getCredentialsProvider(), region);
    final ListFunctionsResult listFunctionsResult = lambda.listFunctions();

    final List<CacheData> cacheData = listFunctionsResult.getFunctions()
                                                       .stream()
                                                       .map(this::convertFnResult)
                                                       .collect(Collectors.toList());

    return new DefaultCacheResult(ImmutableMap.of(Keys.Namespace.FUNCTION.getNs(), cacheData));
  }

  private DefaultCacheData convertFnResult(final FunctionConfiguration functionConfiguration) {
    return new DefaultCacheData(Keys.getFunctionKey(functionConfiguration.getFunctionName(), region, account.getName()),
                                objectMapper.convertValue(functionConfiguration, AwsInfrastructureProvider.ATTRIBUTES),
                                ImmutableMap.of());
  }
}
