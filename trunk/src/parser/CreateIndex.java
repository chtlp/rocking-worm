package parser;

public class CreateIndex extends Absyn{
	public boolean isUnique;
	public String indexName;
	public String tblName;
	public ColName colName;
	public CreateIndex(int p, boolean b, String i, String t, ColName c) 
		{pos=p; isUnique=b; indexName=i; tblName=t; colName=c;};
}
