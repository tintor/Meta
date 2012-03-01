package tintor.devel.r_tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;

public class RTree {
	public final boolean trace = false;

	private Node root;
	private int items, nodes;

	private final int degree, min;
	private final PickSplit pickSplit;
	private final Penalty penalty;

	public static class Entry {
		final Rect key;
		final Object value;

		Entry(final Rect key, final Object value) {
			this.key = key;
			this.value = value;
		}

		final static Comparator<Entry> compareX = new Comparator<Entry>() {
			@Override
			public int compare(final Entry a, final Entry b) {
				if (a.key.xmin < b.key.xmin) return -1;
				if (a.key.xmin > b.key.xmin) return 1;
				if (a.key.ymin < b.key.ymin) return -1;
				if (a.key.ymin > b.key.ymin) return 1;
				return 0;
			}
		};

		final static Comparator<Entry> compareY = new Comparator<Entry>() {
			@Override
			public int compare(final Entry a, final Entry b) {
				if (a.key.ymin < b.key.ymin) return -1;
				if (a.key.ymin > b.key.ymin) return 1;
				if (a.key.xmin < b.key.xmin) return -1;
				if (a.key.xmin > b.key.xmin) return 1;
				return 0;
			}
		};
	}

	private static class Node {
		final List<Entry> entries = new ArrayList<Entry>();
		final int level;
		Node parent;

		Node(final int level) {
			assert level >= 0;
			this.level = level;
		}

		int getIndexOfNode(final Node node) {
			for (int i = entries.size() - 1; i >= 0; i--)
				if (entries.get(i).value == node) return i;
			throw new RuntimeException();
		}

		Entry getEntryOfNode(final Node node) {
			for (final Entry e : entries)
				if (e.value == node) return e;
			throw new RuntimeException();
		}
	}

	public interface Consistent<T> {
		boolean consistent(Rect key, T query);
	}

	public interface PickSplit {
		void pickSplit(final List<Entry> list, int min, final List<Entry> partitionA, final List<Entry> partitionB);
	}

	public interface Penalty {
		double penalty(final Rect key, final Rect newKey);
	}

	public RTree(final int degree, final int min, final PickSplit pickSplit, final Penalty penalty) {
		if (degree < 2) throw new IllegalArgumentException("degree");
		if (degree + 1 < min * 2) throw new IllegalArgumentException("min");
		this.degree = degree;
		this.min = min;
		this.pickSplit = pickSplit;
		this.penalty = penalty;
		root = new Node(0);
	}

	public int height() {
		return root.level + 1;
	}

	public double efficiency() {
		return (double) items / (nodes * degree);
	}

