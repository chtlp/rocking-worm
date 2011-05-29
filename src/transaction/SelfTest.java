package transaction;

public class SelfTest {
	static int a, b;
	static TableLock A, B;

	public static void main(String[] args) {
		a = 10;
		b = 20;
		A = new TableLock();
		B = new TableLock();
		new Thread() {
			@Override
			public void run() {
				Transaction t = Transaction.begin();
				try {
					A.lock(t, false);
					sleep(500);
					B.lock(t, true);
					b = 30;
					System.out.println(a);
					System.out.println(b);
					sleep(1000);
					t.unlockAll();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();

		new Thread() {
			@Override
			public void run() {
				Transaction t = Transaction.begin();
				try {
					B.lock(t, false);
					sleep(500);
					A.lock(t, true);
					a = 30;
					System.out.println(a);
					System.out.println(b);
					t.unlockAll();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}

}
