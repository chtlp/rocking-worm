package jdbc;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import transaction.Transaction;

/**
 * The RMI server-side implementation of RemoteConnection.
 */
@SuppressWarnings("serial")
class RemoteConnectionImpl extends UnicastRemoteObject implements
		RemoteConnection {
	private Transaction tx;
	private boolean autoCommit = true;

	/**
	 * Creates a remote connection and begins a new transaction for it.
	 * 
	 * @throws RemoteException
	 */
	RemoteConnectionImpl() throws RemoteException {
		tx = Transaction.begin();
	}

	/**
	 * Creates a new RemoteStatement for this connection.
	 * 
	 * @see simpledb.remote.RemoteConnection#createStatement()
	 */
	@Override
	public RemoteStatement createStatement() throws RemoteException {
		return new RemoteStatementImpl(this);
	}

	/**
	 * Closes the connection. The current transaction is committed.
	 * 
	 * @see simpledb.remote.RemoteConnection#close()
	 */
	@Override
	public void close() throws RemoteException {
		tx.commit();
		tx = null;
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws RemoteException {
		this.autoCommit = autoCommit;
	}

	@Override
	public boolean getAutoCommit() throws RemoteException {
		return autoCommit;
	}

	/**
	 * Commits the current transaction, and begins a new one.
	 */
	@Override
	public void commit() throws RemoteException {
		tx.commit();
		tx = Transaction.begin();
	}

	/**
	 * Rolls back the current transaction, and begins a new one.
	 */
	@Override
	public void rollback() throws RemoteException {
		tx.rollback();
		tx = Transaction.begin();
	}

	// The following methods are used by the server-side classes.

	/**
	 * Returns the transaction currently associated with this connection.
	 * 
	 * @return the transaction associated with this connection
	 */
	Transaction getTransaction() {
		return tx;
	}
}
