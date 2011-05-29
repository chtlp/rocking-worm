package parser;

public class ExistBoolExpr extends BoolExpr{
	public boolean isExist;
	public Select subquery;
	public plan.QueryPlan plan;
	public ExistBoolExpr(int p, Select s, boolean b) {isExist=b; pos=p; subquery=s;} 
}
