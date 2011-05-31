package plan;

import java.util.concurrent.TimeoutException;

import table.Record;
import tlp.util.Debug;
import transaction.DeadlockException;
import transaction.Transaction;

public class HashJoinPlan extends QueryPlan {
	QueryPlan p1;
	QueryPlan p2;
	int c1; 
	int c2;
	Transaction tr;
	table.HashJoin hj;
	
	public HashJoinPlan(QueryPlan p1, QueryPlan p2, int c1, int c2,
			Transaction tr) {
		super();
		this.p1 = p1;
		this.p2 = p2;
		this.c1 = c1;
		this.c2 = c2;
		this.tr = tr;
		hj = new table.HashJoin(tr, p1, p2, c1, c2);
		this.alias.addAll(p1.alias);
		this.alias.addAll(p2.alias);
		this.columns.addAll(p1.columns);
		this.columns.addAll(p2.columns);
	}

	@Override
	public void open() throws DeadlockException, TimeoutException {
//		Debug.testSimple.debug("Hash Join opens");
		hj.open();

	}

	@Override
	public void close() throws DeadlockException, TimeoutException {
		hj.close();

	}

	@Override
	public Record next() throws DeadlockException, TimeoutException {
		Record ret = hj.next();
//		Debug.testSimple.debug("hash join result = {}", ret);
//		System.out.println(ret);
		return ret;
	}

}
