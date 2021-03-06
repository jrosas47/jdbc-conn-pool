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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Keep track of how often a node replies with a HTimeoutException. If we go 
 * past the threshold of [timeoutCounter] timeouts within [timeWindow] milliseconds,
 * then we mark the node as suspended. (10 timeouts within 500ms by default)
 * 
 * Periodically check the suspended nodes list every retryDelayInSeconds. If 
 * the node has been suspended longer than nodeSuspensionDurationInSeconds,
 * then we unsuspend,  placing it back in the available pool. (10 second 
 * suspension retried every 10 seconds by default). 
 *
 * @author zznate
 */
public class HostTimeoutTracker extends BackgroundCassandraHostService {
  private static final Logger log = LoggerFactory.getLogger(HostTimeoutTracker.class);

  private ConcurrentHashMap<CassandraHost, LinkedBlockingQueue<Long>> timeouts;
  private ConcurrentHashMap<CassandraHost, Long> suspended;
  private int timeoutCounter;
  private int timeoutWindow;
  private int nodeSuspensionDurationInSeconds;
  
  public static final int DEF_TIMEOUT_COUNTER = 10;
  public static final int DEF_TIMEOUT_WINDOW = 500;
  public static final int DEF_NODE_SUSPENSION_DURATION_IN_SECONDS = 10;
  public static final int DEF_NODE_UNSUSPEND_CHECK_DELAY_IN_SECONDS = 10;
  
  
  public HostTimeoutTracker(HConnectionManager connectionManager,
      CassandraHostConfigurator cassandraHostConfigurator) {
    super(connectionManager, cassandraHostConfigurator);
    retryDelayInSeconds = cassandraHostConfigurator.getHostTimeoutUnsuspendCheckDelay();
    timeouts = new ConcurrentHashMap<CassandraHost, LinkedBlockingQueue<Long>>();
    suspended = new ConcurrentHashMap<CassandraHost, Long>();
    sf = executor.scheduleWithFixedDelay(new Unsuspender(), retryDelayInSeconds,retryDelayInSeconds, TimeUnit.SECONDS);
    timeoutCounter = cassandraHostConfigurator.getHostTimeoutCounter();
    timeoutWindow = cassandraHostConfigurator.getHostTimeoutWindow();
    nodeSuspensionDurationInSeconds = cassandraHostConfigurator.getHostTimeoutSuspensionDurationInSeconds();
  }

  public boolean checkTimeout(CassandraHost cassandraHost) {
    timeouts.putIfAbsent(cassandraHost, new LinkedBlockingQueue<Long>());
    long currentTimeMillis = System.currentTimeMillis();
    timeouts.get(cassandraHost).add(currentTimeMillis);
    boolean timeout = false;
    // if there are 3 timeouts within 500ms, return false
    if ( timeouts.get(cassandraHost).size() > timeoutCounter) {
      Long last = timeouts.get(cassandraHost).remove();      
      if (last.longValue() < (currentTimeMillis - timeoutWindow)) {        
        timeout = true;
        connectionManager.suspendCassandraHost(cassandraHost);
        suspended.putIfAbsent(cassandraHost, currentTimeMillis);                                    
      }      
    }
    return timeout;
  }
  
  class Unsuspender implements Runnable {

    @Override
    public void run() {
      for (Iterator<Entry<CassandraHost,Long>> iterator = suspended.entrySet().iterator(); iterator.hasNext();) {
        Entry<CassandraHost,Long> vals = iterator.next();
        if ( vals.getValue() < (System.currentTimeMillis() - (nodeSuspensionDurationInSeconds * 1000)) ) {
          connectionManager.unsuspendCassandraHost(vals.getKey());
          iterator.remove();          
        }
      }      
    }
    
  }
  
  @Override
  void applyRetryDelay() {


  }

  @Override
  void shutdown() {
    log.info("Shutting down HostTimeoutTracker");
    if ( sf != null )
      sf.cancel(true);
    if ( executor != null ) 
      executor.shutdownNow();
    log.info("HostTimeTracker shutdown complete.");
  }

}
