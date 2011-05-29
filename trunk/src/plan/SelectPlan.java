package plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

import table.Column;
import table.Record;
import transaction.DeadlockException;
import transaction.Transaction;

public class SelectPlan extends QueryPlan {
	Cond condition;

	QueryPlan queryPlan;
	parser.SelectExprList selectExprList;
	boolean distinct;
	boolean flag = true;
	HashSet<Integer> recordHash = new HashSet<Integer>();
	HashMap<parser.FuncValue, FuncCal> funcMap = new HashMap<parser.FuncValue, FuncCal>();
	Tail tail;

	ArrayList<Column> fromCols = new ArrayList<Column>();
	ArrayList<Alia> fromAlias = new ArrayList<Alia>();

	public SelectPlan(QueryPlan queryPlan, boolean distinct,
			parser.SelectExprList selectExprList, Cond cond, Transaction tr,
			Tail tail) throws DeadlockException, TimeoutException {
		this.queryPlan = queryPlan;
		this.distinct = distinct;
		this.selectExprList = selectExprList;
		this.condition = cond;
		this.tail = tail;
		parser.SelectExprList sel = selectExprList;

		fromCols.addAll(queryPlan.columns);
		fromAlias.addAll(queryPlan.alias);
		if (tail != null) {
			fromCols.addAll(tail.columns);
			fromAlias.addAll(tail.alias);
		}

		while (sel != null) {
			parser.SelectExpr se = sel.head;
			if (se.selectAll) {
				columns.addAll(queryPlan.columns);
				alias.addAll(queryPlan.alias);
				break;
			}
			table.Column col;
			Alia alia;
			if (se.value instanceof parser.ColNameValue) {

				parser.ColNameValue cnv = (parser.ColNameValue) se.value;
				int idx = new Col2Idx().getIdx(fromAlias, cnv.colName);
				col = fromCols.get(idx);
				alia = fromAlias.get(idx);
			} else {
				new ValueInterpreter().traverseValue(fromAlias, fromCols,
						funcMap, se.value);
				col = new ValueInterpreter().getValueType(fromAlias, fromCols,
						se.value, tr);
				alia = new Alia();
			}

			if (se.alias != null) {
				alia.addColName(se.alias);
				if (col.getName() == null)
					col.setName(se.alias);
			}
			columns.add(col);
			alias.add(alia);
			sel = sel.tail;
		}
		this.tr = tr;

	}

	@Override
	public void open() throws DeadlockException, TimeoutException {
		// System.out.println("selectplan opens");
		queryPlan.open();
		for (FuncCal funcCal : funcMap.values())
			funcCal.setZero();
		recordHash.clear();
	}

	@Override
	public void close() throws DeadlockException, TimeoutException {
		queryPlan.close();
	}

	Record getValues(Record rv) throws DeadlockException, TimeoutException {
		parser.SelectExprList sel = selectExprList;
		Record ret = new Record();
		while (sel != null) {
			parser.SelectExpr se = sel.head;
			if (se.selectAll) {
				if (tail == null)
					return rv;
				for (int i = 0; i < rv.size() - tail.alias.size(); i++)
					ret.addValue(rv.getValue(i));
				return ret;
			}
			ret.addValue(new plan.ValueInterpreter().transValue(se.value,
					fromAlias, fromCols, rv, funcMap, tr));
			sel = sel.tail;
		}
		return ret;
	}

	@Override
	public Record next() throws DeadlockException, TimeoutException {
		if (funcMap.size() > 0) {
			if (!flag)
				return null;
			Record record = null;
			do {
				do {
					record = queryPlan.next();
					if (record == null)
						break;
					if (tail != null)
						record = new Record(record, tail.record);
				} while (!condition.check(fromAlias, fromCols, record, null));

				if (record != null) {
					for (FuncCal funcCal : funcMap.values())
						funcCal.consider(record);
				}
			} while (record != null);
			Record ret = getValues(null);
			flag = false;
			return ret;
		}

		// No aggregation functions
		Record rv;
		Record ret;
		do {
			rv = queryPlan.next();
			if (rv == null)
				return null;
			if (tail != null)
				rv = new Record(rv, tail.record);
			ret = getValues(rv);
		} while (!condition.check(fromAlias, fromCols, rv, null)
				|| (distinct && recordHash.contains(ret.distinctCode())));
		if (distinct)
			recordHash.add(ret.distinctCode());
		return ret;
	}

}