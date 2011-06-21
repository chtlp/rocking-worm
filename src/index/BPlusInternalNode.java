package index;

import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tlp.util.Debug;
import transaction.Transaction;
import value.IntValue;
import value.Value;
import filesystem.BufferManager;
import filesystem.FileStorage;
import filesystem.Page;
import filesystem.PageLocator;

public class BPlusInternalNode extends BPlusNode {

	public static Logger logger = LoggerFactory
			.getLogger(BPlusInternalNode.class);

	private BPlusInternalNode(Transaction tr, BPlusIndex i, int page,
			boolean isNew) {
		index = i;
		pageID = page;
		this.tr = tr;

		if (isNew) {
			Page p = BufferManager.getPage(tr, pageID);

			assert p.getType() == Page.TYPE_EMPTY;

			p.setType(tr, Page.TYPE_BTREE_NODE);
			p.writeByte(tr, Page.HEADER_LENGTH, (byte) 0); // zero entries
			p.writeByte(tr, Page.HEADER_LENGTH + 1, (byte) -1); // no
																// entries

			int offset = TOTAL_HEADER_LEN;
			for (int j = 0; j < index.maxNumEntry; ++j) {
				p.writeByte(tr, offset, (byte) 0); // not occupied
				offset += index.internalEntrySize;
			}

			p.release(tr);
		}
	}

	public static BPlusInternalNode createBPlusNode(Transaction tr,
			BPlusIndex index, int pageID) {
		return new BPlusInternalNode(tr, index, pageID, true);
	}

	public static BPlusInternalNode loadBPlusInternal(Transaction tr,
			BPlusIndex index, int pageID) {
		return new BPlusInternalNode(tr, index, pageID, false);
	}

	public int size(Transaction tr) {
		Page p = BufferManager.getPage(tr, pageID);
		int s = p.readByte(Page.HEADER_LENGTH);
		p.release(tr);
		return s;
	}

	int iter = -1;

	public void open() {
		page = BufferManager.getPage(tr, pageID);
		iter = page.readByte(Page.HEADER_LENGTH + 1);
	}

	public boolean hasNext() {
		return iter >= 0;
	}

	public KeyValuePair getNext() {
		if (iter < 0)
			return null;
		int next = page.readByte(TOTAL_HEADER_LEN + iter
				* index.internalEntrySize + 1);

		Value key = null;
		int rid = -1;
		int child;

		page.seek(TOTAL_HEADER_LEN + iter * index.internalEntrySize + 2);

		if (index.hasRidKey())
			rid = page.readInt();

		if (index.columnID >= 0)
			key = page.readValue(index.keyType);

		child = page.readInt();
		iter = next;
		return new KeyValuePair(key, rid, new IntValue(child));
	}

	public void close() {
		iter = -1;
		page.release(tr);
	}

	static int cc = 0;

