package tlp.test;

import org.junit.Test;

import value.FloatValue;
import value.IntValue;
import value.StrValue;

public class TestCompare {

	@Test
	public void testNull() {
		new Boolean(false).compareTo(false);
	}

	@Test
	public void testCompare() {
		IntValue i = new IntValue(2);
		FloatValue f = new FloatValue((float) 2.1);
		StrValue s = new StrValue("2.2");
		


		
		System.out.println(i.compareTo(f));
		System.out.println(f.compareTo(i));
		System.out.println(i.compareTo(i));
		System.out.println(f.compareTo(f));

		System.out.println(i.compareTo(s));
		System.out.println(s.compareTo(i));
		
		System.out.println(f.compareTo(s));
		System.out.println(s.compareTo(f));

	}
}
