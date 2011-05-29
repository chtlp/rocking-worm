package logging;

import static logging.LogRecord.*;

import java.util.Iterator;

/**
 * A class that provides the ability to read records from the log in reverse
 * order. Unlike the similar class {@link logging.BackwardLogIterator
 * LogIterator}, this class understands the meaning of the log records.
 */
public class BackwardLogRecordIterator implements Iterator<LogRecord> {
	BackwardLogIterator iter = LogManager.getInstance().iterator();

	public boolean hasNext() {
		return iter.hasNext();
	}

	/**
	 * Constructs a log record from the values in the current basic log record.
	 * The method first reads an integer, which denotes the type of the log
	 * record. Based on that type, the method calls the appropriate LogRecord
	 * constructor to read the remaining values.
	 * 
	 * @return the next log record, or null if no more records
	 */
	public LogRecord next() {
		BasicLogRecord rec = iter.next();
		int op = rec.nextInt();
		switch (op) {
			case ENDCHECKPOINT:
				return new EndCheckpointRecord(rec);
			case STARTCHECKPOINT:
				return new StartCheckpointRecord(rec);
			case START:
				return new StartRecord(rec);
			case COMMIT:
				return new CommitRecord(rec);
			case ROLLBACK:
				return new RollbackRecord(rec);
			case UPDATE:
				return new UpdateRecord(rec);
			default:
				throw new RuntimeException("unknown LogRecord type: " + op);
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
