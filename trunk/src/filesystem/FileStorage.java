package filesystem;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;

import table.TableManager;
import tlp.util.Debug;
import transaction.Transaction;

public class FileStorage {

	public static final int ROOT_MARKER = 14342;
	
	public static final int SUPER_ROOT = 0;

	public static final int FREE_LIST_ROOT = 1;

	public static final int ROW_INDEX_ROOT = 2;

	public static final int STRING_POOL_INDEX_ROOT = 3;

	public static final int TABLE_INDEX_ROOT = 4;

	public static final int ROOT_META_PAGES = 5;

	// TODO used to perform some system tasks
	public static Transaction newSystemTransaction() {
		return Transaction.begin();
	}
	
//	public static Transaction readTransaction() {
//		return Transaction.begin();
//	}

	static String name;

	static RandomAccessFile file;

	static FileChannel channel;

	static long length;
	
	public static void loadFile(String fileName) throws IOException {
		name = fileName;
		file = new RandomAccessFile(fileName, "rw");
		channel = file.getChannel();
		length = file.length();

		if (length % Page.PAGE_SIZE != 0
				|| length < ROOT_META_PAGES * Page.PAGE_SIZE) {
			FileStorage.setFileLength(Page.PAGE_SIZE * ROOT_META_PAGES);
		}
	}

	// in this method, BufferManager, FreeList, GlobalStringPool should all be
	// initialized
	// they all reside in one file
	public static void init(){

		Debug.init();
		FreeList.init(FREE_LIST_ROOT);
		RowIdIndex.init(ROW_INDEX_ROOT);
		GlobalStringPool.init(STRING_POOL_INDEX_ROOT);
		TableManager.init(TABLE_INDEX_ROOT);
		
	}
	
	public static void cleanUp(Transaction tr) {
		FreeList.init(FREE_LIST_ROOT);
		GlobalStringPool.init(STRING_POOL_INDEX_ROOT);
		RowIdIndex.init(ROW_INDEX_ROOT);
		BufferManager.cleanUp(tr);
	}

	/**
	 * 
	 * @return the number of total pages
	 */
	public static int totalPages() {
		return (int) (length / Page.PAGE_SIZE);
	}

	/**
	 * increase the number of pages
	 * 
	 * @param num
	 *            how many new pages will be added
	 * @throws IOException
	 */
	public static void increasePages(int num) {
		
		length += num * Page.PAGE_SIZE;
		try {
			setFileLength(length);
		} catch (IOException e) {
			Debug.fsLogger.error("failed to add more pages");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static void writeBack(int pageID, byte[] data) {
		int off = pageID * Page.PAGE_SIZE;
		ByteBuffer buf = ByteBuffer.wrap(data);
		buf.position(0);
		buf.limit(data.length);
		try {
			channel.position(off);
			channel.write(buf);
		} catch (IOException e) {
			Debug.fsLogger.error("page {} write back error", pageID);
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] readPage(int pageID) {
		byte[] data = new byte[Page.PAGE_SIZE];
		try {
			readFully(data, pageID * Page.PAGE_SIZE, Page.PAGE_SIZE);
		} catch (IOException e) {
			Debug.fsLogger.error("Failed to readpage");
			e.printStackTrace();
		}
		return data;
	}
	
    public static void readFully(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }
        if (off + len > length) {
            throw new EOFException();
        }
        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.position(0);
        buf.limit(len);
        channel.position(off);
        channel.read(buf);
    }


	public static void setFileLength(long newLength) throws IOException {
		if (newLength <= channel.size()) {
			long oldPos = channel.position();
			try {
				channel.truncate(newLength);
			} catch (NonWritableChannelException e) {
				throw new IOException("read only");
			}
			if (oldPos > newLength) {
				oldPos = newLength;
			}
			channel.position(oldPos);
		} else {
			// extend by writing to the new location
			ByteBuffer b = ByteBuffer.allocate(1);
			channel.write(b, newLength - 1);
			Debug.fsLogger.debug("file size {}", channel.size());
		}
		length = newLength;
	}

	public void sync() throws IOException {
		channel.force(true);
	}

}
