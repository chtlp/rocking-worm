package table;

import java.util.HashMap;

import transaction.TableLock;

public class LockManager {
	static HashMap<String, TableLock> locks = new HashMap<String, TableLock>();
	
	public static TableLock getLock(String dbName, String tblName) {
		String id = dbName + "$.$" + tblName;
		TableLock l = locks.get(id);
		if (l == null) l = new TableLock();
		locks.put(id, l);
		return l;
	}
}
