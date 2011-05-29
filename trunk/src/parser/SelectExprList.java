package parser;

public class SelectExprList {
	public SelectExpr head;
	public SelectExprList tail;
	public SelectExprList(SelectExpr h, SelectExprList t) {head=h; tail=t;}
}
