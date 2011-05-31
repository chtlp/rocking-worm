package plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import parser.BoolExpr;
import parser.ColName;
import parser.SelectExprList;
import table.Column;
import table.Record;
import tlp.util.Debug;
import transaction.DeadlockException;
import transaction.Transaction;

public class GroupPlan extends QueryPlan {

	HashMap<parser.FuncValue, FuncCal> funcMap = new HashMap<parser.FuncValue, FuncCal>();
	QueryPlan queryPlan;
	SelectExprList selectExprList;
	BoolExpr having;
	Cond cond;
	int SEgroupIdx, groupIdx;
	int functype;
	ColName cn;

	SortPlan sortPlan;
	Record record = null;
	Cond havingCond;

	value.Value aValue = null;
	
	Tail tail;

	ArrayList<Column> fromCols = new ArrayList<Column>();
	ArrayList<Alia> fromAlias = new ArrayList<Alia>();


	public GroupPlan(QueryPlan queryPlan, SelectExprList selectExprList,
			BoolExpr having, ColName cn, Cond cond, Transaction tr, Tail tail)
			throws DeadlockException, TimeoutException {
		super();
		this.queryPlan = queryPlan;
		this.selectExprList = selectExprList;
		this.having = having;
		this.cn = cn;
		this.cond = cond;
		this.tr = tr;
		this.tail = tail;
		
		fromCols.addAll(queryPlan.columns);
		fromAlias.addAll(queryPlan.alias);
		if (tail != null) {
			fromCols.addAll(tail.columns);
			fromAlias.addAll(tail.alias);
		}

		if (having != null) {
			havingCond = new Cond(having, tr);
			havingCond.traverse(fromAlias, fromCols, funcMap);
		}

		parser.SelectExprList sel = selectExprList;
		while (sel != null) {
			parser.SelectExpr se = sel.head;
			table.Column col;
			Alia alia;
			if (se.value instanceof parser.ColNameValue) {
				parser.ColNameValue cnv = (parser.ColNameValue) se.value;
				int idx = new Col2Idx().getIdx(fromAlias, cnv.colName);
				col = fromCols.get(idx);
				alia = fromAlias.get(idx);
			} else {
				new ValueInterpreter().traverseValue(fromAlias,
						fromCols, funcMap, se.value);
				col = new ValueInterpreter().getValueType(fromAlias,
						fromCols, se.value, tr);
				alia = new Alia();
			}

			if (se.alias != null)
				alia.addColName(se.alias);
			columns.add(col);
			alias.add(alia);
			sel = sel.tail;
		}
	}

	@Override
	public void open() throws DeadlockException, TimeoutException {
		ArrayList<Integer> compIdx = new ArrayList<Integer>();
		ArrayList<Integer> compSlt = new ArrayList<Integer>();
		groupIdx = new Col2Idx().getIdx(fromAlias, cn);
		compIdx.add(groupIdx);
		compSlt.add(1);
		Comp comp = new Comp(compIdx, compSlt);
		sortPlan = new SortPlan(tr, queryPlan, comp);
		sortPlan.open();
	}

	@Override
	public void close() {
		sortPlan.close();
	}

	Record getValues(Record record) throws DeadlockException, TimeoutException {
		parser.SelectExprList sel = selectExprList;
		Record ret = new Record();
		while (sel != null) {
			parser.SelectExpr se = sel.head;
			if (se.selectAll)
				return record;
			ret.addValue(new plan.ValueInterpreter().transValue(se.value,
					fromAlias, fromCols, record, funcMap, tr));
			sel = sel.tail;
		}
		return ret;
	}

	public Record nextRecord() throws DeadlockException, TimeoutException {
		if (record == null
				|| !cond.check(fromAlias, fromCols, record, null))
			do {
				record = sortPlan.next();
				if (record == null)
					return null;
				if (tail != null)
					record = new Record(record, tail.record);
			} while (!cond
					.check(fromAlias, fromCols, record, null));
		value.Value colValue = record.getValue(groupIdx);
		Record firstRecord = record;

		for (FuncCal funcCal : funcMap.values())
			funcCal.setZero();
		while (true) {
			for (FuncCal funcCal : funcMap.values()) {
				funcCal.consider(record);
			}
			do {
				record = sortPlan.next();
				if (record == null)
					break;
				if (tail != null)
					record = new Record(record, tail.record);
			} while (!cond
					.check(fromAlias, fromCols, record, null));
			if (record == null)
				break;
			value.Value value = record.getValue(groupIdx);
			if (colValue.compareTo(value) != 0)
				break;
		}

		return getValues(firstRecord);
	}

	@Override
	public Record next() throws DeadlockException, TimeoutException {
		if (having == null) {
			Record n = nextRecord();
			Debug.testSimple.debug("group plan returns {}", n);
			return n;
		}
		else {
			Record n;
			do {
				n = nextRecord();
				if (n == null)
					return null;
			} while (!havingCond.check(alias, columns, n, funcMap));
			return n;
		}
	}

}