	@Override
	public InsertionInfo insert(Transaction tr, Value key, int rid, Value value) {
		++cc;
		// note that a internal node is guaranteed to have children
		open();
		int child = -1, ptr = -1;
		if (!BPlusIndex.BINARY_SEARCH) {
			KeyValuePair pair = null;
			for (open(); hasNext();) {
				int j = iter;
				KeyValuePair p = getNext();
				// the first element is a dummy one
				if (logger.isDebugEnabled()) {
					if (ptr == -1 && p.key != null)
						assert p.key.compareTo(key) <= 0;
					if (ptr != -1)
						assert p.key != null;
				}
				if (ptr == -1 || compare(p.key, p.rid, key, rid) <= 0) {
					ptr = j;
					pair = p;
				} else
					break;
			}
			child = (Integer) pair.value.get();
		}
		if (BPlusIndex.BINARY_SEARCH) {
			PageLocator loc = binarySearch(tr, key, rid, BPlusIndex.SEARCH_AT);
			page.seek(TOTAL_HEADER_LEN + loc.ind * index.internalEntrySize + 2);
			ptr = loc.ind;
//			assert ptr == loc.ind;

			int prid = -1;
			Value pkey = null;
			if (index.hasRidKey())
				prid = page.readInt();

			if (index.columnID >= 0)
				pkey = page.readValue(index.keyType);

			int child2 = page.readInt();
//			assert child == child2;
			child = child2;

			if (Debug.testSimple.isDebugEnabled()) {
				if (loc.ind == -1 && page.readByte(Page.HEADER_LENGTH) > 0) {
					assert false;
				}
				if (loc.ind >= 0) {
					page.seek(TOTAL_HEADER_LEN + loc.ind
							* index.internalEntrySize + 1);
					int next = page.readByte();

					prid = -1;
					pkey = null;
					if (index.hasRidKey())
						prid = page.readInt();

					if (index.columnID >= 0)
						pkey = page.readValue(index.keyType);

					assert pkey == null || pkey.compareTo(key) <= 0;

					if (next >= 0) {
						page.seek(TOTAL_HEADER_LEN + next
								* index.internalEntrySize + 2);
						prid = -1;
						pkey = null;
						if (index.hasRidKey())
							prid = page.readInt();

						if (index.columnID >= 0)
							pkey = page.readValue(index.keyType);

						assert pkey.compareTo(key) >= 0;
					}

				}
			}

		}

		close();

		BPlusNode node = BPlusNode.loadNode(tr, index, child);
		InsertionInfo info = node.insert(tr, key, rid, value);
		if (info.partKey == null && info.partRid == -1)
			return new InsertionInfo(info.rid, null, -1, -1);
		else {
			return addEntry(tr, info.rid, ptr, info.partKey, info.partRid,
					info.pageID);
		}
	}

	// add to a internal node directly
	void add(Transaction tr, Value key, int rid, int child) {
		// note that a internal node is guaranteed to have children
		KeyValuePair pair = null;
		int ptr = -1;
		for (open(); hasNext();) {
			int j = iter;
			KeyValuePair p = getNext();
			if (ptr == -1 || compare(pair.key, pair.rid, key, rid) <= 0) {
				ptr = j;
				pair = p;
			} else
				break;
		}
		close();

		InsertionInfo info = addEntry(tr, rid, ptr, key, rid, child);
		// assert info.pageID == -1; // can't split
	}

	private InsertionInfo addEntry(Transaction tr, int rid, int iter,
			Value partKey, int partRid, int child) {
		// invite null value as the smallest key
		// assert partKey != null || !index.isPrimaryIndex();
		load(tr);
		int num = size(tr);

		// find a vacant entry
		int loc = -1;
		for (int i = 0; i < index.maxNumEntry + 1; ++i) {
			if (page.readByte(TOTAL_HEADER_LEN + i * index.internalEntrySize) == 0) {
				loc = i;
				break;
			}
		}

		assert loc >= 0;

		int place = iter < 0 ? TOTAL_HEADER_LEN - 1 : TOTAL_HEADER_LEN + iter
				* index.internalEntrySize + 1;
		byte next = page.readByte(place);
		page.writeByte(tr, place, (byte) loc);

		page.seek(TOTAL_HEADER_LEN + loc * index.internalEntrySize);
		page.writeByte(tr, (byte) 1);
		page.writeByte(tr, next);
		if (index.hasRidKey())
			page.writeInt(tr, partRid);
		// if (partKey != null)
		if (index.columnID >= 0)
			page.write(tr, partKey);
		page.writeInt(tr, child);

		InsertionInfo info = null;
		++num;
		page.writeByte(tr, Page.HEADER_LENGTH, (byte) num);
		if (num <= index.maxNumEntry) {
			info = new InsertionInfo(-1, null, -1, -1);
		} else {
			InsertionInfo splitInfo = splitPage(tr, page);
			info = new InsertionInfo(rid, splitInfo.partKey, splitInfo.partRid,
					splitInfo.pageID);
		}

		release(tr);
		return info;
	}

