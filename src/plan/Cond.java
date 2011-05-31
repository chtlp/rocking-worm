package plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import parser.FuncValue;

import table.Column;
import table.Record;
import transaction.DeadlockException;
import transaction.Transaction;

public class Cond {
	parser.BoolExpr rootbe;
	Transaction tr;

	public Cond(parser.BoolExpr be, Transaction tr) {
		this.rootbe = be;
		this.tr = tr;
	}

	public boolean check(ArrayList<Alia> alias, ArrayList<Column> columns,
			Record record, HashMap<parser.FuncValue, FuncCal> funcMap)
			throws DeadlockException, TimeoutException {
		return checkBoolExpr(alias, columns, record, rootbe, funcMap);
	}

	boolean checkCop(value.Value left, value.Value right, int coptype) {
		switch (coptype) {
		case parser.Cop.Lt:
			return left.compareTo(right) < 0;
		case parser.Cop.Gt:
			return left.compareTo(right) > 0;
		case parser.Cop.Eq:
			return left.compareTo(right) == 0;
		case parser.Cop.LtEq:
			return left.compareTo(right) <= 0;
		case parser.Cop.GtEq:
			return left.compareTo(right) >= 0;
		case parser.Cop.NotEq:
			return left.compareTo(right) != 0;
		default:
			return false;
		}
	}

	boolean checkBoolExpr(ArrayList<Alia> alias, ArrayList<Column> columns,
			Record record, parser.BoolExpr be,
			HashMap<FuncValue, FuncCal> funcMap) throws DeadlockException,
			TimeoutException {
		if (be == null)
			return true;
		if (be instanceof parser.CopValue) {
			parser.CopValue cvbe = (parser.CopValue) be;
			value.Value left = new plan.ValueInterpreter().transValue(
					cvbe.valueL, alias, columns, record, funcMap, tr);
			value.Value right = new plan.ValueInterpreter().transValue(
					cvbe.valueR, alias, columns, record, funcMap, tr);
			return checkCop(left, right, cvbe.cop.type);
		}
		if (be instanceof parser.AndBoolExpr) {
			parser.AndBoolExpr abebe = (parser.AndBoolExpr) be;
			return checkBoolExpr(alias, columns, record, abebe.left, funcMap)
					& checkBoolExpr(alias, columns, record, abebe.right,
							funcMap);
		}
		if (be instanceof parser.OrBoolExpr) {
			parser.OrBoolExpr obebe = (parser.OrBoolExpr) be;
			return checkBoolExpr(alias, columns, record, obebe.left, funcMap)
					| checkBoolExpr(alias, columns, record, obebe.right,
							funcMap);
		}
		if (be instanceof parser.ExistBoolExpr) {
			parser.ExistBoolExpr ebebe = (parser.ExistBoolExpr) be;
			if (ebebe.plan == null)
				ebebe.plan = Planner.translate(ebebe.subquery, tr, new Tail(
						record, alias, columns));
			ebebe.plan.open();
			boolean ret =  (ebebe.isExist != (ebebe.plan.next() == null));
			ebebe.plan.close();
			return ret;
		}
		if (be instanceof parser.AnyBoolExpr) {
			parser.AnyBoolExpr abebe = (parser.AnyBoolExpr) be;
			if (abebe.plan == null)
				abebe.plan = Planner.translate(abebe.subquery, tr, new Tail(
						record, alias, columns));
			QueryPlan qPlan = abebe.plan;
			qPlan.open();
			while (true) {
				Record rv = qPlan.next();
				if (rv == null)
					break;
				if (checkCop(new plan.ValueInterpreter().transValue(
						abebe.value, alias, columns, record, funcMap, tr),
						rv.getValue(0), abebe.cop.type)) {
					qPlan.close();
					return true;
				}
			}
			qPlan.close();
			return false;
		}
		if (be instanceof parser.InBoolExpr) {
			parser.InBoolExpr ibebe = (parser.InBoolExpr) be;
			if (ibebe.plan == null)
				ibebe.plan = Planner.translate(ibebe.subquery, tr, new Tail(
						record, alias, columns));
			QueryPlan qPlan = ibebe.plan;
			qPlan.open();
			while (true) {
				Record rv = qPlan.next();
				if (rv == null)
					break;
				if (checkCop(rv.getValue(0),
						new plan.ValueInterpreter().transValue(ibebe.value,
								alias, columns, record, funcMap, tr), parser.Cop.Eq)) {
					qPlan.close();
					return true;
				}
			}
			qPlan.close();
			return false;
		}
		if (be instanceof parser.AllBoolExpr) {
			parser.AllBoolExpr abebe = (parser.AllBoolExpr) be;
			if (abebe.plan == null)
				abebe.plan = Planner.translate(abebe.subquery, tr, new Tail(
						record, alias, columns));
			QueryPlan qPlan = abebe.plan;
			qPlan.open();
			while (true) {
				Record rv = qPlan.next();
				if (rv == null)
					break;
				value.Value v = new plan.ValueInterpreter().transValue(
						abebe.value, alias, columns, record, funcMap, tr);
				if (!checkCop(v, rv.getValue(0), abebe.cop.type)) {
					qPlan.close();
					return false;
				}
			}
			qPlan.close();
			return true;
		}
		if (be instanceof parser.BracketBoolExpr) {
			parser.BracketBoolExpr bbe = (parser.BracketBoolExpr) be;
			checkBoolExpr(alias, columns, record, bbe.boolExpr, funcMap);
		}
		System.out.println("WRONG in cond check");
		return false;
	}

