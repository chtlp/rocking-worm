package parser;
import java_cup.runtime.*;

%%
%class lexer
%public
%line
%column
%ignorecase
%cup

%{
	StringBuffer string = new StringBuffer();
	int count;
	
	private java_cup.runtime.Symbol tok(int kind, Object value) 
	{
		return new java_cup.runtime.Symbol(kind, yychar, yychar+yylength(),value);
	}
		
%}

WhiteSpace =\n|\r|\r\n|\t|\f|\n\r|[ \t\r]

Identifier = [a-zA-Z][a-zA-Z0-9_]*

DecIntegerLiteral = [0-9]+

FloatNumber = [0-9]+"."[0-9]+

%state STRING
%%

  <YYINITIAL> "create" {return tok(sym.CREATE,null);}
  <YYINITIAL> "database" {return tok(sym.DATABASE,null);}
  <YYINITIAL> "use" {return tok(sym.USE,null);}
  <YYINITIAL> "drop" {return tok(sym.DROP,null);}
  <YYINITIAL> "table" {return tok(sym.TABLE,null);}
  <YYINITIAL> "primary" {return tok(sym.PRIMARY,null);}
  <YYINITIAL> "key" {return tok(sym.KEY,null);}
  <YYINITIAL> "KEY" {return tok(sym.KEY,null);}
  <YYINITIAL> "not" {return tok(sym.NOT,null);}
  <YYINITIAL> "null" {return tok(sym.NULL,null);}
  <YYINITIAL> "NULL" {return tok(sym.NULL,null);}
  <YYINITIAL> "default" {return tok(sym.DEFAULT,null);}
  <YYINITIAL> "auto_increment" {return tok(sym.AUTOINCREMENT,null);}
  <YYINITIAL> "int" {return tok(sym.INT,null);}
  <YYINITIAL> "float" {return tok(sym.FLOAT,null);}
  <YYINITIAL> "char" {return tok(sym.CHAR,null);}
  <YYINITIAL> "datetime" {return tok(sym.DATETIME,null);}
  <YYINITIAL> "boolean" {return tok(sym.BOOLEAN,null);}
  <YYINITIAL> "decimal" {return tok(sym.DECIMAL,null);}
  <YYINITIAL> "timestamp" {return tok(sym.TIMESTAMP,null);}
  <YYINITIAL> "varchar" {return tok(sym.VARCHAR,null);}
  <YYINITIAL> "insert" {return tok(sym.INSERT,null);}
  <YYINITIAL> "into" {return tok(sym.INTO,null);}
  <YYINITIAL> "INTO" {return tok(sym.INTO,null);}
  <YYINITIAL> "values" {return tok(sym.VALUES,null);}  
  <YYINITIAL> "delete" {return tok(sym.DELETE,null);}
  <YYINITIAL> "from" {return tok(sym.FROM,null);}
  <YYINITIAL> "where" {return tok(sym.WHERE,null);}
  <YYINITIAL> "update" {return tok(sym.UPDATE,null);}
  <YYINITIAL> "set" {return tok(sym.SET,null);}
  <YYINITIAL> "index" {return tok(sym.INDEX,null);}
  <YYINITIAL> "on" {return tok(sym.ON,null);}
  <YYINITIAL> "select" {return tok(sym.SELECT,null);}
  <YYINITIAL> "distinct" {return tok(sym.DISTINCT,null);}
  <YYINITIAL> "group" {return tok(sym.GROUP,null);}
  <YYINITIAL> "by" {return tok(sym.BY,null);}
  <YYINITIAL> "having" {return tok(sym.HAVING,null);}
  <YYINITIAL> "order" {return tok(sym.ORDER,null);}
  <YYINITIAL> "ASC" {return tok(sym.ASC,null);}
  <YYINITIAL> "desc" {return tok(sym.DESC,null);}
  <YYINITIAL> "AS" {return tok(sym.AS,null);}
  <YYINITIAL> "avg" {return tok(sym.AVG,null);}
  <YYINITIAL> "count" {return tok(sym.COUNT,null);}
  <YYINITIAL> "min" {return tok(sym.MIN,null);}
  <YYINITIAL> "max" {return tok(sym.MAX,null);}
  <YYINITIAL> "sum" {return tok(sym.SUM,null);}
  <YYINITIAL> "exists" {return tok(sym.EXISTS,null);}
  <YYINITIAL> "any" {return tok(sym.ANY,null);}
  <YYINITIAL> "in" {return tok(sym.IN,null);}
  <YYINITIAL> "all" {return tok(sym.ALL,null);}
  <YYINITIAL> "update" {return tok(sym.UPDATE,null);}
  <YYINITIAL> "select" {System.out.println("lexer_select");return tok(sym.SELECT,null);}

  /* separators */
  <YYINITIAL> "("                            { return tok(sym.LPAREN,null); }
  <YYINITIAL> ")"                            { return tok(sym.RPAREN,null); }
  <YYINITIAL> ";"                            { return tok(sym.SEMICOLON,null); }
  <YYINITIAL> ","                            { return tok(sym.COMMA,null); }
  <YYINITIAL> "."                            { return tok(sym.DOT,null); }
      
  /* operators */
  <YYINITIAL> "="                            { return tok(sym.EQ,null); }
  <YYINITIAL> ">"                            { return tok(sym.GT,null); }
  <YYINITIAL> "<"                            { return tok(sym.LT,null); }
  <YYINITIAL> "<="                           { return tok(sym.LTEQ,null); }
  <YYINITIAL> ">="                           { return tok(sym.GTEQ,null); }
  <YYINITIAL> "+"                            { return tok(sym.PLUS,null); }
  <YYINITIAL> "-"                            { return tok(sym.MINUS,null); }
  <YYINITIAL> "*"                            {return tok(sym.MULT,null); }
  <YYINITIAL> "/"                            { return tok(sym.DIV,null); }
  <YYINITIAL> "%"                            { return tok(sym.MOD,null); }
  <YYINITIAL> "AND"                            { return tok(sym.AND,null); }
  <YYINITIAL> "OR"                            { return tok(sym.OR,null); }
  <YYINITIAL> "<>"                           {return tok(sym.NEQ,null);}
    
  /* string literal */
  <YYINITIAL> \' {string.setLength(0);yybegin(STRING);}

  /* number */ 
  <YYINITIAL> {DecIntegerLiteral}            { return tok(sym.NUM,new Integer(yytext())); }
  <YYINITIAL> {FloatNumber}            { return tok(sym.FLOAT,new Float(yytext())); }
  
        
  /* whitespace */
   <YYINITIAL> {WhiteSpace}                   { /* ignore */ }

  /* identifiers */ 
  <YYINITIAL> {Identifier}                   {return tok(sym.ID, yytext()); }  


  /* other situations */
  <YYINITIAL> [^]                            {throw new Error("Illegal character < "+yytext()+" >!");}



<STRING> {
  \'                            { yybegin(YYINITIAL); return tok(sym.STR, string.toString()); }
  [^]                            { string.append( yytext() ); }
}

<<EOF>>                          
    {
     if (yystate()==STRING) throw new Error("String presentation error!");
     return tok(sym.EOF, null);
    }