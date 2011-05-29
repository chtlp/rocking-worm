package plan;

import table.TableManager;
import transaction.Transaction;

public class DropDBPlan extends UpdatePlan {
	
	String dbName;
	
	public DropDBPlan(String dbName, Transaction tr) {
		super();
		this.dbName = dbName;
		this.tr = tr;
	}


	@Override
	public boolean run() {
		return TableManager.dropDatabase(tr, dbName);
	}

}
