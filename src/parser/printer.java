package parser;

public class printer {
	public printer(AbsynList a){ 
		if (a!=null )System.out.println("ExpList");
		while (a!=null) {
			printAbsyn(a.head,1);
			a = a.tail;
		}
	}
	
	void print(int p){
		for (int i=0;i<p*2;i++)
			System.out.print(" ");
	}
	
	void prExp(CreateDatabase e, int d){
		print(d);
		if (e!=null )System.out.println("CreateDatabase "+e.dbName);
	}
	
	void prExp(UseDatabase e, int d){
		print(d);
		if (e!=null )System.out.println("UseDatabase "+e.dbName);
	}
	
	void prExp(DropDatabase e, int d){
		print(d);
		if (e!=null )System.out.println("DropDatabase "+e.dbName);
	}
	
	void prExp(CreateTable e, int d){
		print(d);
		if (e!=null )System.out.println("CreateTable "+e.tlbName);
		prExp((CreateDefList)e.cDefList,d+1);
	}
	
	void prExp(CreateDefList e, int d){
		print(d);
		if (e!=null )System.out.println("CreateDefList");
		while (e!=null){
			prExp((CreateDef) e.head, d+1);
			e = e.tail;
		}
	}
	
	void prExp(CreateDef e, int d){
		print(d);
		if (e!=null )System.out.println("CreateDef");
		if (e.cDef!=null) prExp((ColumnDef)e.cDef, d+1);
		else{
			print(d+1);
			System.out.println(e.primaryKey);
		}
	}
	
	void prExp(ColumnDef e, int d){
		print(d);
		if (e!=null )System.out.print("ColumnDef "+e.colName);
		if (e.isNull!=null)
			prExp((IsNull)e.isNull , d+1);
	}
	
	void prExp(IsNull e, int d){
		print(d);
		if (e!=null )System.out.println("IsNull");
		if (e.hasDefault!=null)
			prExp((HasDefault)e.hasDefault,d+1);
	}
	
	void prExp(HasDefault e, int d){
		print(d);
		if (e!=null )System.out.print("HasDefault");
		if (e.isAuto!=null)
			prExp((IsAuto)e.isAuto, d+1);
	}
	
	void prExp(IsAuto e, int d){
		print(d);
		if (e!=null )System.out.println("IsAuto");
	}
	
	
	void prExp(DropTable e, int d){
		print(d);
		if (e!=null )System.out.println("DropTable");
	}
	
	void prExp(Insert e, int d){
		print(d);
		if (e!=null )System.out.println("Insert");
		if (e.valueList!=null)
			prExp((ValueList) e.valueList, d+1);
		if (e.select!=null)
			prExp((Select)e.select, d+1);
		if (e.colNameList!=null)
			prExp((ColNameList)e.colNameList, d+1);
	}
	
	void prExp(ColNameList e ,int d){
		print(d);
		if (e!=null )System.out.println("ColNameList");
		while (e!=null){
			prExp((ColName)e.colName, d+1);
			e = e.tail;
		}
	}
	
	void prExp(ValueList e, int d){
		print(d);
		if (e!=null )System.out.println("ValueList");
		while (e!=null){
			prExp((Value)e.head, d+1);
			e = e.tail;
		}
	}
	
	void prExp(Value e, int d ){
		print(d);
		if (e!=null )System.out.println("Value");
		if (e instanceof BracketValue) prExp((BracketValue)e, d+1);
		else if (e instanceof ColNameValue) prExp((ColNameValue)e, d+1);
		else if (e instanceof ConstValue) prExp((ConstValue)e, d+1);
		else if (e instanceof AopValue) prExp((AopValue)e, d+1);
		else if (e instanceof SubqueryValue) prExp((SubqueryValue)e, d+1);
		else if (e instanceof FuncValue) prExp((FuncValue)e, d+1);
	}
	
	void prExp(BracketValue e, int d){
		print(d);
		if (e!=null )System.out.println("BracketValue");
		prExp((Value)e.value ,d+1);
	}
	
	void prExp(ColNameValue e, int d){
		print(d);
		if (e!=null )System.out.println("ColNameValue");
		prExp((ColName)e.colName, d+1);
	}
	
	void prExp(ColName e, int d){
		print(d);
		if (e!=null )System.out.println("ColName "+e.colName);
	}
	
