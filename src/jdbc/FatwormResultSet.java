package jdbc;

import java.rmi.RemoteException;
import java.sql.*;

/**
 * An adapter class that wraps RemoteResultSet. Its methods do nothing except
 * transform RemoteExceptions into SQLExceptions.
 */
public class FatwormResultSet extends ResultSetAdapter {
	private RemoteResultSet rrs;

	public FatwormResultSet(RemoteResultSet s) {
		rrs = s;
	}

	public void beforeFirst() throws SQLException {
		try {
			rrs.beforeFirst();
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	public boolean next() throws SQLException {
		try {
			return rrs.next();
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	public Object getObject(int columnIndex) throws SQLException {
		try {
			return rrs.getObject(columnIndex);
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		try {
			RemoteMetaData rmd = rrs.getMetaData();
			return new FatwormMetaData(rmd);
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	public void close() throws SQLException {
		try {
			rrs.close();
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}
}
