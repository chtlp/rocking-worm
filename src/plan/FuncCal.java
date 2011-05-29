package plan;

import table.Record;

public class FuncCal {

	double f;
	double max;
	double min;
	int count;
	int idx;
	int functype;
	boolean isInt = false;

	public FuncCal(int idx, int functype) {
		this.idx = idx;
		this.functype = functype;
		setZero();
	}
	
	public void setZero() {
		f = 0;
		max = Integer.MIN_VALUE;
		min = Integer.MAX_VALUE;
		count = 0;
	}

	public value.Value getAns() {
		//System.out.println(f);
		//System.out.println(count);
		double ans = 0;
		switch (functype) {
		case parser.Func.AVG:
			ans = f / count;
			break;
		case parser.Func.COUNT:
			return new value.IntValue(count);
		case parser.Func.MAX:
			ans = max;
			break;
		case parser.Func.MIN:
			ans = min;
			break;
		case parser.Func.SUM:
			ans = f;
			break;
		}
		if (isInt)
			return new value.IntValue((int) ans);
		else
			return new value.FloatValue((float) ans);
	}

	public void consider(Record record) {
		value.Value num = record.getValue(idx);
		double d = 0;
		if (num instanceof value.IntValue) {
			d = ((value.IntValue) num).get();
			isInt = true;
		}
		if (num instanceof value.FloatValue)
			d = ((value.FloatValue) num).get();

		count ++;
		switch (functype) {
		case parser.Func.AVG:
			f += d;
			break;
		case parser.Func.COUNT:
			break;
		case parser.Func.MAX:
			if (d > max)
				max = d;
			break;
		case parser.Func.MIN:
			if (d < min)
				min = d;
			break;
		case parser.Func.SUM:
			f += d;
		}

	}

}
