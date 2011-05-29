package parser;

public class AndBoolExpr extends BoolExpr{
	public BoolExpr left,right;
	public AndBoolExpr(int p, BoolExpr l, BoolExpr r) {pos=p; left=l; right=r;}
}

