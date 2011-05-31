package index;

import java.io.PrintStream;

import tlp.test.TestTransactionUsingBarrier;
import tlp.util.Debug;
import transaction.Transaction;
import value.Value;
import filesystem.BufferManager;
import filesystem.FileStorage;
import filesystem.Page;
import filesystem.PageLocator;
import filesystem.RowIdIndex;

public class BPlusLeaf extends BPlusNode {

	int iter;

	Transaction iterTr = null;

	public void beforeFirst(Transaction tr) {
		this.iterTr = tr;
		page = BufferManager.getPage(tr, pageID);
		iter = page.readByte(Page.HEADER_LENGTH + 1);
	}

	public boolean hasNext() {
		return iter >= 0;
	}

	public KeyValuePair getNext() {
		// Debug.testLogger.debug("B+ leaf insert, get next, iter = {}", iter);
		if (iter < 0)
			return null;
		int next = page.readByte(TOTAL_HEADER_LEN + iter * index.leafEntrySize
				+ 1);

		Value key = null;
		int rid = -1;

		page.seek(TOTAL_HEADER_LEN + iter * index.leafEntrySize + 2);

		// always has rid
		rid = page.readInt();

		if (index.columnID >= 0)
			key = page.readValue(index.keyType);

		// child = page.readInt();
		iter = next;
		return new KeyValuePair(key, rid, null);
	}

	public void close() {
		iter = -1;
		page.release(iterTr);
		iterTr = null;
	}

	static int cc = 0;
	@Override
	public InsertionInfo insert(Transaction tr, Value key, int rid, Value value) {
		KeyValuePair pair = null;
		int ptr = -1;
		for (beforeFirst(tr); hasNext();) {
			int j = iter;
			KeyValuePair p = getNext();
			int c = compare(p.key, p.rid, key, rid);
			assert !(index.isPrimaryIndex() && c == 0);
			if (c < 0) {
				ptr = j;
				pair = p;
			} else
				break;
		}

		// find a vacant entry, mayoverflow
		int loc = -1;
		for (int i = 0; i < index.maxNumEntry; ++i) {
			if (page.readByte(TOTAL_HEADER_LEN + i * index.leafEntrySize) == 0) {
				loc = i;
				break;
			}
			assert page.readByte(TOTAL_HEADER_LEN + i * index.leafEntrySize) == 1;
		}

		int place = ptr < 0 ? TOTAL_HEADER_LEN - 1 : TOTAL_HEADER_LEN + ptr
				* index.leafEntrySize + 1;
		byte next = page.readByte(place);

		page.writeByte(tr, place, (byte) loc);

		page.seek(TOTAL_HEADER_LEN + loc * index.leafEntrySize);
		page.writeByte(tr, (byte) 1);
		page.writeByte(tr, next);

		// hasRidKey always returns true
		page.writeInt(tr, rid);
		if (index.columnID >= 0)
			page.write(tr, key);
		if (index.isPrimaryIndex())
			page.write(tr, value);
		assert page.getPos() <= TOTAL_HEADER_LEN + (loc + 1)
				* index.leafEntrySize : index.table.getName();

		if (index.isPrimaryIndex())
			RowIdIndex.update(tr, rid, new PageLocator(page.getPageID(), loc));

		InsertionInfo info = null;
		int num = page.readByte(Page.HEADER_LENGTH) + 1;

		page.writeByte(tr, Page.HEADER_LENGTH, (byte) num);

		// assert !key.get().equals("abides");
		if (num >= index.maxNumEntry) {
			InsertionInfo splitInfo = splitPage(tr, page);
			info = new InsertionInfo(index.isPrimaryIndex() ? rid : -1,
					splitInfo.partKey, splitInfo.partRid, splitInfo.pageID);
		} else
			info = new InsertionInfo(index.isPrimaryIndex() ? rid : -1, null,
					-1, -1);
		close();

		if (Debug.testLogger.isDebugEnabled())
			print(System.out);
		return info;
	}

