package index;

import java.io.PrintStream;

import filesystem.BufferManager;
import filesystem.Page;
import filesystem.PageLocator;
import filesystem.RowIdIndex;
import tlp.util.Debug;
import transaction.Transaction;
import value.Value;

public abstract class BPlusNode {
	public static final int TOTAL_HEADER_LEN = Page.HEADER_LENGTH + 2;

	protected int pageID;
	protected BPlusIndex index;

	public abstract InsertionInfo insert(Transaction tr, Value key, int rid,
			Value value);

	public static BPlusNode loadNode(Transaction tr, BPlusIndex index,
			int pageID) {
		Page p = BufferManager.getPage(tr, pageID);
		if (p.getType() == Page.TYPE_BTREE_NODE) {
			p.release(tr);
			return BPlusInternalNode.loadBPlusInternal(tr, index, pageID);
		} else if (p.getType() == Page.TYPE_BTREE_LEAF) {
			p.release(tr);
			return BPlusLeaf.loadBPlusLeaf(tr, index, pageID);
		}

		p.release(tr);
		Debug.fsLogger.error("Unsupported BPlusNode types!");
		throw new RuntimeException();
	}

	// TODO what if rid2 == -1, ignore??
	protected int compare(Value key, int rid, Value key2, int rid2) {
		if (key == null && rid == -1)
			return -1;
		// if the two rid are the same, return EQ
		if (rid == rid2 && rid >= 0)
			return 0;
		if (index.columnID < 0)
			return rid - rid2;
		int c = 0;
		if (key == null)
			c = key2 == null ? 0 : -1; // null is the smallest
		else
			c = key.compareTo(key2);
		// rid2 == -1, we don't care rid
		return c != 0 || rid2 == -1 ? c : rid - rid2;
	}

	abstract public PageLocator search(Transaction tr, Value key, int rid,
			int option);

	// only removes the unique one
	abstract public boolean remove(Transaction tr, Value key, int rid,
			Page father, int left, int mid, int right);

	abstract public int entrySize();

	abstract public int minNumEntry();

	protected Page page;

	protected byte[] removeFront(Transaction tr, int num) {
		int size = entrySize();
		byte[] buffer = new byte[(size - 2) * num];

		page.seek(Page.HEADER_LENGTH);
		byte total = page.readByte();
		byte next = page.readByte();

		for (int i = 0; i < num; ++i) {
			assert next != -1;
			page.copyBytes(TOTAL_HEADER_LEN + next * size + 2, buffer, i
					* (size - 2), size - 2);
			page.seek(TOTAL_HEADER_LEN + next * size);
			page.writeByte(tr, (byte) 0);
			next = page.readByte();
		}
		page.seek(Page.HEADER_LENGTH);
		page.writeByte(tr, (byte) (total - num));
		page.writeByte(tr, next);

		return buffer;
	}

	protected byte[] removeBack(Transaction tr, int num) {
		assert num > 0;
		int size = entrySize();
		byte[] buffer = new byte[(size - 2) * num];

		page.seek(Page.HEADER_LENGTH);
		byte total = page.readByte();
		byte next = page.readByte();
		int now = Page.HEADER_LENGTH + 1, end = now;

		for (int i = 0; i < total; ++i) {
			assert next != -1;

			if (i >= total - num) {
				page.copyBytes(TOTAL_HEADER_LEN + next * size + 2, buffer,
						(i - (total - num)) * (size - 2), size - 2);
			}
			page.seek(TOTAL_HEADER_LEN + next * size);
			if (i >= total - num)
				page.writeByte(tr, (byte) 0);
			else
				page.readByte();

			next = page.readByte();

			if (i == total - num - 1)
				end = now;
			now = TOTAL_HEADER_LEN + next * size + 1;

		}
		page.seek(Page.HEADER_LENGTH);
		page.writeByte(tr, (byte) (total - num));

		page.writeByte(tr, end, (byte) -1);
		return buffer;
	}

	protected void addFront(Transaction tr, int num, byte[] buffer) {
		int size = entrySize();
		assert num * (size - 2) == buffer.length;

		page.seek(Page.HEADER_LENGTH);
		byte total = page.readByte();
		byte currentFirst = page.readByte();

		int k = 0, lastPos = -1, pos = -1, first = -1;
		for (int x = 0; x < index.maxNumEntry; ++x) {
			if (k >= num)
				break;

			pos = TOTAL_HEADER_LEN + x * size;
			byte avail = page.readByte(pos);
			if (avail == 1)
				continue;

			if (first < 0)
				first = x;
			page.writeByte(tr, pos, (byte) 1); // vacant ?
			page.writeBytes(tr, pos + 2, buffer, k * (size - 2), size - 2); // the
																			// data

			if (this instanceof BPlusLeaf && index.isPrimaryIndex()) {
				int rid = page.readInt(pos + 2);
				RowIdIndex
						.update(tr, rid, new PageLocator(page.getPageID(), x));

			}

			if (lastPos >= 0)
				page.writeByte(tr, lastPos + 1, (byte) x); // next slot
			lastPos = pos;
			++k;
		}

		page.writeByte(tr, pos + 1, currentFirst);
		page.seek(Page.HEADER_LENGTH);
		page.writeByte(tr, (byte) (num + total));
		page.writeByte(tr, (byte) first);

	}

