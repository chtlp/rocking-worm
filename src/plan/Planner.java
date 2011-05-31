package plan;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import parser.ColNameValue;
import parser.TblRef;
import parser.TblRefList;

import table.Column;
import table.Table;
import table.TableManager;
import transaction.DeadlockException;
import transaction.Transaction;

public class Planner {

	public static Plan translate(parser.Absyn absyn, Transaction tr)
			throws DeadlockException, TimeoutException {
		return translate(absyn, tr, null);
	}

	public static Plan translate(parser.Absyn absyn, Transaction tr, Tail tail)
			throws DeadlockException, TimeoutException {
		if (absyn instanceof parser.CreateDatabase)
			return translate((parser.CreateDatabase) absyn, tr, tail);
		if (absyn instanceof parser.UseDatabase)
			return translate((parser.UseDatabase) absyn, tr, tail);
		if (absyn instanceof parser.DropDatabase)
			return translate((parser.DropDatabase) absyn, tr, tail);
		if (absyn instanceof parser.CreateTable)
			return translate((parser.CreateTable) absyn, tr, tail);
		if (absyn instanceof parser.Insert)
			return translate((parser.Insert) absyn, tr, tail);
		if (absyn instanceof parser.Delete)
			return translate((parser.Delete) absyn, tr, tail);
		if (absyn instanceof parser.Update)
			return translate((parser.Update) absyn, tr, tail);
		if (absyn instanceof parser.CreateIndex)
			return translate((parser.CreateIndex) absyn, tr, tail);
		if (absyn instanceof parser.DropIndex)
			return translate((parser.DropIndex) absyn, tr, tail);

		if (absyn instanceof parser.Select)
			return translate((parser.Select) absyn, tr, tail);
		if (absyn instanceof parser.HasFrom)
			return translate((parser.HasFrom) absyn, tr, tail);
		if (absyn instanceof parser.TblRef)
			return translate((parser.TblRef) absyn, tr, tail);
		return null;

	}

	static UpdatePlan translate(parser.CreateDatabase createDBAbsyn,
			Transaction tr, Tail tail) {
		return new CreateDBPlan(createDBAbsyn.dbName, tr);
	}

	static UpdatePlan translate(parser.UseDatabase useDBAbsyn, Transaction tr,
			Tail tail) {
		return new UseDBPlan(useDBAbsyn.dbName, tr);
	}

	static UpdatePlan translate(parser.DropDatabase dropDBAbsyn,
			Transaction tr, Tail tail) {
		return new DropDBPlan(dropDBAbsyn.dbName, tr);
	}

	static UpdatePlan translate(parser.CreateTable createTableAbsyn,
			Transaction tr, Tail tail) {
		parser.CreateDefList itr = createTableAbsyn.cDefList;
		ArrayList<table.Column> cols = new ArrayList<table.Column>();
		while (itr != null) {
			parser.CreateDef createDef = itr.head;
			parser.ColumnDef columnDef = createDef.cDef;
			if (columnDef != null) {
				String colName = columnDef.colName.colName;
				int ty = 0;
				int p1 = 0;
				int p2 = 0;
				boolean isNullable = true;
				switch (columnDef.dataType.type) {
				case parser.DataType.INT:
					ty = value.Value.TYPE_INT;
					break;
				case parser.DataType.FLOAT:
					ty = value.Value.TYPE_FLOAT;
					break;
				case parser.DataType.CHAR:
					ty = value.Value.TYPE_CHAR;
					p1 = columnDef.dataType.charLength;
					break;
				case parser.DataType.DATATIME:
					ty = value.Value.TYPE_DATETIME;
					break;
				case parser.DataType.BOOLEAN:
					ty = value.Value.TYPE_BOOLEAN;
					break;
				case parser.DataType.DECIMAL1:
				case parser.DataType.DECIMAL2:
					ty = value.Value.TYPE_DECIMAL;
					p1 = columnDef.dataType.decimal1;
					p2 = columnDef.dataType.decimal2;
					break;
				case parser.DataType.TIMESTAMP:
					ty = value.Value.TYPE_TIMESTAMP;
					break;
				case parser.DataType.VARCHAR:
					ty = value.Value.TYPE_VARCHAR;
					p1 = columnDef.dataType.charLength;
					break;
				default:
					System.out.println("DATA TYPE WRONG!");

				}
				parser.IsNull isNull = columnDef.isNull;
				value.Value defaultValue = null;
				if (isNull != null) {
					isNullable = isNull.isNull == 1;
					parser.HasDefault hasDefault = isNull.hasDefault;
					if (hasDefault != null) {
						parser.ConstValue constValue = hasDefault.constValue;
						if (constValue != null) {
							defaultValue = new ConstValueInterpreter()
									.trans(constValue);
							defaultValue = CorrectType.trans(defaultValue, ty);
						}
					}
				}
				table.Column column = new table.Column(colName, ty, p1, p2,
						isNullable, defaultValue);
				cols.add(column);
				// System.out.println(column.getName() + " " +
				// column.getType());
			} else {
				boolean flag = false;
				parser.ColName cn = createDef.primaryKey;
				for (int i = 0; i < cols.size(); i++)
					if (cols.get(i).getName().equals(cn.colName)) {
						cols.get(i).setPrimaryKey(true);
						// System.out.println(cols.get(i).getName() +
						// " set to primary");
						flag = true;
						break;
					}
				if (!flag)
					System.out.println("PRIMARY KEY WRONG");
			}
			itr = itr.tail;
		}
		table.Table table = new table.Table(createTableAbsyn.tlbName, cols);
		return new CreateTablePlan(table, tr);
	}

