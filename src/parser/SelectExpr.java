package parser;

public class SelectExpr extends Absyn{
	public Value value;
	public String alias;
	public boolean selectAll;
	public SelectExpr(int p, Value v, String s, boolean b) {pos=p; value=v; alias=s; selectAll=b;}
}
