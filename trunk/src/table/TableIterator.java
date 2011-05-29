package table;

import java.io.PrintStream;


public interface TableIterator {
	public int numColumns();
	
	public Column getColumn(int index);
	
	public void open();
	
	public boolean hasNext();
	
	public Record next();
	
	public void close();
	
	public void print(PrintStream out);
}
