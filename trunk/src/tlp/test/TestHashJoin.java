package tlp.test;

import static org.junit.Assert.fail;
import index.BPlusIndex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import org.junit.BeforeClass;
import org.junit.Test;

import plan.EnumeratePlan;
import table.Column;
import table.HashJoin;
import table.Record;
import table.Table;
import table.TableIterator;
import table.TableManager;
import tlp.util.Debug;
import tlp.util.RandomPermutation;
import transaction.DeadlockException;
import transaction.Transaction;
import util.Config;
import value.StrValue;
import value.Value;
import filesystem.BufferManager;
import filesystem.FileStorage;


public class TestHashJoin {
	@BeforeClass
	public static void init() throws IOException {
//		System.out.println(System.getProperties().getProperty("java.class.path"));
//		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//	    StatusPrinter.print(lc);
		
		new File("rockingworm-logback.log").delete();
		
		Config.load("test1.config");
		
		System.out.println(new File(".").getCanonicalPath());
		
		String dataFileName = Config.getDataFile();
		new File(dataFileName).delete();

		FileStorage.loadFile(dataFileName);
		FileStorage.init();
		

		
		ArrayList<Column> cols = new ArrayList<Column>();
		Column colWord = new Column("word", Value.TYPE_CHAR, 40, 0, false, null);
		colWord.setPrimaryKey(true);
		cols.add(colWord);
		
		Table words = new Table("words", cols);
		
		ArrayList<Column> cols2 = new ArrayList<Column>();
		Column colWord2 = new Column("word", Value.TYPE_CHAR, 40, 0, false, null);
		colWord2.setPrimaryKey(false);
		cols2.add(colWord2);
		Table words2 = new Table("words2", cols2);
		
		Transaction tr = FileStorage.newSystemTransaction();
		TableManager.createDatabase(tr, "test");
		
		TableManager.createTable(tr, words, true);
		TableManager.createTable(tr, words2, true);
		
		words = TableManager.getTable(tr, "words");
		words2 = TableManager.getTable(tr, "words2");
		
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
		
		
		int N = 20000;
		String[] wordList = new String[N];
		
		
		for(int i=0; i<N; ++i) {
			assert scanner.hasNext();
			String word = scanner.next();
			wordList[i] = word;
						
			Debug.testLoggerB.debug("{} inserted", word);			
			
		}
		
		for (String w : wordList) {
			StrValue s = new StrValue(w);
			Record r = new Record();
			r.addValue(s);
			words.insert(tr, r);
		}
		
		RandomPermutation rand = new RandomPermutation(100);
		rand.permute(wordList);
		
		for (String w : wordList) {
			StrValue s = new StrValue(w);
			Record r = new Record();
			r.addValue(s);
			words2.insert(tr, r);
		}
		
		System.out.println("the two tables created and records inserted");
		
		
		tr.commit();
		BufferManager.flushAll();
	}
	
	@Test
	public void testHashJoin() throws DeadlockException, TimeoutException {
		Transaction tr = Transaction.begin();
		TableManager.useDatabase(tr, "test");
		Table words = TableManager.getTable(tr, "words");
		Table words2 = TableManager.getTable(tr, "words2");
		EnumeratePlan p1 = new EnumeratePlan(words, tr);
		EnumeratePlan p2 = new EnumeratePlan(words2, tr);
		
		HashJoin join = new HashJoin(tr, p1, p2, 0, 0);
		
		tr.commit();
//		join.print(System.out);
		for(join.open();;) {
			Record r = join.next();
			if (r == null) break;
		}
		join.close();

	}
	
	@Test
	public void testIndexJoin() {
		Transaction tr = Transaction.begin();
		TableManager.useDatabase(tr, "test");
		Table words = TableManager.getTable(tr, "words");
		Table words2 = TableManager.getTable(tr, "words2");
		
		BPlusIndex p1 = words.getScanIndex(tr);
		BPlusIndex p2 = words2.getScanIndex(tr);
		for(p2.open();;) {
			Record r2 = p2.next();
			if (r2 == null) break;
			Record r1 = p1.find(r2.getValue(0));
			assert r1 != null;
//			System.out.format("%s %s\n", r2, r2);
		}
		

	}
}
