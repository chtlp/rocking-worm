package value;

import transaction.Transaction;

/**
 * will not be stored in String Pool, representing a list of attributes
 * 
 * @author TLP
 * 
 */
public class BytesValue extends Value {

	byte[] buffer = null;

	public BytesValue(byte[] array) {
		buffer = array;
	}

	@Override
	public int fromBytes(byte[] stream, int offset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] toBytes(Transaction tr) {
		return buffer;
	}

	@Override
	public int byteLength() {
		return buffer.length;
	}

	@Override
	public int compareTo(Value o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] get() {
		return buffer;
	}

	@Override
	public void set(Object newValue) {
		buffer = (byte[]) newValue;
	}

	@Override
	public boolean equals(Object obj) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean matchType(int type) {
		throw new UnsupportedOperationException();
	}



}
