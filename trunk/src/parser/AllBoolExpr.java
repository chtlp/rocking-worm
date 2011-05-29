package parser;

public class AllBoolExpr extends BoolExpr{
	public Value value;
	public Cop cop;
	public Select subquery;
	public plan.QueryPlan plan;
	public AllBoolExpr(int p, Value v, Cop c, Select s) {pos=p; cop=c; subquery=s; value=v;}
}
