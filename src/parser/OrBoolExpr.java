package parser;

public class OrBoolExpr extends BoolExpr{
	public BoolExpr left,right;
	public OrBoolExpr(int p, BoolExpr l, BoolExpr r) {pos=p; left=l; right=r;}
}


