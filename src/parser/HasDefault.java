package parser;

public class HasDefault extends Absyn{
	public ConstValue constValue;
	public IsAuto isAuto;
	public HasDefault(ConstValue c, Object i) {constValue=c; isAuto=(IsAuto)i;}
}
