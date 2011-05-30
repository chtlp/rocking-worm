package plan;

import java.util.concurrent.TimeoutException;

import table.Record;
import transaction.DeadlockException;
import transaction.Transaction;

public class JoinPlan extends QueryPlan {
	QueryPlan queryPlan1, queryPlan2;
	Record r1 = null;
	Record r2 = null;

	public JoinPlan(QueryPlan queryPlan1, QueryPlan queryPlan2, Transaction tr) {
		this.queryPlan1 = queryPlan1;
		this.queryPlan2 = queryPlan2;
		this.alias.addAll(queryPlan1.alias);
		this.alias.addAll(queryPlan2.alias);
		this.columns.addAll(queryPlan1.columns);
		this.columns.addAll(queryPlan2.columns);
		this.tr = tr;
	}

	@Override
	public void open() throws DeadlockException, TimeoutException {
		//System.out.println("Joinplan opens");
		queryPlan1.open();
		queryPlan2.open();
		r1 = queryPlan1.next();
		
	}

	@Override
	public void close() throws DeadlockException, TimeoutException {
		queryPlan1.close();
		queryPlan2.close();
		
	}

	@Override
	public Record next() throws DeadlockException, TimeoutException {
		if (r1 == null) return null;
		r2 = queryPlan2.next();
		if (r2 == null) {
			r1 = queryPlan1.next();
			if (r1 == null) return null;
			queryPlan2.close();
			queryPlan2.open();
			r2 = queryPlan2.next();
			if (r2 == null) return null;
		}
		return new Record(r1, r2);
	}
}
