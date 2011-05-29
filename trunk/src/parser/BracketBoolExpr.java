package parser;

public class BracketBoolExpr extends BoolExpr{
	public BoolExpr boolExpr;
	public BracketBoolExpr(int p, BoolExpr b) {pos=p; boolExpr=b;}
}
