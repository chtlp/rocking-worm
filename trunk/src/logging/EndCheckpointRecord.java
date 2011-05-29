package logging;

/**
 * The ENDCHECKPOINT log record.
 */
class EndCheckpointRecord implements LogRecord {

	/**
	 * Creates a nonquiescent checkpoint record.
	 */
	public EndCheckpointRecord() {
	}

	/**
	 * Creates a log record by reading no other values from the basic log
	 * record.
	 * 
	 * @param rec
	 *            the basic log record
	 */
	public EndCheckpointRecord(BasicLogRecord rec) {
	}

	/**
	 * Writes a checkpoint record to the log. This log record contains the
	 * CHECKPOINT operator, and nothing else.
	 * 
	 * @return the LSN of the last log value
	 */
	public int writeToLog() {
		logger.debug(toString());
		Object[] rec = new Object[] { ENDCHECKPOINT };
		return logMgr.append(rec);
	}

	public int op() {
		return ENDCHECKPOINT;
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
		return "<END CHECKPOINT>";
	}
}
