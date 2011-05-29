package plan;

import java.util.ArrayList;


public class Alia {
	ArrayList<String> colNames = new ArrayList<String>();
	ArrayList<String> tableNames = new ArrayList<String>();
	public void addColName(String colName) {
		colNames.add(colName);
	}
	public void addTableName(String tableName) {
		tableNames.add(tableName);
	}
	public String getName() {
		if (colNames.size() == 0) return "NULL";
		String name = colNames.get(0);
		if (tableNames.size() != 0)
			name = tableNames.get(0) + "." + name;
		return name;
	}
	public boolean check(String name) {
		String tableName = null;
		String colName = name.substring(name.indexOf(".") + 1);
		
		if (name.contains(".")) {
			boolean flag = false;
			tableName = name.substring(0, name.indexOf("."));
			for (int i = 0; i < tableNames.size(); i++)
				if (tableNames.get(i).equals(tableName)) 
					flag = true;
			if (!flag) return false;
		}
		
		for (int i = 0; i < colNames.size(); i++) 
			if (colNames.get(i).equals(colName))
				return true;
		return false;
	}
	
}
