package filesystem;

public class PageReplacementException extends RuntimeException {

	private static final long serialVersionUID = -7160685199619660238L;

	public PageReplacementException(){
		super("No unpinned page for replacement");
	}
}
