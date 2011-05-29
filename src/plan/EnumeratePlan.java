package plan;

import table.Record;
import table.Table;
import transaction.Transaction;

public class EnumeratePlan extends QueryPlan {

	table.TableIterator iter;
	Table table;
	
	public EnumeratePlan(Table table, Transaction tr) {
		this.tr = tr;
		for (int i = 0; i < table.numColumns(); i++) {
			table.Column col = table.getColumn(i);
			Alia alia = new Alia();
			alia.addColName(col.getName());
			alia.addTableName(table.getName());
			columns.add(col);
			alias.add(alia);
		}
		this.table = table;
		iter = table.getScanIndex(tr);
	}

	@Override
	public void open() {
		//System.out.println(table.getName() + "  open");
		iter.open();
	}

	@Override
	public void close() {
		iter.close();
	}

	@Override
	public Record next() {
		if (iter.hasNext()) {
			Record r = iter.next();
			// System.out.println("E " + r);
			return r;
		}
		else 
			return null;
	}

}
