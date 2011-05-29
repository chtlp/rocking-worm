package parser;

import java.io.*;

public class Main {
	static public void main(String argv[]) {
		try{
			FileReader FileIn = new FileReader("E:/fatworm/ARNO/02.txt");
			new lexer(FileIn);
//			parser_test p = new parser_test(new lexer(FileIn));
			parser p = new parser(new lexer(FileIn));
			AbsynList result = (AbsynList)p.parse().value;
			new printer(result);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
