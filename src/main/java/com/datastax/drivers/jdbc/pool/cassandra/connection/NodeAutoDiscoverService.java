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

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.drivers.jdbc.pool.cassandra.jdbc.CassandraConnectionHandle;


public class NodeAutoDiscoverService extends BackgroundCassandraHostService {

  private static final Logger log = LoggerFactory.getLogger(NodeAutoDiscoverService.class);

  public static final int DEF_AUTO_DISCOVERY_DELAY = 30;


  public NodeAutoDiscoverService(HConnectionManager connectionManager,
      CassandraHostConfigurator cassandraHostConfigurator) {
    super(connectionManager, cassandraHostConfigurator);
    //this.retryDelayInSeconds = cassandraHostConfigurator.getAutoDiscoveryDelayInSeconds();
    sf = executor.scheduleWithFixedDelay(new QueryRing(), retryDelayInSeconds,retryDelayInSeconds, TimeUnit.SECONDS);
  }

  @Override
  void shutdown() {
    log.error("Auto Discovery retry shutdown hook called");
    if ( sf != null ) {
      sf.cancel(true);
    }
    if ( executor != null ) {
      executor.shutdownNow();
    }
    log.error("AutoDiscovery retry shutdown complete");
  }

  @Override
  public void applyRetryDelay() {
    // no op for now
  }

  class QueryRing implements Runnable {

    @Override
    public void run() {
      doAddNodes();
    }

  }
  
  public void doAddNodes() {
    if ( log.isDebugEnabled() ) {
      log.debug("Auto discovery service running...");
    }
    Set<CassandraHost> foundHosts = discoverNodes();
    if ( foundHosts != null && foundHosts.size() > 0 ) {
      log.info("Found {} new host(s) in Ring", foundHosts.size());
      for (CassandraHost cassandraHost : foundHosts) {
        log.info("Addding found host {} to pool", cassandraHost);
        cassandraHostConfigurator.applyConfig(cassandraHost);
        connectionManager.addCassandraHost(cassandraHost);
      }
    }
    if ( log.isDebugEnabled() ) {
      log.debug("Auto discovery service run complete.");
    }
  }

  public Set<CassandraHost> discoverNodes() {
    Set<CassandraHost> existingHosts = connectionManager.getHosts();
    Set<CassandraHost> foundHosts = new HashSet<CassandraHost>();

    CassandraConnectionHandle conn  = null;
    log.info("using existing hosts {}", existingHosts);
    try {
      conn = connectionManager.borrowClient();
      /*
      for (KsDef keyspace : thriftClient.getCassandra().describe_keyspaces()) {
        if (!keyspace.getName().equals(Keyspace.KEYSPACE_SYSTEM)) {
          List<TokenRange> tokenRanges = thriftClient.getCassandra().describe_ring(keyspace.getName());
          for (TokenRange tokenRange : tokenRanges) {
            for (String host : tokenRange.getEndpoints()) {
              CassandraHost foundHost = new CassandraHost(host,cassandraHostConfigurator.getPort());
              if ( !existingHosts.contains(foundHost) ) {
                log.info("Found a node we don't know about {} for TokenRange {}", foundHost, tokenRange);
                foundHosts.add(foundHost);
              }
            }
          }
          break;
        }
      }
      */
    } catch (Exception e) {
      log.error("Discovery Service failed attempt to connect", e);
    } finally {
      try {
        connectionManager.releaseClient(conn);
      } catch (SQLException e) {
        log.error("Discovery Service failed at releasing a connection", e);
      }
    }
    return foundHosts;
  }

}

