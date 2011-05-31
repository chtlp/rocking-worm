package table;

import java.util.ArrayList;
import java.util.List;

import filesystem.BufferManager;
import filesystem.Page;

import tlp.util.Debug;
import transaction.Transaction;
import value.Value;

/**
 * store the records in a list of pages
 * 
 * @author TLP
 * 
 */
public class RecordList {

	Transaction tr;
	int recordSize;
	int numPerPage;
	ArrayList<Column> columns;

	int numEntries;

	public int size() {
		return numEntries;
	}

	public RecordList(Transaction tr, ArrayList<Column> col) {
		numEntries = 0;
		this.tr = tr;
		columns = col;
		recordSize = 0;
		for (Column c : col)
			recordSize += c.valueSize();
		recordSize += Page.INT_SIZE; // for the rid
		numPerPage = (Page.PAGE_SIZE - Page.HEADER_LENGTH - Page.INT_SIZE)
				/ recordSize;
		init();
	}

	void setPageNum(Page p, int n) {
		p.writeInt(tr, Page.HEADER_LENGTH, n);
	}

	int getPageNum(Page p) {
		return p.readInt(Page.HEADER_LENGTH);
	}

	void writeRecord(Page p, int ind, Record r) {
		p.seek(Page.HEADER_LENGTH + Page.INT_SIZE + ind * recordSize);
		p.writeInt(tr, r.rowID);
		for (int i = 0; i < r.size(); ++i) {
			Value v = r.getValue(i);
			p.write(tr, v);
		}
	}

	Record readRecord(Page p, int ind) {
		p.seek(Page.HEADER_LENGTH + Page.INT_SIZE + ind * recordSize);
		Record r = new Record();
		r.rowID = p.readInt();
		for (Column c : columns) {
			Value v = p.readValue(c.getType());
			r.addValue(v);
		}
		return r;
	}

	Page head, last;
	int lastNum = 0;

	void init() {
		if (head == null) {
			head = BufferManager.allocatePage(tr);
			head.setType(tr, Page.TYPE_DATA_LIST);
			head.setNextPage(tr, -1);
			last = head;
			setPageNum(last, 0);
			lastNum = 0;
		}
		head.release(tr);
	}

	public void add(Record r) {
		++numEntries;
		last = BufferManager.getPage(tr, last.getPageID());
		if (lastNum == numPerPage) {
			Page p = BufferManager.allocatePage(tr);
			p.setType(tr, Page.TYPE_DATA_LIST);
			p.setNextPage(tr, -1);
			setPageNum(p, 0);
			setPageNum(last, lastNum);
			last.setNextPage(tr, p.getPageID());
			last.release(tr);
			last = p;
			lastNum = 0;
		}
		assert lastNum < numPerPage;
		if (lastNum < numPerPage) {
			writeRecord(last, lastNum++, r);
		}
		setPageNum(last, lastNum);
		last.release(tr);
	}

	public void addAll(List<Record> list) {
		numEntries += list.size();
		last = BufferManager.getPage(tr, last.getPageID());
		for (Record r : list) {
			if (lastNum == numPerPage) {
				Page p = BufferManager.allocatePage(tr);
				p.setType(tr, Page.TYPE_DATA_LIST);
				p.setNextPage(tr, -1);
				setPageNum(p, 0);
				setPageNum(last, lastNum);
				last.setNextPage(tr, p.getPageID());
				last.release(tr);
				last = p;
				lastNum = 0;
			}

			if (lastNum < numPerPage) {
				writeRecord(last, lastNum++, r);
			}
		}
		setPageNum(last, lastNum);
		last.release(tr);
	}

	Page currentPage;
	int currentInd;
	int currentPageNum;

	public void open() {
		currentPage = head;
		currentPage = BufferManager.getPage(tr, currentPage.getPageID());
		currentPageNum = getPageNum(currentPage);
		currentInd = 0;
		assert currentPage != null;
	}

	Record buffer = null;

	public void loadBuffer() {
		if (buffer != null)
			return;

		if (currentInd == currentPageNum && currentPage.hasNextPage()) {
			currentPage.release(tr);
			// assume one record list is used by at most one transaction
			assert currentPage.pinned.get() == 0;
			currentPage = BufferManager.getPage(tr, currentPage.nextPage());
			assert currentPage != null;
			currentInd = 0;
			currentPageNum = getPageNum(currentPage);
		}

		if (currentInd == currentPageNum) {
			return;
		}

		buffer = readRecord(currentPage, currentInd++);
	}

	public Record peek() {
		if (buffer == null)
			loadBuffer();
		return buffer;
	}

	public Record next() {
		if (buffer == null)
			loadBuffer();
//		Debug.testJoin.debug("RecordList current page {}",
//				currentPage.getPageID());
		Record ret = buffer;
		buffer = null;
		return ret;
	}

	public void removeFront() {
		if (buffer == null)
			loadBuffer();
		assert buffer != null;
		buffer = null;
	}

	public void free() {
		if (currentPage != null)
		currentPage.release(tr);

		Page p = BufferManager.getPage(tr, head.getPageID());

		while (true) {
			int n = p.nextPage();
			BufferManager.free(tr, p.getPageID());
//			Debug.testJoin.debug("ReordList frees page {}", p.getPageID());
			if (n == -1)
				break;
			p = BufferManager.getPage(tr, n);
		}
	}

}
