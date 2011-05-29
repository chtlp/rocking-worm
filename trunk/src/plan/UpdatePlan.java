package plan;

import java.util.concurrent.TimeoutException;

import transaction.DeadlockException;

public abstract class UpdatePlan extends Plan {
	
	public abstract boolean run() throws DeadlockException, TimeoutException;

}
