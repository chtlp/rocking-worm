package parser;

public class InBoolExpr extends BoolExpr{
	public Value value;
	public Select subquery;
	public plan.QueryPlan plan;
	public InBoolExpr(int p, Value v, Select s) {pos=p; subquery=s; value=v;}
}
