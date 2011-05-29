package filesystem;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Cachable {
	
	int last;

	// TODO atomic reference ??
	Cachable next;
	
	public final AtomicInteger pinned = new AtomicInteger();
	
	public final AtomicBoolean refBit = new AtomicBoolean();
	
	public final AtomicBoolean dirty = new AtomicBoolean();
	
	
}
