package parser;

public class SubqueryValue extends Value {
	public Select subquery;
	public SubqueryValue(int p, Select s) {pos=p; subquery=s;}
}
