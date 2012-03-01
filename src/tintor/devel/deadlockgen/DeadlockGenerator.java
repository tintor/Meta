package tintor.devel.deadlockgen;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class DeadlockGenerator {
	public static void main(final String[] args) throws Exception {
		final int width = 3, height = 4;
		final long start = 0;
		final long workUnit = 1000;
		final boolean writeAll = false;
		final boolean append = false;
		final int workers = 3;

		Locale.setDefault(Locale.US);

		final Writer out = new BufferedWriter(new FileWriter("+" + width + "x" + height + "_.txt", append));

		final Writer outAll;
		final DataOutputStream outB;

		if (writeAll) {
			outAll = new BufferedWriter(new FileWriter("+" + width + "x" + height + "_all.txt", append));

			outB = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(width + "x" + height
					+ ".bin")));
		}

		final Patterns patterns = new Patterns();
		patterns.load(2, 2);
		patterns.load(2, 3);
		patterns.load(3, 3);
		patterns.load(2, 5);
		patterns.load(3, 4);
		//patterns.load(3, 5);
		//patterns.load(4, 4);

		final Scheduler scheduler = new Scheduler(start, workUnit);
		final CountDownLatch done = new CountDownLatch(workers);

		final Runnable worker = new Runnable() {
			@Override
			public void run() {
				final Map map = new Map(width, height);
				while (true) {
					final long task = scheduler.begin();
					if (task >= map.total) break;

					map.load(task);
					do
						if (map.boxes >= 2 && !map.freeEdge() && map.unique()
								&& !patterns.matches(map) && map.isDeadlock()
								&& map.isMinimal()) map.print(out);
					while (map.next() && map.order < task + workUnit);

					scheduler.end(task);
				}
				done.countDown();
			}
		};

		final ProgressMonitor monitor = new ProgressMonitor(width, height, start, scheduler, append);
		for (int i = 0; i < workers; i++)
			new Thread(worker, "worker" + i).start();
		done.await();
		monitor.stop();

		out.write("done\n");
		out.close();

		if (writeAll) {
			outAll.write("done\n");
			outAll.close();

			outB.close();
		}
	}
}

class Scheduler {
	private long[] finished = new long[16];
	private int size;

	private final AtomicLong head = new AtomicLong();
	private long tail;
	private final long delta;

	public Scheduler(final long init, final long delta) {
		this.head.set(init);
		this.delta = delta;
		if (delta < 1) throw new IllegalArgumentException();
	}

	public long head() {
		return head.get();
	}

	public synchronized long tail() {
		return tail;
	}

	public long begin() {
		return head.incrementAndGet();
	}

	public synchronized void end(final long task) {
		if (task == tail) {
			tail += delta;
			if (size > 0) {
				Arrays.sort(finished, 0, size);
				int i = 0;
				while (i < size && finished[i] == tail) {
					tail += delta;
					i++;
				}
				if (i > 0) System.arraycopy(finished, i, finished, 0, i);
				size -= i;
			}
		}
		else {
			if (size == finished.length) finished = Arrays.copyOf(finished, finished.length * 2);
			finished[size++] = task;
		}
	}
}