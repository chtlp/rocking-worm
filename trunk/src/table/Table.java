package table;

import filesystem.BufferManager;
import filesystem.FileStorage;
import filesystem.Page;
import filesystem.PageLocator;
import filesystem.RowIdIndex;
import index.BPlusIndex;
import index.BPlusNode;
import index.Index;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import tlp.util.Debug;
import transaction.TableLock;
import transaction.Transaction;
import value.Value;

public class Table {

	ArrayList<Column> columns = new ArrayList<Column>();
	ArrayList<Index> indexes = new ArrayList<Index>();

	boolean isPermanent = false;

	int rootPageID = -1;

	String dbName; // name of the database which this Table resides in
	String name;

	BPlusIndex scanIndex;

	TableLock lock = null;

	public Table(String name, List<Column> columns) {
		this.name = name;
		int k = 0;
		for (Column c : columns) {
			c.setColumnId(k++);
			c.setTable(this);
			this.columns.add(c);
		}
	}

	public Table(int root) {
		rootPageID = root;
	}

	// used to locate a table; discouraged, use TableManager.getTable instead
	public static Table getTable(Transaction tr, String name) {
		return TableManager.getTable(tr, name);
	}

	public String getName() {
		return name;
	}

	public boolean isPermanent() {
		return isPermanent;
	}

	public void setPermanent(boolean isPermanent) {
		this.isPermanent = isPermanent;
	}

	public int getRootPageID() {
		return rootPageID;
	}

	public void setRootPageID(int rootPageID) {
		this.rootPageID = rootPageID;
	}

	public Column getColumn(int ind) {
		return columns.get(ind);
	}

	public void insert(Transaction tr, Record r) {
		Debug.tableLogger.debug("{}: insert {}", this, r);
		int rid = scanIndex.add(tr, r);
		for (Index i : indexes) {
			if (i.getRootPage() == scanIndex.getRootPage())
				continue;
			Value key = r.getValue(i.columnID);
			// note that key can be null
			i.add(tr, key, rid);
		}
	}

	public void remove(Transaction tr, Record r) {
		scanIndex.removeUnique(tr, r);

		for (Index i : indexes) {
			if (i == scanIndex)
				continue;

			i.removeUnique(tr, r);
		}
	}

	// public void update(Transaction tr, Record r, int columnID, Value newVal)
	// {
	// // TODO efficiency issue
	// remove(tr, r);
	// r.setValue(columnID, newVal);
	// insert(tr, r);
	// }

	public void update(Transaction tr, Record oldRec, Record newRec) {
		boolean primaryChanged = false;
		for (int i = 0; i < columns.size(); ++i) {
			if (columns.get(i).isPrimaryKey()
					&& oldRec.getValue(i) != newRec.getValue(i)) {
				primaryChanged = true;
			}

		}
		// if more than one index, don't bother
		if (primaryChanged || indexes.size() > 1) {
			// TODO efficiency issue
			remove(tr, oldRec);
			insert(tr, newRec);
		} else {
			PageLocator loc = RowIdIndex.get(tr, oldRec.rowID);
			assert indexes.get(0).getRootPage() == scanIndex.getRootPage();
			assert newRec.rowID == oldRec.rowID;
			
			scanIndex.writeRecord(tr, loc, newRec);
		}
	}

	public int getPrimaryKey() {
		return -1;
	}

