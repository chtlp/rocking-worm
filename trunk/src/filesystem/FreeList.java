package filesystem;

import java.util.BitSet;

import tlp.util.Debug;
import transaction.Transaction;

/**
 * 1 bit for each page; if page size = 2KB, and file is 4GB, FreeList would have
 * 2MBit = 256KB = 64 pages So it can be represented in memory by an array of
 * 128 pages. For simplicity, I would load it all together
 * 
 * @author TLP
 * 
 */
public class FreeList {

	static int pages[] = new int[100];
	static int numIndexPages = 0;

	static final int TOTAL_HEADER_LEN = Page.HEADER_LENGTH + 20;
	static final int ENTRY_PER_PAGE = Page.PAGE_SIZE - TOTAL_HEADER_LEN;

	static final int NUM_INCREMENT = 1 * 8;

	static final int MARKER = 987;

	// true ==> not free, false ==> free
	static BitSet status = new BitSet();

	static int rootPageID;
	
	static int totalSlots;

	public static void init(int root) {
		if (Debug.D)
			Debug.fsLogger.debug("Free List initialize");
		rootPageID = root;
		totalSlots = 0;
		status.clear();

		Transaction tr = FileStorage.newSystemTransaction();
		Page p = BufferManager.getPage(tr,
				rootPageID);

		numIndexPages = 0;
		int k = 0;

		// properly init the root page
		if (p.isEmpty()) {
//			Transaction tr = FileStorage.newSystemTransaction();
			p.setType(tr, Page.TYPE_FREE_LIST);
			p.setNextPage(tr, -1);
			assert NUM_INCREMENT == 8 && NUM_INCREMENT >= FileStorage.ROOT_META_PAGES;
			FileStorage.increasePages(NUM_INCREMENT - FileStorage.totalPages());
			
			
			
			p.writeInt(tr, TOTAL_HEADER_LEN - 4, NUM_INCREMENT);
			// five pages taken up by root meta data
			p.writeBytes(
					tr,
					TOTAL_HEADER_LEN,
					new byte[] { (byte) ((1 << FileStorage.ROOT_META_PAGES) - 1) },
					0, 1);
		}

		while (true) {
			p.seek(TOTAL_HEADER_LEN - 4);
			// entries in this page
			int n = p.readInt();
			totalSlots += n;
			for (int i = 0; i <= n / 8; ++i) {
				int b = p.readByte();
				for (int j = 0; j < 8 && i*8+j < n; ++j) {
					status.set(k++, (b & 1) > 0);
					b = b >> 1;
				}
			}
			pages[numIndexPages++] = p.getPageID();
			if (p.hasNextPage()) {
				Page q = p;
				p = BufferManager.getPage(tr,
						p.nextPage());
				q.release(tr);
			} else
				break;
		}
		
		tr.commit();

	}

	static int pointer = 0;

	/**
	 * allocate a new page, return the pageID (i.e. page offset in the database
	 * file)
	 */
	public static synchronized int allocate(Transaction tr) {
		int pointer0 = pointer;
		pointer = status.nextClearBit(pointer0);
		// no free page
		if (pointer >= totalSlots) {
			addPages(tr, NUM_INCREMENT);
			totalSlots += NUM_INCREMENT;
		}

		pointer = status.nextClearBit(pointer0);
		assert pointer >= 0;
		status.set(pointer);
		Page p = BufferManager.getPage(tr, pages[pointer / ENTRY_PER_PAGE]);
		int b = pointer % ENTRY_PER_PAGE;
		byte seg = p.readBytes(TOTAL_HEADER_LEN + b / 8, 1)[0];
		seg = (byte) (seg ^ (1 << (b % 8)));
		p.writeBytes(tr, TOTAL_HEADER_LEN + b / 8, new byte[] { seg }, 0, 1);
		p.release(tr);

		assert p.pinned.get() >= 0;
		return pointer;

	}

	/**
	 * add new pages
	 * 
	 * @param tr
	 * @param numIncrement
	 */
	private static void addPages(Transaction tr, int numIncrement) {
		assert numIncrement % 8 == 0;

		int currentPages = FileStorage.totalPages();
		FileStorage.increasePages(numIncrement);

		int i = 0;
		while (i < numIncrement) {
			Page p;
			boolean newIndexPage = false;
			if (currentPages % ENTRY_PER_PAGE == 0) {
				// need to use a new page
				pages[numIndexPages] = currentPages;
				p = BufferManager.getPage(tr, numIndexPages);
				p.writeInt(tr, TOTAL_HEADER_LEN-4, 0);
				p.setNextPage(tr, -1);
				status.set(currentPages);
				
				if (numIndexPages > 0) {
					Page q = BufferManager
							.getPage(tr, pages[numIndexPages - 1]);
					q.setNextPage(tr, p.getPageID());
					q.release(tr);
				}
				++numIndexPages;
				newIndexPage = true;
			} else
				p = BufferManager.getPage(tr, pages[numIndexPages - 1]);
			int c = p.readInt(TOTAL_HEADER_LEN - 4);
			int k = Math.min(numIncrement - i, ENTRY_PER_PAGE - c);
			assert k == NUM_INCREMENT;
			byte[] b = new byte[k / 8];
			b[0] = (byte) (newIndexPage ? 1 : 0);
			p.writeInt(tr, TOTAL_HEADER_LEN - 4, c + k);
			p.writeBytes(tr, TOTAL_HEADER_LEN + c / 8, b, 0, b.length);

			i += k;
			currentPages += k;
			p.release(tr);
			
			assert p.pinned.get() >= 0;
		}
	}

	public static synchronized void free(Transaction tr, int pageID) {
		status.set(pageID, false);
		pointer = Math.min(pointer, pageID);
		
		Page p = BufferManager.getPage(tr, pages[pageID / ENTRY_PER_PAGE]);
		
		int pos = TOTAL_HEADER_LEN + pageID % ENTRY_PER_PAGE / 8;
		
		p.seek(pos);
		byte b = p.readByte();
		
		b = (byte)(b ^ (1 << pageID % ENTRY_PER_PAGE % 8));
		
		p.seek(pos);
		p.writeByte(tr, b);
		
		p.release(tr);
	}

}

