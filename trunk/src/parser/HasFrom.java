package parser;

public class HasFrom extends Absyn{
	public TblRefList trl;
	public HasWhere hasWhere;
	public HasFrom(TblRefList t, Object h) {trl=t; hasWhere=(HasWhere)h;}
}
