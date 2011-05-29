package parser;

public class CopValue extends BoolExpr{
	public Value valueL,valueR;
	public Cop cop;
	public CopValue(int p, Value v1, Value v2, Cop c) {pos=p; valueL=v1; valueR=v2; cop=c;}
}
