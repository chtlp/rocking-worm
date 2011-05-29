package jdbc;

import java.rmi.RemoteException;
import java.sql.*;

/**
 * An adapter class that wraps RemoteStatement. Its methods do nothing except
 * transform RemoteExceptions into SQLExceptions.
 */
public class FatwormStatement extends StatementAdapter {
	private RemoteStatement rstmt;

	public FatwormStatement(RemoteStatement s) {
		rstmt = s;
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		try {
			return rstmt.execute(sql);
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		try {
			RemoteResultSet rrs = rstmt.getResultSet();
			return new FatwormResultSet(rrs);
		} catch (RemoteException e) {
			throw new SQLException(e);
		}
	}

	@Override
	public void close() throws SQLException {
		try {
			rstmt.close();
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}
}
