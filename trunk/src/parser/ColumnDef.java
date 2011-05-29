package parser;

public class ColumnDef extends Absyn {
	public DataType dataType;
	public ColName colName;
	public IsNull isNull;
	public ColumnDef(int p, DataType d, ColName c, Object i) 
		{pos=p; dataType=d; colName=c; isNull=(IsNull)i;}
}