	protected void addBack(Transaction tr, int num, byte[] buffer) {
		int size = entrySize();
		assert num * (size - 2) == buffer.length;

		page.seek(Page.HEADER_LENGTH);
		byte total = page.readByte();
		byte now = page.readByte();

		while (now != -1) {
			byte next = page.readByte(TOTAL_HEADER_LEN + now * size + 1);
			if (next == -1)
				break;
			now = next;
		}

		int first = -1, k = 0, lastPos = -1, pos = -1;
		for (int x = 0; x < index.maxNumEntry; ++x) {
			if (k >= num)
				break;

			pos = TOTAL_HEADER_LEN + size * x;
			if (page.readByte(pos) == 1)
				continue;

			if (first == -1)
				first = x;

			page.writeByte(tr, pos, (byte) 1);
			page.writeBytes(tr, pos + 2, buffer, k * (size - 2), size - 2);

			if (this instanceof BPlusLeaf && index.isPrimaryIndex()) {
				int rid = page.readInt(pos + 2);
				RowIdIndex
						.update(tr, rid, new PageLocator(page.getPageID(), x));
			}

			if (lastPos >= 0)
				page.writeByte(tr, lastPos + 1, (byte) x);

			lastPos = pos;
			++k;
		}

		page.writeByte(tr, pos + 1, (byte) -1);

		page.seek(Page.HEADER_LENGTH);
		page.writeByte(tr, (byte) (total + num));

		page.seek(now == -1 ? Page.HEADER_LENGTH + 1 : TOTAL_HEADER_LEN + size
				* now + 1);
		page.writeByte(tr, (byte) first);
	}

	// basically, move all the entries into the left sibling
	protected void mergeLeft(Transaction tr, Page father, Page lp, int left,
			int mid) {
		Debug.testLight.debug("{} merge left {}", shortName(), lp.getPageID());

		int leftNum = lp.readByte(Page.HEADER_LENGTH);
		int minEntry = minNumEntry();
		assert leftNum == minEntry;

		BPlusNode lbro = BPlusNode.loadNode(tr, index, lp.getPageID());
		lbro.load(tr);

		if (Debug.testBulk.isDebugEnabled()) {
			System.out.printf("before %d merge left %d\n", pageID,
					lp.getPageID());
			lbro.print(System.out);
			print(System.out);
		}

		byte[] buffer = removeFront(tr, minEntry - 1);
		lbro.addBack(tr, minEntry - 1, buffer);
		lbro.release(tr);

		if (Debug.testBulk.isDebugEnabled()) {
			Debug.testBulk.debug("after {} merge left {}\n", this.shortName(),
					lbro.shortName());
			lbro.print(System.out);
			BPlusNode f = BPlusNode.loadNode(tr, index, father.getPageID());
			Debug.testBulk.debug("father of {} is {}", this.shortName(),
					f.shortName());
			f.load(tr);
			f.print(System.out);
			f.release(tr);
		}

		lp.setNextPage(tr, page.nextPage());

		byte num = father.readByte(Page.HEADER_LENGTH);
		father.writeByte(tr, Page.HEADER_LENGTH, (byte) (num - 1));
		father.seek(TOTAL_HEADER_LEN + index.internalEntrySize * mid);
		father.writeByte(tr, (byte) 0);
		byte next = father.readByte();
		father.seek(TOTAL_HEADER_LEN + index.internalEntrySize * left + 1);
		father.writeByte(tr, next);

		if (Debug.testBulk.isDebugEnabled()) {
			BPlusNode f = BPlusNode.loadNode(tr, index, father.getPageID());
			Debug.testBulk.debug("After merge \nfather of {} is {}",
					this.shortName(), f.shortName());
			f.load(tr);
			f.print(System.out);
			f.release(tr);

		}
		BufferManager.free(tr, page.getPageID());
	}

