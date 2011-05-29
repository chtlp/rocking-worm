package plan;

import java.util.Date;

public class ConstValueInterpreter {
	@SuppressWarnings("deprecation")
	public value.Value trans(parser.ConstValue cValue) {
		if (cValue instanceof parser.IntConstValue) 
			return new value.IntValue(((parser.IntConstValue) cValue).integer);
		if (cValue instanceof parser.StringConstValue)
			return new value.StrValue(((parser.StringConstValue) cValue).string);
		if (cValue instanceof parser.FloatConstValue)
			return new value.FloatValue(((parser.FloatConstValue) cValue).f);
		if (cValue instanceof parser.TimeConstValue) 
			return new value.DateTimeValue(new Date(((parser.TimeConstValue) cValue).string));
		if (cValue instanceof parser.BoolConstValue)
			return new value.BooleanValue(((parser.BoolConstValue) cValue).b);
		if (cValue instanceof parser.NullConstValue)
			return null;
		System.out.println("Value trans ERROR");
		return null;
	}
	
	public table.Column getCol(parser.ConstValue cValue) {
		int ty = 0;
		if (cValue instanceof parser.IntConstValue) 
			ty = value.Value.TYPE_INT;
		if (cValue instanceof parser.StringConstValue)
			ty = value.Value.TYPE_CHAR;
		if (cValue instanceof parser.FloatConstValue)
			ty = value.Value.TYPE_FLOAT;
		if (cValue instanceof parser.TimeConstValue) 
			ty = value.Value.TYPE_DATETIME;
		if (cValue instanceof parser.BoolConstValue)
			ty = value.Value.TYPE_BOOLEAN;
		return new table.Column(null, ty, 0, 0, false, null);		
	}
}
