package filesystem;

public class PageLocator {
	public final int pageID;
	public final int ind;
	
	public PageLocator(int p, int i) {
		pageID = p;
		ind = i;
	}

	@Override
	public String toString() {
		return String.format("(page=%d, ind=%d)", pageID, ind);
	}
	
	
}
