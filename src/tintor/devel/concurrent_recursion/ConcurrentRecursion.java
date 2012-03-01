package tintor.devel.concurrent_recursion;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentRecursion {
	public static void main(final String[] args) throws Exception {
		final Node a = create(10);
		System.out.println(count(a));
		System.out.println(count2(a));
		pool.shutdown();
	}

	static Node create(final int depth) {
		if (depth == 0) return null;
		final Node a = new Node();
		a.left = create(depth - 1);
		a.right = create(depth - 1);
		return a;
	}

	static final AtomicInteger counter = new AtomicInteger(0);

	static final ExecutorService pool = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());

	static int count2(final Node a) throws Exception {
		if (a == null) return 0;

		final Future<Integer> l = pool.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				return count2(a.left);
			}
		});

		final Future<Integer> r = pool.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				return count2(a.right);
			}
		});

		final int result = 1 + l.get() + r.get();
		System.out.println(counter.incrementAndGet() / 1024.0);
		return result;
	}

	static int count(final Node a) {
		if (a == null) return 0;
		return 1 + count(a.left) + count(a.right);
	}

	static class Node {
		Node left, right;
	}
}