package filesystem;

public class PageOverflowException extends RuntimeException {

	private static final long serialVersionUID = 8124692118460733180L;

	public PageOverflowException() {
		super("fatworm page overflow");
	}
	
	public PageOverflowException(String msg) {
		super(msg);
	}
}
