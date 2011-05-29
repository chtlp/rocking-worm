package logging;

import static logging.Page.INT_SIZE;
import java.util.Iterator;

/**
 * A class that provides the ability to move through the records of the log file
 * in reverse order.
 */
class BackwardLogIterator implements Iterator<BasicLogRecord> {
	Block blk;
	Page pg = new Page();
	int currentrec;

	/**
	 * Creates an iterator for the records in the log file, positioned after the
	 * last log record. This constructor is called exclusively by
	 * {@link LogManager#iterator()}.
	 */
	BackwardLogIterator(Block blk) {
		this.blk = blk;
		pg.read(blk);
		currentrec = pg.getInt(LogManager.LAST_POS);
	}

	/**
	 * Determines if the current log record is the earliest record in the log
	 * file.
	 * 
	 * @return true if there is an earlier record
	 */
	public boolean hasNext() {
		return currentrec > 0 || blk.number() > 0;
	}

	/**
	 * Moves to the next log record in reverse order. If the current log record
	 * is the earliest in its block, then the method moves to the next oldest
	 * block, and returns the log record from there.
	 * 
	 * @return the next earliest log record
	 */
	public BasicLogRecord next() {
		if (currentrec == 0)
			moveToNextBlock();
		currentrec = pg.getInt(currentrec);
		return new BasicLogRecord(pg, currentrec + INT_SIZE);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Moves to the next log block in reverse order, and positions it after the
	 * last record in that block.
	 */
	private void moveToNextBlock() {
		blk = new Block(blk.fileName(), blk.number() - 1);
		pg.read(blk);
		currentrec = pg.getInt(LogManager.LAST_POS);
	}
}
