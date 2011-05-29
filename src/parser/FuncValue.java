package parser;

public class FuncValue extends Value {
	public Func func;
	public ColName colName;
	public FuncValue(int p, Func f, ColName c) {pos=p; colName=c; func=f;}
}
