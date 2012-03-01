package tintor.devel.software_transactional_memmory;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class TransactionalMemory {
	public static void main(final String[] args) throws InterruptedException {
		final Value[] values = new Value[3];
		for (int i = 0; i < values.length; i++)
			values[i] = new Value(0);

		// run worker threads
		final int workers = 1000;
		final CountDownLatch begin = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(workers);

		final Runnable job = new Runnable() {
			@Override
			public void run() {
				final Random rand = new Random();
				try {
					begin.await();
					for (int i = 0; i < 1000; i++) {
						final Value from = values[rand.nextInt(values.length)];
						final Value to = values[rand.nextInt(values.length)];
						transfer(from, to, rand.nextInt(100));
					}
					end.countDown();
				}
				catch (final InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};

		for (int i = 0; i < workers; i++)
			new Thread(job).start();
		begin.countDown();
		end.await();

		// check consistency
		int sum = 0;
		for (final Value v : values)
			sum += v.read();
		System.out.println(sum);
	}

	static void transfer(final Value from, final Value to, final int amount) {
		Manager.atomicaly(new Runnable() {
			@Override
			public void run() {
				final int a = from.read();
				Thread.yield();
				from.write(a - amount);
				Thread.yield();
				final int b = to.read();
				Thread.yield();
				to.write(b + amount);
			}
		});
	}
}

class Value {
	private int value;

	Value(final int value) {
		this.value = value;
	}

	int read() {
		if (!Manager.atomic.get()) throw new RuntimeException("value can be read only in transaction");
		return value;
	}

	void write(final int value) {
		if (!Manager.atomic.get()) throw new RuntimeException("value can be writtend only in transaction");
		this.value = value;
	}
}

class Manager {
	static final ThreadLocal<Boolean> atomic = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		}
	};

	static void atomicaly(final Runnable runnable) {
		if (atomic.get()) throw new RuntimeException("nested transactions not allowed");
		atomic.set(true);
		runnable.run();
		atomic.set(false);
	}

	static void retry() {

	}
}