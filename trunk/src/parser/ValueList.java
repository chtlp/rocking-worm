package parser;

public class ValueList {
	public Value head;
	public ValueList tail;
	public ValueList(Value v, ValueList t) {head=v; tail=t;}
}
