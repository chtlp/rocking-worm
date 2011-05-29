package jdbc;

import java.rmi.*;

/**
 * The RMI remote interface corresponding to Connection. The methods are
 * identical to those of Connection, except that they throw RemoteExceptions
 * instead of SQLExceptions.
 */
public interface RemoteConnection extends Remote {
	public RemoteStatement createStatement() throws RemoteException;

	public void close() throws RemoteException;

	public void setAutoCommit(boolean autoCommit) throws RemoteException;

	public boolean getAutoCommit() throws RemoteException;

	public void commit() throws RemoteException;

	public void rollback() throws RemoteException;
}
