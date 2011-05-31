package table;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import plan.QueryPlan;
import tlp.util.Debug;
import transaction.DeadlockException;
import transaction.Transaction;
import value.Value;

import com.google.common.collect.ArrayListMultimap;

public class HashJoin {
	public static final int NUM_BLOCKS = 20;

	public static final int BUFFER_THRESHOLD = 10000;

	ArrayList<Column> columns;
	Transaction tr;

	QueryPlan p1, p2;
	int c1, c2;

	public HashJoin(Transaction tr, QueryPlan p1, QueryPlan p2, int c1, int c2) {
		this.tr = tr;
		columns = new ArrayList<Column>();
		columns.addAll(p1.getColumns());
		columns.addAll(p2.getColumns());
		this.p1 = p1;
		this.p2 = p2;
		this.c1 = c1;
		this.c2 = c2;
	}

	public int numColumns() {
		return columns.size();
	}

	public Column getColumn(int index) {
		return columns.get(index);
	}

	private int numBlocks() {
		return NUM_BLOCKS;
	}

	RecordList[] list1, list2;

	int blocks;

	public void open() throws DeadlockException, TimeoutException {
		Debug.testJoin.debug("Hash Join opens");
		blocks = numBlocks();
		list1 = new RecordList[blocks];
		list2 = new RecordList[blocks];

		for (int i = 0; i < list1.length; ++i)
			list1[i] = new RecordList(tr, p1.getColumns());
		for (int i = 0; i < list2.length; ++i)
			list2[i] = new RecordList(tr, p2.getColumns());

		for (p1.open();;) {
			Record r = p1.next();
			if (r == null)
				break;
			Value v = r.getValue(c1);
			if (v != null) {
				int h = v.hashCode() % blocks;
				h = h < 0 ? h + blocks : h;
				list1[h].add(r);
			}
		}
		p1.close();

		for (p2.open();;) {
			Record r = p2.next();
			if (r == null)
				break;
			Value v = r.getValue(c2);
			if (v != null) {
				int h = v.hashCode() % blocks;
				h = h < 0 ? h + blocks : h;
				list2[h].add(r);
			}
		}
		p2.close();

		if (Debug.testJoin.isDebugEnabled()) {
			for (int i = 0; i < blocks; ++i) {
				Debug.testJoin.debug("list1[{}] size = {}", i, list1[i].size());
				list1[i].print(System.out);
				Debug.testJoin.debug("list2[{}] size = {}", i, list2[i].size());
				list2[i].print(System.out);
			}
		}

	}

	public boolean hasNext() {
		throw new UnsupportedOperationException();
	}

	int iter = 0;
	boolean started = false;
	boolean swapped = false;
	RecordList lhs, rhs;
	ArrayListMultimap<Value, Record> rhsMap = null;
	int d1, d2;
	Record lrec = null;
	Iterator<Record> riter = null;

	public Record next() {
		// see if we need to proceed the next tuple of lhs
		while (true) {
			// start a new block
			if (started == false) {
				while (iter < blocks
						&& ((list1[iter].size() == 0) || (list2[iter].size() == 0))) {
					list1[iter].free();
					list2[iter].free();
					++iter;
				}
				// no more blocks available
				if (iter == blocks)
					return null;

				// let rhs be the smaller side
				if (list1[iter].size() < list2[iter].size()) {
					swapped = true;
					lhs = list2[iter];
					rhs = list1[iter];
					d1 = c2;
					d2 = c1;
				} else {
					swapped = false;
					lhs = list1[iter];
					rhs = list2[iter];
					d1 = c1;
					d2 = c2;
				}

				// see if we can store rhs in memory
				rhsMap = null;
				if (rhs.size() < BUFFER_THRESHOLD) {
					rhsMap = ArrayListMultimap.create();

					for (rhs.open();;) {
						Record r = rhs.next();
						if (r == null)
							break;

						assert r.getValue(d2) != null;
						rhsMap.put(r.getValue(d2), r);
					}
					rhs.free();
				}

				started = true;
				lhs.open();
				lrec = null;
			}

			// now we are in a block
			assert started == true;

			// find a tuple and return it
			while (true) {
				// find a new tuple in lhs
				if (lrec == null) {
					lrec = lhs.next();

					// the current block exhausted, start a new one
					if (lrec == null) {
						lhs.free();
						if (rhsMap == null)
							rhs.free();
						started = false;
						++iter;
						break;
					}

					assert lrec != null;
					Debug.breakOn(lrec.rowID == 19);

					// else start rhs
					if (rhsMap != null) {
						List<Record> mates = rhsMap.get(lrec.getValue(d1));
						riter = mates.iterator();
					} else {
						rhs.open();
					}

				}

				assert lrec != null;
				if (rhsMap != null) {
					if (riter.hasNext()) {
						Record rrec = riter.next();
						return mergeRecord(lrec, rrec, swapped);
					} else
						lrec = null;
				} else {
					Record rrec = null;
					while (true) {
						Record r = rhs.next();
						if (r == null)
							break;
						if (r.getValue(d2).equals(lrec.getValue(d1))) {
							rrec = r;
							break;
						}
					}
					if (rrec != null)
						return mergeRecord(lrec, rrec, swapped);
					else {
						lrec = null;
					}
				}
			}
		}

	}

	private Record mergeRecord(Record lrec, Record rrec, boolean swapped) {
		if (swapped) {
			Record t = lrec;
			lrec = rrec;
			rrec = t;
		}

		Record r = new Record(lrec);
		for (int i = 0; i < rrec.size(); ++i)
			r.addValue(rrec.getValue(i));
		return r;
	}

	public void close() {
		// nothing to do here, I have freed all the resources in the process
	}

	public void print(PrintStream out) throws DeadlockException,
			TimeoutException {
		out.println(" *** Hash Join *** ");
		for (open();;) {
			Record r = next();
			if (r == null)
				break;
			out.println(r);
		}
	}
}
