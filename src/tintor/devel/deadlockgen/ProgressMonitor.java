package tintor.devel.deadlockgen;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import tintor.devel.deadlockgen.util.Statistics;
import tintor.devel.deadlockgen.util.Timer;

class ProgressMonitor implements Runnable {
	private final long start;
	private final Map map; // TODO remove

	private final Scheduler scheduler;

	private final boolean append;

	private final Thread thread;
	private final Timer timer = new Timer();
	private PrintWriter log;

	ProgressMonitor(final int width, final int height, final long start, final Scheduler scheduler,
			final boolean append) {
		map = new Map(width, height);
		this.start = start;

		this.scheduler = scheduler;
		this.append = append;

		thread = new Thread(this);
		thread.setName("progress monitor");
		thread.setDaemon(true);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}

	@Override
	public void run() {
		try {
			log = new PrintWriter(new FileWriter(map.width + "x" + map.height + ".log", append), true);
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
		timer.restart();

		while (true)
			try {
				Thread.sleep(1000);
				timer.restart();

				final Date now = new Date();

				final long tail = scheduler.tail();
				final long head = scheduler.head();

				final double absH = head * 100.0 / map.total;
				final double absT = tail * 100.0 / map.total;
				final double rel = (tail + head) * 0.5 / (map.total - start);
				map.load(tail);

				final String timeElapsed = timer.toString();
				final String timeLeft = Timer.format((long) (timer.time * (1 - rel) / rel));

				final String cells = new String(map.cells).replace(' ', '.');

				synchronized (log) {
					log.printf("time=%tH:%tM:%tS.%tL cells=%s "
							+ "progress=[%.4f %.4f] time=%s+%s\n", now, cells, absT, absH,
							timeElapsed, timeLeft);
					Statistics.dump(log);
					log.println();
				}
			}
			catch (final InterruptedException e) {
				log.printf("time=%s\n", timer);
				Statistics.dump(log);
				log.close();
				break;
			}
	}

	void stop() {
		timer.stop();
		thread.interrupt();
		System.out.println("time " + timer);
	}
}
