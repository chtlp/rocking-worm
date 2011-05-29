package transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import filesystem.FileStorage;

import table.TableManager;
import util.Constant;

import logging.BackwardLogRecordIterator;
import logging.CommitRecord;
import logging.LogManager;
import logging.LogRecord;
import logging.RollbackRecord;
import logging.StartRecord;

public class Transaction {
	private static int count_id = 0;

	private int id;
	private String dbName;

	private ArrayList<TableLock> locks = new ArrayList<TableLock>();
	private TableLock waitForLock;
	private int lockTimeout = Constant.INITIAL_LOCK_TIMEOUT;

	private static HashSet<Transaction> activeTransactions = new HashSet<Transaction>();

	private static HashMap<Integer, Transaction> i2t = new HashMap<Integer, Transaction>();

	private Transaction() {
		id = count_id++;
	}

	public static Transaction begin() {
		Transaction t = new Transaction();
		i2t.put(t.getID(), t);
		new StartRecord(t.getID()).writeToLog();
		activeTransactions.add(t);

		TableManager.useDatabase(t, "_default");
		return t;
	}

	public static Transaction getX(int tx) {
		return i2t.get(tx);
	}

	public int getID() {
		return id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	/**
	 * Add a lock for the given table. The object is unlocked on commit or
	 * rollback.
	 * 
	 * @param table
	 *            the table that is locked
	 */
	public void addLock(TableLock table) {
		locks.add(table);
	}

	public void setWaitForLock(TableLock table) {
		this.waitForLock = table;
	}

	public TableLock getWaitForLock() {
		return waitForLock;
	}

	public int getLockTimeout() {
		return lockTimeout;
	}

	public void setLockTimeout(int lockTimeout) {
		this.lockTimeout = lockTimeout;
	}

	public void unlockAll() {
		if (locks.size() > 0) {
			synchronized (LockManager.getInstance()) {
				// don't use the enhance for loop to safe memory
				for (int i = 0, size = locks.size(); i < size; i++) {
					TableLock t = locks.get(i);
					t.unlock(this);
				}
				locks.clear();
			}
		}
	}

	public void commit() {
		LogManager.getInstance().flush(new CommitRecord(getID()).writeToLog());
		activeTransactions.remove(this);
		unlockAll();
	}

	public void rollback() {
		BackwardLogRecordIterator it = new BackwardLogRecordIterator();
		while (it.hasNext()) {
			LogRecord rec = it.next();
			rec.undo(id);
			if (rec.op() == LogRecord.START && rec.txID() == id)
				break;
		}
		new RollbackRecord(id).writeToLog();

		activeTransactions.remove(this);
		unlockAll();

		FileStorage.cleanUp(this);
	}

	public static int[] getActiveX() {
		int[] result = new int[activeTransactions.size()];
		int i = 0;
		for (Transaction x : activeTransactions)
			result[i++] = x.getID();
		return result;
	}

	public String getDBName() {
		return dbName;
	}

	public void setDBName(String dbName) {
		this.dbName = dbName;
	}
}
