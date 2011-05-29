package transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableLock {
	private static Logger logger = LoggerFactory
			.getLogger("lq.transaction.TableLock");
	private static int count = 0;
	private int id = count++;
	private volatile Transaction lockExclusive;
	private HashSet<Transaction> lockShared = new HashSet<Transaction>();
	private LockManager lockManager = LockManager.getInstance();
	/**
	 * True if one thread ever was waiting to lock this table. This is to avoid
	 * calling notifyAll if no transaction was ever waiting to lock this table.
	 * If set, the flag stays. In theory, it could be reset, however not sure
	 * when.
	 */
	private boolean waitForLock;

	public void lock(Transaction transaction, boolean exclusive)
			throws DeadlockException, TimeoutException {
		if (lockExclusive == transaction) {
			return;
		}
		synchronized (lockManager) {
			try {
				doLock(transaction, exclusive);
			} finally {
				transaction.setWaitForLock(null);
			}
		}
	}

	private void doLock(Transaction transaction, boolean exclusive)
			throws DeadlockException, TimeoutException {
		traceLock(transaction, exclusive, "requesting for");
		// don't get the current time unless necessary
		long max = 0;
		boolean checkDeadlock = false;
		while (true) {
			if (lockExclusive == transaction) {
				return;
			}
			if (exclusive) {
				if (lockExclusive == null) {
					if (lockShared.isEmpty()) {
						traceLock(transaction, exclusive, "added for");
						transaction.addLock(this);
						lockExclusive = transaction;
						return;
					}
					else if (lockShared.size() == 1
							&& lockShared.contains(transaction)) {
						traceLock(transaction, exclusive, "add (upgraded) for ");
						lockExclusive = transaction;
						return;
					}
				}
			}
			else {
				if (lockExclusive == null) {
					if (!lockShared.contains(transaction)) {
						traceLock(transaction, exclusive, "ok");
						transaction.addLock(this);
						lockShared.add(transaction);
					}
					return;
				}
			}
			transaction.setWaitForLock(this);
			if (checkDeadlock) {
				ArrayList<Transaction> Transactions = checkDeadlock(
						transaction, null, null);
				if (Transactions != null) {
					throw new DeadlockException();
				}
			}
			else {
				// check for deadlocks from now on
				checkDeadlock = true;
			}
			long now = System.currentTimeMillis();
			if (max == 0) {
				// try at least one more time
				max = now + transaction.getLockTimeout();
			}
			else if (now >= max) {
				traceLock(transaction, exclusive, "timeout after "
						+ transaction.getLockTimeout());
				throw new TimeoutException();
			}
			try {
				traceLock(transaction, exclusive, "waiting for");
				// don't wait too long so that deadlocks are detected early
				long sleep = Math.min(LockManager.DEADLOCK_CHECK, max - now);
				if (sleep == 0) {
					sleep = 1;
				}
				waitForLock = true;
				lockManager.wait(sleep);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	public void unlock(Transaction s) {
		traceLock(s, lockExclusive == s, "unlock");
		if (lockExclusive == s) {
			lockExclusive = null;
		}
		if (lockShared.size() > 0) {
			lockShared.remove(s);
		}
		// TODO lock: maybe we need we fifo-queue to make sure nobody
		// starves. check what other databases do
		synchronized (lockManager) {
			if (waitForLock) {
				lockManager.notifyAll();
			}
		}
	}

	public ArrayList<Transaction> checkDeadlock(Transaction transaction,
			Transaction clash, Set<Transaction> visited) {
		// only one deadlock check at any given time
		synchronized (TableLock.class) {
			if (clash == null) {
				// verification is started
				clash = transaction;
				visited = new HashSet<Transaction>();
			}
			else if (clash == transaction) {
				// we found a circle where this transaction is involved
				return new ArrayList<Transaction>();
			}
			else if (visited.contains(transaction)) {
				// we have already checked this transaction.
				// there is a circle, but the transactions in the circle need to
				// find it out themselves
				return null;
			}
			visited.add(transaction);
			ArrayList<Transaction> error = null;
			for (Transaction s : lockShared) {
				if (s == transaction) {
					// it doesn't matter if we have locked the object already
					continue;
				}
				TableLock t = s.getWaitForLock();
				if (t != null) {
					error = t.checkDeadlock(s, clash, visited);
					if (error != null) {
						error.add(transaction);
						break;
					}
				}
			}
			if (error == null && lockExclusive != null) {
				TableLock t = lockExclusive.getWaitForLock();
				if (t != null) {
					error = t.checkDeadlock(lockExclusive, clash, visited);
					if (error != null) {
						error.add(transaction);
					}
				}
			}
			return error;
		}
	}

	private void traceLock(Transaction transaction, boolean exclusive, String s) {
		if (logger.isDebugEnabled()) {
			Object[] msg = { transaction.getID(),
					exclusive ? "exclusive write lock" : "shared read lock", s,
					id };
			logger.debug("{} {} {} {}", msg);
		}
	}
}
