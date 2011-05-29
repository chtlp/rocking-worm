package filesystem;

import java.util.ArrayList;
import java.util.LinkedList;

import tlp.util.Debug;
import transaction.Transaction;

class RowIdPageEntry {
	int id;
	boolean full;
}

public class RowIdIndex {

	public static final int VACANT = -1;
	public static final int RESERVED = -2;
	static final int TOTAL_HEADER_LEN = Page.HEADER_LENGTH + 20;

	static final int ENTRY_SIZE = Page.INT_SIZE * 2;
	// one entry will take up 8 bytes (4 for page, 4 for offset)
	static final int ENTRY_PER_INDEX_PAGE = (Page.PAGE_SIZE - TOTAL_HEADER_LEN)
			/ ENTRY_SIZE;

	static int rootPageID;

	static ArrayList<Integer> pages = new ArrayList<Integer>();

	static LinkedList<Integer> notFull = new LinkedList<Integer>();

	static Page lastIndexPage;


	public static void init(int root) {
		Transaction tr = FileStorage.newSystemTransaction();
		rootPageID = root;
		pages.clear();
		notFull.clear();
		lastIndexPage = null;
		
		Page p = BufferManager.getPage(tr,
				rootPageID);

		if (p.isEmpty()) {
			p.setType(tr, Page.TYPE_ROWID_INEX);
			p.setNextPage(tr, -1);
			p.seek(Page.HEADER_LENGTH);
			p.writeInt(tr, 0);

			p.seek(TOTAL_HEADER_LEN);
			for (int i = 0; i < ENTRY_PER_INDEX_PAGE; ++i) {
				p.writeInt(tr, VACANT);
				p.writeInt(tr, VACANT);
			}
		}

		while (true) {
			p.seek(Page.HEADER_LENGTH);
			// # of entries in this index page
			int num = p.readInt();
			pages.add(p.getPageID());
			if (num < ENTRY_PER_INDEX_PAGE)
				notFull.add(pages.size() - 1);

			if (p.hasNextPage()) {
				Page q = p;
				p = BufferManager.getPage(tr,
						p.nextPage());
				q.release(tr);
			} else
				break;
		}
		p.release(tr);
		lastIndexPage = p;
		tr.commit();
	}

	public static PageLocator get(Transaction tr, int rid) {

		Page p = BufferManager.getPage(tr,
				pages.get(rid / ENTRY_PER_INDEX_PAGE));
		p.seek(TOTAL_HEADER_LEN + ENTRY_SIZE * (rid % ENTRY_PER_INDEX_PAGE));
		int page = p.readInt();
		int ind = p.readInt();
		if (page < 0) {
			Debug.fsLogger.error("This RowId is a vacant spot now");
			System.exit(1);
		}
		p.release(tr);
		return new PageLocator(page, ind);
	}

	public static void update(Transaction tr, int rid, PageLocator loc) {

		Page p = BufferManager.getPage(tr,
				pages.get(rid / ENTRY_PER_INDEX_PAGE));
		p.seek(TOTAL_HEADER_LEN + ENTRY_SIZE * (rid % ENTRY_PER_INDEX_PAGE));
		// int page = p.readInt();
		// if (page < 0) {
		// Debug.fsLogger.error("This RowId is a vacant spot now");
		// System.exit(1);
		// }
		assert loc.pageID != VACANT;
		p.writeInt(tr, loc.pageID);
		p.writeInt(tr, loc.ind);

		p.release(tr);
	}

	/**
	 * add a location in the index
	 * 
	 * @param tr
	 *            the current transaction
	 * @param loc
	 *            the location of the item
	 * @return the rid
	 */
	public static int add(Transaction tr, PageLocator loc) {
		if (notFull.isEmpty()) {
			Page p = BufferManager.allocatePage(tr);
			p.setType(tr, Page.TYPE_ROWID_INEX);
			p.writeInt(tr, Page.HEADER_LENGTH, 0);
			p.seek(TOTAL_HEADER_LEN);
			for(int i=0; i<ENTRY_PER_INDEX_PAGE; ++i) {
				p.writeInt(tr, -1);
				p.writeInt(tr, -1);
			}
			
			lastIndexPage = BufferManager.getPage(tr, lastIndexPage.getPageID());
			lastIndexPage.setNextPage(tr, p.getPageID());
			p.setNextPage(tr, -1);
			lastIndexPage.release(tr);
			
			lastIndexPage = p;
			lastIndexPage.release(tr);
			
			pages.add(p.getPageID());
			
			notFull.add(pages.size() - 1);
		}
		
		int i = notFull.remove();
		Page p = BufferManager.getPage(tr, pages.get(i));

		assert p.getType() == Page.TYPE_ROWID_INEX;
		int num = p.readInt(Page.HEADER_LENGTH);
		assert num < ENTRY_PER_INDEX_PAGE;

		int j = 0;
		for (; j < ENTRY_PER_INDEX_PAGE; ++j) {
			p.seek(TOTAL_HEADER_LEN + j * ENTRY_SIZE);
			int x1 = p.readInt();
			p.readInt();

			if (x1 == VACANT) {
				p.seek(TOTAL_HEADER_LEN + j * ENTRY_SIZE);
				p.writeInt(tr, loc.pageID);
				p.writeInt(tr, loc.ind);
				break;
			}
		}
		assert j < ENTRY_PER_INDEX_PAGE;
		++num;
		p.writeInt(tr, Page.HEADER_LENGTH, num);
		if (num < ENTRY_PER_INDEX_PAGE)
			notFull.add(i);

		p.release(tr);
		Debug.testLogger.debug("RowIndex index add rid={}", i * ENTRY_PER_INDEX_PAGE + j);
		return i * ENTRY_PER_INDEX_PAGE + j;

	}
}
