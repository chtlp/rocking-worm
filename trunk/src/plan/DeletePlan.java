package plan;

import java.util.concurrent.TimeoutException;

import table.Table;
import transaction.DeadlockException;
import transaction.Transaction;

public class DeletePlan extends UpdatePlan {

	table.Table table;
	QueryPlan queryPlan;
	
	public DeletePlan(QueryPlan queryPlan, Table table, Cond cond, Transaction tr) {
		super();
		this.queryPlan = queryPlan;
		this.table = table;
		this.cond = cond;
		this.tr = tr;
	}

	Cond cond;
	
	@Override
	public boolean run() throws DeadlockException, TimeoutException {
		table.Record record = null;
		do {
			record = queryPlan.next();
			if (cond.check(queryPlan.alias, queryPlan.columns, record, null)) {
				table.remove(tr, record);
			}
			
		}
		while (record == null);
		return true;
	}

}