	private InsertionInfo splitPage(Transaction tr, Page page) {
		int size1 = (index.maxNumEntry + 1) / 2;
		int size2 = (index.maxNumEntry + 1) - size1;

		Page q = BufferManager.allocatePage(tr);
		BPlusInternalNode rbro = BPlusInternalNode.createBPlusNode(tr, index,
				q.getPageID());

		byte[] buffer = removeBack(tr, size2);
		rbro.load(tr);
		rbro.addFront(tr, size2, buffer);
		rbro.release(tr);

		int partRid = -1;
		Value partKey = null;
		int first = q.readByte(Page.HEADER_LENGTH + 1);
		q.seek(TOTAL_HEADER_LEN + entrySize() * first + 2);
		if (index.hasRidKey())
			partRid = q.readInt();
		if (index.columnID >= 0)
			partKey = q.readValue(index.keyType);
		Debug.testLoggerB.debug("internal split {} ==> {}, {}", new Object[] {
				pageID, pageID, q.getPageID() });

		q.release(tr);
		return new InsertionInfo(-1, partKey, partRid, q.getPageID());
	}

	public PageLocator binarySearch(Transaction tr, Value key, int rid,
			int option) {
		open();
		int num = page.readByte(Page.HEADER_LENGTH);
		if (num == 0) {
			close();
			return new PageLocator(page.getPageID(), -1);
		}

		int[] loc = new int[num];
		for (int i = 0; i < num; ++i) {
			int p = TOTAL_HEADER_LEN + iter * index.internalEntrySize;
			loc[i] = p;
			iter = page.readByte(p + 1);
		}

		int low = 0, high = num - 1;
		while (low + 1 < high) {
			int mid = (low + high) / 2;

			page.seek(loc[mid] + 2);

			int prid = -1;
			Value pkey = null;
			if (index.hasRidKey())
				prid = page.readInt();

			if (index.columnID >= 0)
				pkey = page.readValue(index.keyType);

			int c = compare(pkey, prid, key, rid);

			if (option == Index.SEARCH_AT) {
				if (c > 0)
					high = mid - 1;
				else
					low = mid;
			} else if (option == Index.SEARCH_AFTER) {
				if (c <= 0)
					low = mid + 1;
				else
					high = mid;
			}
		}

		close();

		int ret = low;
		if (low != high) {
			page.seek(loc[low] + 2);

			int prid = -1;
			Value pkey = null;
			if (index.hasRidKey())
				prid = page.readInt();

			if (index.columnID >= 0)
				pkey = page.readValue(index.keyType);

			int c0 = compare(pkey, prid, key, rid);

			if (c0 < 0) {
				page.seek(loc[high] + 2);

				prid = -1;
				pkey = null;
				if (index.hasRidKey())
					prid = page.readInt();

				if (index.columnID >= 0)
					pkey = page.readValue(index.keyType);

				int c = compare(pkey, prid, key, rid);
				if (c0 < 0 && c <= 0 && option == Index.SEARCH_AT)
					ret = high;
			}
		}

		return new PageLocator(page.getPageID(), (loc[ret] - TOTAL_HEADER_LEN)
				/ index.internalEntrySize);

	}

	@Override
	public PageLocator search(Transaction tr, Value key, int rid, int option) {
		
		int child = -1;
		open();
		if (!BPlusIndex.BINARY_SEARCH) {
			int childL = -1, childE = -1;
			boolean first = true;
			for (; hasNext();) {
				KeyValuePair pair = getNext();
				if (logger.isDebugEnabled()) {
					if (first && pair.key != null)
						assert pair.key.compareTo(key) <= 0;
					if (!first)
						assert pair.key != null;
				}

				int c = first ? -1 : compare(pair.key, pair.rid, key, rid);
				Debug.testLoggerB.debug(
						"internal node({}): search key = {}, tree entry = {}",
						new Object[] { pageID, key, pair.key });
				if (!first)
					assert pair.key != null;
				first = false;
				if (c < 0)
					childL = (Integer) pair.value.get();
				else if (c == 0) {
					childE = (Integer) pair.value.get();
				} else if (c > 0)
					break;
			}
			child = childL;
			if (option == Index.SEARCH_AFTER && childE >= 0)
				child = childE;

		}
		if (BPlusIndex.BINARY_SEARCH){
			PageLocator loc = binarySearch(tr, key, rid, option);

			page.seek(TOTAL_HEADER_LEN + loc.ind * index.internalEntrySize + 2);

			int prid = -1;
			Value pkey = null;
			if (index.hasRidKey())
				prid = page.readInt();

			if (index.columnID >= 0)
				pkey = page.readValue(index.keyType);

			int child2 = page.readInt();
//			assert child2 == child;
			child = child2;

		}
		close();
		BPlusNode node = BPlusNode.loadNode(tr, index, child);
		return node.search(tr, key, rid, option);
	}

