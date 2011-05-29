package parser;

public class Cop  {
	public int type;
	public Cop(int t) {type=t;}
	public final static int Lt=0, Gt=1, Eq=2, LtEq=3,
	    GtEq=4, NotEq=5;
}