	static UpdatePlan translate(parser.Insert insertAbsyn, Transaction tr,
			Tail tail) throws DeadlockException, TimeoutException {
		String tbName = insertAbsyn.name;
		table.Table table = TableManager.getTableExclusive(tr, tbName);
		table.Record record = null;
		QueryPlan queryPlan = null;

		parser.ValueList vl = insertAbsyn.valueList;

		if (insertAbsyn.colNameList != null) {
			record = new table.Record();
			for (int i = 0; i < table.numColumns(); i++) {
				table.Column col = table.getColumn(i);
				parser.ColNameList coltmp = insertAbsyn.colNameList;
				parser.ValueList vltmp = insertAbsyn.valueList;
				value.Value v = null;
				while (coltmp != null) {
					if (coltmp.colName.colName.equals(col.getName())) {
						parser.ConstValue cv = (parser.ConstValue) vltmp.head;
						if (cv instanceof parser.DefaultConstValue)
							v = table.getColumn(i).getDefaultValue();
						else
							v = new plan.ConstValueInterpreter().trans(cv);
						break;
					}
					coltmp = coltmp.tail;
					vltmp = vltmp.tail;
				}

				if (v == null)
					v = table.getColumn(i).getDefaultValue();
				v = CorrectType.trans(v, table.getColumn(i).getType());
				record.addValue(v);
			}

		} else if (vl != null) {
			record = new table.Record();
			int i = 0;
			while (vl != null) {
				parser.Value v = vl.head;
				parser.ConstValue cv = (parser.ConstValue) v;
				value.Value vt;
				if (cv instanceof parser.DefaultConstValue)
					vt = table.getColumn(i).getDefaultValue();
				else
					vt = new plan.ConstValueInterpreter().trans(cv);
				vt = CorrectType.trans(vt, table.getColumn(i).getType());
				record.addValue(vt);
				vl = vl.tail;
				i++;
			}
		} else if (insertAbsyn.select != null) {
			queryPlan = translate(insertAbsyn.select, tr, tail);
		} else
			System.out.println("INSERT WRONG");
		return new InsertPlan(table, record, queryPlan, tr);
	}

	static UpdatePlan translate(parser.Delete deleteAbsyn, Transaction tr,
			Tail tail) throws DeadlockException, TimeoutException {
		String tbName = deleteAbsyn.tblName;
		table.Table table = TableManager.getTableExclusive(tr, tbName);
		Cond cond = new Cond(deleteAbsyn.whereCondition, tr);
		QueryPlan queryPlan = new EnumeratePlan(table, tr);
		return new DeletePlan(queryPlan, table, cond, tr);
	}

	static UpdatePlan translate(parser.Update updateAbsyn, Transaction tr,
			Tail tail) throws DeadlockException, TimeoutException {
		String tbName = updateAbsyn.tblName;
		table.Table table = TableManager.getTableExclusive(tr, tbName);
		return new UpdateDataPlan(table, updateAbsyn, tr);
	}

	static UpdatePlan translate(parser.CreateIndex ciAbsyn, Transaction tr,
			Tail tail) throws DeadlockException, TimeoutException {
		String tbName = ciAbsyn.tblName;
		table.Table table = TableManager.getTableExclusive(tr, tbName);
		int idx = 0;
		for (; idx < table.numColumns(); idx++) {
			Column column = table.getColumn(idx);
			if (column.getName().equals(ciAbsyn.colName.colName))
				break;
		}

		return new CreateIndexPlan(table, idx, ciAbsyn.indexName, tr);
	}

