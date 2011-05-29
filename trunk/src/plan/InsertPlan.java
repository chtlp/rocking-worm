package plan;

import java.util.concurrent.TimeoutException;

import table.Record;
import table.Table;
import transaction.DeadlockException;
import transaction.Transaction;

public class InsertPlan extends UpdatePlan {

	table.Table table;
	table.Record record;
	QueryPlan queryPlan;
	
	public InsertPlan(Table table, Record record, QueryPlan queryPlan, Transaction tr) {
		super();
		this.table = table;
		this.record = record;
		this.queryPlan = queryPlan;
		this.tr = tr;
	}
	
	@Override
	public boolean run() throws DeadlockException, TimeoutException {
		if (record != null) 
			table.insert(tr, record);
		else if (queryPlan != null){
			queryPlan.open();
			table.Record record = queryPlan.next();
			while (record != null) {
				table.insert(tr, record);
				record = queryPlan.next();
			}
		}
		return true;
	}

}
