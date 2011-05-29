package parser;

public class Insert extends Absyn {
	public String name;
	public ValueList valueList;
	public Select select;
	public ColNameList colNameList;
	public Insert(int p, String n, ValueList v, Select s, ColNameList c) 
		{pos=p; name=n; valueList=v; select=s; colNameList=c;}
}