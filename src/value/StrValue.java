package value;

import java.util.Arrays;

import transaction.Transaction;
import filesystem.GlobalStringPool;

public class StrValue extends Value {

	String value = null;

	// rid < 0: loaded, rid >= 0: not loaded
	Integer rid = null;

	public StrValue() {

	}

	public StrValue(String v) {
		set(v);
	}

	// the byte length maybe shorter than the string length because we don't
	// store the whole string in the page if it is too long, instead we store it
	// in the String Pool
	@Override
	public int byteLength() {
		return Math.min(GlobalStringPool.LENGTH_THRESHOLD, value.length() + 1);
	}

	@Override
	public String get() {
		if (rid != null && rid >= 0) {
			value = GlobalStringPool.get(rid);
			rid = -rid - 1;
		}
		return value;
	}

	@Override
	public void set(Object newValue) {
		value = (String) newValue;
		rid = null;
	}

	@Override
	public int fromBytes(byte[] stream, int offset) {
		byte len = stream[offset];
		if (len >= 0) {
			value = new String(Arrays.copyOfRange(stream, offset + 1, offset
					+ 1 + len));
		} else {
			IntValue rid = new IntValue();
			rid.fromBytes(stream, offset + 1);
			this.rid = rid.get();

			value = new String(Arrays.copyOfRange(stream, offset + 5, offset
					+ 1 + Math.abs(len)));

		}
		return byteLength();
	}

	public void remove(Transaction tr) {
		assert rid != null;
		int i = rid >= 0 ? rid : -rid - 1;
		GlobalStringPool.remove(tr, i);
	}

	@Override
	public byte[] toBytes(Transaction tr) {
		byte[] res;
		if (value.length() + 1 > GlobalStringPool.LENGTH_THRESHOLD) {
			res = new byte[GlobalStringPool.LENGTH_THRESHOLD];

			if (rid == null) {
				rid = -GlobalStringPool.put(tr, value) - 1;
			}

			IntValue r = new IntValue();
			r.set(rid >= 0 ? rid : -rid - 1);

			int intSize = r.byteLength();
			System.arraycopy(r.toBytes(tr), 0, res, 1, intSize);

			System.arraycopy(value.getBytes(), 0, res, 1 + intSize, res.length
					- 1 - intSize);
			res[0] = (byte) (-(res.length - 1 - intSize));

		} else {
			res = new byte[value.length() + 1];
			res[0] = (byte) value.length();
			System.arraycopy(value.getBytes(), 0, res, 1, value.length());
		}

		return res;

	}

	@Override
	public int compareTo(Value o) {
		// null is the smallest
		if (o == null) return 1;

		if (!(o instanceof StrValue)) {
			return -o.compareTo(this);
		}
		
		StrValue other = (StrValue) o;

		int len = Math.min(value.length(), other.value.length());

		for (int i = 0; i < len; ++i) {
			if (value.charAt(i) < other.value.charAt(i))
				return -1;
			else if (value.charAt(i) > other.value.charAt(i))
				return 1;
		}

		if (rid == null && other.rid == null) // no more to compare
			return value.length() - other.value.length();
		else
			return get().compareTo(other.get());
	}

	@Override
	public String toString() {
		return value == null ? "NULL" : value.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof StrValue && ((StrValue)obj).value.equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean matchType(int type) {
		return type == Value.TYPE_CHAR || type == Value.TYPE_VARCHAR;
	}



}
