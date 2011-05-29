package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import tlp.util.Debug;

/**
 * This class is for loading and reading the configurations
 * @author TLP
 *
 */
public class Config {

	static Properties props = null;
	static HashMap<String, String> options = new HashMap<String, String>();
	
	public static void load(String fileName) {
		props = new Properties();
		try {
			props.load(new FileInputStream(fileName));
		} catch (IOException e) {
			e.printStackTrace();
			Debug.fsLogger.error("failed to load config file {}", fileName);
		}
		
	}
	
	public static String get(String key) {
		String res = options.get(key);
		if (res == null && props != null) res = props.getProperty(key);
		return res;
	}
	
	public static String getDataFile() {
		return get("DataFile");
	}
	
	public static String getLogFile() {
		return get("LogFile");
	}
	
	public static int getTimedOut() {
		return Integer.parseInt(get("TimedOut"));
	}

	public static boolean getLoggingOption() {
		return Boolean.parseBoolean(get("Logging"));
	}

	public static void set(String key, String val) {
		options.put(key, val);
	}

	public static boolean getBoolean(String key) {
		return Boolean.parseBoolean(props.getProperty(key));
	}


}
