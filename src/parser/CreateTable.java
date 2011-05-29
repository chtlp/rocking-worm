package parser;
public class CreateTable extends Absyn {
   public String tlbName;
   public CreateDefList cDefList;
   public CreateTable(int p, String s, CreateDefList cd) {pos=p; tlbName=s; cDefList=cd;}
}

