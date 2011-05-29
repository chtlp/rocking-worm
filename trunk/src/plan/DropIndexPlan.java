package plan;

import table.Table;
import table.TableManager;
import transaction.Transaction;

public class DropIndexPlan extends UpdatePlan {
	
	Table table;
	String indexName;

	public DropIndexPlan(Table table, String indexName, Transaction tr) {
		this.table = table;
		this.indexName = indexName;
	}

	@Override
	public boolean run() {
		TableManager.dropIndex(tr, table.getName(), indexName);
		return true;
	}

}
