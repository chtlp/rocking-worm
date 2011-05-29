package parser;

public class DropIndex extends Absyn{
	public String indexName;
	public String tblName;
	public DropIndex(int p,  String i, String t) 
		{pos=p;  indexName=i; tblName=t; };
}
