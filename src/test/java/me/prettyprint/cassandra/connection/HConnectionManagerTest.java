package me.prettyprint.cassandra.connection;

import static org.junit.Assert.*;

import org.apache.cassandra.thrift.Cassandra.Client;
import org.junit.Before;
import org.junit.Test;

import com.datastax.drivers.jdbc.pool.cassandra.BaseEmbededServerSetupTest;
import com.datastax.drivers.jdbc.pool.cassandra.connection.CassandraHost;
import com.datastax.drivers.jdbc.pool.cassandra.connection.CassandraHostConfigurator;
import com.datastax.drivers.jdbc.pool.cassandra.connection.HConnectionManager;
import com.datastax.drivers.jdbc.pool.cassandra.exceptions.HTimedOutException;
import com.datastax.drivers.jdbc.pool.cassandra.exceptions.HectorException;
import com.datastax.drivers.jdbc.pool.cassandra.exceptions.HectorTransportException;
import com.datastax.drivers.jdbc.pool.cassandra.service.FailoverPolicy;
import com.datastax.drivers.jdbc.pool.cassandra.service.Operation;
import com.datastax.drivers.jdbc.pool.cassandra.service.OperationType;


public class HConnectionManagerTest extends BaseEmbededServerSetupTest {

  
 
  
  @Test
  public void testRemoveHost() {
    setupClient();
    CassandraHost cassandraHost = new CassandraHost("127.0.0.1", 9170);
    connectionManager.removeCassandraHost(cassandraHost);
    assertEquals(0,connectionManager.getActivePools().size());
    assertTrue(connectionManager.addCassandraHost(cassandraHost));
    assertEquals(1,connectionManager.getActivePools().size());
    connectionManager.markHostAsDown(cassandraHost);
    assertTrue(connectionManager.removeCassandraHost(cassandraHost));
  }
  
  @Test 
  public void testAddCassandraHostFail() {
    setupClient();
    CassandraHost cassandraHost = new CassandraHost("127.0.0.1", 9180);
    assertFalse(connectionManager.addCassandraHost(cassandraHost));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testNullHostList() {
    HConnectionManager hcm = new HConnectionManager(clusterName, new CassandraHostConfigurator());
  }
  
  @Test
  public void testMarkHostDownWithNoRetry() {
    cassandraHostConfigurator = new CassandraHostConfigurator("127.0.0.1:9170");
    cassandraHostConfigurator.setRetryDownedHosts(false);
    connectionManager = new HConnectionManager(clusterName, cassandraHostConfigurator);
    CassandraHost cassandraHost = new CassandraHost("127.0.0.1", 9170);    
    HThriftClient client = connectionManager.borrowClient();
    connectionManager.markHostAsDown(client.cassandraHost);
    assertEquals(0,connectionManager.getActivePools().size());
  }
  
  @Test
  public void testSuspendCassandraHost() {
    setupClient();
    CassandraHost cassandraHost = new CassandraHost("127.0.0.1", 9170);
    assertTrue(connectionManager.suspendCassandraHost(cassandraHost));
    assertEquals(1,connectionManager.getSuspendedCassandraHosts().size());
    assertTrue(connectionManager.unsuspendCassandraHost(cassandraHost));
  }
  
  @Test(expected=HTimedOutException.class)
  public void testTimedOutOperateWithFailover() {
    setupClient();
    FailoverPolicy fp = FailoverPolicy.ON_FAIL_TRY_ONE_NEXT_AVAILABLE;
    connectionManager.operateWithFailover(new TimeoutOp(fp));
  }
  
  abstract class StubOp extends Operation<String> {

    StubOp(FailoverPolicy fp) {      
      this(OperationType.META_READ);
      failoverPolicy = fp;
    }
    
    public StubOp(OperationType operationType) {
      super(operationType);
    }    
  }
  
  class TimeoutOp extends StubOp {
    
    TimeoutOp(FailoverPolicy fp) {
      super(fp);
    }

    @Override
    public String execute(Client cassandra) throws HectorException {
      throw new HTimedOutException("fake timeout");
    }
  }
}