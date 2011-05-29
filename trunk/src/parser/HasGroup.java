package parser;

public class HasGroup extends Absyn{
	public ColName cn;
	public HasHaving hasHaving;
	public HasGroup(ColName c, Object h) {cn=c; hasHaving=(HasHaving)h;}
}
