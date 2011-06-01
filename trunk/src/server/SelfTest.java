package server;

import java.io.File;

import logging.Recovery;

import table.TableManager;
import util.Config;
import filesystem.FileStorage;

public class SelfTest {
	public static void main(String[] args) throws Exception {
		new File("myApp.log").delete();

		Config.load("jdbc.config");
		Config.set("MakingErrors", "false");

		System.out.println(new File(".").getCanonicalPath());

		String dataFileName = Config.getDataFile();

		FileStorage.loadFile(dataFileName);
		Recovery.recover();
		FileStorage.init();

		TableManager.printAllTables();
	}

}
