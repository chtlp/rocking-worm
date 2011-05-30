package jdbc;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import logging.BuildCheckpoint;
import logging.Recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Config;
import util.Constant;
import filesystem.FileStorage;

public class Startup {
	static Logger logger = LoggerFactory.getLogger("lq.jdbc");

	public static void main(String args[]) throws Exception {
		// initialize database
		System.out.println("initialzing database...");

		Config.load("rockingworm.config");
		Constant.initialize();

		String dataFileName = Config.getDataFile();

		FileStorage.loadFile(dataFileName);
		Recovery.recover(); // try to recover from a crash
		FileStorage.init();

		// start a new thread to build checkpoint every 10 minutes
		BuildCheckpoint.start();
		
		System.out.println("finish database initialzing");

		// post the server entry in the RMI registry
		RemoteDriver d = new RemoteDriverImpl();
		Registry registry = LocateRegistry.createRegistry(Constant.rmiPort);
		registry.bind(Constant.rmiName, d);

		System.out.println("database server ready");
	}
}
