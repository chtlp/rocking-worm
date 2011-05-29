package tlp.test;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;

import index.BPlusIndex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import table.Column;
import table.Record;
import table.Table;
import table.TableManager;
import tlp.util.Debug;
import transaction.Transaction;
import util.Config;
import value.StrValue;
import value.Value;
import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.Threaded;
import filesystem.BufferManager;
import filesystem.FileStorage;

public class TestTransactionSharedLock extends MultithreadedTestCase {

	boolean created = false;
	static int numThreads = 2;
	static CyclicBarrier barrier = new CyclicBarrier(2), barrier2 = new CyclicBarrier(2);
	ArrayList<Table> tables = new ArrayList<Table>();

	@Before
	public void init() throws IOException {
		// System.out.println(System.getProperties().getProperty("java.class.path"));
		// LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		// StatusPrinter.print(lc);

		new File("myApp.log").delete();

		Config.load("test1.config");

		System.out.println(new File(".").getCanonicalPath());

		String dataFileName = Config.getDataFile();
		new File(dataFileName).delete();

		FileStorage.loadFile(dataFileName);
		FileStorage.init();

		Transaction tr = FileStorage.newSystemTransaction();
		TableManager.createDatabase(tr, "test2");

		for (int i = 0; i < numThreads; ++i) {
			ArrayList<Column> cols = new ArrayList<Column>();
			Column colWord = new Column("word", Value.TYPE_CHAR, 40, 0, false, null);
			colWord.setPrimaryKey(true);
			cols.add(colWord);

			Table words = new Table("words" + i, cols);

			TableManager.createTable(tr, words, true);

			words = TableManager.getTable(tr, "words" + i);
			tables.add(words);
		}

		FileInputStream dict = null;
		try {
			dict = new FileInputStream("dictionary.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Debug.fsLogger.error("file {} not found", "dictionary.txt");
			fail();
		}

		Debug.testLogger.debug("dictionary file opened");
		Scanner scanner = new Scanner(dict);

		LinkedList<String> queries = new LinkedList<String>();
		HashSet<String> deletion = new HashSet<String>();
		int N = 20000;
		int M1 = (int) (N * 0.2), M2 = (int) (N * 0.3);
		for (int i = 0; i < N; ++i) {
			if (!scanner.hasNext()) {
				break;
			}
			String word = scanner.next();

			Record r = new Record();
			r.addValue(new StrValue(word));

			// evenly distribute the words
			tables.get(i % numThreads).insert(tr, r);

			Debug.testLoggerB.debug("{} inserted", word);

			if (i % 50 == 0)
				queries.add(word);

			if (i >= M1 && i < M2 || i % 10 == 0)
				deletion.add(word);
		}

		created = true;
		tr.commit();
	}

	@Threaded("worker1")
	public void worker1() {
		new TransactionWorker2("worker0", "words0", "words1").run();
	}

	@Threaded("worker2")
	public void worker2() {
		new TransactionWorker2("worker1", "words1", "words0").run();

	}

	@Test
	public void assertions() {
		// Transaction tr = Transaction.begin();
		// TableManager.useDatabase(tr, "test2");
		// Table t = TableManager.getTable(tr, "words0");
		// BPlusIndex indx = t.getScanIndex();
		// assertTrue(indx.findEqualUnique(new StrValue("_prefix_" + "worker0"))
		// != null);
		// assertTrue(indx.findEqualUnique(new StrValue("_prefix2_" +
		// "worker1")) != null);
		//
		// t = TableManager.getTable(tr, "words1");
		// indx = t.getScanIndex();
		// assertTrue(indx.findEqualUnique(new StrValue("_prefix2_" +
		// "worker0")) != null);
		// assertTrue(indx.findEqualUnique(new StrValue("_prefix_" + "worker1"))
		// != null);
		//
		// tr.commit();

		BufferManager.flushAll();
	}

	static int MAX_RETRY = 10;

	class TransactionWorker2 implements Runnable {

		String tbl1, tbl2;
		String name;

		public TransactionWorker2(String name, String tbl1, String tbl2) {
			this.tbl1 = tbl1;
			this.tbl2 = tbl2;
			this.name = name;
		}

		@Override
		public void run() {
			Transaction tr = Transaction.begin();
			TableManager.useDatabase(tr, "test2");
			int k;
			for (k = 0; k < MAX_RETRY; ++k) {
				try {
					Debug.trLogger.debug("asking for sharedlock on {}", tbl1);
					TableManager.sharedLock(tr, tbl1);
					Debug.trLogger.debug("acquire sharedlock on {}", tbl1);
					Table t1 = TableManager.getTable(tr, tbl1);

//					barrier.wait();
					waitForTick(1);

					Debug.trLogger.debug("asking for sharedlock on {}", tbl2);
					TableManager.sharedLock(tr, tbl2);
					Debug.trLogger.debug("acquire sharedlock on {}", tbl2);
					Table t2 = TableManager.getTable(tr, tbl2);
					
//					barrier2.wait();
					waitForTick(2);

					Debug.trLogger.debug("asking for exlock on {}", tbl2);
					TableManager.exclusiveLock(tr, tbl2);
					Debug.trLogger.debug("acquire exlock on {}", tbl2);

					
					break;
				} catch (Exception ex) {
					tr.rollback();
					Debug.trLogger
							.debug("forced to release all locks");
					Debug.trLogger.info("{} dectected ", ex.getClass());
				}
			}

			Debug.trLogger.debug("retried times: {}", k);

			tr.commit();

		}

	}
}
