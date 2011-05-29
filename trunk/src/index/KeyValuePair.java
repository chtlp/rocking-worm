package index;

import value.Value;

public class KeyValuePair {
	public final Value key;
	public final int rid;
	public final Value value;
	
	public KeyValuePair(Value k, int r, Value v) {
		rid = r;
		key = k;
		value = v;
	}
	
	public KeyValuePair(Value k, Value v) {
		this(k, -1, v);
	}
	
}
