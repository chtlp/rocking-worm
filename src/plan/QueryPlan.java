package plan;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import table.Column;
import table.Record;
import transaction.DeadlockException;

public abstract class QueryPlan extends Plan{
	
	ArrayList<Column> columns = new ArrayList<Column>();
	ArrayList<Alia> alias = new ArrayList<Alia>();
	
	public void addPrefix(String tableName) {
		for (int i = 0; i < alias.size(); i++) {
			alias.get(i).addTableName(tableName);
		}
	}
	
	public abstract void open() throws DeadlockException, TimeoutException;
	
	public abstract void close() throws DeadlockException, TimeoutException;
	
	public abstract table.Record next() throws DeadlockException, TimeoutException;
	
	public ArrayList<Column> getColumns() {
		return columns;
	}
	
	public ArrayList<Alia> getAlias() {
		return alias;
	}
}
