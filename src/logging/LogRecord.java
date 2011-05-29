package logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The interface implemented by each type of log record.
 */
public abstract class LogRecord {
	protected static Logger logger = LoggerFactory.getLogger("lq.logging.LogRecord");

	/**
	 * The six different types of log record
	 */
	public static final int STARTCHECKPOINT = 0, ENDCHECKPOINT = 1, START = 2,
			COMMIT = 3, ROLLBACK = 4, UPDATE = 5;

	protected static final LogManager logMgr = LogManager.getInstance();

	/**
	 * Writes the record to the log and returns its LSN.
	 * 
	 * @return the LSN of the record in the log
	 */
	public abstract int writeToLog();

	/**
	 * Returns the log record's type.
	 * 
	 * @return the log record's type
	 */
	public abstract int op();

	/**
	 * Returns the transaction id stored with the log record.
	 * 
	 * @return the log record's transaction id
	 */
	public abstract int txID();

	/**
	 * Undoes the operation encoded by this log record. The only log record
	 * types for which this method does anything interesting are SETINT and
	 * SETSTRING.
	 * 
	 * @param txnum
	 *            the id of the transaction that is performing the undo.
	 */
	public abstract void undo(int txnum);

	public abstract void redo();
}