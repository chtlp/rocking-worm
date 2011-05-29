package parser;

public class TblRef extends Absyn{
	public String tblName;
	public String tblNameAlias;
	public Select subquery;
	public String subqueryAlias;
	public TblRef(int p, String t, String s1, Select s, String s2) {
		pos=p; tblName=t; tblNameAlias=s1; subquery=s; subqueryAlias=s2;
	}
}
