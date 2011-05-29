package parser;

public class Func extends Absyn{
	public int type;
	public Func(int t) { type=t;}
	public final static int AVG=0, COUNT=1, MIN=2, MAX=3, SUM=4;
}