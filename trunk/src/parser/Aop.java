package parser;

public class Aop extends Absyn {
	public int type;
	public Aop(int p, int t) {pos=p; type=t;}
	public final static int PLUS=0, MINUS=1, MULT=2, DIVIDE=3,
	    MOD=4;
}