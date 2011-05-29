package engine;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import plan.Plan;
import plan.Planner;
import plan.QueryPlan;
import plan.UpdatePlan;
import table.TableManager;
import transaction.DeadlockException;
import transaction.Transaction;

/**
 * 	The engine processing queries.
 */
public class Engine {
	
	public void run() throws DeadlockException, TimeoutException {
		ArrayList<parser.Absyn> absynArray = new ArrayList<parser.Absyn>();
		transaction.Transaction tr = Transaction.begin();
		for (int i = 0; i < absynArray.size(); i++) {
			Plan plan = Planner.translate(absynArray.get(i), tr);
			if (plan instanceof QueryPlan) {
				table.Record record = null;
				do {
					record = ((QueryPlan)plan).next();
					//output record;
				}
				while (record == null);
			}
			else {
				((UpdatePlan)plan).run();
			}
		}
		tr.commit();
	}
}
