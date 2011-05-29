package parser;
import java_cup.runtime.*;

%%
%class lexer
%public
%line
%column

%cup

%{
	StringBuffer string = new StringBuffer();
	int count;
	
	private java_cup.runtime.Symbol tok(int kind, Object value) 
	{
		return new java_cup.runtime.Symbol(kind, yychar, yychar+yylength(),value);
	}
		
%}

WhiteSpace =\n|\r|\r\n|\t|\f|\n\r

Identifier = [a-zA-Z][:a-zA-Z0-9_:]*

DecIntegerLiteral = [0-9]+

FloatNumber = [0-9]+.[0-9]+

Date = [0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]

Time = [0-9][0-9]:[0-9][0-9]:[0-9][0-9]

%%


  <YYINITIAL> 
  "SELECT" { System.out.println("SELECT%%");return tok(sym.SELECT,null);}


  /* other situations */
  [^]                            {throw new Error("Illegal character < "+yytext()+" >!");}


<<EOF>>                          
    {
      return tok(sym.EOF, null);
    }