	void prExp(ConstValue e, int d){
		print(d);
		if (e!=null )System.out.println("ConstValue");
		if (e instanceof IntConstValue) prExp((IntConstValue)e,d+1);
		else if (e instanceof StringConstValue) prExp((StringConstValue)e,d+1);
		else if (e instanceof FloatConstValue) prExp((FloatConstValue)e, d+1);
		else if (e instanceof BoolConstValue) prExp((BoolConstValue)e, d+1);
		else if (e instanceof NullConstValue) prExp((NullConstValue)e, d+1);
		else if (e instanceof DefaultConstValue) prExp((DefaultConstValue)e, d+1);
	}
	
	void prExp(AopValue e, int d){
		print(d);
		if (e!=null )System.out.println("AopValue");
		prExp((Value)e.valueL, d+1);
		prExp((Value)e.valueR, d+1);
	}
	
	void prExp(SubqueryValue e, int d){
		print(d);
		if (e!=null )System.out.println("SubqueryValue");
		prExp((Select)e.subquery, d+1);
	}
	
	void prExp(FuncValue e, int d){
		print(d);
		if (e!=null )System.out.println("FuncValue");
		prExp((ColName)e.colName, d+1);
	}
	
	void prExp(IntConstValue e, int d){
		print(d);
		if (e!=null )System.out.println("IntConstValue");
	}
	
	void prExp(StringConstValue e, int d){
		print(d);
		if (e!=null )System.out.println("StringConstValue");
	}
	
	void prExp(FloatConstValue e, int d){
		print(d);
		if (e!=null )System.out.println("FloatConstValue");
	}
	
	void prExp(BoolConstValue e, int d){
		print(d);
		if (e!=null )System.out.println("BoolConstValue");
	}
	
	void prExp(NullConstValue e, int d){
		print(d);
		if (e!=null )System.out.println("NullConstValue");
	}
	
	void prExp(DefaultConstValue e, int d){
		print(d);
		if (e!=null )System.out.println("DefaultConstValue");
	}
	
	
	void prExp(Delete e, int d){
		print(d);
		if (e!=null )System.out.println("Delete");
		prExp((BoolExpr)e.whereCondition, d+1);
	}
	
	void prExp(BoolExpr e, int d){
		print(d);
		if (e!=null )System.out.println("BoolExpr");
		if (e instanceof CopValue) prExp((CopValue)e,d+1);
		else if (e instanceof AndBoolExpr) prExp((AndBoolExpr)e,d+1);
		else if (e instanceof OrBoolExpr) prExp((OrBoolExpr)e,d+1);
		else if (e instanceof ExistBoolExpr) prExp((ExistBoolExpr)e,d+1);
		else if (e instanceof AnyBoolExpr) prExp((AnyBoolExpr)e,d+1);
		else if (e instanceof InBoolExpr) prExp((InBoolExpr)e,d+1);
		else if (e instanceof AllBoolExpr) prExp((AllBoolExpr)e,d+1);
		else if (e instanceof BracketBoolExpr) prExp((BracketBoolExpr)e,d+1);
	}
	
	void prExp(CopValue e, int d){
		print(d);
		if (e!=null )System.out.println("CopValue");
		prExp((Value)e.valueL, d+1);
		prExp((Value)e.valueR, d+1);
	}
	
	void prExp(AndBoolExpr e, int d){
		print(d);
		if (e!=null )System.out.println("AndBoolExpr");
		prExp((BoolExpr)e.left, d+1);
		prExp((BoolExpr)e.right, d+1);
	}
	
	void prExp(OrBoolExpr e, int d){
		print(d);
		if (e!=null )System.out.println("OrBoolExpr");
		prExp((BoolExpr)e.left, d+1);
		prExp((BoolExpr)e.right, d+1);
	}
	
	void prExp(ExistBoolExpr e, int d){
		print(d);
		if (e!=null )System.out.println("ExistBoolExpr");
		prExp((Select)e.subquery, d+1);
	}
	
	void prExp(AnyBoolExpr e, int d){
		print(d);
		if (e!=null )System.out.println("AnyBoolExpr");
		prExp((Value)e.value,d+1);
		prExp((Select)e.subquery, d+1);
	}
	
	void prExp(InBoolExpr e, int d){
		print(d);
		if (e!=null )System.out.print("InBoolExpr");
		prExp((Value)e.value,d+1);
		prExp((Select)e.subquery, d+1);
	}
	
