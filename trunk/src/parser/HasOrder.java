package parser;

public class HasOrder extends Absyn{
	public ColNameList colNameList;
	public HasOrder(int p, Object c) {pos=p; colNameList=(ColNameList)c;}
}
