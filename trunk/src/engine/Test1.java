package engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import parser.*;
import plan.Plan;
import plan.Planner;
import plan.QueryPlan;
import plan.UpdatePlan;
import table.TableManager;
import transaction.DeadlockException;
import transaction.Transaction;
import util.Config;
import filesystem.FileStorage;

public class Test1 {

	/**
	 * @param args
	 * @throws TimeoutException 
	 * @throws DeadlockException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {

		System.out.println("initialzing database...");
		new File("myApp.log").delete();

		Config.load("test1.config");

		String dataFileName = Config.getDataFile();
		new File(dataFileName).delete();

		FileStorage.loadFile(dataFileName);
		FileStorage.init();
		System.out.println("finish database initialzing");

		ArrayList<Absyn> absynArray = new ArrayList<Absyn>();
		
		absynArray.add(new CreateDatabase(0, "DB1"));
		
		ColName cn1 = new ColName(null, "A");
		IsNull in1 = new IsNull(0, null);
		ColumnDef cd1 = new ColumnDef(0, new DataType(DataType.INT, 0, 0, 0), cn1, in1);
		CreateDef created1 = new CreateDef(0, cd1, null);
		
		ColName cn2 = new ColName(null, "B");
		IsNull in2 = new IsNull(0, null);
		ColumnDef cd2 = new ColumnDef(0, new DataType(DataType.FLOAT, 0, 0, 0), cn2, in2);
		CreateDef created2 = new CreateDef(0, cd2, null);
		
		CreateDef created3 = new CreateDef(0, null, cn1);
		
		CreateDefList cdf = new CreateDefList(created3, null);
		cdf = new CreateDefList(created2, cdf);
		cdf = new CreateDefList(created1 ,cdf);
		
		absynArray.add(new CreateTable(0, "Table1", cdf));
		
		IntConstValue icv;
		FloatConstValue fcv;
		ValueList vl;
		
		icv = new IntConstValue(0, 3);
		fcv = new FloatConstValue(0, (float) 0.4);
		vl = new ValueList(fcv, null);
		vl = new ValueList(icv, vl);
		absynArray.add(new Insert(0, "Table1", vl, null, null));
		
		icv = new IntConstValue(0, 5);
		fcv = new FloatConstValue(0, (float) 0.6);
		vl = new ValueList(fcv, null);
		vl = new ValueList(icv, vl);
		absynArray.add(new Insert(0, "Table1", vl, null, null));
		
		icv = new IntConstValue(0, 2);
		fcv = new FloatConstValue(0, (float) 0.5);
		vl = new ValueList(fcv, null);
		vl = new ValueList(icv, vl);
		absynArray.add(new Insert(0, "Table1", vl, null, null));
		
		icv = new IntConstValue(0, 1);
		fcv = new FloatConstValue(0, (float) 0.2);
		vl = new ValueList(fcv, null);
		vl = new ValueList(icv, vl);
		absynArray.add(new Insert(0, "Table1", vl, null, null));
		
		icv = new IntConstValue(0, 8);
		fcv = new FloatConstValue(0, (float) 1.7);
		vl = new ValueList(fcv, null);
		vl = new ValueList(icv, vl);
		absynArray.add(new Insert(0, "Table1", vl, null, null));
		
		ColNameValue cnv = new ColNameValue(0, new ColName(null, "A"));
		SelectExpr se = new SelectExpr(0, cnv, "C", false);
		SelectExprList sel = new SelectExprList(se, null);
		cnv = new ColNameValue(0, new ColName(null, "B"));
		se = new SelectExpr(0, cnv, null, false);
		sel = new SelectExprList(se, sel);
		
		TblRef trr = new TblRef(0, "Table1", "Table1alias", null, null);
		TblRefList trl = new TblRefList(trr, null);
		HasOrder hasOrder = new HasOrder(0, null);
		HasHaving hasHaving = new HasHaving(null, hasOrder);
		HasGroup hasGroup = new HasGroup(null, hasHaving);
		HasWhere hasWhere =  new HasWhere(null, hasGroup);
		HasFrom hasFrom = new HasFrom(trl, hasWhere);
		Select select = new Select(sel, false, hasFrom);
		absynArray.add(select);
			

		transaction.Transaction tr = Transaction.begin();
		for (int i = 0; i < absynArray.size(); i++) {
			Plan plan = Planner.translate(absynArray.get(i), tr);
			if (plan instanceof QueryPlan) {
				System.out.println("QUERYPLAN");
				table.Record record = null;
				QueryPlan qPlan = (QueryPlan)plan;
				qPlan.open();
				System.out.println(qPlan.getColumns().get(0).getName() + " | " + 
						qPlan.getColumns().get(1).getName() );
				do {
					record = qPlan.next();
					if (record != null)
						System.out.println(record.getValue(0).get() + " | "
								+ record.getValue(1).get());
					//output record;
				}
				while (record != null);
			}
			else {
				((UpdatePlan)plan).run();
			}
		}
		table.Table table1 = TableManager.getTable(tr, "Table1");
		for (int i = 0; i < table1.numColumns(); i++) {
			System.out.println(table1.getName() + " -> Col: " + table1.getColumn(i).getName());
			System.out.println(table1.getColumn(i).getType());
		}
		table1.printTable(System.out);
		tr.commit();

	}

}