	public <T> Iterator<Entry> search(final T query, final Consistent<T> consistent) {
		final int height = root.level + 1;
		final Stack<Iterator<Entry>> stack = new Stack<Iterator<Entry>>();
		stack.push(root.entries.iterator());

		return new Iterator<Entry>() {
			@Override
			public boolean hasNext() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Entry next() {
				while (stack.size() > 0) {
					final Iterator<Entry> i = stack.pop();
					if (!i.hasNext()) continue;

					final Entry e = i.next();
					stack.push(i);

					if (!consistent.consistent(e.key, query)) continue;
					if (stack.size() == height) return e;
					stack.push(((Node) e.value).entries.iterator());
				}
				return null;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	private static class Pair implements Comparable<Pair> {
		double distance;
		Entry entry;
		boolean leaf;

		@Override
		public int compareTo(final Pair a) {
			return Double.compare(distance, a.distance);
		}
	}

	public Iterator<Entry> nearestNeighbors(final double x, final double y) {
		final Queue<Pair> queue = new PriorityQueue<Pair>();
		for (final Entry e : root.entries) {
			final Pair p = new Pair();
			p.entry = e;
			p.distance = p.entry.key.distanceSquared(x, y);
			queue.add(p);
		}

		return new Iterator<Entry>() {
			@Override
			public boolean hasNext() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Entry next() {
				if (queue.isEmpty()) return null;

				while (true) {
					final Pair pair = queue.remove();
					if (pair.leaf) return pair.entry;

					final Node node = (Node) pair.entry.value;
					for (final Entry e : node.entries) {
						final Pair p = new Pair();
						p.entry = e;
						p.distance = p.entry.key.distanceSquared(x, y);
						if (node.level == 0) p.leaf = true;
						queue.add(p);
					}
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public void insert(final Entry entry) {
		items++;
		if (trace) System.out.printf("INSERTING key:%s value:%s\n", entry.key, entry.value);
		insert(entry, 0, true);

		if (trace) {
			System.out.printf("AFTER INSERT\n");
			printTree(root);
			System.out.println();

			assert root.parent == null;
			assert root.entries.size() >= 2 || root.level == 0;
			assertValid(root);
		}
	}

	public void assertValid(final Node node) {
		assert node == root || node.entries.size() >= min;
		if (node.level > 0) {
			for (final Entry e : node.entries) {
				final Node a = (Node) e.value;
				assert a.parent == node;
				assert a.level + 1 == node.level;
				assert union(a.entries).equals(e.key) : union(a.entries) + " <-> " + e.key;
				assertValid(a);
			}
		}
	}

	private static double overlap(final List<Entry> entries) {
		double s = 0;
		for (int a = 1; a < entries.size(); a++)
			for (int b = 0; b < a; b++)
				s += entries.get(a).key.overlapArea(entries.get(b).key);
		return s;
	}

	private static double area(final List<Entry> entries) {
		double s = 0;
		for (final Entry e : entries)
			s += e.key.area();
		return s;
	}

	public static void printTree(final Node node) {
		final double area = area(node.entries);
		System.out.printf("node:%s level:%s parent:%s overlap/area:%s area:%s union:%s\n", System
				.identityHashCode(node), node.level, System.identityHashCode(node.parent),
				overlap(node.entries) / area / node.entries.size(), area, union(node.entries));
		if (node.level == 0) {
			for (final Entry e : node.entries)
				System.out.printf("\tkey:%s value:%s\n", e.key, e.value);
		} else {
			for (final Entry e : node.entries)
				System.out.printf("\tkey:%s node:%s\n", e.key, System.identityHashCode(e.value));
			for (final Entry e : node.entries)
				printTree((Node) e.value);
		}
	}

	private void insert(final Entry newEntry, final int level, final boolean canReinsert) {
		final Node node = chooseNodeForInsert(root, newEntry, level);
		if (trace)
			System.out.printf("INSERTING INTO node:%s level:%s parent:%s\n", System.identityHashCode(node),
					node.level, System.identityHashCode(node.parent));
		insert(node, newEntry, canReinsert);
	}

	private Node chooseNodeForInsert(final Node node, final Entry newEntry, final int level) {
		if (node.level == level) return node;

		assert node.level > 0;
		assert node.entries.size() > 0;

		Entry best = null;
		double minPenalty = Double.POSITIVE_INFINITY;

		for (final Entry entry : node.entries) {
			final double p = penalty.penalty(entry.key, newEntry.key);
			if (p < minPenalty) {
				minPenalty = p;
				best = entry;
			}
		}

		return chooseNodeForInsert((Node) best.value, newEntry, level);
	}

	private void insert(final Node nodeA, final Entry newEntry, final boolean canReinsert) {
		if (nodeA.entries.size() < degree) {
			nodeA.entries.add(newEntry);
			adjustKeys(nodeA);
			return;
		}

		//		if (canReinsert && nodeA != root) {
		//			nodeA.entries.add(newEntry);
		//			int limit = (int) Math.ceil(nodeA.entries.size() * 0.3);
		//			sortByCenterDistance(nodeA.entries, nodeA.parent.getEntryOfNode(nodeA).key);
		//			List<Node> eliminated = 
		//			while (nodeA.entries.size() >= limit) {
		//				eliminated.add(nodeA.entries.get(nodeA.entries.size() - 1));
		//			}
		//		}

		// Node is full, perform split
		assert nodeA.entries.size() == degree;

		// If splitting the root then increase the tree height
		if (nodeA == root) {
			nodes++;
			root = new Node(nodeA.level + 1);
			root.entries.add(new Entry(union(nodeA.entries), nodeA));
			nodeA.parent = root;
		}

		final List<Entry> entries = new ArrayList<Entry>(degree + 1);
		entries.addAll(nodeA.entries);
		entries.add(newEntry);

		nodeA.entries.clear();
		nodes++;
		final Node nodeB = new Node(nodeA.level);
		nodeB.parent = nodeA.parent;

		pickSplit.pickSplit(entries, min, nodeA.entries, nodeB.entries);
		assert min <= nodeB.entries.size() && nodeB.entries.size() <= degree : String.format("%s %s %s", min,
				nodeB.entries.size(), degree);
		assert nodeA.entries.size() + nodeB.entries.size() == degree + 1 : String.format("%s %s %s", nodeA.entries
				.size(), nodeB.entries.size(), degree + 1);

		// fix parent pointers
		if (nodeA.level > 0) {
			for (final Entry e : nodeA.entries)
				((Node) e.value).parent = nodeA;
			for (final Entry e : nodeB.entries)
				((Node) e.value).parent = nodeB;
		}

		insert(nodeA.parent, new Entry(union(nodeB.entries), nodeB), canReinsert);

		adjustKeys(nodeA);
		adjustKeys(nodeB);
	}

	private static void sortByCenterDistance(final List<Entry> entries, final Rect box) {
		Collections.sort(entries, new Comparator<Entry>() {
			@Override
			public int compare(final Entry a, final Entry b) {
				return Double.compare(a.key.centerDistanceSquared4(box), b.key.centerDistanceSquared4(box));
			}
		});
	}

	public boolean delete(final Entry entry) {
		final Node node = findNodeForDelete(root, entry);
		if (node == null) return false;

		items--;
		node.entries.remove(entry);

		// Condense tree
		final List<Node> eliminated = new ArrayList<Node>();
		adjustKeys(node, eliminated);
		for (final Node n : eliminated)
			for (final Entry e : n.entries)
				insert(e, n.level, false);

		if (root.entries.size() == 1 && root.level > 0) root = (Node) root.entries.get(0).value;
		return true;
	}

	private static Node findNodeForDelete(final Node node, final Entry entry) {
		if (node.level == 0) {
			for (final Entry e : node.entries)
				if (e == entry) return node;
			return null;
		}

		for (final Entry e : node.entries)
			if (e.key.contains(entry.key)) {
				final Node result = findNodeForDelete((Node) e.value, entry);
				if (result != null) return result;
			}
		return null;
	}

	private void adjustKeys(final Node node, final List<Node> eliminated) {
		if (node.parent == null) return;

		final int i = node.parent.getIndexOfNode(node);
		if (node.entries.size() >= min) {
			final Rect union = union(node.entries);
			if (node.parent.entries.get(i).key.equals(union)) return;

			node.parent.entries.set(i, new Entry(union, node));
			adjustKeys(node.parent);
			return;
		}

		node.parent.entries.remove(i);
		node.parent = null;
		eliminated.add(node);
		adjustKeys(node.parent, eliminated);
	}

	private static Rect union(final List<Entry> entries) {
		final MutableRect result = new MutableRect();
		for (final Entry e : entries)
			result.union(e.key);
		return result.toRect();
	}

	private static void adjustKeys(final Node node) {
		if (node.parent == null) return;

		final int i = node.parent.getIndexOfNode(node);
		final Rect union = union(node.entries);
		if (node.parent.entries.get(i).key.equals(union)) return;

		node.parent.entries.set(i, new Entry(union, node));
		adjustKeys(node.parent);
	}

	public static void main(final String[] args) {
		final RTree tree = new RTree(4, 2, RTreeExtra.quadraticAreaSplit, RTreeExtra.deadSpacePenalty);

		for (int i = 0; i < 5; i++)
			tree.insert(new Entry(new Rect(i, i + 0.5, 0, 1), i));

		print("contains", tree.search(new Rect(2.2, 4.2, -1, 2), RTreeExtra.containsQuery));
		print("intersects", tree.search(new Rect(2.2, 4.2, -1, 2), RTreeExtra.intersectsQuery));
		print("neighbors", tree.nearestNeighbors(2.6, 0));
	}

	static void print(final String name, final Iterator<Entry> i) {
		System.out.println(name);
		Entry e;
		while ((e = i.next()) != null) {
			System.out.printf("%s -> %s\n", e.key, e.value);
		}
	}
}