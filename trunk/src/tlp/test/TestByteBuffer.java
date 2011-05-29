package tlp.test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import org.junit.Test;

import table.Record;
import table.Table;
import table.TableIterator;
import transaction.Transaction;
import value.Value;

public class TestByteBuffer {

	@Test
	public void testByteBuffer() {
		Transaction tr = Transaction.begin();
		ByteBuffer buffer = ByteBuffer.allocate(100);

		buffer.put("Hello World".getBytes());
		buffer.put(" smile".getBytes());

		byte[] d = new byte[buffer.position()];

		buffer.position(0);
		buffer.get(d);
		System.out.println(buffer);

		System.out.println(Arrays.toString(d));
		System.out.println(new String(d));
		
		Table t = null;
		TableIterator iter = t.getScanIndex(tr);
		for(iter.open();iter.hasNext();) {
			Record r = iter.next();
			Value v = r.getValue(0);
		}
		tr.commit();
 	}
	
	@Test
	public void testAssertion() {
		System.out.printf("a = %d, A = %d\n", (byte)'a', (byte)'A');
		System.out.println(new String("a").compareTo(null));
	}
	
	@Test
	public void testShift() {
		byte b = -1;
		for(int i=0; i<8; ++i) {
			assert (b & 1) == 1;
			b = (byte) (b >> 1);
		}
	}
	
	@Test
	public void testBitSet() {
		BitSet set = new BitSet(1);
		assert set.nextClearBit(0) >=0;
		set.set(0);
		assert set.nextClearBit(0) < 0 : String.format("current set size: %d", set.size());
	}
	
	@Test
	public void testArrayList() {
		new ArrayList<Integer>().add(null);
	}
	
	@Test
	public void testHashCode() {
		System.out.println(new Boolean(true).hashCode());
		System.out.println(new Boolean(false).hashCode());
		System.out.println(new Integer(2).hashCode());
	}
	
}
