package tlp.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;

import table.Column;
import table.Record;
import table.Table;
import table.TableIterator;
import table.TableManager;
import tlp.util.Debug;
import transaction.Transaction;
import util.Config;
import util.Counter;
import value.StrValue;
import value.Value;
import filesystem.BufferManager;
import filesystem.FileStorage;

public class TestTransactionUsingBarrier {

	boolean created = false;
	static int numThreads = 2;
	ArrayList<Table> tables = new ArrayList<Table>();
	static CyclicBarrier barrier = new CyclicBarrier(numThreads);

	public static void main(String[] args) throws IOException {
		TestTransactionUsingBarrier t = new TestTransactionUsingBarrier();
		t.init();
		t.testDeadlock();
	}

	public static boolean show = false;

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

		show = true;
		for (Table t : tables) {
			System.out.printf("Table %s\n", t);
			TableIterator i = t.getRecords(tr);
			for (i.open(); i.hasNext();) {
				Record r = i.next();
				System.out.println(r.getValue(0).get());
			}
		}

		created = true;
		tr.commit();
	}

	public void testDeadlock() {
		assert created;
		Counter.getCounter("error1").set(223);

		TransactionWorker w1 = new TransactionWorker("words0", "words1");
		TransactionWorker w2 = new TransactionWorker("words1", "words0");

		new Thread(w1).start();
		new Thread(w2).start();
	}

}

class TransactionWorker implements Runnable {

	String tbl1, tbl2;

	public TransactionWorker(String tbl1, String tbl2) {
		this.tbl1 = tbl1;
		this.tbl2 = tbl2;
	}

	boolean waited = false;

	@Override
	public void run() {
		Transaction tr = Transaction.begin();
		TableManager.useDatabase(tr, "test2");

		int k;
		for (k = 0; k < 10; ++k) {
			try {
				Debug.trLogger.debug("asking for exlock on {}", tbl1);
				TableManager.exclusiveLock(tr, tbl1);
				Debug.trLogger.debug("acquire exlock on {}", tbl1);
				Table t1 = TableManager.getTable(tr, tbl1);
				
				Record r = new Record();
				r.addValue(new StrValue("_prefix_" + tbl1));
				t1.insert(tr, r);
				// do something on t1

				if (!waited) {
					TestTransactionUsingBarrier.barrier.await();
					waited = true;
				}

				Debug.trLogger.debug("asking for exlock on {}", tbl2);
				TableManager.exclusiveLock(tr, tbl2);
				Debug.trLogger.debug("acquire exlock on {}", tbl2);
				Table t2 = TableManager.getTable(tr, tbl2);
				break;
			} catch (Exception ex) {
				tr.rollback();
				Debug.trLogger
						.debug("forced to roll back on {}", ex.getClass());
				ex.printStackTrace();
				tr = Transaction.begin();
				TableManager.useDatabase(tr, "test2");
			}
		}
		Debug.trLogger.debug("tried times: {}", k);

		tr.commit();

		BufferManager.flushAll();
	}

}
