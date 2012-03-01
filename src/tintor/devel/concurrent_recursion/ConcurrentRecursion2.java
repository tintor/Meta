package tintor.devel.concurrent_recursion;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConcurrentRecursion2 {
	public static void main(final String[] args) throws Exception {
		final Node a = create(1, 3);
		// visit(a);
		visit2(a);
	}

	static Node create(final int name, final int depth) {
		if (depth == 0) return null;
		final Node a = new Node();
		a.name = name;
		a.left = create(name * 2, depth - 1);
		a.right = create(name * 2 + 1, depth - 1);
		return a;
	}

	static final ExecutorService pool = new ThreadPoolExecutor(0, 4, 1, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());

	static void visit2(final Node a) {
		if (a == null) return;
		System.out.println(a.name);

		pool.execute(new Runnable() {
			@Override
			public void run() {
				visit2(a.left);
			}
		});

		pool.execute(new Runnable() {
			@Override
			public void run() {
				visit2(a.right);
			}
		});
	}

	static void visit(final Node a) {
		if (a == null) return;
		System.out.println(a.name);
		visit(a.left);
		visit(a.right);
	}

	static class Node {
		int name;
		Node left, right;
	}
}