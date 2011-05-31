package filesystem;

import java.util.Arrays;
import java.util.LinkedList;

import logging.LogManager;
import table.Storable;
import tlp.util.Debug;
import transaction.Transaction;
import util.Counter;
import value.BytesValue;
import value.IntValue;
import value.Value;

public final class Page extends Cachable {
	public static final int PAGE_SIZE = 5120;

	int blockID = -1;

	byte[] data = null;

	/**
	 * location & ID of page
	 */
	private int pageID;

	/**
	 * This is the last page of a chain.
	 */
	public static final int FLAG_LAST = 16;

	/**
	 * An empty page.
	 */
	public static final int TYPE_EMPTY = 0;

	/**
	 * A data leaf page (without overflow: + FLAG_LAST).
	 */
	public static final int TYPE_DATA_LEAF = 1;

	/**
	 * A data node page (never has overflow pages).
	 */
	public static final int TYPE_DATA_NODE = 2;

	/**
	 * A data overflow page (the last page: + FLAG_LAST).
	 */
	public static final int TYPE_DATA_OVERFLOW = 3;

	/**
	 * A b-tree leaf page (without overflow: + FLAG_LAST).
	 */
	public static final int TYPE_BTREE_LEAF = 4;

	/**
	 * A b-tree node page (never has overflow pages).
	 */
	public static final int TYPE_BTREE_NODE = 5;

	/**
	 * A b-tree header page: stores the meta-information about B+ Tree, the root
	 * page, index types and so on
	 */
	public static final int TYPE_BTREE_HEADER = 6;

	public static final int TYPE_STRING_POOL = 7;

	public static final int TYPE_FREE_LIST = 8;

	public static final int TYPE_STRING_POOL_INDEX = 9;

	public static final int TYPE_ROWID_INEX = 10;

	public static final int TYPE_TABLE_INDEX = 11;

	public static final int TYPE_DATA_LIST = 12;

	public static final int INT_SIZE = 4;

	public static final int HEADER_LENGTH = 20;


	// the operations below mimic a file system
	int pos = 0;

	public boolean valid = true;

	public Page(int pageID) {
		this.pageID = pageID;
	}

	public int getPageID() {
		return pageID;
	}

	public int getPos() {
		return pos;
	}

	public void seek(int p) {
		pos = p;
	}

	public int getType() {
		return readInt(0);
	}

	public boolean isEmpty() {
		return readInt(0) == TYPE_EMPTY;
	}

	public void setType(Transaction tr, int ty) {
		writeInt(tr, 0, ty);
	}

	public int getMarker() {
		return readInt(INT_SIZE);
	}

	public void setMarker(Transaction tr, int marker) {
		writeInt(tr, INT_SIZE, marker);
	}

	public void writeInt(Transaction tr, int num) {
		writeInt(tr, pos, num);
		pos += 4;
	}

	public void writeInt(Transaction tr, int offset, int num) {
		dirty.set(true);
		byte[] old = Arrays.copyOfRange(data, offset, offset + 4);

		byte[] n = new IntValue(num).toBytes(tr);
		System.arraycopy(n, 0, data, offset, Page.INT_SIZE);
		LogManager.write(tr, pageID, offset, old, n);
	}

	public void writeString(Transaction tr, String s) {
		dirty.set(true);
		byte[] sdata = s.getBytes();
		writeInt(tr, sdata.length);

		byte[] old = Arrays.copyOfRange(data, pos, pos + sdata.length);

		System.arraycopy(sdata, 0, data, pos, sdata.length);
		LogManager.write(tr, pageID, pos, old, sdata);
		pos += sdata.length;
	}

	public void write(Transaction tr, Value... values) {
		dirty.set(true);
		for (Value v : values) {

			byte[] newValue = null;
			if (v instanceof BytesValue) {
				newValue = v.toBytes(tr);
			} else if (v != null) {
				byte[] t = v.toBytes(tr);
				newValue = new byte[t.length + 1];
				newValue[0] = 1;
				System.arraycopy(t, 0, newValue, 1, t.length);
			} else {
				newValue = new byte[] { 0 };
			}

			byte[] oldValue = Arrays.copyOfRange(data, pos, pos
					+ newValue.length);
			System.arraycopy(newValue, 0, data, pos, newValue.length);
			LogManager.write(tr, pageID, pos, oldValue, newValue);
			pos += oldValue.length;
		}
		if (pos > data.length)
			throw new ArrayIndexOutOfBoundsException();
	}

	@Override
	public String toString() {
		return String.format("page(%d): type=%s, pin=%d, ref=%s", pageID,
				typeString(getType()), pinned.get(), refBit.get());
	}

