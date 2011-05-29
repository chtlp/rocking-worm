package plan;

import java.util.ArrayList;


public class Col2Idx {
	public 	int getIdx(ArrayList<Alia> alias, parser.ColName col) {
		String tblName = col.tblName;
		String colName = col.colName;
		String name = colName;
		if (tblName != null) name = tblName + "." + name;
		int idx = 0;
		while (idx < alias.size()) {
			if (alias.get(idx).check(name))
				return idx;
			idx++;
		}
		
		return -1;
	}
}
