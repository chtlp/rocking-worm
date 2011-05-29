package plan;

import java.util.ArrayList;

import table.Column;
import table.Record;

public class Tail {
	public table.Record record;
	ArrayList<Column> columns = new ArrayList<Column>();
	ArrayList<Alia> alias = new ArrayList<Alia>();
	public Tail(Record record, ArrayList<Alia> alias, ArrayList<Column> columns) {
		super();
		this.record = record;
		this.columns.addAll(columns);
		this.alias.addAll(alias);
	}
}
