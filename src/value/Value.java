package value;

import java.sql.Types;

import table.Column;
import table.Record;
import table.Storable;
import tlp.util.Debug;
import transaction.Transaction;

/**
 * Value represents the value of a cell in a tuple
 * 
 * @author TLP
 * 
 */
public abstract class Value implements Storable, Comparable<Value> {
	public final static int TYPE_INT = Types.INTEGER, TYPE_FLOAT = Types.FLOAT, TYPE_CHAR = Types.CHAR,
			TYPE_DATETIME = Types.DATE, TYPE_BOOLEAN = Types.BOOLEAN, TYPE_TIMESTAMP = Types.TIMESTAMP,
			TYPE_DECIMAL = Types.DECIMAL, TYPE_VARCHAR = Types.VARCHAR;

	// the column this Value belongs to
	protected Column column;

	// the record this Value belongs to
	protected Record record;

	public abstract Object get();

	public abstract void set(Object newValue);

	public static Value valueFromBytes(byte[] buffer, int offset, int type) {
		Value v = null;
		if (buffer[offset] == 0)
			return null;

		switch (type) {
		case TYPE_INT:
			v = new IntValue();
			break;
		case TYPE_FLOAT:
			v = new FloatValue();
			break;
		case TYPE_CHAR:
		case TYPE_VARCHAR:
			v = new StrValue();
			break;
		case TYPE_DATETIME:
		case TYPE_TIMESTAMP:
			v = new DateTimeValue();
			break;
		case TYPE_BOOLEAN:
			v = new BooleanValue();
			break;
		case TYPE_DECIMAL:
			v = new DecimalValue();
			break;
		default:
			Debug.fsLogger.error("unknown date types: {}", type);
			throw new UnsupportedOperationException();
		}

		v.fromBytes(buffer, offset+1);
		return v;
	}

	abstract public int compareTo(Value o);

	@Override
	abstract public int fromBytes(byte[] stream, int offset);

	@Override
	abstract public byte[] toBytes(Transaction tr);

	@Override
	abstract public int byteLength();

	// TODO: what about StrValue
	// @Override
	// public String toString() {
	// if (this instanceof StrValue) {
	// return ((StrValue)this).value;
	// }
	// else return new String(toBytes(null));
	// }

	public static Value minValue(int type) {
		switch (type) {
		case TYPE_INT:
		case TYPE_FLOAT:
		case TYPE_CHAR:
		case TYPE_VARCHAR:
		case TYPE_DATETIME:
		case TYPE_TIMESTAMP:
		case TYPE_BOOLEAN:
		case TYPE_DECIMAL:
			// null is the smallest value
			return null;
		default:
			Debug.fsLogger.error("unknown date types: {}", type);
			throw new UnsupportedOperationException();
		}
	}

	public static String typeString(int type) {
		switch (type) {
		case TYPE_INT:
			return "INT";
		case TYPE_FLOAT:
			return "FLOAT";
		case TYPE_CHAR:
			return "CHAR";
		case TYPE_VARCHAR:
			return "VARCHAR";
		case TYPE_DATETIME:
			return "DATETIME";
		case TYPE_TIMESTAMP:
			return "TIMESTAMP";
		case TYPE_BOOLEAN:
			return "BOOLEAN";
		case TYPE_DECIMAL:
			return "DECIMAL"; // hard to define
		default:
			Debug.fsLogger.error("unknown date types: {}", type);
			throw new UnsupportedOperationException();
		}
	}

	public static byte[] valueToBytes(Transaction tr, Value value) {
		assert !(value instanceof BytesValue);
		
		if (value == null) {
			return new byte[] { 0 };
		} else {
			byte[] t = value.toBytes(tr);
			byte[] res = new byte[t.length + 1];
			res[0] = 1;
			System.arraycopy(t, 0, res, 1, t.length);
			return res;
		}
	}

	public abstract boolean equals(Object obj);

	public abstract int hashCode();

	public abstract boolean matchType(int type);
	

}