	void prExp(AllBoolExpr e, int d){
		print(d);
		if (e!=null )System.out.print("AllBoolExpr");
		prExp((Value)e.value,d+1);
		prExp((Select)e.subquery, d+1);
	}
	
	void prExp(BracketBoolExpr e, int d){
		print(d);
		if (e!=null )System.out.print("BracketBoolExpr");
		prExp((BoolExpr)e.boolExpr, d+1);
	}
	
	void prExp(Update e, int d){
		print(d);
		if (e!=null )System.out.println("Update");
		prExp((UpdateList)e.updateList, d+1);
		if (e.boolExpr!=null) prExp((BoolExpr)e.boolExpr, d+1);
	}
	
	void prExp(UpdateList e, int d){
		print(d);
		if (e!=null )System.out.println("UpDateList");
		while (e!=null){
			prExp((ColName)e.head, d+1);
			prExp((Value)e.value, d+1);
			e = e.tail;
		}
	}
	
	
	void prExp(CreateIndex e, int d){
		print(d);
		if (e!=null )System.out.println("CreateIndex");
		prExp((ColName)e.colName, d+1);
	}
	
	void prExp(DropIndex e, int d){
		print(d);
		if (e!=null )System.out.println("DropIndex");
	}
	
	void prExp(Select e, int d){
		print(d);
		if (e!=null )System.out.println("Select");
		prExp((SelectExprList)e.selectExprList, d+1);
		if (e.hasFrom!=null) prExp((HasFrom)e.hasFrom, d+1);
	}
	
	void prExp(SelectExprList e, int d){
		print(d);
		if (e!=null )System.out.println("SlectExprList");
		while (e!=null){
			prExp((SelectExpr)e.head, d+1);
			e = e.tail;
		}
	}
	
	void prExp(SelectExpr e, int d){
		print(d);
		if (e!=null )System.out.println("SelectExpr");
		if (e.value!=null) prExp((Value)e.value, d+1);
	}
	
	void prExp(HasFrom e, int d){
		print(d);
		if (e!=null )System.out.println("HasFrom");
	//	prExp((TblRefList)e.trl,d+1);
		if (e.hasWhere!=null) prExp((HasWhere)e.hasWhere, d+1);
	}
	
	void prExp(HasWhere e, int d){
		print(d);
		if (e!=null )System.out.println("HasWhere");
		if (e.be!=null) prExp((BoolExpr)e.be, d+1);
		if (e.hasGroup!=null) prExp((HasGroup)e.hasGroup, d+1);
	}
	
	void prExp(HasGroup e, int d){
		print(d);
		if (e!=null )System.out.println("HasGroup");
		if (e.cn!=null) prExp((ColName)e.cn, d+1);
		if (e.hasHaving!=null) prExp((HasHaving) e.hasHaving, d+1);
	}
	
	void prExp(HasHaving e, int d){
		print(d);
		if (e!=null )System.out.println("HasHaving");
		if (e.boolExpr!=null) prExp((BoolExpr)e.boolExpr, d+1);
		if (e.hasOrder!=null) prExp((HasOrder)e.hasOrder, d+1);
	}
	
	void prExp(HasOrder e, int d){
		print(d);
		if (e!=null )System.out.println("HasOrder");
		prExp((ColNameList)e.colNameList, d+1);
	}
		
	
	public void printAbsyn(Absyn e, int d) {
		print(d);
		if (e!=null )System.out.println("Absyn");
		if (e instanceof CreateDatabase) prExp((CreateDatabase) e, d+1);
		else if (e instanceof UseDatabase) prExp((UseDatabase) e, d+1);
		else if (e instanceof DropDatabase) prExp((DropDatabase) e, d+1);
		else if (e instanceof CreateTable) prExp((CreateTable) e, d+1);
		else if (e instanceof DropTable) prExp((DropTable) e, d+1);
		else if (e instanceof Insert) prExp((Insert) e, d+1);
		else if (e instanceof Delete) prExp((Delete) e, d+1);
		else if (e instanceof Update) prExp((Update) e, d+1);
		else if (e instanceof CreateIndex) prExp((CreateIndex) e, d+1);
		else if (e instanceof DropIndex) prExp((DropIndex) e, d+1);
		else if (e instanceof Select) prExp((Select) e, d+1);
		else throw new Error("Print.prExp");
	  }
}