	static UpdatePlan translate(parser.DropIndex diAbsyn, Transaction tr,
			Tail tail) throws DeadlockException, TimeoutException {
		String tbName = diAbsyn.tblName;
		table.Table table = TableManager.getTableExclusive(tr, tbName);
		return new DropIndexPlan(table, diAbsyn.indexName, tr);
	}

	static void checkHashApp(parser.BoolExpr be, TblRefList trl) {
		if (be == null) {
			hashflag = false;
			return;
		}
		if (be instanceof parser.CopValue) {
			parser.CopValue cvbe = (parser.CopValue) be;
			if (cvbe.cop.type != parser.Cop.Eq
					|| !(cvbe.valueL instanceof parser.ColNameValue)
					|| !(cvbe.valueR instanceof parser.ColNameValue))
				return;
			parser.ColNameValue left = (parser.ColNameValue) cvbe.valueL;
			parser.ColNameValue right = (parser.ColNameValue) cvbe.valueR;
			int i = 0, il = -1, ir = -1, idxl = -1, idxr = -1;
			TblRefList tmp = trl;
			int idx;
			/*
			 * System.out.println(left.colName.tblName);
			 * System.out.println(right.colName.tblName);
			 * System.out.println(left.colName.colName);
			 * System.out.println(right.colName.colName);
			 */
			while (tmp != null) {
				idx = new Col2Idx().getIdx(hashfromPlans.get(i).alias,
						left.colName);
				if (idx != -1) {
					il = i;
					idxl = idx;
				}
				idx = new Col2Idx().getIdx(hashfromPlans.get(i).alias,
						right.colName);
				if (idx != -1) {
					ir = i;
					idxr = idx;
				}
				tmp = tmp.tail;
				i++;
			}/*
			 * System.out.println(il); System.out.println(ir);
			 * System.out.println(idxl); System.out.println(idxr);
			 */
			if (il != -1 && ir != -1) {
				hashAval.add(il);
				hashAval.add(ir);
				hashCols.add(idxl);
				hashCols.add(idxr);
			}
		} else if (be instanceof parser.AndBoolExpr) {
			parser.AndBoolExpr abebe = (parser.AndBoolExpr) be;
			checkHashApp(abebe.left, trl);
			checkHashApp(abebe.right, trl);
		} else
			hashflag = false;
	}

	static boolean hashflag;
	static ArrayList<QueryPlan> hashfromPlans = new ArrayList<QueryPlan>();
	static ArrayList<Integer> hashAval = new ArrayList<Integer>();
	static ArrayList<Integer> hashCols = new ArrayList<Integer>();
	static final boolean useHashJoin = false;