	@Override
	public boolean remove(Transaction tr, Value key, int rid, Page father,
			int left, int mid, int right) {
		if (Debug.testLight.isDebugEnabled() && pageID == 12) {
			open();
			Debug.testLight.debug("internal node {}, numEntry = {}", pageID,
					page.readByte(Page.HEADER_LENGTH));
			close();
		}
		boolean removed = false;
		int childL = -1, child = -1, childR = -1, childPage = -1;
		int ptr = -1;
		boolean first = true;
		for (open(); hasNext();) {
			ptr = iter;
			KeyValuePair pair = getNext();
			if (pair == null)
				return false;
			int c = first ? -1 : compare(pair.key, pair.rid, key, rid);
			Debug.testLoggerB
					.debug("internal node({}) remove: search key = {}, tree entry = {}",
							new Object[] { pageID, key, pair.key });
			first = false;
			if (c <= 0) {
				childL = child;
				child = ptr;
				childPage = (Integer) pair.value.get();
			} else if (c > 0)
				break;
		}

		page.seek(TOTAL_HEADER_LEN + entrySize() * child + 1);
		childR = page.readByte();

		Page q = BufferManager.getPage(tr, childPage);

		BPlusNode n = BPlusNode.loadNode(tr, index, q.getPageID());
		removed = n.remove(tr, key, rid, page, childL, child, childR);

		q.release(tr);

		if (!removed) {
			close();
			return removed;
		}

		int num = page.readByte(Page.HEADER_LENGTH);

		int minEntry = minNumEntry();
		if (num < minEntry && father != null) {

			Page lp = null, rp = null;
			int lc = 0, rc = 0;

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

		return removed;
	}

	public BPlusNode firstChild(Transaction tr) {
		Page p = BufferManager.getPage(tr, pageID);
		p.seek(TOTAL_HEADER_LEN - 1);
		int next = p.readByte();
		if (next == -1)
			return null;
		p.seek(TOTAL_HEADER_LEN + next * index.internalEntrySize);

		byte b = p.readByte(); // vacant
		assert b > 0;
		p.readByte(); // next
		if (index.hasRidKey())
			p.readInt(); // rid
		if (index.columnID >= 0)
			p.readValue(index.keyType);

		int c = p.readInt();

		p.release(tr);

		return BPlusNode.loadNode(tr, index, c);

	}

	@Override
	public int entrySize() {
		return index.internalEntrySize;
	}

	@Override
	public int minNumEntry() {
		return (index.maxNumEntry + 1) / 2;
	}

	@Override
	public void print(PrintStream out) {
		Transaction tr = FileStorage.newSystemTransaction();
		out.printf("BPlusInternalNode at page(%d):\n", page.getPageID());
		page = BufferManager.getPage(tr, pageID);
		page.seek(Page.HEADER_LENGTH);
		byte total = page.readByte();
		out.printf("total entris %d\n", total);
		int next = page.readByte();
		Value last = null;
		for (int i = 0; i < total; ++i) {
			assert next >= 0 : String.format("i = %d, total = %d", i, total);
			page.seek(TOTAL_HEADER_LEN + next * index.internalEntrySize);
			byte b = page.readByte();
			assert b == 1;
			next = page.readByte();
			Integer rid = index.hasRidKey() ? page.readInt() : null;
			Value key = index.columnID >= 0 ? page.readValue(index.keyType)
					: null;
			int c = page.readInt();
			out.printf("(rid=%s, key=%s, child-page=%d), ", rid, key, c);

			if (last != null)
				assert last.compareTo(key) <= 0;
			last = key;
		}
		assert next == -1;
		out.println();
		page.release(tr);
		tr.commit();

	}

	@Override
	public String shortName() {
		return String.format("BPlus internal (%d)", pageID);
	}

}