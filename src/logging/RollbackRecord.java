package logging;

/**
 * The ROLLBACK log record.
 */
public class RollbackRecord extends LogRecord {
	private int txnum;

	/**
	 * Creates a new rollback log record for the specified transaction.
	 * 
	 * @param txnum
	 *            the ID of the specified transaction
	 */
	public RollbackRecord(int txnum) {
		this.txnum = txnum;
	}

	/**
	 * Creates a log record by reading one other value from the log.
	 * 
	 * @param rec
	 *            the basic log record
	 */
	public RollbackRecord(BasicLogRecord rec) {
		txnum = rec.nextInt();
	}

	/**
	 * Writes a rollback record to the log. This log record contains the
	 * ROLLBACK operator, followed by the transaction id.
	 * 
	 * @return the LSN of the last log value
	 */
	public int writeToLog() {
		logger.debug(toString());
		Object[] rec = new Object[] { ROLLBACK, txnum };
		return logMgr.append(rec);
	}

	public int op() {
		return ROLLBACK;
	}

	public int txID() {
		return txnum;
	}

	/**
	 * Does nothing, because a rollback record contains no undo information.
	 */
	public void undo(int txnum) {
	}

	public void redo() {
	}

	public String toString() {
		return "<ROLLBACK " + txnum + ">";
	}
}
