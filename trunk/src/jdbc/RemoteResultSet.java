package jdbc;

import java.rmi.*;

/**
 * The RMI remote interface corresponding to ResultSet. The methods are
 * identical to those of ResultSet, except that they throw RemoteExceptions
 * instead of SQLExceptions.
 */
public interface RemoteResultSet extends Remote {
	public boolean next() throws RemoteException;

	public Object getObject(int columnIndex) throws RemoteException;

	public RemoteMetaData getMetaData() throws RemoteException;

	public void close() throws RemoteException;

	public void beforeFirst() throws RemoteException;
}
