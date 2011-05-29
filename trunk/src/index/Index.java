package index;

import table.Record;
import table.Storable;
import table.Table;
import table.TableIterator;
import transaction.Transaction;
import value.IntValue;
import value.Value;
import filesystem.BufferManager;
import filesystem.Page;

public abstract class Index implements Storable, TableIterator {

	public static Index loadIndex(Transaction tr, Table table, int rootPageID) {
		int root = rootPageID;
		Page p = BufferManager.getPage(tr, root);
		if (p.getType() == Page.TYPE_BTREE_HEADER) {
			BPlusIndex index = BPlusIndex.loadFrom(tr, table, p.getPageID());
			p.release(tr);
			return index;
		} else {
			p.release(tr);
			throw new UnsupportedOperationException();
		}

	}

	@Override
	public int fromBytes(byte[] stream, int offset) {
		IntValue i = new IntValue();
		i.fromBytes(stream, offset);
		return byteLength();
	}

	@Override
	public byte[] toBytes(Transaction tr) {
		IntValue i = new IntValue(metaPageID);
		return i.toBytes(tr);
	}

	@Override
	public int byteLength() {
		return Page.INT_SIZE;
	}

	public static final int SEARCH_BEFORE = -1;
	public static final int SEARCH_AT = 0; // GT
	public static final int SEARCH_AFTER = 1;

	// index name
	protected String name;
	protected Table table;
	public int columnID;

	int metaPageID;

	// type of the key
	public int keyType;
	// length of the key
	public int keyLength;

	// whether this is a primary index, i.e. stores the actual data
	protected boolean isPrimary;

	abstract public Record find(Value key);

	abstract public TableIterator findEqual(Value key);

	abstract public TableIterator findRange(Integer left, Value lv, Integer right, Value rv);

	/**
	 * The table this index is on
	 * 
	 * @return
	 */
	public Table getTable() {
		return table;
	}

	/**
	 * whether this index is a primary index; primary index stores the actual
	 * data while second index just stores a rowID
	 * 
	 * @return
	 */
	public boolean isPrimaryIndex() {
		return isPrimary;
	}

	/**
	 * add a record into a primary index
	 * 
	 * @param tr
	 * @param r
	 * @return the rid of the newly add record
	 */
	abstract public int add(Transaction tr, Record r);

	/**
	 * add a rid into a secondary index
	 * 
	 * @param tr
	 * @param key
	 * @param rid
	 */
	abstract public void add(Transaction tr, Value key, int rid);

	// add many records at the same time
	abstract public void bulkadd(Transaction tr, TableIterator r);

	public int getRootPage() {
		return metaPageID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// primary index ==> remove on primary key; secondary index ==> remove on key & rid
	abstract public boolean removeUnique(Transaction tr, Record r);
	

	// abstract public void update(Transaction tr, Record r, int columnID,
	// Value newVal);

	abstract public void saveMetaData(Transaction tr);
	abstract public void drop(Transaction tr);

}
