package parser;

public class AnyBoolExpr extends BoolExpr{
	public Value value;
	public Cop cop;
	public Select subquery;
	public plan.QueryPlan plan;
	public AnyBoolExpr(int p, Value v, Cop c, Select s) {pos=p; cop=c; subquery=s; value=v;}
}
