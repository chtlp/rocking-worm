package plan;

import table.Table;
import table.TableManager;
import transaction.Transaction;

public class CreateIndexPlan extends UpdatePlan {

	Table table;
	int columnID;
	String indexName;

	public CreateIndexPlan(Table table, int columnID, String indexName,
			Transaction tr) {
		this.table = table;
		this.columnID = columnID;
		this.indexName = indexName;
		this.tr = tr;
	}

	@Override
	public boolean run() {
		//if (table.getName().equals("fconnection"))
			//System.out.println(table.getColumn(columnID).getName() + " " + table
				//.getColumn(columnID).isPrimaryKey());
		TableManager.createIndex(tr, table, indexName, columnID, table
				.getColumn(columnID).isPrimaryKey());
		return true;
	}

}
