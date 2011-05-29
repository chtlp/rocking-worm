package filesystem;

import java.util.ArrayList;
import java.util.HashMap;

import tlp.util.Debug;
import transaction.Transaction;

public class GlobalStringPool {

	// String longer than the threshold will be stored here
	public static final int LENGTH_THRESHOLD = 80;

	public static final int MAX_LENGTH = 4000;

	static final int ENTRY_PER_INDEX_PAGE = (Page.PAGE_SIZE - Page.HEADER_LENGTH)
			/ Page.INT_SIZE - 1;

	static int rootPageID;

	static ArrayList<Integer> pages = new ArrayList<Integer>();

	static Page lastIndexPage;

	static HashMap<Integer, StringPoolPage> buffer = new HashMap<Integer, StringPoolPage>();

	static Transaction readStrTrans = null;
	public static void init(int root) {
		readStrTrans = Transaction.begin();
		Transaction initTr = FileStorage.newSystemTransaction();
		rootPageID = root;
		pages.clear();
		lastIndexPage = null;
		buffer.clear();
		head = 0;
		
		Page p = BufferManager.getPage(initTr,
				rootPageID);

		if (p.isEmpty()) {
			
			p.setType(initTr, Page.TYPE_STRING_POOL_INDEX);
			p.setNextPage(initTr, -1);
		}

		while (true) {
			p.seek(Page.HEADER_LENGTH);
			// this integer indicates the number of StringPoolPages pointed to
			// in
			// this page
			int num = p.readInt();
			for (int i = 0; i < num; ++i) {
				int k = p.readInt();
				pages.add(k);
			}
			if (p.hasNextPage()) {
				Page q = p;
				p = BufferManager.getPage(initTr,
						p.nextPage());
				q.release(initTr);
			} else
				break;
		}
		p.release(initTr);
		lastIndexPage = p;
		initTr.commit();
	}

	static int head = 0;

	// XXX very coarse parallelism

	public static synchronized String get(int rid) {
		Transaction tr = readStrTrans;
		PageLocator loc = RowIdIndex.get(tr, rid);
		StringPoolPage p = getPage(tr, loc.pageID);
		String ret = p.getString(tr, loc.ind);
		p.release(tr); // unpin
		return ret;
	}

	// XXX search for a page with enough space is linear, too slow, may remember
	// the page with the largest free space with HEAD?
	/**
	 * return the rid of the newly put string
	 */
	public static synchronized int put(Transaction tr, String value) {
		assert value.length() <= MAX_LENGTH;
		if (pages.isEmpty()) {
			addStringPoolPage(tr);
		}

		int k = head;
		while (true) {
			StringPoolPage p = getPage(tr, pages.get(k));

			int ind = p.tryAdd(tr, value);
			p.release(tr);
			if (ind >= 0) {
				head = k;
				return RowIdIndex.add(tr, new PageLocator(p.getPageID(), ind));
			}

			k = k + 1 < pages.size() ? k + 1 : 0;
			if (k == head)
				break;
		}

		// no free space available
		addStringPoolPage(tr);
		head = k = pages.size() - 1;
		StringPoolPage p = getPage(tr, pages.get(k));
		int ind = p.putString(tr, value);
		int rid = RowIdIndex.add(tr, new PageLocator(p.getPageID(), ind));
		p.release(tr);
		return rid;
	}

	static void addStringPoolPage(Transaction tr) {
		lastIndexPage = BufferManager.getPage(tr, lastIndexPage.getPageID());
		int num;
		if ((num = lastIndexPage.readInt(Page.HEADER_LENGTH)) >= ENTRY_PER_INDEX_PAGE) {
			Page i = BufferManager.allocatePage(tr);
			lastIndexPage.setNextPage(tr, i.getPageID());
			i.setType(tr, Page.TYPE_STRING_POOL_INDEX);
			i.writeInt(tr, Page.HEADER_LENGTH, 0);
			i.setNextPage(tr, -1);
			lastIndexPage.release(tr);
			lastIndexPage = i;
			num = 0;
		}
		Page p = BufferManager.allocatePage(tr);
		pages.add(p.getPageID());
		lastIndexPage.writeInt(tr, Page.HEADER_LENGTH, num + 1);
		lastIndexPage.writeInt(tr, Page.HEADER_LENGTH + (1 + num)
				* Page.INT_SIZE, p.getPageID());
		StringPoolPage page = new StringPoolPage(p);
		page.clearContents(tr);
		p.release(tr);
		lastIndexPage.release(tr);
	}

	static StringPoolPage getPage(Transaction tr, int pageID) {
		StringPoolPage sp = buffer.get(pageID);
		if (sp == null) {
			Page p = BufferManager.getPage(tr, pageID);
			sp = new StringPoolPage(p);
			buffer.put(pageID, sp);
		} else
			sp.load(tr, pageID);

		return sp;
	}

	public static synchronized void remove(Transaction tr, int rid) {
		PageLocator loc = RowIdIndex.get(tr, rid);
		StringPoolPage p = getPage(tr, loc.pageID);
		p.removeString(tr, loc.ind);
		p.release(tr);
	}
}
