package tintor.devel.rigidbody;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

import tintor.devel.Visitor;

public class Factory {
	static class WorkerX extends Thread {

	}

	public static <T> void eachConcurrent(final List<T> list, final Visitor<T> visitor) throws InterruptedException {
		final SynchronousQueue queue = new SynchronousQueue();
		final Object dummy = new Object();

		Executors.newCachedThreadPool();

		final WorkerX[] workers = new WorkerX[Runtime.getRuntime().availableProcessors()];
		for (int i = 0; i < workers.length; i++)
			workers[i] = new WorkerX();

		for (final T item : list)
			queue.put(item);

		for (int i = 0; i < workers.length; i++)
			queue.put(dummy);

		for (int i = 0; i < workers.length; i++)
			workers[i].join();
	}

	public static void main(final String[] args) throws Exception {
		final CyclicBarrier barrier = new CyclicBarrier(3);
		final SynchronousQueue<Integer> orders = new SynchronousQueue<Integer>();
		final SynchronousQueue<Node> products = new SynchronousQueue<Node>();
		final Worker a = new Worker(orders, products, 1), b = new Worker(orders, products, 2);

		a.setPriority(Thread.currentThread().getPriority() + 1);
		b.setPriority(Thread.currentThread().getPriority() + 1);

		a.start();
		b.start();

		for (int i = 1; i <= 20; i++) {
			System.out.printf("0 put %s\n", i);
			orders.put(i);
		}

		orders.put(-2);
		orders.put(-2);

		for (Node e = products.take(); e != null; e = e.next)
			System.out.printf("list1: %s\n", e.data);
		for (Node e = products.take(); e != null; e = e.next)
			System.out.printf("list2: %s\n", e.data);

		for (int i = 21; i <= 40; i++) {
			System.out.printf("0 put %s\n", i);
			orders.put(i);
		}

		orders.put(-2);
		orders.put(-2);

		for (Node e = products.take(); e != null; e = e.next)
			System.out.printf("list1: %s\n", e.data);
		for (Node e = products.take(); e != null; e = e.next)
			System.out.printf("list2: %s\n", e.data);

		orders.put(-1);
		orders.put(-1);

		a.join();
		b.join();

		System.out.printf("0 exit\n");
	}
}

class Node {
	final int data;
	final Node next;

	Node(final int data, final Node next) {
		this.data = data;
		this.next = next;
	}
}

class Worker extends Thread {
	Worker(final BlockingQueue<Integer> inQueue, final BlockingQueue<Node> outQueue, final int id) {
		this.orders = inQueue;
		this.products = outQueue;
		this.id = id;
	}

	final BlockingQueue<Integer> orders;
	final BlockingQueue<Node> products;
	private final int id;
	private Node list;

	@Override
	public void run() {
		while (true)
			try {
				final int order = orders.take();
				if (order == -1) break;
				if (order == -2) {
					products.put(list);
					list = null;
					continue;
				}
				System.out.printf("%s take %s\n", id, order);
				list = new Node(-order, list);
			}
			catch (final Exception e) {
				throw new RuntimeException(e);
			}
		System.out.printf("%s exit\n", id);
	}
}