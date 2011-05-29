package jdbc;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import plan.Alia;

import table.Column;

/**
 * The RMI server-side implementation of RemoteMetaData.
 */
@SuppressWarnings("serial")
public class RemoteMetaDataImpl extends UnicastRemoteObject implements
		RemoteMetaData {

	private ArrayList<Alia> alias;
	private ArrayList<Column> columns;

	/**
	 * Creates a metadata object that wraps the specified schema. The method
	 * also creates a list to hold the schema's collection of field names, so
	 * that the fields can be accessed by position.
	 * 
	 * @param sch
	 *            the schema
	 * @throws RemoteException
	 */
	public RemoteMetaDataImpl(ArrayList<Column> columns, ArrayList<Alia> alias) throws RemoteException {
		this.columns = columns;
		this.alias = alias;
	}

	/**
	 * Returns the size of the field list.
	 * 
	 * @see simpledb.remote.RemoteMetaData#getColumnCount()
	 */
	public int getColumnCount() throws RemoteException {
		Startup.logger.debug("column num = " + alias.size());
		return alias.size();
	}

	/**
	 * Returns the field name for the specified column number. In JDBC, column
	 * numbers start with 1, so the field is taken from position (column-1) in
	 * the list.
	 * 
	 * @see simpledb.remote.RemoteMetaData#getColumnName(int)
	 */
	public String getColumnName(int column) throws RemoteException {
		return alias.get(column - 1).getName();
	}

	/**
	 * Returns the type of the specified column. The method first finds the name
	 * of the field in that column, and then looks up its type in the schema.
	 * 
	 * @see simpledb.remote.RemoteMetaData#getColumnType(int)
	 */
	public int getColumnType(int column) throws RemoteException {
		return columns.get(column - 1).getType();
	}
}
