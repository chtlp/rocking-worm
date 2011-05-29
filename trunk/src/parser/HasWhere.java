package parser;

public class HasWhere extends Absyn{
	public BoolExpr be;
	public HasGroup hasGroup;
	public HasWhere(BoolExpr b, Object h) {be=b; hasGroup=(HasGroup)h;};
}
