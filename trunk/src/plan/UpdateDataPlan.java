package plan;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import parser.Update;
import table.Table;
import transaction.DeadlockException;
import transaction.Transaction;

public class UpdateDataPlan extends UpdatePlan {

	ArrayList<table.Record> records = new ArrayList<table.Record>();
	table.Table table;
	parser.Update updatePlan;
	Cond cond;
	table.TableIterator iter;

	public UpdateDataPlan(Table table, Update updatePlan, Transaction tr) throws DeadlockException, TimeoutException {
		super();
		this.table = table;
		this.updatePlan = updatePlan;
		cond = new Cond(updatePlan.boolExpr, tr);
		this.tr = tr;		
	}

	@Override
	public boolean run() throws DeadlockException, TimeoutException {
		records.clear();
		EnumeratePlan tempPlan = new EnumeratePlan(table, tr);
		iter = table.getScanIndex(tr);
		iter.open();
		while (iter.hasNext()) {
			table.Record record = iter.next();
			if (cond.check(tempPlan.alias, tempPlan.columns, record, null)) {
				records.add(record);
				//System.out.println("CHANGE " + record);
			}
		}
		iter.close();
		
		for (int i = 0; i < records.size(); i++) {
			table.Record record = records.get(i);
			parser.UpdateList ul = updatePlan.updateList;
			int idx = new Col2Idx().getIdx(tempPlan.alias, ul.head);
			value.Value value = new ValueInterpreter().transValue(ul.value,
					tempPlan.alias, tempPlan.columns, record, null, tr);
			value = CorrectType.trans(value, tempPlan.columns.get(idx).getType());
			System.out.println(tempPlan.columns.get(idx).getType());
			table.Record newRecord = new table.Record(record);

			newRecord.setValue(idx, value);
			table.update(tr, record, newRecord);
			ul = ul.tail;
		}
		return true;
	}

}
