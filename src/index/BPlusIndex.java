package index;

import java.io.PrintStream;

import table.Column;
import table.Record;
import table.Table;
import table.TableIterator;
import tlp.util.Debug;
import transaction.Transaction;
import util.ByteArrayList;
import util.Counter;
import value.BytesValue;
import value.Value;
import filesystem.BufferManager;
import filesystem.FileStorage;
import filesystem.Page;
import filesystem.PageLocator;
import filesystem.RowIdIndex;

// only supports single column key
public class BPlusIndex extends Index implements TableIterator {

	public int internalEntrySize;
	public int leafEntrySize;

	public int maxNumEntry;

	int treeRoot = -1;

	/**
	 * create a new BPlusIndex
	 * 
	 * @param tr
	 * @param table
	 * @param columnID
	 *            -1 means the rid
	 */

	public static BPlusIndex createBPlusIndex(Transaction tr, int pageID,
			String name, Table tbl, int columnID, boolean isPrimary) {
		BPlusIndex index = new BPlusIndex(tr, name, tbl, columnID, pageID,
				isPrimary);
		Page q = BufferManager.allocatePage(tr);
		BPlusLeaf.createBPlusNode(tr, index, q.getPageID());
		q.release(tr);
		index.treeRoot = q.getPageID();

		index.saveMetaData(tr);
		return index;
	}

	private void calcParams() {
		if (columnID >= 0) {
			keyType = table.getColumn(columnID).getType();
			keyLength = table.getColumn(columnID).valueSize();
		} else {
			assert isPrimary;
			keyType = Value.TYPE_INT;
			keyLength = Page.INT_SIZE;
		}

		// valid, next, key, child-page, rid
		internalEntrySize = 2 + keyLength + Page.INT_SIZE
				+ (isPrimaryIndex() ? 0 : Page.INT_SIZE);

		if (!isPrimaryIndex())
			leafEntrySize = internalEntrySize - Page.INT_SIZE; // no child-page
		else {
			int s = 0;
			// Debug.testSimple.debug("table {} index", table.getName());
			for (int i = 0; i < table.numColumns(); ++i) {
				s += table.getColumn(i).valueSize();
				// Debug.testSimple.debug("column {}, size {}",
				// table.getColumn(i).getName(),
				// table.getColumn(i).valueSize());
			}

			leafEntrySize = s + Page.INT_SIZE + 2;
		}

		maxNumEntry = Math.min((Page.PAGE_SIZE - BPlusLeaf.TOTAL_HEADER_LEN)
				/ Math.max(internalEntrySize, leafEntrySize) - 2, 125);

		assert maxNumEntry >= 20;

	}

	public BPlusIndex(Transaction tr, String name, Table table, int columnID,
			int rootPageID, boolean isPrimary) {
		this.tr = tr;
		this.name = name;
		this.table = table;
		this.columnID = columnID;
		this.metaPageID = rootPageID;
		this.isPrimary = isPrimary;
		calcParams();
	}

	/**
	 * save meta data to the root page, indexname + tablename + columnID +
	 * rootpageID + isPrimary
	 */
	public void saveMetaData(Transaction tr) {
		Debug.indexLogger.debug("save BPlus index meta data on page {}",
				metaPageID);
		Page page = BufferManager.getPage(tr, metaPageID);

		page.setType(tr, Page.TYPE_BTREE_HEADER);
		// always remember: first seek, then write
		page.seek(Page.HEADER_LENGTH);
		page.writeString(tr, name);
		page.writeString(tr, table.getName());
		page.writeInt(tr, columnID);
		page.writeInt(tr, metaPageID);
		page.writeInt(tr, isPrimary ? 1 : 0);
		page.writeInt(tr, treeRoot);

		page.release(tr);

	}

	public static BPlusIndex loadFrom(Transaction tr, Table tbl, int pageID) {
		Page page = BufferManager.getPage(tr, pageID);

		page.seek(Page.HEADER_LENGTH);
		String name = page.readString();
		page.readString(); // for table name
		int columnID = page.readInt();
		int rootPageID = page.readInt();
		boolean isPrimary = page.readInt() > 0;
		int treeRoot = page.readInt();

		page.release(tr);
		BPlusIndex ret = new BPlusIndex(tr, name, tbl, columnID, rootPageID,
				isPrimary);
		ret.treeRoot = treeRoot;
		return ret;
	}

	@Override
	public int add(Transaction tr, Record r) {
		if (Debug.MAKING_ERRORS) {
			if (Counter.getCounter("error1").get() == 223) {
				Counter.getCounter("error1").set(0);
				Debug.errorLogger.debug("making errors");
				treeRoot = -1;
				saveMetaData(tr);
				throw new RuntimeException();
			}

		}

		if (!isPrimaryIndex()) {
			Debug.fsLogger
					.error("can't insert a record into a secondary index");
			throw new IndexException();
		}

		Value key = null;
		if (columnID >= 0) {
			key = r.getValue(columnID);
		}

		ByteArrayList buffer = new ByteArrayList(100);
		for (int i = 0; i < r.size(); ++i) {
			if (i == columnID)
				continue;

			Value v = r.getValue(i);
			buffer.addAll(Value.valueToBytes(tr, v));
		}

		BytesValue bv = new BytesValue(buffer.toArray());

		BPlusNode root = BPlusNode.loadNode(tr, this, treeRoot);

		int rid = RowIdIndex.add(tr, new PageLocator(RowIdIndex.RESERVED, -1));

		InsertionInfo info = root.insert(tr, key, rid, bv);

		if (info.pageID >= 0) {
			Page q = BufferManager.allocatePage(tr);
			BPlusInternalNode t = BPlusInternalNode.createBPlusNode(tr, this,
					q.getPageID());
			t.add(tr, Value.minValue(keyType), -1, treeRoot);
			t.add(tr, info.partKey, info.partRid, info.pageID);
			treeRoot = q.getPageID();
			saveMetaData(tr);
			q.release(tr);
		}

		return rid;
	}

