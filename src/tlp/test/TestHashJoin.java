package tlp.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import plan.EnumeratePlan;
import table.Column;
import table.HashJoin;
import table.Record;
import table.Table;
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
	boolean created = false;
	@Before
	public void init() throws IOException {
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
		
		created = true;
	}
	
	@Test
	public void createTable() throws DeadlockException, TimeoutException {
		assertTrue(created);
		
		ArrayList<Column> cols = new ArrayList<Column>();
		Column colWord = new Column("word", Value.TYPE_CHAR, 40, 0, false, null);
		colWord.setPrimaryKey(false);
		cols.add(colWord);
		
		Table words = new Table("words", cols);
		
		Table words2 = new Table("words2", cols);
		
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
		
		EnumeratePlan p1 = new EnumeratePlan(words, tr);
		EnumeratePlan p2 = new EnumeratePlan(words2, tr);
		
		HashJoin join = new HashJoin(tr, p1, p2, 0, 0);
		
		
		join.print(System.out);
		
		tr.commit();
		BufferManager.flushAll();
	}
}
