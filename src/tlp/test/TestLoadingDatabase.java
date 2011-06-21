package tlp.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import logging.Recovery;

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

		System.out.println(new File(".").getCanonicalPath());

		String dataFileName = Config.getDataFile();
		// new File(dataFileName).delete();

		FileStorage.loadFile(dataFileName);
		Recovery.recover();
		FileStorage.init();


		TableManager.printAllTables();

	}

}
