package parser;

public class ColNameList {
	public ColName colName;
	public boolean isASC,isDESC;
	public ColNameList tail;
	public ColNameList(ColName c, boolean b1, boolean b2, ColNameList cl)
		{colName=c; isASC=b1; isDESC=b2; tail=cl;}
}
