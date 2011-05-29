package parser;

public class Update extends Absyn{
	public String tblName;
	public UpdateList updateList;
	public BoolExpr boolExpr;
	public Update(int p, String t,UpdateList u,BoolExpr b) {pos=p; tblName=t; updateList=u; boolExpr=b;}
}
