package value;

import transaction.Transaction;

public class BooleanValue extends Value {

	Boolean value = null;

	public BooleanValue() {

	}

	public BooleanValue(Boolean b) {
		value = b;
	}

	@Override
	public int byteLength() {
		return 1;
	}

	@Override
	public int compareTo(Value o) {
		// null is the smallest
		if (o == null)
			return 1;
		assert o != null && o instanceof BooleanValue;
		BooleanValue other = (BooleanValue) o;
		return value.compareTo(other.value);
	}

	@Override
	public Boolean get() {
		return value;
	}

	@Override
	public void set(Object newValue) {
		assert newValue instanceof Boolean;
		value = (Boolean) newValue;
	}

	@Override
	public int fromBytes(byte[] stream, int offset) {
		value = stream[offset] > 0;
		return 1;
	}

	@Override
	public byte[] toBytes(Transaction tr) {
		byte[] res = new byte[1];
		res[0] = value ? (byte) 1 : (byte) 0;
		return res;
	}

	@Override
	public String toString() {
		return value == null ? "NULL" : value.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return ((obj) instanceof BooleanValue)
				&& ((BooleanValue) obj).value.equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean matchType(int type) {
		return type == Value.TYPE_BOOLEAN;
	}

}
