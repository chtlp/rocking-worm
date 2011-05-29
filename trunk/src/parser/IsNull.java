package parser;

public class IsNull extends Absyn{
	public int isNull;
	public HasDefault hasDefault;
	public IsNull(int i, Object h) {isNull=i; hasDefault=(HasDefault)h;}
}
