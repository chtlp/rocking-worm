package tlp.test;

import static org.junit.Assert.assertTrue;
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

import org.junit.Before;
import org.junit.Test;

import table.Column;
import table.Record;
import table.Table;
import table.TableIterator;
import table.TableManager;
import tlp.util.Debug;
import tlp.util.RandomPermutation;
import transaction.Transaction;
import util.Config;
import value.IntValue;
import value.StrValue;
import value.Value;
import filesystem.BufferManager;
import filesystem.FileStorage;

public class TestNullValues {

	boolean created = false;

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

		created = true;
	}

	@Test
	public void createTable() {
		assertTrue(created);

		ArrayList<Column> cols = new ArrayList<Column>();
		Column colid = new Column("id", Value.TYPE_INT, 0, 0, true, null);
		colid.setPrimaryKey(true);
		cols.add(colid);
		Column colWord = new Column("word", Value.TYPE_CHAR, 40, 0, false, null);
		cols.add(colWord);

		Table words = new Table("words", cols);

		Transaction tr = FileStorage.newSystemTransaction();
		TableManager.createDatabase(tr, "test");

		TableManager.createTable(tr, words, true);

		words = TableManager.getTable(tr, "words");

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

		Integer[] id = new Integer[N];
		for (int i = 0; i < N; ++i)
			id[i] = i;
		RandomPermutation perm = new RandomPermutation(100);
		perm.permute(id);

		HashSet<String> inserted = new HashSet<String>();
		for (int i = 0; i < N; ++i) {
			if (!scanner.hasNext()) {
				break;
			}
			String word = scanner.next();

			Record r = new Record();
			r.addValue(new IntValue(id[i]));
			r.addValue(i % 4 > 0 ? new StrValue(word) : null);
			if (i % 4 > 0) inserted.add(word);

			words.insert(tr, r);

			if (i % 4 > 0)
				Debug.testNullLogger.debug("{} inserted", word);
			else
				Debug.testNullLogger.debug("{} discarded", word);

			if (i % 50 == 0) {
				queries.add(word);
			}

			if (i >= M1 && i < M2 || i % 10 == 0 && i % 4 > 0)
				deletion.add(word);
		}

		TableManager.createIndex(tr, words, "word-index", 1, false);

		System.out.println(" *** word-index *** ");
		TableIterator wordIndx = words.getIndex(tr, "word-index");

		wordIndx.print(System.out);

		System.out.println(" *** word-index queries *** ");
		for (String w : queries) {
			assert !inserted.contains(w) || ((BPlusIndex) wordIndx).find(new StrValue(w)) != null;
			System.out.format("query %s OK\n", w);
		}

		System.out.println(" *** word-index deletions *** ");
		for (String w : deletion) {
			Record r = ((BPlusIndex) wordIndx).find(new StrValue(w));
			if (r != null) assert r.getRowID() >= 0;
			assert inserted.contains(w) ^ (r == null);
			if (r == null) continue;
			words.remove(tr, r);
			System.out.format("delete %s OK\n", w);

		}

		// TableIterator iter = words.getRecords(Jr);
		//
		// int k = 0;
		// for(iter.open(); iter.hasNext();) {
		// Record r = iter.next();
		// System.out.println(r);
		// ++k;
		// }
		// iter.close();
		// assertEquals(N, k);
		//
		// Index scan = words.getScanIndex(tr);
		// for(String w : queries) {
		// assert scan.findEqualUnique(new StrValue(w)) != null :
		// String.format("%s not found", w);
		// }
		//
		// for(String w : deletion) {
		// Record r = new Record();
		// r.addValue(new StrValue(w));
		// assert scan.removeUnique(tr, r) : r.getValue(0).get();
		// Debug.testLight.debug("{} removed", w);
		// }
		//
		// for(String w : queries) {
		// assert (scan.findEqualUnique(new StrValue(w)) != null) !=
		// deletion.contains(w);
		// }
		//
		// for(String w : deletion) {
		// Record r = new Record();
		// r.addValue(new StrValue(w));
		// scan.add(tr, r);
		// Debug.testLight.debug("{} inserted", w);
		// }
		//
		// for(String w : queries) {
		// assert scan.findEqualUnique(new StrValue(w)) != null :
		// String.format("%s not found", w);
		// }

		tr.commit();

		words.printTable(System.out);

		BufferManager.flushAll();
	}
}