	public static String typeString(int type) {
		switch (type) {
		case TYPE_EMPTY:
			return "empty";
		case TYPE_TABLE_INDEX:
			return "table-index";
		case TYPE_ROWID_INEX:
			return "row-id-index";
		case TYPE_BTREE_HEADER:
			return "bplus-header";
		case TYPE_BTREE_NODE:
			return "bplus-internal-node";
		case TYPE_BTREE_LEAF:
			return "bplus-leaf-node";
		case TYPE_STRING_POOL_INDEX:
			return "string-pool-index";
		case TYPE_STRING_POOL:
			return "string-pool-data";
		case TYPE_FREE_LIST:
			return "free-list";
		case TYPE_DATA_LIST:
			return "data-list";
		default:
			throw new UnsupportedOperationException();
		}
	}

	public String readString() {
		int l = readInt();
		String s = new String(data, pos, l);
		pos += l;
		return s;
	}

	public int readInt() {
		int ret = readInt(pos);
		pos += 4;
		return ret;
	}

	public int readInt(int offset) {
		IntValue i = new IntValue();
		i.fromBytes(data, offset);
		return i.get();
	}

	public byte readByte() {
		return data[pos++];
	}

	public byte readByte(int offset) {
		return data[offset];
	}

	public byte[] readBytes(int offset, int len) {
		return Arrays.copyOfRange(data, offset, offset + len);
	}

	public byte[] readBytes(int len) {
		byte[] buffer = Arrays.copyOfRange(data, pos, pos + len);
		pos += len;
		return buffer;
	}

	/**
	 * 
	 * @return whether this page is in a chain of pages and is not the end
	 */
	public boolean hasNextPage() {
		int next = readInt(INT_SIZE * 2);
		return next != -1;
	}

	/**
	 * 
	 * @return the next page in the chain, if no such page, return -1
	 */
	public int nextPage() {
		return readInt(INT_SIZE * 2);
	}

	public void setNextPage(Transaction tr, int next) {
		dirty.set(true);
		writeInt(tr, INT_SIZE * 2, next);
	}

	// free the space, dispose this page
	public void dispose() {
		writeBack();
		dirty.set(false);
		refBit.set(false);
		pinned.set(0);
		data = null;
	}

	public boolean isDisposed() {
		return data == null;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	// public void load(Transaction tr) {
	// if (!isDisposed())
	// return;
	// BufferManager.loadPage(tr, this);
	// }

	public void writeByte(Transaction tr, int offset, byte b) {
		dirty.set(true);
		byte old = data[offset];
		data[offset] = b;
		LogManager.write(tr, pageID, offset, new byte[] { old },
				new byte[] { b });
	}

	public void writeByte(Transaction tr, byte b) {
		writeByte(tr, pos, b);
		pos++;
	}

	public void writeBytes(Transaction tr, int offset, byte[] src, int srcPos,
			int len) {
		dirty.set(true);
		byte[] old = Arrays.copyOfRange(data, offset, offset + len);
		byte[] now = Arrays.copyOfRange(src, srcPos, srcPos + len);
		System.arraycopy(src, srcPos, data, offset, len);
		LogManager.write(tr, pageID, offset, old, now);
	}

	public void writeBytesNoLogging(int offset, byte[] src, int srcPos, int len) {
		dirty.set(true);
		System.arraycopy(src, srcPos, data, offset, len);
	}

	public void copyBytes(int offset, byte[] dest, int destPos, int len) {
		System.arraycopy(data, offset, dest, destPos, len);
	}

	// public void pin() {
	// pinned.getAndIncrement();
	// }

	public void release(Transaction tr) {
		// when already deleted, ignore it
		if (!valid) return;
		
		pinned.getAndDecrement();
		assert pinners.contains(tr) : tr;
		pinners.remove(tr);
		if (Debug.bufferLogger.isDebugEnabled()
				&& pinned.get() != pinners.size()) {
			Debug.bufferLogger.debug("pageID = {}, pinned = {}, pinners = {}",
					new Object[] { pageID, pinned, pinners });
			BufferManager.checkBufferStatus();
		}

		assert pinned.get() == pinners.size();

	}

	public Value readValue(int offset, int valueType) {
		return Value.valueFromBytes(data, offset, valueType);
	}

	public void readStorable(int offset, Storable obj) {
		obj.fromBytes(data, offset);
	}

	public Value readValue(int valueType) {
		Value v = readValue(pos, valueType);
		pos += 1; // for null values
		if (v != null)
			pos += v.byteLength();
		return v;
	}

	public void writeBytes(Transaction tr, byte[] src, int srcPos, int len) {
		writeBytes(tr, pos, src, srcPos, len);
		pos += len;
	}

	public void readStorable(Storable obj) {
		readStorable(pos, obj);
		pos += obj.byteLength();
	}

	Counter writeCount = Counter.getCounter("write-back-counter");
	public void writeBack() {
		if (dirty.get()) {
			writeCount.inc();
			LogManager.flushAll();
			FileStorage.writeBack(pageID, data);
		}
	}

}
