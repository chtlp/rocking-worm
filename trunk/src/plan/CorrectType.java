package plan;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import value.*;

public class CorrectType {
	static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static Value trans(Value value, int type) {
		if (type == Value.TYPE_INT) {
			if (value instanceof IntValue)
				return value;
			if (value instanceof StrValue) 
				return new IntValue(Integer.parseInt(((StrValue)value).get()));
			if (value instanceof FloatValue) {
				float f = ((FloatValue)value).get();
				return new IntValue((int)f);
			}
		}
		if (type == Value.TYPE_FLOAT) {
			if (value instanceof FloatValue)
				return value;
			if (value instanceof StrValue) 
				return new FloatValue(Float.parseFloat(((StrValue)value).get()));
			if (value instanceof IntValue) {
			    int i = ((IntValue)value).get();
				return new FloatValue(i);
			}
		}
		if (type == Value.TYPE_CHAR || type == Value.TYPE_VARCHAR) {
			if (value instanceof StrValue)
				return value;
			if (value instanceof IntValue) {
				Integer i = ((IntValue)value).get();
				return new StrValue(i.toString());
			}
			if (value instanceof FloatValue) {
				Float f = ((FloatValue)value).get();
				return new StrValue(f.toString());
			}
			if (value instanceof DateTimeValue) 
				return new StrValue(((DateTimeValue)value).toString());
		}
		if (type == Value.TYPE_DATETIME) {
			if (value instanceof DateTimeValue)
				return value;
			if (value instanceof StrValue) {
				String s = ((StrValue) value).get();
				try {
					return new DateTimeValue(formatter.parse(s));
				} catch (ParseException e) {
					System.out.println("DATETIME ERROR");
					e.printStackTrace();
				}
			}
		}
		return value;
	}
}
