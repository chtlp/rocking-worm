package index;

import java.io.PrintStream;

import table.Column;
import table.Record;
import table.Table;
import table.TableIterator;
import tlp.util.Debug;
import transaction.Transaction;
import value.Value;
import filesystem.BufferManager;
import filesystem.FileStorage;
import filesystem.Page;
import filesystem.PageLocator;
import filesystem.RowIdIndex;

public class RangeIterator implements TableIterator{

	BPlusIndex index;
	Transaction tr;
	
	Value v, lv, rv;
	Integer left, right;
	boolean equalRange;
	
	public RangeIterator(Transaction tr, Table t, Value v) {
		this.tr = tr;
		index = t.getScanIndex(tr);
		this.v = v;
		equalRange = true;
	}
	
	public RangeIterator(Transaction tr, Table t, Integer left, Value lv, Integer right, Value rv) {
		this.tr = tr;
		index = t.getScanIndex(tr);
		this.left = left;
		this.lv = lv;
		this.right = right;
		this.rv = rv;
		equalRange = false;
	}
	
	
	@Override
	public int numColumns() {
		return index.table.numColumns();
	}

	@Override
	public Column getColumn(int i) {
		return index.table.getColumn(i);
	}

	Page iterPage = null;
	int iterInd = -1;
	@Override
	public void open() {
		iterPage = null;
		iterInd = -1;
		BPlusNode root = BPlusNode.loadNode(tr, index, index.treeRoot);
		PageLocator loc = null;
		if (equalRange) {
			loc = root.search(tr, v, -1, Index.SEARCH_AT);
			rv = v;
			right = Index.SEARCH_AT;
			lv = v;
			left = Index.SEARCH_AT;
		}
		else if (left != null) {
			loc = root.search(tr, lv, -1, left);
		}
		else { // left == null
			loc = root.search(tr, null, -1, Index.SEARCH_AT);
		}
		
		if (loc != null && loc.pageID >= 0 && loc.ind >= 0) {
			iterPage = BufferManager.getPage(tr, loc.pageID);
			iterInd = loc.ind;
		}
		
	}

	@Override
	public boolean hasNext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Record next() {
		while(true) {
			Record r = next1();
			if (r == null) return null;
			if (checkLeftBoundary(r)) return r;
			// continue
		}
	}
	
	
	public Record next1() {
		if (iterPage == null)
			return null;
		Debug.testLogger.debug("RangeIterator iter page = {}",
				iterPage.getPageID());
		if (iterInd != -1) {
			Record ret = null;
			if (index.isPrimaryIndex()) {
				ret = index.table.getRecord(tr, new PageLocator(iterPage.getPageID(),
						iterInd));
			} else {
				int offset = BPlusNode.TOTAL_HEADER_LEN + index.leafEntrySize
						* iterInd + 2;
				PageLocator loc = RowIdIndex.get(tr, iterPage.readInt(offset));
				ret = index.table.getRecord(tr, loc);
			}
			iterInd = iterPage.readByte(BPlusNode.TOTAL_HEADER_LEN
					+ index.leafEntrySize * iterInd + 1);
			
			if (!checkRightBoundary(ret)) {
				iterPage.release(tr);
				iterPage = null;
				return null;
			}
			return ret;
		} else {
			if (iterPage.hasNextPage()) {
				Page q = BufferManager.getPage(tr, iterPage.nextPage());
				iterPage.release(tr);
				iterPage = q;
				iterInd = iterPage.readByte(Page.HEADER_LENGTH + 1);
			} else
				iterPage = null;
			return next();
		}
	}

	private boolean checkLeftBoundary(Record r) {
		if (left == null) return true;
		Value recValue = r.getValue(index.columnID);
		if (left == Index.SEARCH_AT) {
			return (recValue == null && lv == null) || (recValue != null && recValue.compareTo(lv) >= 0);
		}
		else if (left == Index.SEARCH_AFTER) {
			return (recValue != null && recValue.compareTo(lv) > 0);
		}
		
		throw new UnsupportedOperationException();
	}

	private boolean checkRightBoundary(Record r) {
		if (right == null) return true;
		Value recValue = r.getValue(index.columnID);
		if (right == Index.SEARCH_AT) {
			return recValue == null || recValue.compareTo(rv) <= 0;
		}
		else if (right == Index.SEARCH_BEFORE) {
			return (recValue == null && rv != null) || (recValue != null && recValue.compareTo(rv) < 0);
		}
		
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
		if (iterPage != null)
			iterPage.release(tr);
		iterInd = -1;
		iterPage = null;
	}

	@Override
	public void print(PrintStream out) {
		Transaction tr = FileStorage.newSystemTransaction();
		out.println(" *** Range Iterator ***");
		for (open();;) {
			Record r = next();
			if (r == null) break;
			out.println(r);
		}

		tr.commit();		
	}

}