	protected void borrowFromLeft(Transaction tr, Page father, Page lp,
			int left, int mid) {
		Debug.testLight.debug("{} borrow from left {}", shortName(),
				lp.getPageID());
		int leftNum = lp.readByte(Page.HEADER_LENGTH);
		int minEntry = minNumEntry();
		int total = leftNum + minEntry - 1;
		int half = total / 2;

		BPlusNode lbro = BPlusNode.loadNode(tr, index, lp.getPageID());
		lbro.load(tr);
		if (Debug.testBulk.isDebugEnabled()) {
			Debug.testBulk.debug("{} borrows from left {}", shortName(),
					lbro.shortName());
			lbro.print(System.out);
			print(System.out);

			BPlusNode f = BPlusNode.loadNode(tr, index, father.getPageID());
			f.load(tr);
			Debug.testBulk.debug("father of {} is {}", shortName(),
					f.shortName());
			f.print(System.out);
			f.release(tr);
		}

		byte[] buffer = lbro.removeBack(tr, leftNum - half);
		addFront(tr, leftNum - half, buffer);
		lbro.release(tr);
		father.seek(TOTAL_HEADER_LEN + index.internalEntrySize * mid + 2);

		int first = page.readByte(Page.HEADER_LENGTH + 1);
		page.seek(TOTAL_HEADER_LEN + entrySize() * first + 2);
		if (index.hasRidKey() || this instanceof BPlusLeaf) {
			int newRid = page.readInt();
			if (index.hasRidKey())
				father.writeInt(tr, newRid);
		}
		if (index.columnID >= 0) {
			Value newKey = page.readValue(index.keyType);
			father.write(tr, newKey);
		}
		father.writeInt(tr, pageID);
	}

	protected void mergeRight(Transaction tr, Page father, Page rp, int right,
			int mid) {
		Debug.testLight.debug("{} merge right {}", shortName(), rp.getPageID());

		int rightNum = rp.readByte(Page.HEADER_LENGTH);
		int minEntry = minNumEntry();
		assert rightNum == minEntry;

		BPlusNode rbro = BPlusNode.loadNode(tr, index, rp.getPageID());

		rbro.load(tr);
		byte[] buffer = rbro.removeFront(tr, rightNum);
		addBack(tr, rightNum, buffer);
		rbro.release(tr);
		page.setNextPage(tr, rp.nextPage());

		byte num = father.readByte(Page.HEADER_LENGTH);
		father.writeByte(tr, Page.HEADER_LENGTH, (byte) (num - 1));

		father.seek(TOTAL_HEADER_LEN + index.internalEntrySize * right);
		father.writeByte(tr, (byte) 0);
		byte next = father.readByte();
		father.seek(TOTAL_HEADER_LEN + index.internalEntrySize * mid + 1);
		father.writeByte(tr, next);

		BufferManager.free(tr, rp.getPageID());
	}

	protected void borrowFromRight(Transaction tr, Page father, Page rp,
			int right, int mid) {
		Debug.testLight.debug("{} borrow from right {}", shortName(),
				rp.getPageID());

		int rightNum = rp.readByte(Page.HEADER_LENGTH);
		int minEntry = minNumEntry();
		int total = rightNum + minEntry - 1;
		int half = total / 2;

		BPlusNode rbro = BPlusNode.loadNode(tr, index, rp.getPageID());
		rbro.load(tr);
		byte[] buffer = rbro.removeFront(tr, rightNum - half);
		addBack(tr, rightNum - half, buffer);
		rbro.release(tr);

		father.seek(TOTAL_HEADER_LEN + index.internalEntrySize * right + 2);
		int first = rp.readByte(Page.HEADER_LENGTH + 1);
		rp.seek(TOTAL_HEADER_LEN + first * entrySize() + 2);
		if (index.hasRidKey() || this instanceof BPlusLeaf) {
			int newRid = rp.readInt();
			if (index.hasRidKey())
				father.writeInt(tr, newRid);
		}
		if (index.columnID >= 0) {
			Value newKey = rp.readValue(index.keyType);
			father.write(tr, newKey);
		}
		father.writeInt(tr, rp.getPageID());

	}

	public abstract void print(PrintStream out);

	public void load(Transaction tr) {
		page = BufferManager.getPage(tr, pageID);
	}

	public void release(Transaction tr) {
		page.release(tr);
	}

	public abstract String shortName();

	public abstract void beforeFirst(Transaction tr);

	public abstract boolean hasNext();

	public abstract KeyValuePair getNext();

	public abstract void close();

	public void drop(Transaction tr) {
		if (this instanceof BPlusLeaf) {
			BufferManager.free(tr, pageID);
			return;
		}

		for (beforeFirst(tr); hasNext();) {
			KeyValuePair pair = getNext();
			int pageID = (Integer) pair.value.get();
			BPlusNode n = BPlusNode.loadNode(tr, this.index, pageID);
			n.drop(tr);
		}

		close();

		BufferManager.free(tr, pageID);
	}

}
