package tlp.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;

import org.junit.Before;

import table.Column;
import table.Record;
import table.Table;
import table.TableManager;
import tlp.util.Debug;
import transaction.Transaction;
import util.Config;
import value.StrValue;
import value.Value;
import filesystem.FileStorage;


public class TestExternalSort {
	@Before
	public void init() throws IOException {
//		System.out.println(System.getProperties().getProperty("java.class.path"));
//		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//	    StatusPrinter.print(lc);
		
		new File("myApp.log").delete();
		
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
		int M1 = (int)(N * 0.2), M2 = (int)(N * 0.3);
		for(int i=0; i<N; ++i) {
			if (!scanner.hasNext()) {
				break;
			}
			String word = scanner.next();
						
			Record r = new Record();
			r.addValue(new StrValue(word));
			
			words.insert(tr, r);
			
			Debug.testLoggerB.debug("{} inserted", word);
			
			if (i % 50 == 0) queries.add(word);
			
			if (i >= M1 && i < M2 || i % 10 == 0) deletion.add(word);
		}
	}
}
