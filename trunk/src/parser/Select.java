package parser;
public class Select extends Absyn{
	public SelectExprList selectExprList;
	public boolean isDistinct;
	public HasFrom hasFrom;
	public Select(SelectExprList s, boolean b, Object h) 
	{selectExprList=s; isDistinct=b; hasFrom=(HasFrom)h;}
}