package jdbc;

import java.io.File;
import java.io.FileInputStream;

import parser.Absyn;
import parser.AbsynList;
import parser.lexer;
import parser.parser;
import parser.printer;
import plan.Plan;
import plan.Planner;
import plan.QueryPlan;
import plan.UpdatePlan;
import transaction.Transaction;
import util.Config;
import util.Constant;
import filesystem.BufferManager;
import filesystem.FileStorage;

public class SelfTest {

	public static void main(String[] args) throws Exception {
		new File("myApp.log").delete();

		new File(Constant.LoggingFile).delete();

		Config.load("test1.config");

		String dataFileName = Config.getDataFile();
		new File(dataFileName).delete();

		FileStorage.loadFile(dataFileName);
		FileStorage.init();

		String[] files = new String[] { "test_jdbc.txt" };

		for (String fileName : files) {
			parser p = new parser(new lexer(new FileInputStream(fileName)));
			AbsynList result = (AbsynList) p.parse().value;
			printer print = new printer(result);

			Transaction tr = Transaction.begin();

			while (result != null) {
				Absyn absyn = result.head;
				print.printAbsyn(absyn, 0);

				Plan plan = Planner.translate(absyn, tr);
				if (plan instanceof QueryPlan) {
					QueryPlan qPlan = (QueryPlan) plan;
					qPlan.open();
					table.Record record = null;
					do {
						record = qPlan.next();
						if (record != null)
							System.out.println(record);
					} while (record != null);
					qPlan.close();
				}
				else {
					((UpdatePlan) plan).run();
				}
				result = result.tail;
			}

			tr.commit();
		}

		BufferManager.flushAll();
	}

}
