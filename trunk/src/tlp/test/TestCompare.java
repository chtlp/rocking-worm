package tlp.test;

import org.junit.Test;

import value.FloatValue;
import value.IntValue;

public class TestCompare {

	@Test
	public void testNull() {
		new Boolean(false).compareTo(false);
	}

	@Test
	public void testCompare() {
		IntValue i = new IntValue(2);
		FloatValue f = new FloatValue((float) 2.1);


		System.out.println(i.compareTo(f));
		System.out.println(f.compareTo(i));
		System.out.println(i.compareTo(i));
		System.out.println(f.compareTo(f));

	}
}
