package plan; 

import java.util.ArrayList;
import java.util.Comparator;

import table.Record;

public class Comp implements Comparator<Record>{
	ArrayList<Integer> compIdx;
	ArrayList<Integer> compSlt;
	public Comp(ArrayList<Integer> compIdx, ArrayList<Integer> compSlt) {
		super();
		this.compIdx = compIdx;
		this.compSlt = compSlt;
	}
	
	/*
	 * -1 lhs < rhs
	 * 0 lhs == rhs
	 * 1 lhs > rhs
	 */

	@Override
	public int compare(Record lhs, Record rhs) {
		for (int i = 0; i < compIdx.size(); i++) {
			int tmp = lhs.getValue(compIdx.get(i)).compareTo(
					rhs.getValue(compIdx.get(i)));
			if (tmp != 0) return tmp * compSlt.get(i);
		}
		return 0;
	}
	
}