	static QueryPlan translate(parser.Select selectAbsyn, Transaction tr,
			Tail tail) throws DeadlockException, TimeoutException {
		parser.HasFrom hasFrom = selectAbsyn.hasFrom;
		parser.HasWhere hasWhere = hasFrom.hasWhere;
		parser.HasGroup hasGroup = null;
		parser.HasHaving hasHaving = null;
		parser.HasOrder hasOrder = null;
		if (hasWhere != null)
			hasGroup = hasWhere.hasGroup;
		if (hasGroup != null)
			hasHaving = hasGroup.hasHaving;
		if (hasHaving != null)
			hasOrder = hasHaving.hasOrder;

		QueryPlan fromPlan = null;
		/*
		 * Brute Join
		 */
		if (!useHashJoin)
			fromPlan = translate(hasFrom, tr, tail);
		/*
		 * End of the Brute Methods
		 */
		else {
			/*
			 * Apply Hash Join
			 */
			parser.TblRefList trl = hasFrom.trl;

			hashfromPlans.clear();
			hashAval.clear();
			hashCols.clear();
			while (trl != null) {
				parser.TblRef trf = trl.head;
				QueryPlan p = translate(trf, tr, tail);
				hashfromPlans.add(p);
				trl = trl.tail;
			}
			if (hashfromPlans.size() == 1)
				fromPlan = hashfromPlans.get(0);
			else {
				if (hasWhere != null) {
					hashflag = true;
					checkHashApp(hasWhere.be, hasFrom.trl);
					if (hashflag && hashAval.size() > 0) {
						for (int i = 0; i < hashAval.size() / 2; i++) {
							QueryPlan p1 = hashfromPlans.get(hashAval
									.get(i * 2));
							QueryPlan p2 = hashfromPlans.get(hashAval
									.get(i * 2 + 1));
							if (p1 == null || p2 == null)
								continue;
							int c1 = hashCols.get(i * 2);
							int c2 = hashCols.get(i * 2 + 1);

							HashJoinPlan hj = new HashJoinPlan(p1, p2, c1, c2,
									tr);
							if (fromPlan == null)
								fromPlan = hj;
							else
								fromPlan = new JoinPlan(fromPlan, hj, tr);
							hashfromPlans.set(hashAval.get(i * 2), null);
							hashfromPlans.set(hashAval.get(i * 2 + 1), null);
						}
					}
				}
				for (int i = 0; i < hashfromPlans.size(); i++)
					if (hashfromPlans.get(i) != null) {
						if (fromPlan == null)
							fromPlan = hashfromPlans.get(i);
						else
							fromPlan = new JoinPlan(fromPlan,
									hashfromPlans.get(i), tr);
					}
			}
			/*
			 * End of the Application of Hash Join
			 */
		}
		Cond cond;
		if (hasWhere != null)
			cond = new Cond(hasWhere.be, tr);
		else
			cond = new Cond(null, tr);
		QueryPlan ret;
		if (hasGroup == null || hasGroup.cn == null) {
			SelectPlan sPlan = new SelectPlan(fromPlan, selectAbsyn.isDistinct,
					selectAbsyn.selectExprList, cond, tr, tail);
			ret = sPlan;
		} else {
			parser.BoolExpr be = null;
			if (hasHaving != null)
				be = hasHaving.boolExpr;
			GroupPlan gPlan = new GroupPlan(fromPlan,
					selectAbsyn.selectExprList, be, hasGroup.cn, cond, tr, tail);
			ret = gPlan;

		}
		if (hasOrder != null && hasOrder.colNameList != null) {
			parser.ColNameList cnList = hasOrder.colNameList;
			ArrayList<Integer> compIdx = new ArrayList<Integer>();
			ArrayList<Integer> compSlt = new ArrayList<Integer>();
			while (cnList != null) {
				int idx = new Col2Idx().getIdx(ret.alias, cnList.colName);
				compIdx.add(idx);
				if (cnList.isASC)
					compSlt.add(1);
				else if (cnList.isDESC)
					compSlt.add(-1);
				else
					compSlt.add(0);
				cnList = cnList.tail;
			}
			for (int i = compSlt.size() - 1; i >= 0; i--) {
				if (compSlt.get(i) == 0) {
					if (i == compSlt.size() - 1)
						compSlt.set(i, 1);
					else
						compSlt.set(i, compSlt.get(i + 1));
				}
			}
			Comp comp = new Comp(compIdx, compSlt);
			return new SortPlan(tr, ret, comp);
		}

		return ret;
	}

	static QueryPlan translate(parser.HasFrom hasFromAbsyn, Transaction tr,
			Tail tail) throws DeadlockException, TimeoutException {
		parser.TblRefList trl = hasFromAbsyn.trl;
		if (trl.tail == null)
			return translate(trl.head, tr, tail);
		parser.TblRef tr1 = trl.head;
		trl = trl.tail;
		parser.TblRef tr2 = trl.head;

		QueryPlan ret = new JoinPlan((QueryPlan) translate(tr1, tr, tail),
				(QueryPlan) translate(tr2, tr, tail), tr);
		while (trl.tail != null) {
			trl = trl.tail;
			ret = new JoinPlan(ret, (QueryPlan) translate(trl.head, tr, tail),
					tr);
		}
		return ret;
	}

	static QueryPlan translate(parser.TblRef tblRefAbsyn, Transaction tr,
			Tail tail) throws DeadlockException, TimeoutException {
		if (tblRefAbsyn.tblName != null) {
			// System.out.println("Enumerating " + tblRefAbsyn.tblName);
			String tableName = tblRefAbsyn.tblName;
			Table table = TableManager.getTableShared(tr, tableName);
			String tableAlias = tblRefAbsyn.tblNameAlias;
			EnumeratePlan ret = new EnumeratePlan(table, tr);
			if (tableAlias != null) {
				for (int i = 0; i < table.numColumns(); i++) {
					ret.alias.get(i).addTableName(tableAlias);
				}
			}
			return ret;
		} else {
			QueryPlan ret = translate(tblRefAbsyn.subquery, tr, tail);
			String subqueryAlias = tblRefAbsyn.subqueryAlias;
			if (subqueryAlias != null) {
				for (int i = 0; i < ret.alias.size(); i++)
					ret.alias.get(i).addTableName(subqueryAlias);
			}
			return ret;
		}
	}
}
