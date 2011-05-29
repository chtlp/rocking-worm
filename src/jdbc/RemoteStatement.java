package jdbc;

import java.rmi.*;

/**
 * The RMI remote interface corresponding to Statement. The methods are
 * identical to those of Statement, except that they throw RemoteExceptions
 * instead of SQLExceptions.
 */
public interface RemoteStatement extends Remote {
	public boolean execute(String sql) throws RemoteException;

	public RemoteResultSet getResultSet() throws RemoteException;

	public void close() throws RemoteException;
}
