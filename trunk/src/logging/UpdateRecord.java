package logging;

import transaction.Transaction;
import filesystem.BufferManager;
import filesystem.Page;

public class UpdateRecord implements LogRecord {
	private int txnum, offset;
	private int pageID;
	private byte[] newVal;
	private byte[] oldVal;

	/**
	 * Creates a new UPDATE log record.
	 * 
	 * @param txnum
	 *            the ID of the specified transaction
	 * @param blk
	 *            the block containing the value
	 * @param offset
	 *            the offset of the value in the block
	 * @param val
	 *            the new value
	 */
	public UpdateRecord(int txnum, int pageID, int offset, byte[] oldValue,
			byte[] newValue) {
		this.txnum = txnum;
		this.pageID = pageID;
		this.offset = offset;
		this.oldVal = oldValue;
		this.newVal = newValue;
	}

	/**
	 * Creates a log record by reading five other values from the log.
	 * 
	 * @param rec
	 *            the basic log record
	 */
	public UpdateRecord(BasicLogRecord rec) {
		txnum = rec.nextInt();
		pageID = rec.nextInt();
		offset = rec.nextInt();
		oldVal = rec.nextBytes();
		newVal = rec.nextBytes();
	}

	/**
	 * Writes a update record to the log. This log record contains the UPDATE
	 * operator, followed by the transaction id, the pageID, and offset of the
	 * modified block, and the old and new value of bytes.
	 * 
	 * @return the LSN of the last log value
	 */
	public int writeToLog() {
		logger.debug(toString());
		Object[] rec = new Object[] { UPDATE, txnum, pageID, offset, oldVal,
				newVal };
		return logMgr.append(rec);
	}

	public int op() {
		return UPDATE;
	}

	public int txID() {
		return txnum;
	}

	public String toString() {
		return "<UPDATE " + txnum + " " + pageID + " " + offset + " "
				+ new String(oldVal).hashCode() + " "
				+ new String(newVal).hashCode() + ">";
	}

	/**
	 * Replaces the specified data value with the value saved in the log record.
	 * The method pins a buffer to the specified block, calls update to restore
	 * the saved value.
	 * 
	 * @see logging.LogRecord#undo(int)
	 */
	public void undo(int txnum) {
		if (txnum == txID()) {
			logger.debug("UNDO " + toString());
			Page page = BufferManager.getPage(Transaction.getX(txnum), pageID);
			page.writeBytesNoLogging(offset, oldVal, 0, oldVal.length);
			page.release(Transaction.getX(txnum));
		}
	}

	public void redo() {
		logger.debug("REDO " + toString());
		Page page = BufferManager.getPage(Transaction.getX(txnum), pageID);
		page.writeBytesNoLogging(offset, newVal, 0, newVal.length);
		page.release(Transaction.getX(txnum));
	}
}
