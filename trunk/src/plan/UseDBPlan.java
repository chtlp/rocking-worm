package plan;

import table.TableManager;
import transaction.Transaction;

public class UseDBPlan extends UpdatePlan {

	public UseDBPlan(String dbName, Transaction tr) {
		this.tr = tr;
		TableManager.useDatabase(tr, dbName);
	}

	@Override
	public boolean run() {
		// TODO Auto-generated method stub
		return false;
	}

}
