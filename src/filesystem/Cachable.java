package filesystem;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import transaction.Transaction;

public abstract class Cachable {
	
	int last;

	// TODO atomic reference ??
	Cachable next;
	
	public final AtomicInteger pinned = new AtomicInteger();
	
	public final LinkedList<Transaction> pinners = new LinkedList<Transaction>();
	
	public final AtomicBoolean refBit = new AtomicBoolean();
	
	public final AtomicBoolean dirty = new AtomicBoolean();
	
	
}