	public TableIterator getRecords(Transaction tr) {
		return getScanIndex(tr);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Table))
			return false;
		Table t = (Table) obj;
		assert !(this != t && t.name.equals(t.name));
		return t.name.equals(t.name) && t.dbName.equals(t.dbName);
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ dbName.hashCode();
	}

	public int numColumns() {
		return columns.size();
	}

	int numIndexes() {
		return indexes.size();
	}

	Index getIndex(int i) {
		return indexes.get(i);
	}

	public Index getIndex(Transaction tr, String name) {
		for (Index i : indexes) {
			if (i.getName().equals(name))
				return Index.loadIndex(tr, this, i.getRootPage());
		}

		throw new ArrayIndexOutOfBoundsException();
	}

	public void saveMetaData(Transaction tr) {
		Debug.tableManagerLogger.debug("Table {} saving meta data", name);
		Page p = BufferManager.getPage(tr, rootPageID);

		p.seek(Page.HEADER_LENGTH);
		p.writeString(tr, name);
		p.writeString(tr, dbName);
		p.writeInt(tr, numColumns());

		for (int i = 0; i < numColumns(); ++i) {
			byte[] b = getColumn(i).toBytes(tr);
			p.writeBytes(tr, b, 0, b.length);
		}

		p.writeInt(tr, numIndexes());
		for (int i = 0; i < numIndexes(); ++i) {
			byte[] b = getIndex(i).toBytes(tr);
			p.writeBytes(tr, b, 0, b.length);
		}

		p.release(tr);
	}

	public void loadMetaData(Transaction tr) {
		columns.clear();
		indexes.clear();
		Page p = BufferManager.getPage(tr, rootPageID);

		p.seek(Page.HEADER_LENGTH);
		name = p.readString();
		dbName = p.readString();
		int numColumns = p.readInt();
		for (int i = 0; i < numColumns; ++i) {
			Column c = new Column();
			p.readStorable(c);
			c.setTable(this);
			columns.add(c);
		}

		int numIndexes = p.readInt();
		for (int i = 0; i < numIndexes; ++i) {
			int root = p.readInt();
			Index ind = Index.loadIndex(tr, this, root);
			indexes.add(ind);
		}

		p.release(tr);

		scanIndex = (BPlusIndex) indexes.get(0);
	}

	public void addIndex(Index index) {
		indexes.add(index);
	}

	public Record getRecord(Transaction tr, PageLocator loc) {
		Record r = new Record();
		r.setTable(this);

		Page p = BufferManager.getPage(tr, loc.pageID);
		p.seek(BPlusNode.TOTAL_HEADER_LEN + scanIndex.leafEntrySize * loc.ind
				+ 2);
		r.rowID = p.readInt(); // for the rid

		if (Debug.testLight2.isDebugEnabled()) {
			BPlusNode n = BPlusNode.loadNode(tr, this.scanIndex, loc.pageID);
			n.load(tr);
			// n.print(System.out);
			n.release(tr);
		}

		Value key = null;
		if (scanIndex.columnID >= 0) {
			key = p.readValue(scanIndex.keyType);
		}

		for (int i = 0; i < columns.size(); ++i) {
			Column col = columns.get(i);

			if (i == scanIndex.columnID)
				r.addValue(key);
			else
				r.addValue(p.readValue(col.getType()));
		}

		p.release(tr);
		return r;
	}

	public BPlusIndex getScanIndex(Transaction tr) {
		return BPlusIndex.loadFrom(tr, this, scanIndex.getRootPage());
	}

	public String getDBName() {
		return dbName;
	}

	public void setDBName(String dbName) {
		this.dbName = dbName;
	}

	public void drop(Transaction tr) {
		for (Index i : indexes) {
			i.drop(tr);
		}

		BufferManager.free(tr, rootPageID);
	}

	public boolean dropIndex(Transaction tr, String indxName) {
		for (Index i : indexes) {
			if (i.getName().equals(indxName)) {
				if (i == scanIndex) {
					i.setName(name + ".SanIndex");
					i.saveMetaData(tr);
					saveMetaData(tr);
				} else {
					i.drop(tr);
					indexes.remove(i);
					saveMetaData(tr);
				}
				break;

			}
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("%s.%s", dbName, name);
	}

	public void printTable(PrintStream out) {
		Transaction tr = FileStorage.newSystemTransaction();
		out.format("Table %s, ", this);
		out.print("column: ");
		for (Column c : columns) {
			out.format("%s ", c);
		}
		out.println();
		TableIterator i = getScanIndex(tr);
		for (i.open(); i.hasNext();) {
			Record r = i.next();
			out.println(r);
		}

		tr.commit();
	}

}
