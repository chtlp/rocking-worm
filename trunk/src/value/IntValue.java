package value;

import transaction.Transaction;
import filesystem.Page;

public class IntValue extends Value {

	Integer value;

	public IntValue() {
		value = null;
	}

	public IntValue(int v) {
		value = v;
	}

	@Override
	public int byteLength() {
		return Page.INT_SIZE;
	}

	@Override
	public int fromBytes(byte[] stream, int offset) {
		int res = 0;
		for (int i = 0; i < 4; ++i) {
			res = (res << 8) ^ ((int) stream[offset + i] & 0xff);
		}
		value = res;
		return 4;
	}

	@Override
	public byte[] toBytes(Transaction tr) {
		byte[] res = new byte[4];
		for (int i = 0; i < 4; ++i) {
			res[3 - i] = (byte) ((value >> (8 * i)) & 0xff);
		}
		return res;
	}

	@Override
	public Integer get() {
		return value;
	}

	@Override
	public void set(Object newValue) {
		value = (Integer) newValue;
	}

	@Override
	public int compareTo(Value o) {
		// null is the smallest
		if (o == null)
			return 1;

		if (o instanceof IntValue) {
			IntValue other = (IntValue) o;
			return value.compareTo(other.value);
		}
		else if (o instanceof FloatValue) {
			Float f = ((FloatValue) o).f;
			return -f.compareTo((float) value);
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return value == null ? "NULL" : value.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FloatValue){
			return ((FloatValue)obj).f.equals(value);
		}
		else if (obj instanceof IntValue) {
			return ((IntValue)obj).value.equals(value);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean matchType(int type) {
		return type == Value.TYPE_INT;
	}



}
