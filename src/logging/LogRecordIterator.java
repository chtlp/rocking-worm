package logging;

import static logging.LogRecord.*;
import static logging.Page.INT_SIZE;

import java.util.Iterator;

/**
 * A class that provides the ability to read records from the log.
 */
class LogRecordIterator implements Iterator<LogRecord> {
	private Block blk;
	private Page pg = new Page();
	private int currentrec;
	private int lastpos;

	LogRecordIterator(Block blk, int offset) {
		this.blk = blk;
		pg.read(blk);
		currentrec = offset;
		lastpos = pg.getInt(LogManager.LAST_POS);
	}

	public boolean hasNext() {
		return currentrec < lastpos
				|| blk.number() + 1 < FileManager.getInstance().size(
						LogManager.getLogfile());
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
		if (currentrec == lastpos)
			moveToNextBlock();
		BasicLogRecord rec = new BasicLogRecord(pg, currentrec + INT_SIZE);
		LogRecord result;
		int op = rec.nextInt();
		switch (op) {
			case ENDCHECKPOINT:
				result = new EndCheckpointRecord(rec);
				break;
			case STARTCHECKPOINT:
				result = new StartCheckpointRecord(rec);
				break;
			case START:
				result = new StartRecord(rec);
				break;
			case COMMIT:
				result = new CommitRecord(rec);
				break;
			case ROLLBACK:
				result = new RollbackRecord(rec);
				break;
			case UPDATE:
				result = new UpdateRecord(rec);
				break;
			default:
				throw new RuntimeException("unknown LogRecord type: " + op);
		}
		currentrec = rec.position();
		return result;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	private void moveToNextBlock() {
		blk = new Block(blk.fileName(), blk.number() + 1);
		pg.read(blk);
		currentrec = 0;
		lastpos = pg.getInt(LogManager.LAST_POS);
	}
}
