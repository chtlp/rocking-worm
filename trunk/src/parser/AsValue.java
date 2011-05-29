package parser;

public class AsValue extends Absyn{
	public Value value;
	public String alias; 
	public AsValue(int p, Value v, String s) {pos=p; value=v; alias=s;}
}