	public void add(Transaction tr, Value key, int rid) {

		if (isPrimaryIndex()) {
			Debug.fsLogger.error("can't insert a value into a primary index");
			throw new IndexException();
		}
		BPlusNode root = BPlusNode.loadNode(tr, this, treeRoot);
		InsertionInfo info = root.insert(tr, key, rid, new BytesValue(
				new byte[] {}));

		if (info.pageID >= 0) {
			Page q = BufferManager.allocatePage(tr);
			BPlusInternalNode t = BPlusInternalNode.createBPlusNode(tr, this,
					q.getPageID());
			t.add(tr, Value.minValue(keyType), -1, treeRoot);
			t.add(tr, info.partKey, info.partRid, info.pageID);
			treeRoot = q.getPageID();
			saveMetaData(tr);
			q.release(tr);
		}

	}

	public void remove(Transaction tr, Value key, int rid) {
		assert treeRoot >= 0;
		BPlusNode node = BPlusNode.loadNode(tr, this, treeRoot);
		boolean removed = node.remove(tr, key, rid, null, -1, -1, -1);
		assert removed;
	}

	public boolean hasRidKey() {
		return (columnID < 0) || (!isPrimaryIndex());
	}

	@Override
	public void bulkadd(Transaction tr, TableIterator r) {
		throw new UnsupportedOperationException();

	}

	@Override
	public Record find(Value key) {
		TableIterator i = findEqual(key);
		i.open();
		Record r = i.next();
		i.close();
		return r;
	}

	@Override
	public TableIterator findEqual(Value key) {
		return new RangeIterator(tr, this, key);
	}

	@Override
	public TableIterator findRange(Integer left, Value lv, Integer right,
			Value rv) {
		return new RangeIterator(tr, this, left, lv, right, rv);
	}

	@Override
	public int numColumns() {
		return table.numColumns();
	}

	@Override
	public Column getColumn(int ind) {
		return table.getColumn(ind);
	}

	Page iterPage = null;
	int iterInd = -1;

	Transaction tr = null;

	@Override
	public void open() {
		BPlusNode n = BPlusNode.loadNode(tr, this, treeRoot);
		while (n != null && n instanceof BPlusInternalNode) {
			n = ((BPlusInternalNode) n).firstChild(tr);
		}
		if (n != null) {
			iterPage = BufferManager.getPage(tr, n.pageID);
			iterInd = iterPage.readByte(Page.HEADER_LENGTH + 1);
		}
	}

	@Override
	public boolean hasNext() {
		return iterPage != null && (iterInd >= 0 || iterPage.nextPage() >= 0);
	}

	@Override
	public Record next() {

		if (iterPage == null)
			return null;
		Debug.testLogger.debug("BPlusIndex iter page = {}",
				iterPage.getPageID());
		if (iterInd != -1) {
			Record ret = null;
			if (isPrimaryIndex()) {
				ret = table.getRecord(tr, new PageLocator(iterPage.getPageID(),
						iterInd));
			} else {
				int offset = BPlusNode.TOTAL_HEADER_LEN + leafEntrySize
						* iterInd + 2;
				PageLocator loc = RowIdIndex.get(tr, iterPage.readInt(offset));
				ret = table.getRecord(tr, loc);
			}
			iterInd = iterPage.readByte(BPlusNode.TOTAL_HEADER_LEN
					+ leafEntrySize * iterInd + 1);
			return ret;
		} else {
			if (iterPage.hasNextPage()) {
				Page q = BufferManager.getPage(tr, iterPage.nextPage());
				iterPage.release(tr);
				iterPage = q;
				iterInd = iterPage.readByte(Page.HEADER_LENGTH + 1);
			} else
				iterPage = null;
			return next();
		}
	}

	@Override
	public void close() {
		if (iterPage != null)
			iterPage.release(tr);
		iterInd = -1;
		iterPage = null;
	}

	@Override
	public boolean removeUnique(Transaction tr, Record r) {
		if (!isPrimaryIndex())
			assert r.getRowID() >= 0;
		// if (columnID >= 0)
		// assert r.getValue(columnID) != null;

		BPlusNode node = BPlusNode.loadNode(tr, this, treeRoot);

		// note tha key can be null
		Value key = columnID >= 0 ? r.getValue(columnID) : null;
		return node.remove(tr, key, r.getRowID(), null, -1, -1, -1);

	}

	@Override
	public void drop(Transaction tr) {
		if (treeRoot >= 0) {
			BPlusNode n = BPlusNode.loadNode(tr, this, treeRoot);
			n.drop(tr);
		}

		BufferManager.free(tr, treeRoot);
	}

	@Override
	public void print(PrintStream out) {
		Transaction tr = FileStorage.newSystemTransaction();

		for (open(); hasNext();) {
			Record r = next();
			out.println(r);
		}

		tr.commit();
	}

	public void writeRecord(Transaction tr, PageLocator loc, Record rec) {
		assert isPrimaryIndex();
		Page p = BufferManager.getPage(tr, loc.pageID);

		p.seek(BPlusLeaf.TOTAL_HEADER_LEN + leafEntrySize * loc.ind + 2);

		p.writeInt(tr, rec.getRowID()); // for the rid
		if (columnID >= 0)
			p.write(tr, rec.getValue(columnID));
		for (int i = 0; i < table.numColumns(); ++i) {
			if (i != columnID)
				p.write(tr, rec.getValue(i));
		}
	}
}
