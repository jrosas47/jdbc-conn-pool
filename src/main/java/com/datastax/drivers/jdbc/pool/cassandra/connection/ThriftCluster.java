/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.drivers.jdbc.pool.cassandra.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.drivers.jdbc.pool.cassandra.service.ExceptionsTranslator;
import com.datastax.drivers.jdbc.pool.cassandra.service.ExceptionsTranslatorImpl;
import com.datastax.drivers.jdbc.pool.cassandra.service.FailoverPolicy;
import com.datastax.drivers.jdbc.pool.cassandra.service.JmxMonitor;

/**
 * A cluster instance the client side representation of a cassandra server cluster.
 *
 * The cluster is usually the main entry point for programs using hector. To start operating on
 * cassandra cluster you first get or create a cluster, then a keyspace operator for the keyspace
 * you're interested in and then create mutations of queries
 * <code>
 * //get a cluster:
 * Cluster cluster = getOrCreateCluster("MyCluster", new CassandraHostConfigurator("127.0.0.1:9170"));
 * //get a keyspace from this cluster:
 * Keyspace ko = createKeyspace("Keyspace1", cluster);
 * //Create a mutator:
 * Mutator m = createMutator(ko);
 * // Make a mutation:
 * MutationResult mr = m.insert("key", cf, createColumn("name", "value", serializer, serializer));
 * </code>
 *
 * THREAD SAFETY: This class is thread safe.
 *
 * @author Ran Tavory
 * @author zznate
 */
public class ThriftCluster implements Cluster {

  private static final Map<String, String> EMPTY_CREDENTIALS = Collections.emptyMap();

  private final Logger log = LoggerFactory.getLogger(ThriftCluster.class);
  
  /**
   * Linked to Cassandra StorageProxy.
   */
  private static final int RING_DELAY = 30 * 1000; // delay after which we assume ring has stablized

  protected final HConnectionManager connectionManager;
  private final String name;
  private final CassandraHostConfigurator configurator;
  private final FailoverPolicy failoverPolicy;
  private final CassandraClientMonitor cassandraClientMonitor;
  private Set<String> knownClusterHosts;
  private Set<CassandraHost> knownPoolHosts;
  protected final ExceptionsTranslator xtrans;
  private final Map<String, String> credentials;

  public ThriftCluster(String clusterName, CassandraHostConfigurator cassandraHostConfigurator) {
    this(clusterName, cassandraHostConfigurator, EMPTY_CREDENTIALS);
  }

  public ThriftCluster(String clusterName, CassandraHostConfigurator cassandraHostConfigurator, Map<String, String> credentials) {
    connectionManager = new HConnectionManager(clusterName, cassandraHostConfigurator);
    name = clusterName;
    configurator = cassandraHostConfigurator;
    failoverPolicy = FailoverPolicy.ON_FAIL_TRY_ALL_AVAILABLE;
    cassandraClientMonitor = JmxMonitor.getInstance().getCassandraMonitor(connectionManager);
    xtrans = new ExceptionsTranslatorImpl();
    this.credentials = Collections.unmodifiableMap(credentials);
  }

  @Override
  public HConnectionManager getConnectionManager() {
    return connectionManager;
  }

  /* (non-Javadoc)
   * @see me.prettyprint.cassandra.service.Cluster#getKnownPoolHosts(boolean)
   */
  @Override
  public Set<CassandraHost> getKnownPoolHosts(boolean refresh) {
    if (refresh || knownPoolHosts == null) {
      knownPoolHosts = connectionManager.getHosts();
      if ( log.isInfoEnabled() ) {
        log.info("found knownPoolHosts: {}", knownPoolHosts);
      }
    }
    return knownPoolHosts;
  }


  /* (non-Javadoc)
   * @see me.prettyprint.cassandra.service.Cluster#addHost(me.prettyprint.cassandra.service.CassandraHost, boolean)
   */
  @Override
  public void addHost(CassandraHost cassandraHost, boolean skipApplyConfig) {
    if (!skipApplyConfig && configurator != null) {
      configurator.applyConfig(cassandraHost);
    }
    connectionManager.addCassandraHost(cassandraHost);
  }


  /* (non-Javadoc)
   * @see me.prettyprint.cassandra.service.Cluster#getName()
   */
  @Override
  public String getName() {
    return name;
  }


  @Override
  public Map<String, String> getCredentials() {
    return credentials;
  }


  protected static void waitForSchemaAgreement(Cassandra.Client cassandra) throws InvalidRequestException, TException, InterruptedException {
    int waited = 0;
    int versions = 0;
    while (versions != 1) {
      ArrayList<String> liveschemas = new ArrayList<String>();
      Map<String, List<String>> schema = cassandra.describe_schema_versions();
      for (Map.Entry<String, List<String>> entry : schema.entrySet()) {
        if (!entry.getKey().equals("UNREACHABLE"))
          liveschemas.add(entry.getKey());
      }
      versions = liveschemas.size();
      Thread.sleep(1000);
      waited += 1000;
      if (waited > RING_DELAY)
        throw new RuntimeException("Could not reach schema agreement in " + RING_DELAY + "ms");
    }
  }


}
