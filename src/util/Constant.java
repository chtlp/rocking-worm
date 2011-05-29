package util;

public class Constant {
	public static final String LoggingFile = "rockingworm.log";
	public static final String rmiName = "fatworm";
	public static final int rmiPort = 30000;
	public static int INITIAL_LOCK_TIMEOUT = 2000;
	public static final int CKPT_PERIOD_TIME = 2*1000;//10 * 60 * 1000; // build checkpoint every 10 minutes
	
	public static void initialize() {
		INITIAL_LOCK_TIMEOUT = Config.getTimedOut(); // TODO
	}
}
