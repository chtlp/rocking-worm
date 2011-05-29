package parser;

public class UpdateList extends Absyn{
	public ColName head;
	public Value value;
	public UpdateList tail;
	public UpdateList(int p, ColName c, Value v, UpdateList u) {pos=p; head=c; value=v; tail=u;}
}