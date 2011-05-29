package tlp.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

import value.DateTimeValue;
import value.IntValue;


public class TestDateTimeValue {

	
	@Test
	public void TestBytesFunc() {
		DateTimeValue d = new DateTimeValue();
		d.set(new Date(12334458));
		
		byte[] buffer = d.toBytes(null);
		
		DateTimeValue d2 = new DateTimeValue();
		
		d2.fromBytes(buffer, 0);
		
		System.out.printf("%d %d\n", d.get().getTime(), d2.get().getTime());
		assertEquals(d2.compareTo(d), 0);
	}
	
	@Test(expected= IndexOutOfBoundsException.class) public void empty() { 
	    new ArrayList<Object>().get(0); 
	}
	
	@Test
	public void testIntValue() {
		new IntValue(3).toBytes(null);
	}
	
	@Test
	public void testByte() {
		byte b = (byte)200;
		System.out.println(b);
	}
}
