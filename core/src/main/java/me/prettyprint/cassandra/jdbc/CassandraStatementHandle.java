package me.prettyprint.cassandra.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import me.prettyprint.cassandra.connection.CassandraClientMonitor.Counter;
import me.prettyprint.cassandra.connection.CassandraHost;
import me.prettyprint.cassandra.connection.HConnectionManager;
import me.prettyprint.cassandra.service.FailoverPolicy;
import me.prettyprint.cassandra.service.Operation;
import me.prettyprint.cassandra.service.OperationType;
import me.prettyprint.hector.api.exceptions.HectorException;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Cassandra.Client;

/**
 * Wrapper around JDBC Statement.
 */
public class CassandraStatementHandle implements Statement {

  public FailoverPolicy failoverPolicy = FailoverPolicy.ON_FAIL_TRY_ALL_AVAILABLE;
  
  private Statement internalStatement;
  private HConnectionManager manager;
  public CassandraConnectionHandle cassandraConnectionHandle;
  
  public CassandraStatementHandle(Statement internalStatement, HConnectionManager manager, 
      CassandraConnectionHandle cassandraConnectionHandle) {
    this.internalStatement = internalStatement;
    this.manager = manager;
    this.cassandraConnectionHandle = cassandraConnectionHandle;
  }

  /**
   * Performs the operation on the given cassandra instance.
   */
  public void execute(Operation<?> op) throws SQLException {
    manager.operateWithFailover(op);
  }

  @Override
  public boolean execute(final String sql) throws SQLException {
    final Statement stm = this.internalStatement;

    Operation<Boolean> op = new Operation<Boolean>(OperationType.READ, this) {

      @Override
      public Boolean execute() throws SQLException {
        return stm.execute(sql);
      }
    };

    execute(op);
    return op.getResult();
  }

  @Override
  public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
    final Statement stm = this.internalStatement;

    Operation<Boolean> op = new Operation<Boolean>(OperationType.READ, this) {

      @Override
      public Boolean execute() throws SQLException {
        return stm.execute(sql, autoGeneratedKeys);
      }
    };

    execute(op);
    return op.getResult();
  }

  @Override
  public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
    final Statement stm = this.internalStatement;

    Operation<Boolean> op = new Operation<Boolean>(OperationType.READ, this) {

      @Override
      public Boolean execute() throws SQLException {
        return stm.execute(sql, columnIndexes);
      }
    };

    execute(op);
    return op.getResult();
  }

  @Override
  public boolean execute(final String sql, final String[] columnNames) throws SQLException {
    final Statement stm = this.internalStatement;

    Operation<Boolean> op = new Operation<Boolean>(OperationType.READ, this) {

      @Override
      public Boolean execute() throws SQLException {
        return stm.execute(sql, columnNames);
      }
    };

    execute(op);
    return op.getResult();
  }

  @Override
  public int[] executeBatch() throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys)
      throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames)
      throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }


  /**
   * Checks if the connection is (logically) closed and throws an exception if it is.
   * 
   * @throws SQLException
   *             on error
   */
  private void checkClosed() throws SQLException {
    if (this.cassandraConnectionHandle.isClosed()) {
      throw new SQLException("Connection is closed!");
    }
  }


  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    boolean result = false;
    try{
      result = this.internalStatement.isWrapperFor(iface);
    } catch (SQLException e) {
      throw this.cassandraConnectionHandle.markPossiblyBroken(e);
    }
    return result;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    T result = null;
    try{
      result = this.internalStatement.unwrap(iface);
    } catch (SQLException e) {
      throw this.cassandraConnectionHandle.markPossiblyBroken(e);
    }
    return result;
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    checkClosed();
    try{
      this.internalStatement.addBatch(sql);
    } catch (SQLException e) {
      throw this.cassandraConnectionHandle.markPossiblyBroken(e);
    }
  }

  @Override
  public void cancel() throws SQLException {
    checkClosed();
    try{
      this.internalStatement.cancel();
    } catch (SQLException e) {
      throw this.cassandraConnectionHandle.markPossiblyBroken(e);

    }
  }

  @Override
  public void clearBatch() throws SQLException {
    checkClosed();
    try{
      this.internalStatement.clearBatch();
    } catch (SQLException e) {
      throw this.cassandraConnectionHandle.markPossiblyBroken(e);

    }
  }

  @Override
  public void clearWarnings() throws SQLException {
    checkClosed();
    try{
      this.internalStatement.clearWarnings();
    } catch (SQLException e) {
      throw this.cassandraConnectionHandle.markPossiblyBroken(e);
    }
  }

  @Override
  public void close() throws SQLException {
    this.internalStatement.close();
  }


  @Override
  public Connection getConnection() throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getFetchDirection() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getFetchSize() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getMaxRows() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean getMoreResults(int current) throws SQLException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getResultSetType() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getUpdateCount() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isClosed() throws SQLException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isPoolable() throws SQLException {
    boolean result = false;
    checkClosed();
    try{
      result = this.internalStatement.isPoolable();
    } catch (SQLException e) {
      throw this.connectionHandle.markPossiblyBroken(e);
    }
    return result;
  }

  @Override
  public void setCursorName(String name) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setFetchDirection(int direction) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setFetchSize(int rows) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setMaxRows(int max) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    // TODO Auto-generated method stub
    
  }  

}