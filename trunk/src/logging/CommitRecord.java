package logging;

/**
 * The COMMIT log record
 * 
 */
public class CommitRecord implements LogRecord {
	private int txnum;

	/**
	 * Creates a new commit log record for the specified transaction.
	 * 
	 * @param txnum
	 *            the ID of the specified transaction
	 */
	public CommitRecord(int txnum) {
		this.txnum = txnum;
	}

	/**
	 * Creates a log record by reading one other value from the log.
	 * 
	 * @param rec
	 *            the basic log record
	 */
	public CommitRecord(BasicLogRecord rec) {
		txnum = rec.nextInt();
	}

	/**
	 * Writes a commit record to the log. This log record contains the COMMIT
	 * operator, followed by the transaction id.
	 * 
	 * @return the LSN of the last log value
	 */
	public int writeToLog() {
		logger.debug(toString());
		Object[] rec = new Object[] { COMMIT, txnum };
		return logMgr.append(rec);
	}

	public int op() {
		return COMMIT;
	}

	public int txID() {
		return txnum;
	}

	/**
	 * Does nothing, because a commit record contains no undo information.
	 */
	public void undo(int txnum) {
	}

	public void redo() {
	}

	public String toString() {
		return "<COMMIT " + txnum + ">";
	}
}
