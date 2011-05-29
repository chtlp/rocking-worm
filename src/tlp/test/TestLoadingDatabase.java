package tlp.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import table.Table;
import table.TableManager;
import transaction.Transaction;
import util.Config;
import filesystem.FileStorage;

public class TestLoadingDatabase {

	// boolean created = false;
	// static int numThreads = 2;
	// ArrayList<Table> tables = new ArrayList<Table>();

	@Test
	public void create() throws IOException {
		new File("myApp.log").delete();

		Config.load("test1.config");
		Config.set("MakingErrors", "false");

		System.out.println(new File(".").getCanonicalPath());

		String dataFileName = Config.getDataFile();
		// new File(dataFileName).delete();

		FileStorage.loadFile(dataFileName);
		FileStorage.init();

		// Transaction tr = FileStorage.newSystemTransaction();
		// TableManager.createDatabase(tr, "test2");
		//
		// for (int i = 0; i < numThreads; ++i) {
		// Table words = TableManager.getTable(tr, "words" + i);
		// words.printTable(System.out);
		// }

		// tr.commit();

		TableManager.printAllTables();

	}

}
