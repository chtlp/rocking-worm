package table;

import java.util.ArrayList;
import java.util.List;

import transaction.Transaction;
import value.Value;

public class Record {
	int rowID = -1;
	
	Table table;
	
	
	ArrayList<Value> values = new ArrayList<Value>();
	
	public Record() {
		
	}
	
	public Record(Record r) {
		rowID = r.rowID;
		table = r.table;
		values.addAll(r.values);
	}
	
	public Record(List<Value> values) {
		this.values.addAll(values);
	}
	
	public Record(Record record1, Record record2) {
		values.addAll(record1.values);
		values.addAll(record2.values); 
	}
	
	// simply modify this object, no effect on the storage
	public void putValue(int ind, Value v) {
		values.set(ind, v);
	}
	
	public void addValue(Value v) {
		values.add(v);
	}
	
	public void setValue(int column, Value v) {
		values.set(column, v);
	}

	// this will has effect on the file system
	public void update(Transaction tr, int ind, Value v) {
		
	}
	
	public Value getValue(int ind) {
		return values.get(ind);
	}
	
	public int size() {
		return values.size();
	}

	public int getRowID() {
		return rowID;
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table t) {
		table = t;
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(String.format("Record: rid = %d ", rowID));
		b.append(values.get(0));
		for(int i=1; i<values.size(); ++i) {
			b.append(String.format(" %s", values.get(i)));
		}
		return b.toString();
	}
	
	public String shortString() {
		StringBuilder b = new StringBuilder();
//		b.append(String.format("Record: rid = %d", rowID));
		b.append(values.get(0));
		for(int i=1; i<values.size(); ++i) {
			b.append(String.format(" %s", values.get(i)));
		}
		return b.toString();
	}

	
	public int distinctCode() {
		StringBuilder b = new StringBuilder();
//		b.append(String.format("Record: rid = %d", rowID));
		b.append(values.get(0));
		for(int i=1; i<values.size(); ++i) {
			b.append(String.format(" %s", values.get(i)));
		}
		return b.toString().hashCode();
	}
}
