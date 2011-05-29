package index;

import value.Value;

/**
 * the information of inserting into a BPlus Tree
 * @author TLP
 *
 */
public class InsertionInfo {
	public final int rid; // the rid of the inserted data;
	public final Value partKey;
	public final int partRid;
	public final int pageID;
	
	public InsertionInfo(int r, Value pK, int pR, int page) {
		rid = r;
		partKey = pK;
		partRid = pR;
		pageID = page;
	}
}