	private InsertionInfo splitPage(Transaction tr, Page page) {
		int size1 = index.maxNumEntry / 2;
		int size2 = index.maxNumEntry - size1;

		Page q = BufferManager.allocatePage(tr);
		BPlusLeaf rbro = BPlusLeaf.createBPlusNode(tr, index, q.getPageID());

		byte[] buffer = removeBack(tr, size2);

		rbro.load(tr);
		rbro.addFront(tr, size2, buffer);
		if (Debug.testLogger.isDebugEnabled())
			rbro.print(System.out);

		rbro.release(tr);

		if (Debug.testLogger.isDebugEnabled()) {
			System.out.println("**** splitting ****");
			this.print(System.out);
			rbro.print(System.out);
		}
		Debug.testLogger.debug("split: {} to ({}, {})", new Object[] { pageID,
				pageID, rbro.pageID });

		int partRid = -1;
		Value partKey = null;
		int first = q.readByte(Page.HEADER_LENGTH + 1);
		q.seek(TOTAL_HEADER_LEN + entrySize() * first + 2);
		partRid = q.readInt();
		if (index.columnID >= 0)
			partKey = q.readValue(index.keyType);

		q.setNextPage(tr, page.nextPage());
		page.setNextPage(tr, q.getPageID());

		Debug.testLogger.debug("next({}) = {}, next({}) = {}",
				new Object[] { page.getPageID(), page.nextPage(),
						q.getPageID(), q.nextPage() });
		q.release(tr);
		return new InsertionInfo(-1, partKey, index.hasRidKey() ? partRid : -1,
				q.getPageID());
	}

	private BPlusLeaf(Transaction tr, BPlusIndex i, int page, boolean isNew) {
		index = i;
		pageID = page;

		if (isNew) {
			Page p = BufferManager.getPage(tr, pageID);
			assert p.getType() == Page.TYPE_EMPTY;
			
			p.setType(tr, Page.TYPE_BTREE_LEAF);
			p.seek(Page.HEADER_LENGTH);
			p.writeByte(tr, (byte) 0);
			p.writeByte(tr, (byte) -1);
			p.setNextPage(tr, -1);
			
			for(int j=0; j<index.maxNumEntry; ++j) {
				p.writeByte(tr, TOTAL_HEADER_LEN + j * index.leafEntrySize, (byte)0);
			}
			p.release(tr);
		}
	}

	public static BPlusLeaf createBPlusNode(Transaction tr, BPlusIndex index,
			int pageID) {
		return new BPlusLeaf(tr, index, pageID, true);
	}

	public static BPlusLeaf loadBPlusLeaf(Transaction tr, BPlusIndex index,
			int pageID) {
		return new BPlusLeaf(tr, index, pageID, false);
	}

	@Override
	public PageLocator search(Transaction tr, Value key, int rid, int option) {
		int childL = -1, childE = -1, childR = -1;
		int ptr = -1;
		for (beforeFirst(tr); hasNext();) {
			ptr = iter;
			KeyValuePair pair = getNext();
			int c = compare(pair.key, pair.rid, key, rid);
			Debug.testLoggerB.debug(
					"leaf node({}): search key = {}, tree entry = {}",
					new Object[] { pageID, key, pair.key });
			if (c < 0)
				childL = ptr;
			else if (c == 0) {
				childE = ptr;
			} else if (c > 0) {
				childR = ptr;
				break;
			}
		}

		close();

		PageLocator loc = null;
		switch (option) {
		case Index.SEARCH_AT:
			int child = -1;
			if (childL >= 0)
				child = childL;
			else if (childE >= 0)
				child = childE; // childL < 0
			else if (childR > 0)
				child = childR;

			loc = child >= 0 ? new PageLocator(page.getPageID(), child) : null;
			break;
		case Index.SEARCH_AFTER:
			child = -1;
			if (childR >= 0)
				child = childR;
			else if (childE >= 0) // childR < 0
				child = childE;
			else if (childL >= 0)
				child = childL;

			loc = child >= 0 ? new PageLocator(page.getPageID(), child) : null;
			break;
		default:
			Debug.fsLogger.error("Unknown search options");
			throw new UnsupportedOperationException();
		}
		return loc;
	}

