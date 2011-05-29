package jdbc;

import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import parser.Absyn;
import parser.AbsynList;
import parser.lexer;
import parser.parser;
import plan.Plan;
import plan.Planner;
import plan.QueryPlan;
import plan.UpdatePlan;

import transaction.Transaction;

/**
 * The RMI server-side implementation of RemoteStatement.
 */
@SuppressWarnings("serial")
class RemoteStatementImpl extends UnicastRemoteObject implements
		RemoteStatement {
	private RemoteConnectionImpl rconn;
	private RemoteResultSet resultSet;
	private QueryPlan qPlan = null;

	public RemoteStatementImpl(RemoteConnectionImpl rconn)
			throws RemoteException {
		this.rconn = rconn;
	}

	/**
	 * Executes the specified SQL command. Return true if it's a query command.
	 */
	@Override
	public boolean execute(String sql) throws RemoteException {
		try {
			// a SQL command passed to Parser should end with a semicolon.
			Startup.logger.debug("Executing: " + sql);
			if (!sql.endsWith(";"))
				sql = sql + ";";

			Transaction tx = rconn.getTransaction();
			parser p = new parser(new lexer(new ByteArrayInputStream(sql
					.getBytes())));
			AbsynList result = (AbsynList) p.parse().value;
			Absyn qry = result.head;
			Plan plan = Planner.translate(qry, tx);
			if (plan instanceof QueryPlan) {
				qPlan = (QueryPlan) plan;
				resultSet = new RemoteResultSetImpl(qPlan, rconn);
				return true;
			}
			else {
				((UpdatePlan) plan).run();
				qPlan = null;
				result = null;
				return false;
			}
		} catch (Exception e) {
			resultSet = null;
			rconn.rollback();
			e.printStackTrace();
			throw new RemoteException("RemoteStatement.execute()", e);
		}
	}

	@Override
	public RemoteResultSet getResultSet() throws RemoteException {
		return resultSet;
	}

	@Override
	public void close() throws RemoteException {
		qPlan = null;
		if (rconn.getAutoCommit())
			rconn.commit();
	}
}
