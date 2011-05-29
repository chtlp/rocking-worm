package parser;

public class HasHaving extends Absyn{
	public BoolExpr boolExpr;
	public HasOrder hasOrder;
	public HasHaving(BoolExpr b, Object h) {boolExpr=b; hasOrder=(HasOrder)h;}
}
