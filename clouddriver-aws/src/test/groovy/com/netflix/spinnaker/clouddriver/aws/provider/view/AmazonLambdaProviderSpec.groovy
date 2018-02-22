package com.netflix.spinnaker.clouddriver.aws.provider.view

import com.amazonaws.services.lambda.model.FunctionConfiguration
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.awsobjectmapper.AmazonObjectMapperConfigurer
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.CacheFilter
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.clouddriver.aws.cache.Keys
import com.netflix.spinnaker.clouddriver.aws.model.AmazonLambda
import com.netflix.spinnaker.clouddriver.aws.provider.AwsInfrastructureProvider
import spock.lang.Specification
import spock.lang.Subject

class AmazonLambdaProviderSpec extends Specification {
  static final String account = 'account'
  static final String region = 'us-west-2'

  Cache cache = Mock(Cache)
  ObjectMapper objectMapper = AmazonObjectMapperConfigurer.createConfigured()

  @Subject
  AmazonLambdaProvider provider = new AmazonLambdaProvider(cache, objectMapper)

  def "lists all the lambdas of an account in a region"() {
    when:
    def result = provider.listAll(account, region)

    then:
    result == [
      AmazonLambda.builder()
        .name('test1')
        .description('test 1 description')
        .version('test1.0')
        .region(region)
        .handler('index')
        .runtime('node')
        .build(),
      AmazonLambda.builder()
        .name('test2')
        .description('test 2 description')
        .version('test2.0')
        .region(region)
        .handler('index')
        .runtime('node')
        .build(),

    ] as List

    and:
    1 * cache.filterIdentifiers(Keys.Namespace.FUNCTION.ns, "aws:$Keys.Namespace.FUNCTION.ns:*:$account:$region")
    1 * cache.getAll(Keys.Namespace.FUNCTION.ns, _, _ as CacheFilter) >> [
            cacheData(account, region,
            new FunctionConfiguration(
              functionName: 'test1',
              description: 'test 1 description',
              version: 'test1.0',
              handler: 'index',
              runtime: 'node'
            )),
            cacheData(account, region,
              new FunctionConfiguration(
                functionName: 'test2',
                description: 'test 2 description',
                version: 'test2.0',
                handler: 'index',
                runtime: 'node'
              ))
    ]
  }

  def "returns empty list if there are no lambdas in given account in a region"() {
    when:
    def result = provider.listAll(account, region)

    then:
    result == [] as List

    and:
    1 * cache.filterIdentifiers(Keys.Namespace.FUNCTION.ns, "aws:$Keys.Namespace.FUNCTION.ns:*:$account:$region")
    1 * cache.getAll(Keys.Namespace.FUNCTION.ns, _, _ as CacheFilter) >> []
  }

  def "retrieves a function given its name in an account and region"() {
    setup:
    def name = 'test1'

    when:
    def result = provider.byName(account, region, name)

    then:
    result == AmazonLambda.builder()
      .name(name)
      .description('test 1 description')
      .version('test1.0')
      .region(region)
      .handler('index')
      .runtime('node')
      .build()

    and:
    1 * cache.filterIdentifiers(Keys.Namespace.FUNCTION.ns, "aws:$Keys.Namespace.FUNCTION.ns:$name:$account:$region")
    1 * cache.getAll(Keys.Namespace.FUNCTION.ns, _, _ as CacheFilter) >> [
      cacheData(account, region,
        new FunctionConfiguration(
          functionName: name,
          description: 'test 1 description',
          version: 'test1.0',
          handler: 'index',
          runtime: 'node'
        ))
    ]
  }

  def "returns null if a function does not exist in given account and region"() {
    setup:
    def name = 'test1'

    when:
    def result = provider.byName(account, region, name)

    then:
    result == null

    and:
    1 * cache.filterIdentifiers(Keys.Namespace.FUNCTION.ns, "aws:$Keys.Namespace.FUNCTION.ns:$name:$account:$region")
    1 * cache.getAll(Keys.Namespace.FUNCTION.ns, _, _ as CacheFilter) >> []
  }

  CacheData cacheData(String account, String region, FunctionConfiguration functionConfiguration) {
    Map<String, Object> attributes = objectMapper.convertValue(functionConfiguration, AwsInfrastructureProvider.ATTRIBUTES)
    new DefaultCacheData(Keys.getFunctionKey(functionConfiguration.functionName, region, account),
      attributes,
      [:]
    )
  }
}
