package tintor.devel.index.sandbox;

import tintor.Timer;
import tintor.devel.index.BPlusTree;
import tintor.devel.index.BPlusTree.Iterator;

public class BPlusTree_Test {
	public static void main(final String[] args) {
		basicTest();
	}

	public static void basicTest() {
		final BPlusTree tree = new BPlusTree("bplus.tree", true, 3);

		for (int i = 1; i <= 10; i++)
			tree.put(i, i * 10);

		for (final BPlusTree.Iterator it = tree.iterator(); it.valid(); it.next())
			System.out.println(it.key() + " " + it.value());
	}

	public static void putLoadTest() {
		final Timer timer = new Timer();
		timer.restart();
		final BPlusTree tree = new BPlusTree("bplus.tree", true, 150);

		for (int i = 1; i <= 10000000; i++)
			tree.put(i, i * 10);
		timer.stop();

		System.out.println(timer + " height=" + tree.height());
	}
}
