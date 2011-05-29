package logging;

import static logging.Page.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tlp.util.Debug;
import transaction.Transaction;
import util.Constant;

/**
 * The low-level log manager. This log manager is responsible for writing log
 * records into a log file. A log record can be any sequence of integer and
 * string values. The log manager does not understand the meaning of these
 * values, which are written and read by the {@link LogRecord}.
 */
public class LogManager implements Iterable<BasicLogRecord> {
	private static LogManager logMgr;
	private static Logger logger = LoggerFactory
			.getLogger("lq.logging.LogManager");

	public static LogManager getInstance() {
		if (logMgr == null) {
			logMgr = new LogManager(Constant.LoggingFile);
		}
		return logMgr;
	}

	/**
	 * The location where the pointer to the last integer in the page is. A
	 * value of 0 means that the pointer is the first value in the page.
	 */
	public static final int LAST_POS = 0;

	private String logfile;
	private Page mypage = new Page();
	private Block currentblk;
	private int currentPos;

	/**
	 * Creates the manager for the specified log file. If the log file does not
	 * yet exist, it is created with an empty first block. This constructor
	 * depends on a {@link FileManager} object that it gets from the method
	 * {@link simpledb.server.SimpleDB#fileMgr()}. That object is created during
	 * system initialization. Thus this constructor cannot be called until
	 * {@link simpledb.server.SimpleDB#initFileMgr(String)} is called first.
	 * 
	 * @param logfile
	 *            the name of the log file
	 */
	public LogManager(String logfile) {
		this.logfile = logfile;
		int logsize = FileManager.getInstance().size(logfile);
		if (logsize == 0)
			appendNewBlock();
		else {
			currentblk = new Block(logfile, logsize - 1);
			mypage.read(currentblk);
			currentPos = getLastRecordPosition() + INT_SIZE;
		}
	}

	/**
	 * Ensures that the log records corresponding to the specified LSN has been
	 * written to disk. All earlier log records will also be written to disk.
	 * 
	 * @param lsn
	 *            the LSN of a log record
	 */
	public synchronized void flush(int lsn) {
		if (lsn >= currentLSN())
			flush();
	}

	public static synchronized void flushAll() {
		getInstance().flush();
	}

	/**
	 * Returns an iterator for the log records, which will be returned in
	 * reverse order starting with the most recent.
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	public synchronized BackwardLogIterator iterator() {
		flush();
		return new BackwardLogIterator(currentblk);
	}

	/**
	 * Appends a log record to the file. The record contains an arbitrary array
	 * of strings and integers. The method also writes an integer to the end of
	 * each log record whose value is the offset of the corresponding integer
	 * for the previous log record. These integers allow log records to be read
	 * in reverse order.
	 * 
	 * @param rec
	 *            the list of values
	 * @return the LSN of the final value
	 */
	public synchronized int append(Object[] rec) {
		int recsize = INT_SIZE; // 4 bytes for the integer that points to the previous log record
		for (Object obj : rec)
			recsize += size(obj);
		if (currentPos + recsize >= BLOCK_SIZE) { // the log record doesn't fit,
			flush(); // so move to the next block.
			appendNewBlock();
		}
		for (Object obj : rec)
			appendVal(obj);
		finalizeRecord();
		return currentLSN();
	}

	/**
	 * Adds the specified value to the page at the position denoted by
	 * currentpos. Then increments currentpos by the size of the value.
	 * 
	 * @param val
	 *            the integer or string to be added to the page
	 */
	private void appendVal(Object val) {
		if (val instanceof String)
			mypage.setString(currentPos, (String) val);
		else if (val instanceof Integer)
			mypage.setInt(currentPos, (Integer) val);
		else if (val instanceof byte[])
			mypage.setBytes(currentPos, (byte[]) val);
		else if (val instanceof int[])
			mypage.setInts(currentPos, (int[]) val);
		else
			throw new RuntimeException("unkown object");
		currentPos += size(val);
	}

	/**
	 * Calculates the size of the specified integer or string.
	 * 
	 * @param val
	 *            the value
	 * @return the size of the value, in bytes
	 */
	private int size(Object val) {
		if (val instanceof String) {
			String sval = (String) val;
			return STR_SIZE(sval.length());
		}
		else if (val instanceof Integer)
			return INT_SIZE;
		else if (val instanceof byte[])
			return INT_SIZE + ((byte[]) val).length;
		else if (val instanceof int[])
			return INT_SIZE + INT_SIZE * ((int[]) val).length;
		else
			throw new RuntimeException("unkown object");
	}

	/**
	 * Returns the LSN of the most recent log record. As implemented, the LSN is
	 * the block number where the record is stored. Thus every log record in a
	 * block has the same LSN.
	 * 
	 * @return the LSN of the most recent log record
	 */
	private int currentLSN() {
		return currentblk.number();
	}

	static int countFlush = 0;

	/**
	 * Writes the current page to the log file.
	 */
	private void flush() {
		countFlush++;
		if (countFlush % 100 == 0)
			logger.debug("logging write count: " + countFlush);
		mypage.write(currentblk);
	}

	/**
	 * Clear the current page, and append it to the log file.
	 */
	private void appendNewBlock() {
		setLastRecordPosition(0);
		currentPos = INT_SIZE;
		currentblk = mypage.append(logfile);
	}

	/**
	 * Sets up a circular chain of pointers to the records in the page. There is
	 * an integer added to the end of each log record whose value is the offset
	 * of the previous log record. The first four bytes of the page contain an
	 * integer whose value is the offset of the integer for the last log record
	 * in the page.
	 */
	private void finalizeRecord() {
		mypage.setInt(currentPos, getLastRecordPosition());
		setLastRecordPosition(currentPos);
		currentPos += INT_SIZE;
	}

	private int getLastRecordPosition() {
		return mypage.getInt(LAST_POS);
	}

	private void setLastRecordPosition(int pos) {
		mypage.setInt(LAST_POS, pos);
	}

	public static void write(Transaction t, int pageID, int offset,
			byte[] oldVal, byte[] newVal) {
		if (!Debug.LOGGING)
			return;
		if (oldVal.length != newVal.length)
			throw new RuntimeException(
					"length of oldVal and newVal should be same");

		new UpdateRecord(t.getID(), pageID, offset, oldVal, newVal)
				.writeToLog();
	}

	public static String getLogfile() {
		return getInstance().logfile;
	}

	public void clear() {
		FileManager.getInstance().delete(logfile);
		mypage = new Page();
		appendNewBlock();
		new StartCheckpointRecord(new int[0]).writeToLog();
		new EndCheckpointRecord().writeToLog();
	}
}
