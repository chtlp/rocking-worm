package util;

import java.util.HashMap;

public class Counter {
	static HashMap<String, Counter> repo = new HashMap<String, Counter>();
	
	String name;
	
	Counter(String n) {
		name = n;
	}
	
	@Override
	public String toString() {
		return String.format("Counter %s, counts %s ", name, num_counter);
	}
	
	public static synchronized Counter getCounter(String name) {
		Counter c = repo.get(name);
		if (c == null) {
			c = new Counter(name);
		}
		repo.put(name, c);
		return c;
	}
	
	int num_counter = 0;
	
	public synchronized void inc() {
		++num_counter;
	}
	
	public synchronized void dec() {
		--num_counter;
	}
	
	public synchronized int get() {
		return num_counter;
	}
	
	public synchronized void set(int value) {
		num_counter = value;
	}
	
}

