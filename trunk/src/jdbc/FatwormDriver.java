package jdbc;

import java.sql.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.Constant;

/**
 * The SimpleDB database driver.
 */
public class FatwormDriver extends DriverAdapter {
	static {
		try {
			java.sql.DriverManager.registerDriver(new FatwormDriver());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static final String hostPattern = "[a-z0-9]+";
	private static final String portPattern = "[0-9]+";
	private static final String patternString = String.format(
			"^jdbc:%1$s://(%2$s)(:(%2$s))?/", Constant.rmiName, hostPattern,
			portPattern);
	private static final Pattern pattern = Pattern.compile(patternString);

	/**
	 * Connects to the SimpleDB server on the specified host. The method
	 * retrieves the RemoteDriver stub from the RMI registry on the specified
	 * host. It then calls the connect method on that stub, which in turn
	 * creates a new connection and returns the RemoteConnection stub for it.
	 * This stub is wrapped in a SimpleConnection object and is returned.
	 * <P>
	 * The current implementation of this method ignores the properties
	 * argument.
	 * 
	 * @see java.sql.Driver#connect(java.lang.String, Properties)
	 */
	public Connection connect(String url, Properties prop) throws SQLException {
		try {
			Matcher matcher = pattern.matcher(url);
			if (matcher.find()) {
				String host = matcher.group(1);
				int port = Integer.parseInt(matcher.group(3));
				Registry registry = LocateRegistry.getRegistry(host, port);
				RemoteDriver rdvr = (RemoteDriver) registry
						.lookup(Constant.rmiName);
				RemoteConnection rconn = rdvr.connect();
				return new FatwormConnection(rconn);
			}
			else
				return null;
		} catch (RemoteException e) {
			throw new SQLException(e);
		} catch (NotBoundException e) {
			e.printStackTrace();
			return null;
		}
	}
}
