package logging;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Recovery {
	private static Logger logger = LoggerFactory.getLogger("lq.logging.Recovery");
	private static HashSet<Integer> commitX = new HashSet<Integer>();
	private static HashSet<Integer> abortX = new HashSet<Integer>();
	private static HashSet<Integer> activeX = new HashSet<Integer>();

	private static Block blk;
	private static int offset;

	public static void recover() {
		blk = null;
		offset = 0;
		
		scan();
		redo();
		undo();
		
		LogManager.getInstance().clear();
	}

	private static void scan() {
		BackwardLogRecordIterator it = new BackwardLogRecordIterator();
		boolean checkpoint = false;
		while (it.hasNext()) {
			LogRecord rec = it.next();
			logger.debug(rec.toString());
			switch (rec.op()) {
				case LogRecord.COMMIT:
					commitX.add(rec.txID());
					break;
				case LogRecord.ROLLBACK:
					abortX.add(rec.txID());
					break;
				case LogRecord.START:
					if (!commitX.contains(rec.txID())
							&& !abortX.contains(rec.txID()))
						activeX.add(rec.txID());
					break;
				case LogRecord.ENDCHECKPOINT:
					checkpoint = true;
					break;
				case LogRecord.STARTCHECKPOINT:
					if (checkpoint) {
						StartCheckpointRecord check = (StartCheckpointRecord) rec;
						for (Integer i : check.txs)
							if (!commitX.contains(i) && !abortX.contains(i))
								activeX.add(i);
						blk = it.iter.blk;
						offset = it.iter.currentrec;
						return;
					}
					break;
			}
		}
	}

	private static void redo() {
		if (blk == null)
			return;
		LogRecordIterator it = new LogRecordIterator(blk, offset);
		while (it.hasNext()) {
			LogRecord rec = it.next();
			if (rec.op() == LogRecord.UPDATE && commitX.contains(rec.txID()))
				rec.redo();
		}
	}

	private static void undo() {
		BackwardLogRecordIterator it = new BackwardLogRecordIterator();
		while (it.hasNext()) {
			LogRecord rec = it.next();
			if (rec.op() == LogRecord.UPDATE
					&& (abortX.contains(rec.txID()) || activeX.contains(rec
							.txID())))
				rec.undo(rec.txID());
		}
	}
}
