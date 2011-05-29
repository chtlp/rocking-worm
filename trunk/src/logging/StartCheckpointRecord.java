package logging;

/**
 * The STARTCHECKPOINT log record.
 */
class StartCheckpointRecord extends LogRecord {
	int[] txs;

	/**
	 * Creates a nonquiescent checkpoint record.
	 */
	public StartCheckpointRecord(int[] txs) {
		this.txs = txs;
	}

	/**
	 * Creates a log record by reading no other values from the basic log
	 * record.
	 * 
	 * @param rec
	 *            the basic log record
	 */
	public StartCheckpointRecord(BasicLogRecord rec) {
		txs = rec.nextInts();
	}

	/**
	 * Writes a checkpoint record to the log. This log record contains the
	 * CHECKPOINT operator, and nothing else.
	 * 
	 * @return the LSN of the last log value
	 */
	public int writeToLog() {
		logger.debug(toString());
		Object[] rec = new Object[] { STARTCHECKPOINT, txs };
		return logMgr.append(rec);
	}

	public int op() {
		return STARTCHECKPOINT;
	}

	/**
	 * Checkpoint records have no associated transaction, and so the method
	 * returns a "dummy", negative txid.
	 */
	public int txID() {
		return -1; // dummy value
	}

	/**
	 * Does nothing, because a checkpoint record contains no undo information.
	 */
	public void undo(int txnum) {
	}

	public void redo() {
	}

	public String toString() {
		return "<START CHECKPOINT>";
	}
}
