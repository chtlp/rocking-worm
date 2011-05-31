package plan;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeoutException;

import table.Column;
import table.Record;
import table.RecordList;
import tlp.util.Debug;
import transaction.DeadlockException;
import transaction.Transaction;

public class SortPlan extends QueryPlan {

	static final int BLOCKSIZE = 1024 * 1024 * 10; // 1MB
	QueryPlan iterator;
	Comparator<Record> comp;

	// TODO the problem with long strings
	public SortPlan(Transaction tr, QueryPlan iter, Comparator<Record> comp) {
		this.tr = tr;
		iterator = iter;
		this.comp = comp;
		this.alias.addAll(iter.alias);
		this.columns.addAll(iter.columns);
	}

	Transaction tr = null;
	int blockNum = -1;
	ArrayList<RecordList> results;

	public void open() throws DeadlockException, TimeoutException {
		int recSize = 0;
		for (Column c : iterator.columns) {
			recSize += c.valueSize();
		}

		// so one block of data will have so many records
		blockNum = BLOCKSIZE / recSize;

		results = new ArrayList<RecordList>();

		int k = 0;
		ArrayList<Record> blockSort = new ArrayList<Record>();
		for (iterator.open();;) {
			Record r = iterator.next();

			if (r != null) {
				++k;
				blockSort.add(r);
				// Debug.testLight3.debug("sort plan input: {}", r);
			}

			if (k == blockNum || r == null) {
				Collections.sort(blockSort, comp);
				RecordList list = new RecordList(tr, iterator.columns);
				list.addAll(blockSort);
				results.add(list);

				k = 0;
				blockSort.clear();
			}

			if (r == null)
				break;
		}
		iterator.close();

		for (RecordList l : results) {
			l.open();
		}

	}

	public Record next() {
		Record r = null;
		RecordList list = null;
		for (RecordList l : results) {
			Record x = l.peek();
			if (x != null && (r == null || comp.compare(x, r) < 0)) {
				r = x;
				list = l;
			}
		}

		if (r != null)
			list.removeFront();

		return r;
	}

	public void close() {
		for (RecordList l : results) {
			l.free();
		}
//		print(System.out);

	}

	public void print(PrintStream out) {
		out.println(" *** sort plan *** ");
		try {
			for (open();;) {
				Record r = next();
				if (r != null)
					out.format("%s \n", r);
				else break;
			}
//			close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
