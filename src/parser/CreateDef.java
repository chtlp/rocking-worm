package parser;
public class CreateDef extends Absyn {
   public ColumnDef cDef;
   public ColName primaryKey;
   public CreateDef(int p, ColumnDef c, ColName cn) {pos=p; cDef=c; primaryKey=cn;}
}