package jdbc;

import java.rmi.RemoteException;
import java.sql.*;

/**
 * An adapter class that wraps RemoteMetaData. Its methods do nothing except
 * transform RemoteExceptions into SQLExceptions.
 */
public class FatwormMetaData extends ResultSetMetaDataAdapter {
	private RemoteMetaData rmd;

	public FatwormMetaData(RemoteMetaData md) {
		rmd = md;
	}

	public int getColumnCount() throws SQLException {
		try {
			return rmd.getColumnCount();
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	public String getColumnName(int column) throws SQLException {
		try {
			return rmd.getColumnName(column);
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	public int getColumnType(int column) throws SQLException {
		try {
			return rmd.getColumnType(column);
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}
}
