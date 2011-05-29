package filesystem;

import tlp.util.Debug;
import transaction.Transaction;

public class StringPoolPage {
	// a page has 5KB, and the threshold for a long string is 80, so one page
	// here stores about 50 strings
	public final static int MAX_NUM_STRING = 50;

	public final static int SPAGE_HEADER_SIZE = MAX_NUM_STRING * Page.INT_SIZE
			* 2;

	public final static int TOTAL_HEADER_LEN = MAX_NUM_STRING
			+ SPAGE_HEADER_SIZE;

	Page content;

	public StringPoolPage() {
		content = null;
	}

	public StringPoolPage(Page p) {
		content = p;
	}

	public int getPageID() {
		return content.getPageID();
	}

	public void clearContents(Transaction tr) {
		content.seek(Page.HEADER_LENGTH);
		for (int i = 0; i < MAX_NUM_STRING; ++i) {
			content.writeInt(tr, -1);
			content.writeInt(tr, -1);
		}
	}

	public int getfreeSpace() {
		int totalLen = 0;
		content.seek(Page.HEADER_LENGTH);
		boolean ok = false;
		for (int i = 0; i < MAX_NUM_STRING; ++i) {
			int pos = content.readInt();
			int len = content.readInt();
			if (pos >= 0) {
				totalLen += len;
			} else
				ok = true;
		}
		return ok ? Page.PAGE_SIZE - Page.HEADER_LENGTH - SPAGE_HEADER_SIZE
				- totalLen : -1;
	}

	// note that store only modifies buffer, don't care when to write back to
	// disk
	public int putString(Transaction tr, String s) {
		int q = 0;
		int head[] = new int[MAX_NUM_STRING];
		int len[] = new int[MAX_NUM_STRING];

		content.seek(Page.HEADER_LENGTH);

		for (int i = 0; i < MAX_NUM_STRING; ++i) {
			int pos = content.readInt();
			int l = content.readInt();
			if (pos >= 0) {
				len[q] = l;
				head[q++] = pos;
			}
		}

		if (q == 0) {
			return putAt(tr, TOTAL_HEADER_LEN, s, -1);

		}

		for (int i = 1; i < q; ++i) {
			int x = head[i], y = len[i], j;
			for (j = i - 1; j >= 0; --j) {
				if (head[j] > x) {
					len[j + 1] = len[j];
					head[j + 1] = head[j];
				} else
					break;
			}
			len[j + 1] = y;
			head[j + 1] = x;
		}

		for (int i = 0; i < q; ++i) {
			int t = i + 1 < q ? head[i + 1] : Page.PAGE_SIZE;
			if (t - head[i] - len[i] >= s.length()) {
				return putAt(tr, head[i] + len[i], s, -1);
			}
		}

		// this can always be done with no harm
		int pos = arrange(tr);
		return putAt(tr, pos, s, -1);

	}

	public void removeString(Transaction tr, int ind) {
		content.seek(Page.HEADER_LENGTH + SPAGE_HEADER_SIZE + ind
				* Page.INT_SIZE * 2);
		content.writeInt(tr, -1);
	}

	private int arrange(Transaction tr) {
		int top = 0;
		byte buffer[] = new byte[Page.PAGE_SIZE];
		content.seek(Page.HEADER_LENGTH);
		for (int i = 0; i < MAX_NUM_STRING; ++i) {
			int pos = content.readInt();
			int len = content.readInt();
			content.copyBytes(pos, buffer, top, len);
			content.seek(Page.HEADER_LENGTH + SPAGE_HEADER_SIZE + i
					* Page.INT_SIZE * 2);
			content.writeInt(tr, top);
			top += len;
			content.seek(Page.HEADER_LENGTH + SPAGE_HEADER_SIZE + (i + 1)
					* Page.INT_SIZE * 2);
		}
		content.writeBytes(tr, Page.HEADER_LENGTH + SPAGE_HEADER_SIZE, buffer,
				0, top);
		return Page.HEADER_LENGTH + SPAGE_HEADER_SIZE + top;
	}

	private int putAt(Transaction tr, int pos, String s, int ind) {
		content.seek(Page.HEADER_LENGTH);
		int i = 0;

		if (ind < 0) {
			for (; i < MAX_NUM_STRING; ++i) {
				int p = content.readInt(Page.HEADER_LENGTH + 2 * Page.INT_SIZE
						* i);
				if (p < 0)
					break;
			}

			if (i == MAX_NUM_STRING)
				throw new PageOverflowException(
						"no vacant index space for new strings");
			ind = i;

		}
		assert content.readInt(Page.HEADER_LENGTH + 2 * Page.INT_SIZE * i) < 0;
		content.seek(Page.HEADER_LENGTH + ind * Page.INT_SIZE * 2);
		
		content.writeInt(tr, pos);
		content.writeInt(tr, s.length());
		content.writeBytes(tr, pos, s.getBytes(), 0, s.length());
		
		return ind;

	}

	public String getString(Transaction tr, int ind) {
		content.seek(Page.HEADER_LENGTH + ind
				* Page.INT_SIZE * 2);
		int pos = content.readInt();
		int len = content.readInt();
		byte[] d = content.readBytes(pos, len);
		return new String(d);
	}

	public void release(Transaction tr) {
		content.release(tr);
	}

	public void load(Transaction tr, int pageID) {
		content = BufferManager.getPage(tr, pageID);
	}

	Integer freeSpace = null;
	Integer top = null;
	Integer numEntries = null;
	int k = 0;

	public int tryAdd(Transaction tr, String value) {
		if (freeSpace == null || top == null || numEntries == null) {
			analysis();
		}
		if (freeSpace < value.length() || numEntries >= MAX_NUM_STRING)
			return -1;

		if (top + value.length() > Page.PAGE_SIZE) {
			arrange(tr);
			analysis();
		}

		assert top + value.length() <= Page.PAGE_SIZE;

		for (int i = 0; i < MAX_NUM_STRING; ++i) {
			int j = k;
			k = (k + 1) % MAX_NUM_STRING;

			int off = Page.HEADER_LENGTH + j * 2 * Page.INT_SIZE;
			int pos = content.readInt(off);
			if (pos < 0) {
				content.writeInt(tr, off, top);
				content.writeInt(tr, off + Page.INT_SIZE, value.length());
				content.writeBytes(tr, top, value.getBytes(), 0, value.length());
				
				numEntries++;
				top += value.length();
				freeSpace -= value.length();
				return j;
			}
		}

		throw new ArrayIndexOutOfBoundsException(
				"long string insertion failed on " + Debug.showStr(value));
	}

	private void analysis() {
		numEntries = 0;
		freeSpace = Page.PAGE_SIZE - TOTAL_HEADER_LEN;
		top = TOTAL_HEADER_LEN;
		for (int i = 0; i < MAX_NUM_STRING; ++i) {
			int offset = Page.HEADER_LENGTH + 2 * i * Page.INT_SIZE;
			int pos = content.readInt(offset);
			int len = content.readInt(offset + Page.INT_SIZE);
			if (pos >= 0) {
				++numEntries;
				freeSpace -= len;
				top = Math.max(top, pos + len);
			} else
				k = i;
		}
	}
}
