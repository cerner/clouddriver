package com.netflix.spinnaker.clouddriver.aws.provider.agent

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.FunctionConfiguration
import com.amazonaws.services.lambda.model.ListFunctionsResult
import com.netflix.awsobjectmapper.AmazonObjectMapperConfigurer
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.aws.cache.Keys
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials
import spock.lang.Specification
import spock.lang.Subject

class AmazonLambdaCachingAgentSpec extends Specification {
  static final String account = 'account'
  static final String region = 'us-west-2'

  AWSLambda awsLambda = Mock(AWSLambda)
  ProviderCache providerCache = Mock(ProviderCache)

  AWSCredentialsProvider credentialsProvider = Mock(AWSCredentialsProvider)

  NetflixAmazonCredentials creds = Stub(NetflixAmazonCredentials) {
    getName() >> account
    getCredentialsProvider() >> credentialsProvider
  }

  AmazonClientProvider clientProvider = Stub(AmazonClientProvider) {
    getAmazonLambda(account, credentialsProvider, region) >> awsLambda
  }

  def objectMapper = AmazonObjectMapperConfigurer.createConfigured()

  @Subject
  AmazonLambdaCachingAgent agent = new AmazonLambdaCachingAgent(clientProvider, creds, region, objectMapper)

  def "populates cache on initial run"() {
    when:
    def result = agent.loadData(providerCache)

    then:
    1 * awsLambda.listFunctions() >> new ListFunctionsResult(functions: [
            new FunctionConfiguration(functionName: "testname1", description: "test function 1", handler: "index1"),
            new FunctionConfiguration(functionName: "testname2", description: "test function 2", handler: "index2"),
    ])

    with(result.cacheResults.get(Keys.Namespace.FUNCTION.ns)) { List<CacheData> cache ->
      cache.size() == 2
      cache.find { it.id == Keys.getFunctionKey('testname1', region, account)}
      cache.find { it.attributes.get("handler") == "index1" }
      cache.find { it.id == Keys.getFunctionKey('testname2', region, account)}
      cache.find { it.attributes.get("handler") == "index2" }
    }
  }

}
