package table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Multimap<T1, T2> {
	private Map<T1, List<T2>> map = new HashMap<T1, List<T2>>();

	public List<T2> get(T1 key) {
		List<T2> list = map.get(key);
		if (list == null) {
			list = new ArrayList<T2>();
			map.put(key, list);
		}
		return list;
	}

	public void put(T1 key, T2 value) {
		List<T2> list = map.get(key);
		if (list == null) {
			list = new ArrayList<T2>();
			map.put(key, list);
		}
		list.add(value);
	}
}
