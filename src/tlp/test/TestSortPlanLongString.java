package tlp.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import index.Index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

import plan.EnumeratePlan;
import plan.SortPlan;
import table.Column;
import table.Record;
import table.Table;
import table.TableIterator;
import table.TableManager;
import tlp.util.Debug;
import transaction.Transaction;
import util.Config;
import value.StrValue;
import value.Value;
import filesystem.BufferManager;
import filesystem.FileStorage;


public class TestSortPlanLongString {
	boolean created = false;
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
		
		created = true;
	}
	
	@Test
	public void createTable() {
		assertTrue(created);
		
		ArrayList<Column> cols = new ArrayList<Column>();
		Column colWord = new Column("word", Value.TYPE_CHAR, 40, 0, false, null);
		colWord.setPrimaryKey(true);
		cols.add(colWord);

		Column colLine = new Column("word", Value.TYPE_CHAR, 150, 0, false, null);
		colLine.setPrimaryKey(false);
		cols.add(colLine);

		Table tbl = new Table("word-line", cols);
		

		Transaction tr = FileStorage.newSystemTransaction();
		TableManager.createDatabase(tr, "test");
		
		TableManager.createTable(tr, tbl, true);
		
		tbl = TableManager.getTable(tr, "word-line");
		
		Index lineIndex = TableManager.createIndex(tr, tbl, "line-index", 1, false);
		
		FileInputStream text = null, words = null;
		try {
			words = new FileInputStream("dictionary.txt");
			text = new FileInputStream("harry1.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Debug.fsLogger.error("file {} not found", "harry1.txt");
			fail();
		}
		
		Debug.testLogger.debug("story file opened");
		Scanner textScanner = new Scanner(text);
		Scanner wordScanner = new Scanner(words);
		
		
		LinkedList<String> queries = new LinkedList<String>();
		LinkedList<String> textQueries = new LinkedList<String>();

		HashSet<String> deletion = new HashSet<String>();
		int N = 2000;
		int M1 = (int)(N * 0.2), M2 = (int)(N * 0.3);
		for(int i=0; i<N; ++i) {
			if (!textScanner.hasNextLine() || !wordScanner.hasNext()) {
				break;
			}
			String line = textScanner.nextLine();
			String word = wordScanner.next();
			if (line.equals("")) continue;
			line = line.substring(0, Math.min(150, line.length()));
						
			Record r = new Record();
			r.addValue(new StrValue(word));
			r.addValue(new StrValue(line));
			
			tbl.insert(tr, r);
			
			Debug.testLoggerB.debug("({}, {}) inserted", word, Debug.showStr(line));
			
			if (i % 10 == 0) {
				queries.add(word);
				textQueries.add(line);
			}
			
			if (i >= M1 && i < M2 || i % 10 == 0) deletion.add(word);
		}
		
		for(int i=0; i < queries.size(); ++i) {
			
			Record r = lineIndex.find(new StrValue(textQueries
					.get(i)));
			
			assert r.getValue(0).get().equals(queries.get(i)) : queries.get(i);
			
			Debug.testLight2.debug("{} queried", r.getValue(0));
		}
		
		Index scan = tbl.getScanIndex(tr);
		for(String line :queries) {
			assert scan.find(new StrValue(line)) != null : String
					.format("%s not found", line);
			Debug.testLight.debug("{} queried", Debug.showStr(line));
		}
		
		for(String word : deletion) {
			Record r = new Record();
			r.addValue(new StrValue(word));
			assert scan.removeUnique(tr, r);
			Debug.testLight.debug("{} removed", Debug.showStr(word));
		}
		
		for(String word :queries) {
			assert (scan.find(new StrValue(word)) != null) != deletion
					.contains(word);
			Debug.testLight.debug("{} queried", Debug.showStr(word));
		}

		for(String word : deletion) {
			Record r = new Record();
			r.addValue(new StrValue(word));
			scan.add(tr, r);
			Debug.testLight.debug("{} removed", Debug.showStr(word));
		}

		
		for(String word :queries) {
			assert (scan.find(new StrValue(word)) != null);
			Debug.testLight.debug("{} queried", Debug.showStr(word));
		}
		
		EnumeratePlan plan = new EnumeratePlan(tbl, tr);
		SortPlan p = new SortPlan(tr, plan, new ReverseWordComparator());
		
		try {
		for(p.open();;) {
			Record r = p.next();
			if (r == null) break;
			
			System.out.println(r);
		}
		p.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		tr.commit();

		BufferManager.flushAll();
	}
}


