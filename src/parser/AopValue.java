package parser;

public class AopValue extends Value{
	public Value valueL;
	public Value valueR;	
	public int ty;
	public AopValue(int p, Value v1, int p2, Value v2) {pos=p; valueL=v1; ty=p2; valueR=v2;}
}
