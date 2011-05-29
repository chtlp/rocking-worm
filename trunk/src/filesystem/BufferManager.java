package filesystem;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.TreeSet;

import logging.LogManager;

import tlp.util.Debug;
import transaction.Transaction;

class BufferedBlock {
	byte[] data = new byte[Page.PAGE_SIZE];
	boolean used = false;
}

public class BufferManager {

	public static final int MAX_BUFFERED_PAGES = 40000;
	public static final int NUM_BLOCKS = MAX_BUFFERED_PAGES;

	// static BufferedBlock blocks[] = new BufferedBlock[NUM_BLOCKS];
	// static HashSet<Integer> avail = new HashSet<Integer>();

	static HashMap<Integer, Page> buffer = new HashMap<Integer, Page>();

	static Page head = null, tail = null;

	// public static void init() {
	// for(int i=0; i<NUM_BLOCKS; ++i) {
	// avail.add(i);
	// }
	// }

	private static void addToBufferLinkedList(Page p) {
		Debug.bufferLogger.debug("add to buffer list: {}", p.getPageID());
		if (head == null) {
			head = p;
			head.next = head;
			tail = head;
		} else {
			tail.next = p;
			p.next = head;
			head = p;
		}
		assert tail.next == head;
	}

	// note: need to unpin
	public static synchronized Page allocatePage(Transaction tr) {
		int id = FreeList.allocate(tr);

		Page p = new Page(id);
		p.setData(new byte[Page.PAGE_SIZE]);
		p.pinned.set(1);
		p.pinners.add(tr);
		assert p.pinners.size() == p.pinned.get();
		p.refBit.set(true);

		Debug.fsLogger.debug("allocate page {}, buffer size = {}", id,
				buffer.size());

		assert buffer.get(id) == null : id;
		if (buffer.size() >= MAX_BUFFERED_PAGES) {
			replacePage();
		}

		buffer.put(id, p);
		addToBufferLinkedList(p);

		if (Debug.testLight2.isDebugEnabled())
			checkBufferStatus();
		return p;
	}

	public static synchronized Page getPage(Transaction tr, int pageID) {
		Page p = buffer.get(pageID);
		if (p != null) {
			p.pinned.getAndIncrement();
			p.pinners.add(tr);
			return p;
		}
		if (buffer.size() >= MAX_BUFFERED_PAGES) {
			replacePage();
		}

		p = new Page(pageID);
		loadPage(tr, p);

		Debug.fsLogger.debug("Load page {}, buffer size = {}", pageID,
				buffer.size());

		assert buffer.size() < MAX_BUFFERED_PAGES;

		buffer.put(pageID, p);
		addToBufferLinkedList(p);
		p.pinned.getAndIncrement();
		p.pinners.add(tr);
		assert p.pinners.size() == p.pinned.get();
		p.refBit.set(true);

		if (Debug.testLight2.isDebugEnabled())
			checkBufferStatus();
		return p;
	}

	/**
	 * if the page has been swapped, load it into buffer, otherwise do nothing
	 * 
	 * @param tr
	 * @param page
	 */
	private static synchronized void loadPage(Transaction tr, Page page) {
		if (!page.isDisposed())
			return;

		Debug.bufferLogger.debug("start loading page {}", page.getPageID());
		byte[] b = FileStorage.readPage(page.getPageID());
		page.setData(b);
		Debug.bufferLogger.debug("page {} loaded", page.getPageID());
	}

	/**
	 * the page replacement algorithm
	 */
	public static synchronized void replacePage() {
		if (buffer.size() < MAX_BUFFERED_PAGES) {
			Debug.fsLogger.debug("no need to replace pages.");
			return;
		}
		Debug.fsLogger.debug("page replacement - clock algorithm");

		boolean replaced = false;
		for (int i = 0; i < buffer.size() * 2; ++i) {
			assert tail.next == head;
			Page p = (Page) head.next;
			// this loop is guaranteed to end, since there are
			// max_buffered_pages
			while (head.valid == false) {
				head = (Page) head.next;
				tail.next = head;
			}
			Debug.fsLogger.debug("clock cycle at page {}", p.getPageID());
			if (p.pinned.get() == 0 && p.refBit.get() == false) {
				if (head.next != head) {
					head.next = p.next;
					assert tail.next == head;
				} else {
					head = null;
					tail = null;
				}
				p.dispose();
				buffer.remove(p.getPageID());
				replaced = true;
				Debug.fsLogger.debug("{} replaced", p.getPageID());
				break;
			}

			p.refBit.set(false);
			tail = head;
			head = p;
		}

		if (!replaced) {
			Debug.fsLogger.error("no page ready for replacement");
			printBufferStatus(System.out);
			throw new OutOfBufferSpaceException();
		}

	}

	private static void printBufferStatus(PrintStream out) {
		out.printf("buffer status(size = %d)\n", buffer.size());
		for (Page p : buffer.values()) {
			out.println(p);
		}
	}

	public static void checkBufferStatus() {
		TreeSet<Integer> set = new TreeSet<Integer>();
		Page iter = head;
		do {
			if (iter.valid) {
				assert !set.contains(iter.getPageID());
				set.add(iter.getPageID());
			}
			iter = (Page) iter.next;
			
			if (Debug.testLight2.isDebugEnabled() && iter.valid) {
				assert buffer.values().contains(iter);
			}
		} while(iter != head);
		
		Debug.testLight2.debug("cycle length: {}, buffer size: {}",
				set.size(), buffer.size());
		if (set.size() != buffer.size()) {
			Debug.testLight2.debug("cycle: {}", set);
			Debug.testLight2.debug("buffer: {}", new TreeSet<Integer>(buffer.keySet()));
		} else {
			Debug.testLight2.debug("cycle: {}", set);
		}
		assert set.size() == buffer.size();
		for (Page p : buffer.values())
			assert set.contains(p.getPageID());
	}

	public static void free(Transaction tr, int pageID) {
		Debug.bufferLogger.debug("free page {}", pageID);
		Page p = buffer.get(pageID);
		p.setType(tr, Page.TYPE_EMPTY);
		p.release(tr);
		assert p.valid;
		p.valid = false;
		buffer.remove(pageID);

		FreeList.free(tr, pageID);
		
		checkBufferStatus();
	}

	// called by the log manager to flush all the dirty pages
	public static void flushAll() {
		Debug.bufferLogger.debug("flush all buffers");
		for(Page p : buffer.values()) {
			p.writeBack();
		}
		LogManager.flushAll();
	}
	
	public static void cleanUp(Transaction tr) {
		for(Page p : buffer.values()) {
			while(p.pinners.remove(tr)) {
				p.pinned.getAndDecrement();
			}
			assert p.pinners.size() == p.pinned.get();
		}
		
	}

}
