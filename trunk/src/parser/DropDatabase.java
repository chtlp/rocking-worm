package parser;
public class DropDatabase extends Absyn {
   public String dbName;
   public DropDatabase(int p, String s) {pos=p; dbName=s;}
}
