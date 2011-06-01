package jdbc;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import plan.QueryPlan;
import table.Record;
import value.BooleanValue;
import value.DateTimeValue;
import value.DecimalValue;
import value.FloatValue;
import value.IntValue;
import value.StrValue;
import value.Value;

/**
 * The RMI server-side implementation of RemoteResultSet.
 */
@SuppressWarnings("serial")
class RemoteResultSetImpl extends UnicastRemoteObject implements
		RemoteResultSet {
	private static Logger logger = LoggerFactory.getLogger("lq.jdbc");
	private QueryPlan plan;
	private Record currentRecord;
	private RemoteConnectionImpl rconn;

	/**
	 * Creates a RemoteResultSet object. The specified plan is opened, and the
	 * scan is saved.
	 * 
	 * @param plan
	 *            the query plan
	 * @param rconn
	 * @throws RemoteException
	 */
	public RemoteResultSetImpl(QueryPlan pln, RemoteConnectionImpl conn)
			throws RemoteException {
		plan = pln;
		rconn = conn;
		try {
			plan.open();
		} catch (Exception e) {
			rconn.rollback();
			e.printStackTrace();
			throw new RemoteException("", e);
		}
	}

	/**
	 * Moves to the next record in the result set, by moving to the next record
	 * in the saved scan.
	 */
	@Override
	public boolean next() throws RemoteException {
		try {
			logger.debug("next row");
			currentRecord = plan.next();
			return currentRecord != null;
		} catch (Exception e) {
			rconn.rollback();
			e.printStackTrace();
			throw new RemoteException("", e);
		}
	}

	/**
	 * Returns the value of the specified column, by returning the corresponding
	 * value on the saved scan.
	 */
	@Override
	public Object getObject(int columnIndex) throws RemoteException {
		try {
			Value v = currentRecord.getValue(columnIndex - 1);
			Object res;
			if (v instanceof BooleanValue || v instanceof FloatValue
					|| v instanceof IntValue || v instanceof StrValue)
				res = v.get();
			else if (v instanceof DateTimeValue)
				res = new Date(((java.util.Date) v.get()).getTime());
			else if (v instanceof DecimalValue)
				res = new BigDecimal((String) v.get());
			else
				throw new RuntimeException("unknown value in RemoteResultSet.getObject()");
			logger.debug("getObject = " + res);
			return res;
		} catch (Exception e) {
			rconn.rollback();
			e.printStackTrace();
			throw new RemoteException("", e);
		}
	}

	/**
	 * Returns the result set's metadata, by passing its schema into the
	 * RemoteMetaData constructor.
	 */
	@Override
	public RemoteMetaData getMetaData() throws RemoteException {
		return new RemoteMetaDataImpl(plan.getColumns(), plan.getAlias());
	}

	/**
	 * Closes the result set by closing its scan.
	 */
	@Override
	public void close() throws RemoteException {
		try {
			plan.close();
		} catch (Exception e) {
			rconn.rollback();
			e.printStackTrace();
			throw new RemoteException("", e);
		}
	}

	@Override
	public void beforeFirst() throws RemoteException {
		try {
			plan.close();
			plan.open();
		} catch (Exception e) {
			rconn.rollback();
			e.printStackTrace();
			throw new RemoteException("", e);
		}
	}
}
