package jdbc;

import java.rmi.RemoteException;
import java.sql.*;

/**
 * An adapter class that wraps RemoteConnection. Its methods do nothing except
 * transform RemoteExceptions into SQLExceptions.
 */
public class FatwormConnection extends ConnectionAdapter {
	private RemoteConnection rconn;

	public FatwormConnection(RemoteConnection c) {
		rconn = c;
	}

	public Statement createStatement() throws SQLException {
		try {
			RemoteStatement rstmt = rconn.createStatement();
			return new FatwormStatement(rstmt);
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	public void close() throws SQLException {
		try {
			rconn.close();
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		try {
			rconn.setAutoCommit(autoCommit);
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	public boolean getAutoCommit() throws SQLException {
		try {
			return rconn.getAutoCommit();
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	public void commit() throws SQLException {
		try {
			rconn.commit();
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}
	
	public void rollback() throws SQLException {
		try {
			rconn.rollback();
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}
}
