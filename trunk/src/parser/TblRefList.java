package parser;

public class TblRefList {
	public TblRef head;
	public TblRefList tail;
	public TblRefList(TblRef tr, TblRefList t) {head=tr; tail=t;}
}