	public void traverse(ArrayList<Alia> alias, ArrayList<Column> columns,
			HashMap<FuncValue, FuncCal> funcMap) {
		traverseBoolExpr(alias, columns, funcMap, rootbe);
	}

	void traverseBoolExpr(ArrayList<Alia> alias, ArrayList<Column> columns,
			HashMap<FuncValue, FuncCal> funcMap, parser.BoolExpr be) {
		if (be instanceof parser.CopValue) {
			parser.CopValue cvbe = (parser.CopValue) be;
			new plan.ValueInterpreter().traverseValue(alias, columns, funcMap,
					cvbe.valueL);
			new plan.ValueInterpreter().traverseValue(alias, columns, funcMap,
					cvbe.valueR);
		}
		if (be instanceof parser.AndBoolExpr) {
			parser.AndBoolExpr abebe = (parser.AndBoolExpr) be;
			traverseBoolExpr(alias, columns, funcMap, abebe.left);
			traverseBoolExpr(alias, columns, funcMap, abebe.right);
		}
		if (be instanceof parser.OrBoolExpr) {
			parser.OrBoolExpr obebe = (parser.OrBoolExpr) be;
			traverseBoolExpr(alias, columns, funcMap, obebe.left);
			traverseBoolExpr(alias, columns, funcMap, obebe.right);
		}
		if (be instanceof parser.AnyBoolExpr) {
			parser.AnyBoolExpr abebe = (parser.AnyBoolExpr) be;
			new plan.ValueInterpreter().traverseValue(alias, columns, funcMap,
					abebe.value);
		}
		if (be instanceof parser.InBoolExpr) {
			parser.InBoolExpr ibebe = (parser.InBoolExpr) be;
			new plan.ValueInterpreter().traverseValue(alias, columns, funcMap,
					ibebe.value);
		}
		if (be instanceof parser.AllBoolExpr) {
			parser.AllBoolExpr abebe = (parser.AllBoolExpr) be;
			new plan.ValueInterpreter().traverseValue(alias, columns, funcMap,
					abebe.value);
		}
		if (be instanceof parser.BracketBoolExpr) {
			parser.BracketBoolExpr bbe = (parser.BracketBoolExpr) be;
			traverseBoolExpr(alias, columns, funcMap, bbe.boolExpr);
		}
	}

}
