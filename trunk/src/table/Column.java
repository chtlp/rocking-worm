package table;

import java.util.Arrays;

import logging.Page;
import tlp.util.Debug;
import transaction.Transaction;
import util.ByteArrayList;
import value.BooleanValue;
import value.IntValue;
import value.Value;
import filesystem.GlobalStringPool;

public class Column implements Storable {

	public int getType() {
		return type;
	}

	public int getPara1() {
		return para1;
	}

	public int getPara2() {
		return para2;
	}

	public Table getTable() {
		return table;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getColumnID() {
		return columnID;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}

	public int rawValueSize() {
		switch (type) {
		case Value.TYPE_BOOLEAN:
			return 1;
		case Value.TYPE_CHAR:
		case Value.TYPE_VARCHAR:
			return Math.min(para1 + 1, GlobalStringPool.LENGTH_THRESHOLD);
		case Value.TYPE_DATETIME:
		case Value.TYPE_TIMESTAMP:
			return Page.INT_SIZE * 2;
		case Value.TYPE_FLOAT:
		case Value.TYPE_INT:
			return Page.INT_SIZE;
		case Value.TYPE_DECIMAL:
			return para1 + 5;
		default:
			Debug.fsLogger.error("Unknown data types {}", type);
			throw new UnsupportedOperationException();
		}
	}

	public int valueSize() {
		// 1 byte for null
		return rawValueSize() + 1;
	}

	@Override
	public int byteLength() {
		int len = Page.INT_SIZE * 4 + 1 * 2 + Page.INT_SIZE + name.length();
		len += 1;
		if (defaultValue != null)
			len += defaultValue.byteLength();
		return len;
	}

	@Override
	public int fromBytes(byte[] stream, int offset) {
		IntValue i = new IntValue();

		i.fromBytes(stream, offset);
		type = i.get();
		i.fromBytes(stream, offset + Page.INT_SIZE);
		para1 = i.get();
		i.fromBytes(stream, offset + Page.INT_SIZE * 2);
		para2 = i.get();
		i.fromBytes(stream, offset + Page.INT_SIZE * 3);
		columnID = i.get();
		BooleanValue b = new BooleanValue();
		b.fromBytes(stream, offset + Page.INT_SIZE * 4);
		isNullable = b.get();
		b.fromBytes(stream, offset + Page.INT_SIZE * 4 + 1);
		isPrimaryKey = b.get();
		int pos = offset + Page.INT_SIZE * 4 + 2;
		i.fromBytes(stream, pos);
		int len = i.get();

		pos += Page.INT_SIZE;
		name = new String(Arrays.copyOfRange(stream, pos, pos + len));

		pos += len;

		defaultValue = Value.valueFromBytes(stream, pos, type);

		return 0;
	}

	@Override
	public byte[] toBytes(Transaction tr) {
		ByteArrayList buffer = new ByteArrayList(1000);
		buffer.addAll(new IntValue(type).toBytes(tr));
		buffer.addAll(new IntValue(para1).toBytes(tr));
		buffer.addAll(new IntValue(para2).toBytes(tr));
		buffer.addAll(new IntValue(columnID).toBytes(tr));
		buffer.addAll(new BooleanValue(isNullable).toBytes(tr));
		buffer.addAll(new BooleanValue(isPrimaryKey).toBytes(tr));

		buffer.addAll(new IntValue(name.length()).toBytes(tr));
		buffer.addAll(name.getBytes());

		if (defaultValue != null)
			assert defaultValue.matchType(type);
		buffer.addAll(Value.valueToBytes(tr, defaultValue));
		return buffer.toArray();
	}

	private int type;
	// para1: length of a CHAR, precision of a decimal; para2: scale of a
	// decimal
	private int para1, para2;
	private Table table;
	private String name;

	private int columnID;
	private boolean isNullable;
	private boolean isPrimaryKey;

	private Value defaultValue;

	public Column(String name) {
		this.name = name;
	}

	public Column(Column column) {
		this.type = column.type;
		this.para1 = column.para1;
		this.para1 = column.para2;
		this.table = column.table;
		this.name = column.name;
		this.columnID = column.columnID;
		this.isNullable = column.isNullable;
		this.isPrimaryKey = column.isPrimaryKey;
	}

	public Column(Column column, String name) {
		this(column);
		this.name = name;
	}

	public Column(String n, int ty, int p1, int p2, boolean nullable,
			Value defaultValue) {
		name = n;
		type = ty;
		para1 = p1;
		para2 = p2;
		isNullable = nullable;
		this.defaultValue = defaultValue;
	}

	public Column() {
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public void setColumnId(int columnId) {
		this.columnID = columnId;
	}

	public void setPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
	}

	public String toString() {
		return String.format("(name=%s, type=%s)", getName(),
				Value.typeString(getType()));
	}

	public Value getDefaultValue() {
		return defaultValue;
	}

}
