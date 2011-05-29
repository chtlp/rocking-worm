package jdbc;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * The RMI server-side implementation of RemoteDriver.
 */
@SuppressWarnings("serial")
public class RemoteDriverImpl extends UnicastRemoteObject implements
		RemoteDriver {
	public RemoteDriverImpl() throws RemoteException {
	}

	/**
	 * Creates a new RemoteConnectionImpl object and returns it.
	 * 
	 * @see simpledb.remote.RemoteDriver#connect()
	 */
	public RemoteConnection connect() throws RemoteException {
		return new RemoteConnectionImpl();
	}
}
