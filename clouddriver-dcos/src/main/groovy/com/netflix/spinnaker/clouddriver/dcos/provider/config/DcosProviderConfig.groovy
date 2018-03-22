/*
 * Copyright 2017 Cerner Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.dcos.provider.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.collect.Sets
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.Agent
import com.netflix.spinnaker.cats.provider.ProviderSynchronizerTypeWrapper
import com.netflix.spinnaker.clouddriver.dcos.DcosClientProvider
import com.netflix.spinnaker.clouddriver.dcos.DcosCloudProvider
import com.netflix.spinnaker.clouddriver.dcos.provider.DcosProvider

import com.netflix.spinnaker.clouddriver.dcos.provider.agent.DcosLoadBalancerCachingAgent
import com.netflix.spinnaker.clouddriver.dcos.provider.agent.DcosSecretsCachingAgent
import com.netflix.spinnaker.clouddriver.dcos.provider.agent.DcosServerGroupCachingAgent
import com.netflix.spinnaker.clouddriver.dcos.security.DcosAccountCredentials
import com.netflix.spinnaker.clouddriver.dcos.security.DcosClusterCredentials
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.security.ProviderUtils
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.tuple.Pair
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Scope

import java.util.concurrent.ConcurrentHashMap

@Slf4j
@Configuration
class DcosProviderConfig {
  @Bean
  @DependsOn('dcosCredentials')
  DcosProvider dcosProvider(DcosCloudProvider dcosCloudProvider,
                            AccountCredentialsProvider accountCredentialsProvider,
                            AccountCredentialsRepository accountCredentialsRepository,
                            ObjectMapper objectMapper,
                            Registry registry) {

    def provider = new DcosProvider(dcosCloudProvider, Collections.newSetFromMap(new ConcurrentHashMap<Agent, Boolean>()))
    synchronizeDcosProvider(provider, accountCredentialsProvider, accountCredentialsRepository, objectMapper, registry)
    provider
  }

  @Bean
  DcosProviderSynchronizerTypeWrapper dcosProviderSynchronizerTypeWrapper() {
    new DcosProviderSynchronizerTypeWrapper()
  }

  class DcosProviderSynchronizerTypeWrapper implements ProviderSynchronizerTypeWrapper {

    @Override
    Class getSynchronizerType() {
      return DcosProviderSynchronizer
    }
  }

  class DcosProviderSynchronizer {}

  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Bean
  DcosProviderSynchronizer synchronizeDcosProvider(DcosProvider dcosProvider,
                                                   AccountCredentialsProvider accountCredentialsProvider,
                                                   AccountCredentialsRepository accountCredentialsRepository,
                                                   ObjectMapper objectMapper,
                                                   Registry registry) {

    def accounts = ProviderUtils.getScheduledAccounts(dcosProvider)
    Set<DcosAccountCredentials> allAccounts = ProviderUtils.buildThreadSafeSetOfAccounts(accountCredentialsRepository, DcosAccountCredentials)

    objectMapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    def newlyAddedAgents = []

    // Go through all accounts and extract unique cluster credentials by cluster name and UID
    def addedClusters = Sets.newHashSet()

    allAccounts.each { DcosAccountCredentials credentials ->
      if (!accounts.contains(credentials.account)) {

        def allClusterCredentials = credentials.getCredentials().credentials

        allClusterCredentials.each { DcosClusterCredentials clusterCredentials ->

          if (!addedClusters.contains(Pair.of(clusterCredentials.cluster, clusterCredentials.dcosConfig.credentials.uid))) {
            //log.info("Adding caching agents for cluster=${clusterCredentials.cluster} and UID=${clusterCredentials.dcosConfig.credentials.uid}")
            newlyAddedAgents << new DcosServerGroupCachingAgent(allAccounts, clusterCredentials.cluster, credentials, new DcosClientProvider(accountCredentialsProvider), objectMapper, registry)
            newlyAddedAgents << new DcosSecretsCachingAgent(clusterCredentials.cluster, credentials, new DcosClientProvider(accountCredentialsProvider), objectMapper)
            newlyAddedAgents << new DcosLoadBalancerCachingAgent(allAccounts, clusterCredentials.cluster, credentials, new DcosClientProvider(accountCredentialsProvider), objectMapper, registry)

            addedClusters.add(Pair.of(clusterCredentials.cluster, clusterCredentials.dcosConfig.credentials.uid))
          }

          // TODO Need to hand synchronization of accounts - see AWS provider.
        }
      }
    }

    if (!newlyAddedAgents.isEmpty()) {
      dcosProvider.agents.addAll(newlyAddedAgents)
    }

    new DcosProviderSynchronizer()
  }
}
