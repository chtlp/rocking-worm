package plan;

import table.TableManager;
import transaction.Transaction;

public class CreateDBPlan extends UpdatePlan {

	String dbName;
	
	public CreateDBPlan(String dbName, Transaction tr) {
		this.dbName = dbName;
		this.tr = tr;
	}

	@Override
	public boolean run() {
		return TableManager.createDatabase(tr, dbName);
	}

}
