package tintor.devel.deadlockgen.util;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Statistics {
	private double sum, sumOfSquares;
	private int count;
	private int max = Integer.MIN_VALUE;
	private final Statistics next;

	private static Map<String, Statistics> map = new HashMap<String, Statistics>();

	public Statistics(final String name) {
		synchronized (map) {
			next = map.get(name);
			map.put(name, this);
		}
	}

	public void add(final int a) {
		if (a > max) max = a;
		count += 1;
		sum += a;
		sumOfSquares += (double) a * (double) a;
	}

	int max() {
		int aggregate = Integer.MIN_VALUE;
		for (Statistics s = this; s != null; s = s.next)
			aggregate = Math.max(aggregate, s.max);
		return aggregate;
	}

	int count() {
		int aggregate = 0;
		for (Statistics s = this; s != null; s = s.next)
			aggregate += s.count;
		return aggregate;
	}

	double sum() {
		double aggregate = 0;
		for (Statistics s = this; s != null; s = s.next)
			aggregate += s.sum;
		return aggregate;
	}

	double sumOfSquares() {
		double aggregate = 0;
		for (Statistics s = this; s != null; s = s.next)
			aggregate += s.sumOfSquares;
		return aggregate;
	}

	public static void dump(final PrintWriter w) {
		synchronized (map) {
			for (final Map.Entry<String, Statistics> e : map.entrySet()) {
				final Statistics s = e.getValue();

				final int count = s.count();
				final double average = s.sum() / count;
				final double deviation = Math.sqrt(s.sumOfSquares() / count - average * average);

				w.printf("%s: average=%.2f deviation=%.2f max=%d\n", e.getKey(), average, deviation, s
						.max());
			}
			w.flush();
		}
	}
}