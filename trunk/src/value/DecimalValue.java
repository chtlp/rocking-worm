package value;

import java.math.BigDecimal;
import java.util.Arrays;

import transaction.Transaction;

public class DecimalValue extends Value {

	String numStr = null;

	public DecimalValue(String string) {
		numStr = string;
	}

	public DecimalValue() {
	}

	@Override
	public int byteLength() {
		return numStr.length() + 1;
	}

	@Override
	public int fromBytes(byte[] stream, int offset) {
		int len = stream[offset];
		numStr = new String(Arrays.copyOfRange(stream, offset + 1, offset + 1
				+ len));
		return len;
	}

	@Override
	public byte[] toBytes(Transaction tr) {
		byte[] buffer = new byte[byteLength()];
		buffer[0] = (byte) numStr.length();
		System.arraycopy(numStr.getBytes(), 0, buffer, 1, numStr.length());
		return null;
	}

	@Override
	public int compareTo(Value o) {
		// null is the smallest
		if (o == null) return 1;
		
		if (o instanceof StrValue) {
			StrValue s = (StrValue)o;
			return new BigDecimal(numStr).compareTo(new BigDecimal(s.value));
		}
		DecimalValue other = (DecimalValue) o;
		return new BigDecimal(numStr).compareTo(new BigDecimal(other.numStr));
	}

	@Override
	public String get() {
		return numStr;
	}

	@Override
	public void set(Object newValue) {
		numStr = (String) newValue;
	}

	@Override
	public String toString() {
		return numStr == null ? "NULL" : numStr;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof DecimalValue && ((DecimalValue)obj).numStr.equals(numStr);
	}

	@Override
	public int hashCode() {
		return numStr.hashCode();
	}
	
	@Override
	public boolean matchType(int type) {
		return type == Value.TYPE_DECIMAL;
	}



}
