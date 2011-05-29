package logging;

import static logging.Page.*;

/**
 * A class that provides the ability to read the values of a log record. The
 * class has no idea what values are there. Instead, the methods
 * {@link #nextInt() nextInt}, {@link #nextString() nextString} and
 * {@link #nextBytes() nextBytes} read the values sequentially. Thus the client
 * is responsible for knowing how many values are in the log record, and what
 * their types are.
 */
public class BasicLogRecord {
	private Page pg;
	private int pos;

	/**
	 * A log record located at the specified position of the specified page.
	 * This constructor is called exclusively by
	 * {@link BackwardLogIterator#next()}.
	 * 
	 * @param pg
	 *            the page containing the log record
	 * @param pos
	 *            the position of the log record
	 */
	public BasicLogRecord(Page pg, int pos) {
		this.pg = pg;
		this.pos = pos;
	}

	/**
	 * Returns the next value of the current log record, assuming it is an
	 * integer.
	 * 
	 * @return the next value of the current log record
	 */
	public int nextInt() {
		int result = pg.getInt(pos);
		pos += INT_SIZE;
		return result;
	}

	/**
	 * Returns the next value of the current log record, assuming it is a
	 * string.
	 * 
	 * @return the next value of the current log record
	 */
	public String nextString() {
		String result = pg.getString(pos);
		pos += STR_SIZE(result.length());
		return result;
	}

	/**
	 * Returns the next value of the current log record, assuming it is a byte
	 * array.
	 * 
	 * @return the next value of the current log record
	 */
	public byte[] nextBytes() {
		byte[] result = pg.getBytes(pos);
		pos += INT_SIZE + result.length;
		return result;
	}

	public int[] nextInts() {
		int[] result = pg.getInts(pos);
		pos += INT_SIZE + INT_SIZE * result.length;
		return result;
	}

	public int position() {
		return pos;
	}
}
