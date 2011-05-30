package util;

public class Constant {
	public static final String rmiName = "fatworm";
	public static final int rmiPort = 30000;
	public static int INITIAL_LOCK_TIMEOUT = 2000;
	public static String LoggingFile = "rockingworm.log";
	public static final int CKPT_PERIOD_TIME = 10 * 60 * 1000; // build checkpoint every 10 minutes
	
	public static void initialize() {
		INITIAL_LOCK_TIMEOUT = Config.getTimedOut();
		LoggingFile = Config.getLoggingFile();
	}
}
