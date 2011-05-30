package plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import parser.Func;
import parser.FuncValue;
import parser.Value;
import table.Column;
import table.Record;
import transaction.DeadlockException;
import transaction.Transaction;

public class ValueInterpreter {
	public value.Value transValue(parser.Value value, ArrayList<Alia> alias,
			ArrayList<Column> columns, Record record,
			HashMap<parser.FuncValue, FuncCal> funcMap, Transaction tr)
			throws DeadlockException, TimeoutException {
		if (value instanceof parser.BracketValue) {
			parser.BracketValue bv = (parser.BracketValue) value;
			return transValue(bv.value, alias, columns, record, funcMap, tr);
		}
		if (value instanceof parser.ColNameValue) {
			parser.ColNameValue cnv = (parser.ColNameValue) value;
			int idx = new Col2Idx().getIdx(alias, cnv.colName);
			return record.getValue(idx);
		}
		if (value instanceof parser.ConstValue) {
			parser.ConstValue cvv = (parser.ConstValue) value;
			return new plan.ConstValueInterpreter().trans(cvv);
		}
		if (value instanceof parser.AopValue) {
			parser.AopValue av = ((parser.AopValue) value);
			Value left = av.valueL;
			Value right = av.valueR;
			value.Value leftValue = transValue(left, alias, columns, record,
					funcMap, tr);
			value.Value rightValue = transValue(right, alias, columns, record,
					funcMap, tr);
			double L = 0;
			double R = 0;
			if (leftValue instanceof value.IntValue) {
				value.IntValue leftI = (value.IntValue) leftValue;
				value.IntValue rightI = (value.IntValue) rightValue;
				L = leftI.get();
				R = rightI.get();
			}
			if (leftValue instanceof value.FloatValue) {
				value.FloatValue leftF = (value.FloatValue) leftValue;
				value.FloatValue rightF = (value.FloatValue) rightValue;
				L = leftF.get();
				R = rightF.get();
			}
			double ans = 0;
			switch (av.ty) {
			case parser.Aop.PLUS:
				ans = L + R;
				break;
			case parser.Aop.MINUS:
				ans = L - R;
				break;
			case parser.Aop.DIVIDE:
				ans = L / R;
				break;
			case parser.Aop.MULT:
				ans = L * R;
				break;
			case parser.Aop.MOD:
				ans = (int) L % (int) R;
			}
			if (leftValue instanceof value.IntValue)
				return new value.IntValue((int) ans);
			if (leftValue instanceof value.FloatValue)
				return new value.FloatValue((float) ans);
		}
		if (value instanceof parser.SubqueryValue) {
			parser.SubqueryValue sv = (parser.SubqueryValue) value;
			QueryPlan p = Planner.translate(sv.subquery, tr, new Tail(record,
					alias, columns));
			p.open();
			Record r = p.next();
			p.close();
			return r.getValue(0);
		}
		if (value instanceof parser.FuncValue)
			return funcMap.get((parser.FuncValue) value).getAns();
		return null;

	}

	public void traverseValue(ArrayList<Alia> alias, ArrayList<Column> columns,
			HashMap<parser.FuncValue, FuncCal> funcMap, parser.Value value) {
		if (value instanceof parser.BracketValue) {
			parser.BracketValue bv = (parser.BracketValue) value;
			traverseValue(alias, columns, funcMap, bv.value);
		}
		if (value instanceof parser.AopValue) {
			parser.AopValue av = ((parser.AopValue) value);
			Value left = av.valueL;
			Value right = av.valueR;
			traverseValue(alias, columns, funcMap, left);
			traverseValue(alias, columns, funcMap, right);
		}
		if (value instanceof parser.FuncValue) {
			parser.FuncValue fv = (parser.FuncValue) value;
			int idx = new Col2Idx().getIdx(alias, fv.colName);
			funcMap.put((FuncValue) value, new FuncCal(idx, fv.func.type));
		}
	}

	public table.Column getValueType(ArrayList<Alia> alias,
			ArrayList<Column> columns, parser.Value pvalue, Transaction tr)
			throws DeadlockException, TimeoutException {
		if (pvalue instanceof parser.BracketValue) {
			parser.BracketValue bv = (parser.BracketValue) pvalue;
			return getValueType(alias, columns, bv.value, tr);
		}
		if (pvalue instanceof parser.ColNameValue) {
			parser.ColNameValue cnv = (parser.ColNameValue) pvalue;
			int idx = new Col2Idx().getIdx(alias, cnv.colName);
			return columns.get(idx);
		}
		if (pvalue instanceof parser.ConstValue) {
			parser.ConstValue cvv = (parser.ConstValue) pvalue;
			return new ConstValueInterpreter().getCol(cvv);
		}
		if (pvalue instanceof parser.AopValue) {
			parser.AopValue av = ((parser.AopValue) pvalue);
			Value left = av.valueL;
			Value right = av.valueR;
			table.Column lc = getValueType(alias, columns, left, tr);
			table.Column rc = getValueType(alias, columns, right, tr);
			if (rc.getType() == value.Value.TYPE_FLOAT)
				return rc;
			return lc;
		}
		if (pvalue instanceof parser.SubqueryValue) {
			parser.SubqueryValue sv = (parser.SubqueryValue) pvalue;
			QueryPlan p = Planner.translate(sv.subquery, tr, new Tail(null,
					alias, columns));
			return p.getColumns().get(0);
		}
		if (pvalue instanceof parser.FuncValue) {
			parser.FuncValue fv = (parser.FuncValue) pvalue;
			int idx = new Col2Idx().getIdx(alias, fv.colName);
			if (fv.func.type == Func.AVG) {
				Column c = new Column(columns.get(idx));
				c.setType(value.Value.TYPE_FLOAT);
				return c;
			}
			if (fv.func.type == Func.SUM) {
				Column c = new Column(columns.get(idx));
				c.setType(value.Value.TYPE_FLOAT);
				return c;
			}
			return columns.get(idx);
		}
		return null;
	}
}
