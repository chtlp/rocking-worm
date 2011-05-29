package parser;
public class Delete extends Absyn {
   public String tblName;
   public BoolExpr whereCondition;
   public Delete(int p, String t, BoolExpr w) {pos=p; tblName=t; whereCondition=w;}
}
