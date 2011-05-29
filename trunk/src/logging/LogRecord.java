package logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The interface implemented by each type of log record.
 */
public interface LogRecord {
	static Logger logger = LoggerFactory.getLogger("lq.logging.LogRecord");

	/**
	 * The six different types of log record
	 */
	static final int STARTCHECKPOINT = 0, ENDCHECKPOINT = 1, START = 2,
			COMMIT = 3, ROLLBACK = 4, UPDATE = 5;

	static final LogManager logMgr = LogManager.getInstance();

	/**
	 * Writes the record to the log and returns its LSN.
	 * 
	 * @return the LSN of the record in the log
	 */
	int writeToLog();

	/**
	 * Returns the log record's type.
	 * 
	 * @return the log record's type
	 */
	int op();

	/**
	 * Returns the transaction id stored with the log record.
	 * 
	 * @return the log record's transaction id
	 */
	int txID();

	/**
	 * Undoes the operation encoded by this log record. The only log record
	 * types for which this method does anything interesting are SETINT and
	 * SETSTRING.
	 * 
	 * @param txnum
	 *            the id of the transaction that is performing the undo.
	 */
	void undo(int txnum);

	void redo();
}