package tlp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Config;


public class Debug {
	/**
	 * the global debug switch
	 */
	public static final boolean D = true;
	
	/**
	 * logger for filesystem
	 */
	public static Logger fsLogger = LoggerFactory.getLogger("filesystem");

	public static Logger bufferLogger = LoggerFactory.getLogger("buffermanager");
	
	public static Logger indexLogger = LoggerFactory.getLogger("indexmanager");
	
	public static Logger testLogger = LoggerFactory.getLogger("TestLogger.logger1");

	public static Logger testLoggerB = LoggerFactory.getLogger("TestLogger.logger2");
	
	public static Logger testLight = LoggerFactory.getLogger("TestLogger.testLight");

	public static Logger testLight2 = LoggerFactory.getLogger("TestLogger.testLight2");

	public static Logger testSimple = LoggerFactory.getLogger("TestLogger.testLight3");

	public static Logger tableLogger = LoggerFactory.getLogger("tablelogger");

	public static Logger testBulk = LoggerFactory.getLogger("TestLogger.testBulk");

	public static Logger tableManagerLogger = LoggerFactory.getLogger("tablemanager");
	
	public static Logger trLogger = LoggerFactory.getLogger("transaction.logger");
	
	// used for generating exceptions
	public static Logger errorLogger = LoggerFactory.getLogger("testing.errorlogger");
	
	public static Logger testJoin = LoggerFactory.getLogger("test.HashJoin");
	
	public static Logger testNullLogger = LoggerFactory.getLogger("testnull");

	public static boolean LOGGING = true;
	
	public static boolean MAKING_ERRORS = true;
	
	public static void init() {
		LOGGING = Config.getLoggingOption();
		MAKING_ERRORS = Boolean.parseBoolean(Config.get("MakingErrors"));
	}
	
	public static void breakHere() {
		testLogger.debug("break here");
	}
	
	public static boolean breakOn(boolean cond) {
		if (cond && testSimple.isInfoEnabled()) {
			testSimple.debug("testSimpleLogger breaks here");
			return true;
		}
		return false;
	}
	
	public static String showStr(String lstr) {
		String sub = lstr.length() < 10 ? lstr : lstr.substring(0, 10) + "...";
		return String.format("\"%s\"", sub);
	}
}
