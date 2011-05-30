package value;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import transaction.Transaction;

import filesystem.Page;

public class DateTimeValue extends Value {

	Date value = null;
	
	public DateTimeValue(Date v) {
		value = v;
	}
	
	public DateTimeValue() {
	}

	@Override
	public int byteLength() {
		return Page.INT_SIZE * 2;
	}

	@Override
	public int fromBytes(byte[] stream, int offset) {
		long res = 0;
		for(int i=0; i<8; ++i) {
			res = (res << 8) ^ ((long) stream[offset+i] & 0xff);
		}
		value = new Date(res);
		return 8;
	}

	@Override
	public byte[] toBytes(Transaction tr) {
		byte[] res = new byte[8];
		long v = value.getTime();
		for(int i=0; i<8; ++i) {
			res[7-i] = (byte) ((v >> (8 * i)) & 0xff);
		}
		return res;
	}
	
	static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public int compareTo(Value o) {
		// null is the smallest
		if (o == null) return 1;
		
		if (o instanceof StrValue) {
			StrValue s = (StrValue)o;
			int r = 0;
			try {
				r = value.compareTo(formatter.parse(s.value));
			}
			catch (ParseException e) {
				System.err.format("fail to compare %s and %s \n", value, s);
			}
			return r;
		}
		DateTimeValue other = (DateTimeValue) o;
		return value.compareTo(other.value);
	}

	@Override
	public Date get() {
		return value;
	}

	@Override
	public void set(Object newValue) {
		value = (Date)newValue;
	}

	@Override
	public String toString() {
		return value == null ? "NULL" : value.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DateTimeValue && ((DateTimeValue)obj).value.equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
	
	@Override
	public boolean matchType(int type) {
		return type == Value.TYPE_DATETIME || type == Value.TYPE_TIMESTAMP;
	}


}
