package parser;

public class ColName extends Absyn {
	public String tblName;
	public String colName;

	public ColName(String t, String s) {
		tblName = t;
		colName = s;
		colName = colName.toLowerCase();
	}
}
