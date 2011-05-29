package plan;

import table.Table;
import table.TableManager;
import transaction.Transaction;

public class CreateTablePlan extends UpdatePlan {
	
	table.Table table;
	

	public CreateTablePlan(Table table, Transaction tr) {
		this.table = table;
		this.tr = tr;
	}


	@Override
	public boolean run() {
		TableManager.createTable(tr, table, true);
		return true;
	}

}
