package tintor.devel.work_stealing_deque;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
	public static void main(final String[] args) throws InterruptedException {
		final int[] tasks = new int[100];
		final Random random = new Random();
		for (int i = 0; i < tasks.length; i++)
			tasks[i] = random.nextInt(400);

		final long time = System.nanoTime();
		runSequential(tasks);
		System.err.println(System.nanoTime() - time);

		final long time2 = System.nanoTime();
		runParallel(tasks);
		System.err.println(System.nanoTime() - time2);
	}

	static void runSequential(final int[] tasks) {
		for (int id = 0; id < tasks.length; id++)
			runTask(id, tasks[id]);
	}

	static void runParallel(final int[] tasks) throws InterruptedException {
		final ExecutorService executor = new ThreadPoolExecutor(10, 10, 30, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(tasks.length));

		for (int i = 0; i < tasks.length; i++) {
			final int id = i;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					runTask(id, tasks[id]);
				}
			});
		}
		executor.shutdown();
		System.out.println(executor.awaitTermination(10, TimeUnit.MINUTES));
		System.out.flush();
	}

	static void runTask(final int id, final int task) {
		try {
			System.out.println(id);
			System.out.flush();
			Thread.sleep(task);
		}
		catch (final InterruptedException e) {
		}
	}
}