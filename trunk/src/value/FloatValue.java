package value;

import transaction.Transaction;

public class FloatValue extends Value {

	Float f = null;

	public FloatValue(float v) {
		f = v;
	}

	public FloatValue() {
	}

	@Override
	public int byteLength() {
		return 4;
	}

	@Override
	public int fromBytes(byte[] stream, int offset) {
		int res = 0;
		for (int i = 0; i < 4; ++i) {
			res = (res << 8) ^ ((int) stream[offset + i] & 0xff);
		}
		f = Float.intBitsToFloat(res);
		return 4;
	}

	@Override
	public byte[] toBytes(Transaction tr) {
		byte[] res = new byte[4];
		int v = Float.floatToIntBits(f);
		for (int i = 0; i < 4; ++i) {
			res[3 - i] = (byte) ((v >> (8 * i)) & 0xff);
		}
		return res;
	}

	@Override
	public int compareTo(Value o) {
		// null is the smallest
		if (o == null)
			return 1;
		
		if (o instanceof StrValue) {
			StrValue s = (StrValue)o;
			Float d = Float.valueOf(s.value);
			return f.compareTo(d);
		}

		if (o instanceof FloatValue) {
			FloatValue other = (FloatValue) o;
			return f.compareTo(other.f);
		}
		else if (o instanceof IntValue) {
			int i = ((IntValue) o).value;
			return f.compareTo((float) i);
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public Float get() {
		return f;
	}

	@Override
	public void set(Object newValue) {
		f = (Float) newValue;
	}

	@Override
	public String toString() {
		return f == null ? "NULL" : f.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FloatValue){
			return ((FloatValue)obj).f.equals(f);
		}
		else if (obj instanceof IntValue) {
			return ((IntValue)obj).value.equals(f);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		return f.hashCode();
	}
	
	@Override
	public boolean matchType(int type) {
		return type == Value.TYPE_FLOAT;
	}



}
