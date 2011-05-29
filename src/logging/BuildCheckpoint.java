package logging;

import filesystem.BufferManager;
import transaction.Transaction;
import util.Config;
import util.Constant;

public class BuildCheckpoint implements Runnable {
	private volatile boolean doing = true;

	public synchronized int buildCheckpoint(int[] activeX) {
		new StartCheckpointRecord(activeX).writeToLog();
		BufferManager.flushAll();
		return new EndCheckpointRecord().writeToLog();
	}

	public void stop() {
		doing = false;
	}

	@Override
	public void run() {
		while (doing) {
			try {
				buildCheckpoint(Transaction.getActiveX());
				Thread.sleep(Constant.CKPT_PERIOD_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void start() {
		if (Config.getBoolean("CheckpointOption"))
			new Thread(new BuildCheckpoint()).start();
	}
}
