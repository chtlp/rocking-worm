package transaction;

import java.util.HashMap;

import table.Table;

public class LockManager {
	public static final int DEADLOCK_CHECK = 100;
	private static LockManager lockManager;

	private static HashMap<Table, TableLock> t2f;

	public static LockManager getInstance() {
		if (lockManager == null)
			lockManager = new LockManager();
		return lockManager;
	}

	public static TableLock getFakeTable(Table table) {
		TableLock result = t2f.get(table);
		if (result == null) {
			result = new TableLock();
			t2f.put(table, result);
		}
		return result;
	}
}
