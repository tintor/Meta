package tintor.devel.sandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ThreadedSimulator {
	static final int threads = 4;
	final CyclicBarrier begin = new CyclicBarrier(threads + 1);
	final CyclicBarrier end = new CyclicBarrier(threads + 1);
	private final List<Contact> list = new ArrayList<Contact>();

	class Worker implements Runnable {
		public void run() {
			while (true) {
				try {
					begin.await();

					end.await();
				} catch (InterruptedException e) {
					System.err.println(e);
					break;
				} catch (BrokenBarrierException e) {
					System.err.println(e);
					break;
				}
			}
		}
	}

	static class Body {

	}

	static class Contact {
		Body a, b;

		void process() {
			// read-process-write a and b
		}
	}

	public static void main(String[] args) {
		new ThreadedSimulator().step();
	}

	ThreadedSimulator() {
		for (int i = 0; i < threads; i++) {
			new Thread(new Worker()).start();
		}
	}

	void step() {
		// discover contacts
		try {
			begin.await();
			for (int i = 0; i < 10; i++)
				for (Contact c : list)
					c.process();
			end.await();
		} catch (InterruptedException e) {
			System.err.println(e);
		} catch (BrokenBarrierException e) {
			System.err.println(e);
		}
	}
}