package table;

import transaction.Transaction;

public interface Storable {
	public int fromBytes(byte[] stream, int offset);
	
	public byte[] toBytes(Transaction tr);
	
	
	public int byteLength();
}
