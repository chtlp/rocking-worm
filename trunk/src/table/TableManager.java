package table;

import filesystem.BufferManager;
import filesystem.FileStorage;
import filesystem.Page;
import index.BPlusIndex;
import index.Index;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import tlp.util.Debug;
import transaction.DeadlockException;
import transaction.Transaction;

public class TableManager {
	static int rootPageID;

	// static HashSet<Table> tables = new HashSet<Table>();

	public static synchronized boolean createDatabase(Transaction tr,
			String dbName) {
		// TODO no type checking
		tr.setDBName(dbName);
		return true;
		// throw new UnsupportedOperationException();
	}

	public static synchronized Index createIndex(Transaction tr, Table table,
			String name, int columnID, boolean isPrimary) {
		Debug.breakOn(table.getName().equals("Battles"));
		Page p = BufferManager.allocatePage(tr);
		Debug.indexLogger.debug("create index on page {}", p.getPageID());
		Index i = BPlusIndex.createBPlusIndex(tr, p.getPageID(), name, table,
				columnID, isPrimary);
		table.addIndex(i);
		table.saveMetaData(tr);
		p.release(tr);

		// insert the data already in the table
		if (!isPrimary) {
			TableIterator scan = table.getScanIndex(tr);
			for (scan.open(); scan.hasNext();) {
				Record r = scan.next();
				Debug.testNullLogger.debug("word-index: insert {}", r);
				i.add(tr, r.getValue(columnID), r.rowID);
			}
		}
		return i;

	}

	public static synchronized void createTable(Transaction tr, Table table,
			boolean isPermanent) {
		if (!isPermanent) {
			throw new UnsupportedOperationException();
		}
		assert getTable(tr, table.getName()) == null;
		Page q = BufferManager.allocatePage(tr);
		table.setRootPageID(q.getPageID());
		table.dbName = tr.getDBName();
		table.saveMetaData(tr);

		int col = -1;
		for (int i = 0; i < table.numColumns(); ++i)
			if (table.getColumn(i).isPrimaryKey())
				col = i;
		createIndex(tr, table, table.getName() + ".ScanIndex", col, true);

		table.scanIndex = (BPlusIndex) table.getIndex(0);

		Page p = BufferManager.getPage(tr, rootPageID);

		int num = p.readInt(Page.HEADER_LENGTH);
		p.seek(Page.HEADER_LENGTH + Page.INT_SIZE + num * Page.INT_SIZE);
		p.writeInt(tr, q.getPageID());
		p.seek(Page.HEADER_LENGTH);
		p.writeInt(tr, num + 1);

		q.release(tr);
		p.release(tr);
	}

	public static synchronized void saveTableIndexInfo(Transaction tr,
			List<Integer> tables) {
		Page p = BufferManager.getPage(tr, rootPageID);
		p.seek(Page.HEADER_LENGTH);
		p.writeInt(tr, tables.size());
		for (Integer r : tables) {
			p.writeInt(tr, r);
		}
		p.release(tr);
	}

	public static synchronized boolean dropDatabase(Transaction tr,
			String dbName) {
		ArrayList<Integer> left = new ArrayList<Integer>();

		Page p = BufferManager.getPage(tr, rootPageID);
		p.seek(Page.HEADER_LENGTH);
		int num = p.readInt();
		for (int i = 0; i < num; ++i) {
			int r = p.readInt();
			Table t = loadTable(tr, r);
			if (t.getDBName().equals(dbName)) {
				t.drop(tr);
			} else
				left.add(r);
		}

		saveTableIndexInfo(tr, left);

		p.release(tr);

		return true;
	}

	public static synchronized boolean dropIndex(Transaction tr,
			String tableName, String indexName) {
		Table table = getTable(tr, tableName);
		boolean t = table.dropIndex(tr, indexName);
		assert t;
		return true;
	}

	public static synchronized boolean dropTable(Transaction tr,
			String tableName) {
		Table table = getTable(tr, tableName);
		table.drop(tr);
		ArrayList<Integer> tables = new ArrayList<Integer>();
		Page p = BufferManager.getPage(tr, rootPageID);
		p.seek(Page.HEADER_LENGTH);
		int num = p.readInt();
		for (int i = 0; i < num; ++i) {
			int r = p.readInt();
			if (r != table.getRootPageID())
				tables.add(r);
		}
		saveTableIndexInfo(tr, tables);
		p.release(tr);
		return true;
	}

	public static synchronized Table getTable(Transaction tr, String name) {
		Page p = BufferManager.getPage(tr, rootPageID);
		p.seek(Page.HEADER_LENGTH);
		int num = p.readInt();
		for (int i = 0; i < num; ++i) {
			int r = p.readInt();
			Table t = loadTable(tr, r);
			if (t.name.equals(name) && t.getDBName().equals(tr.getDBName())) {
				return t;
			}
		}
		return null;
	}

	public static void init(int root) {
		rootPageID = root;
		Transaction tr = FileStorage.newSystemTransaction();
		Page p = BufferManager.getPage(tr, rootPageID);

		if (p.isEmpty()) {
			p.setType(tr, Page.TYPE_TABLE_INDEX);
			p.writeInt(tr, Page.HEADER_LENGTH, 0);
			p.setNextPage(tr, -1);
		}

		p.release(tr);
		tr.commit();
	}

	public static synchronized boolean useDatabase(Transaction tr, String dbName) {
		// TODO no type checking here
		tr.setDBName(dbName);
		return true;
		// throw new UnsupportedOperationException();
	}

	private static Table loadTable(Transaction tr, int root) {
		Table t = new Table(root);
		t.loadMetaData(tr);
		return t;
	}

	public static void sharedLock(Transaction tr, String tblName)
			throws DeadlockException, TimeoutException {
		LockManager.getLock(tr.getDBName(), tblName).lock(tr, false);
	}

	public static void exclusiveLock(Transaction tr, String tblName)
			throws DeadlockException, TimeoutException {
		LockManager.getLock(tr.getDBName(), tblName).lock(tr, true);
	}

	public static Table getTableShared(Transaction tr, String tblName)
			throws DeadlockException, TimeoutException {
		sharedLock(tr, tblName);
		return getTable(tr, tblName);
	}

	public static Table getTableExclusive(Transaction tr, String tblName)
			throws DeadlockException, TimeoutException {
		exclusiveLock(tr, tblName);
		return getTable(tr, tblName);
	}

	public static void printAllTables() {
		Transaction tr = Transaction.begin();
		Page p = BufferManager.getPage(tr, rootPageID);
		p.seek(Page.HEADER_LENGTH);
		int num = p.readInt();
		for (int i = 0; i < num; ++i) {
			int r = p.readInt();
			Table t = loadTable(tr, r);
			t.printTable(System.out);
		}
		tr.commit();
	}

}
