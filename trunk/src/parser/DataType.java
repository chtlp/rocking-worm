package parser;

public class DataType extends Absyn {
	public int type;
	public int charLength;
	public int decimal1;
	public int decimal2;
	public DataType(int t, int l, int i, int j) {type=t; charLength=l;decimal1=i; decimal2=j;}
	public final static int INT=0, FLOAT=1, CHAR=2, DATATIME=3,
	    BOOLEAN=4, DECIMAL1=5, TIMESTAMP=6, VARCHAR=7, DECIMAL2=8;
}