	// only removes a unique one
	@Override
	public boolean remove(Transaction tr, Value key, int rid, Page father,
			int left, int mid, int right) {
		int childL = -1, childE = -1;
		int ptr = -1;
		for (beforeFirst(tr); hasNext();) {
			ptr = iter;
			KeyValuePair pair = getNext();
			if (pair == null) {
				close();
				return false;
			}
			int c = compare(pair.key, pair.rid, key, rid);
			Debug.testLoggerB.debug(
					"leaf node({}) remove: search key = {}, tree entry = {}",
					new Object[] { pageID, key, pair.key });
			if (c < 0)
				childL = ptr;
			else if (c == 0) { // no duplicates
				childE = ptr;
				break;
			} else if (c > 0)
				break;
		}

		if (childE < 0) {
			close();
			return false;
		}

		page.seek(TOTAL_HEADER_LEN + index.leafEntrySize * childE);
		page.writeByte(tr, (byte) 0);
		byte next = page.readByte();

		if (childL < 0)
			page.writeByte(tr, Page.HEADER_LENGTH + 1, next);
		else
			page.writeByte(tr, TOTAL_HEADER_LEN + entrySize() * childL + 1,
					next);

		int num = page.readByte(Page.HEADER_LENGTH);
		--num;
		page.writeByte(tr, Page.HEADER_LENGTH, (byte) num);

		int minEntry = minNumEntry();
		if (num < minEntry && father != null) {

			Page lp = null, rp = null;
			int lc = 0, rc = 0;
			// int delta = 2 + (index.hasRidKey() ? Page.INT_SIZE : 0)
			// + index.keyLength;

			if (left >= 0) {
				int offset = BPlusInternalNode.TOTAL_HEADER_LEN + left
						* index.internalEntrySize + 2;
				father.seek(offset);
				if (index.hasRidKey())
					father.readInt();
				if (index.columnID >= 0)
					father.readValue(index.keyType);
				int leftSibling = father.readInt();

				lp = BufferManager.getPage(tr, leftSibling);
				lc = lp.readByte(Page.HEADER_LENGTH);
			}

			if (right >= 0) {
				int offset = BPlusInternalNode.TOTAL_HEADER_LEN + right
						* index.internalEntrySize + 2;
				father.seek(offset);
				if (index.hasRidKey())
					father.readInt();
				if (index.columnID >= 0)
					father.readValue(index.keyType);
				int rightSibling = father.readInt();

				rp = BufferManager.getPage(tr, rightSibling);
				rc = rp.readByte(Page.HEADER_LENGTH);
			}

			if (lp == null) {
				if (rc > minEntry)
					borrowFromRight(tr, father, rp, right, mid);
				else
					mergeRight(tr, father, rp, right, mid);
			} else if (rp == null) {
				if (lc > minEntry)
					borrowFromLeft(tr, father, lp, left, mid);
				else
					mergeLeft(tr, father, lp, left, mid);
			} else {
				if (lc >= rc && lc > minEntry)
					borrowFromLeft(tr, father, lp, left, mid);
				else if (rc >= lc && rc > minEntry)
					borrowFromRight(tr, father, rp, right, mid);
				else
					mergeLeft(tr, father, lp, left, mid); // both have minEntry
															// children
			}

			if (lp != null)
				lp.release(tr);
			if (rp != null)
				rp.release(tr);
		}

		close();

		if (Debug.testBulk.isDebugEnabled()) {
			Debug.testBulk.debug("{} status after removing {}",
					this.shortName(), key.get());
			this.print(System.out);
		}
		return true;
	}

	@Override
	public int minNumEntry() {
		return (index.maxNumEntry - 1) / 2;
	}

	@Override
	public int entrySize() {
		return index.leafEntrySize;
	}

	@Override
	public void print(PrintStream out) {
		Transaction tr = FileStorage.newSystemTransaction();
		out.printf("Index: %s, BPlusLeaf at page(%d):\n", index.name,
				page.getPageID());
		page = BufferManager.getPage(tr, pageID);
		page.seek(Page.HEADER_LENGTH);
		byte total = page.readByte();
		out.printf("total entris %d\n", total);
		int next = page.readByte();
		Value last = null;
		for (int i = 0; i < total; ++i) {
			assert next >= 0 : String.format("i = %d, total = %d", i, total);
			page.seek(TOTAL_HEADER_LEN + next * index.leafEntrySize);
			byte b = page.readByte();
			assert b > 0 : String.format("i = %d, total = %d", i, total);
			next = page.readByte();
			int rid = page.readInt();
			Value key = index.columnID >= 0 ? page.readValue(index.keyType)
					: null;
			out.printf("(rid=%d, key=%s), ", rid, key);
			if (last != null)
				assert last.compareTo(key) <= 0;
			last = key;
		}
		out.println();
		page.release(tr);
		assert next == -1;
		tr.commit();
	}

	@Override
	public String shortName() {
		return String.format("BPlus leaf (%d)", pageID);
	}

}
