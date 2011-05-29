package tlp.test;

import java.io.File;
import java.io.FileInputStream;

import logging.Recovery;

import parser.Absyn;
import parser.AbsynList;
import parser.lexer;
import parser.parser;
import plan.Plan;
import plan.Planner;
import plan.QueryPlan;
import plan.UpdatePlan;
import table.TableManager;
import transaction.Transaction;
import util.Config;
import util.Constant;
import filesystem.BufferManager;
import filesystem.FileStorage;

public class BackEndTestEngine {

	public static void main(String[] args) throws Exception {
//		String fileName = "create.txt";
//		if (args.length > 0) {
//			fileName = args[0];
//		}

		//System.out.println("initialzing database...");
		new File("myApp.log").delete();
		
		new File(Constant.LoggingFile).delete();

		Config.load("test1.config");

		String dataFileName = Config.getDataFile();
		new File(dataFileName).delete();

		FileStorage.loadFile(dataFileName);
		Recovery.recover();
		FileStorage.init();
		//System.out.println("finish database initialzing");

//		String[] files = new String[] { "./test/ARNO2/create_tables.txt", "./test/ARNO2/insert_ATOM.txt"};
		
		String[] files = new String[] { "./test/sample/sample-create.txt" };

		for (String fileName : files) {
			parser p = new parser(new lexer(new FileInputStream(fileName)));
			AbsynList result = (AbsynList) p.parse().value;

			Transaction tr = Transaction.begin();

			while (result != null) {
				Absyn absyn = result.head;

				Plan plan = Planner.translate(absyn, tr);
				if (plan instanceof QueryPlan) {
					QueryPlan qPlan = (QueryPlan)plan;
					qPlan.open();
					table.Record record = null;
					do {
						record = qPlan.next();
						if (record != null) System.out.println("# " + record.shortString());
					} while (record != null);
					qPlan.close();
										
				} else {
					((UpdatePlan) plan).run();
				}
				result = result.tail;
			}

			tr.commit();
		}

//		BufferManager.flushAll();

		TableManager.printAllTables();
	}

}
