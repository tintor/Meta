package tintor.devel.index;

import java.io.File;
import java.nio.ByteBuffer;

public final class BPlusTreeBuffered implements BPlusTree {
	private static class Node {
		int id;
		Node next;

		int keyCount;
		int[] keys, branches;

		Node(final Node n, final int order) {
			assert order >= 3;
			next = n;
			keys = new int[order - 1];
			branches = new int[order];
		}

		void write(final ByteBuffer bb) {
			bb.putShort((short) keyCount);
			for (int i = 0; i < keyCount; i++)
				bb.putInt(keys[i]);
			for (int i = 0; i <= keyCount; i++)
				bb.putInt(branches[i]);
		}

		void read(final ByteBuffer bb) {
			keyCount = bb.getShort();
			for (int i = 0; i < keyCount; i++)
				keys[i] = bb.getInt();
			for (int i = 0; i <= keyCount; i++)
				branches[i] = bb.getInt();
		}

		int find(final int key) {
			for (int i = 0; i < keyCount; i++)
				if (key < keys[i]) return i;
			return keyCount;
		}

		void set(final int b, final int key, final int branch) {
			keys[b] = key;
			branches[b + 1] = branch;
		}
	}

	private static final int None = -1, HeaderSize = 16;

	private final Storage _storage;
	private int _alloc, _height;
	int _order, _blockSize;
	private int _insertKey, _insertBranch;
	private Node _root;

	private BPlusTreeBuffered(final File file) {
		_storage = new Storage(file, "rw");
	}

	public static BPlusTreeBuffered create(final File file, final int order) {
		if (order < 3) throw new IllegalArgumentException("order");

		final BPlusTreeBuffered tree = new BPlusTreeBuffered(file);
		tree._storage.truncate(0);

		tree._height = 1;
		tree._alloc = 1;
		tree._order = order;
		tree._blockSize = tree._order * 8;
		tree._storage.allocBuffer(tree._blockSize);

		tree._root = new Node(null, tree._order);
		tree._root.id = 0;
		tree._root.branches[0] = None;
		tree.write(tree._root);

		tree.writeHeader();
		return tree;
	}

	public static BPlusTreeBuffered open(final File file) {
		final BPlusTreeBuffered tree = new BPlusTreeBuffered(file);

		tree._storage.allocBuffer(HeaderSize);
		tree._storage.read(0, HeaderSize);
		final int root = tree._storage.buffer.getInt();
		tree._height = tree._storage.buffer.getInt();
		tree._alloc = tree._storage.buffer.getInt();
		tree._order = tree._storage.buffer.getInt();
		if (tree._order < 3) throw new IllegalArgumentException("order");
		tree._blockSize = tree._order * 8;
		tree._storage.allocBuffer(tree._blockSize);

		tree._root = new Node(null, tree._order);
		tree.read(tree._root, root);
		return tree;
	}

	private void writeHeader() {
		_storage.buffer.clear();
		_storage.buffer.putInt(_root.id);
		_storage.buffer.putInt(_height);
		_storage.buffer.putInt(_alloc);
		_storage.buffer.putInt(_order);
		_storage.write(0);
	}

	@Override
	public void close() {
		_storage.close();
	}

	private void read(final Node node, final int id) {
		node.id = id;
		_storage.read(HeaderSize + node.id * _blockSize, _blockSize);
		node.read(_storage.buffer);
	}

	private void write(final Node node) {
		_storage.buffer.clear();
		node.write(_storage.buffer);
		_storage.write(HeaderSize + node.id * _blockSize);
	}

	private Node split(final Node nodeA, final int b) {
		final Node nodeB = new Node(null, _order);
		nodeB.id = _alloc++;
		writeHeader();

		final int sizeB = (nodeA.keyCount + 1) / 2;
		final int sizeA = nodeA.keyCount + 1 - sizeB;

		// redistribute
		for (int i = nodeA.keyCount - 1; i >= Math.min(b, sizeA); i--) {
			final int j = b <= i ? i + 1 : i;
			if (j >= sizeA)
				nodeB.set(j - sizeA, nodeA.keys[i], nodeA.branches[i + 1]);
			else
				nodeA.set(j, nodeA.keys[i], nodeA.branches[i + 1]);
		}

		// add
		if (b >= sizeA)
			nodeB.set(b - sizeA, _insertKey, _insertBranch);
		else
			nodeA.set(b, _insertKey, _insertBranch);

		nodeA.keyCount = sizeA;
		nodeB.keyCount = sizeB;
		return nodeB;
	}

	private boolean nodeInsert(final Node node, final int b, final boolean leaf) {
		// if (no overflow) do (insert key/branch)
		if (node.keyCount < node.keys.length) {
			for (int i = node.keyCount - 1; i >= b; i--)
				node.set(i + 1, node.keys[i], node.branches[i + 1]);
			node.set(b, _insertKey, _insertBranch);
			node.keyCount += 1;
			write(node);
			return false;
		}
		// else (overflow) do (split internal node)

		// add and split node
		final Node nodeB = split(node, b);

		if (leaf) { // insert new leaf node in linked list
			nodeB.branches[0] = node.branches[0];
			node.branches[0] = nodeB.id;
		}
		else { // extract middle key from internal node
			nodeB.branches[0] = node.branches[node.keyCount];
			node.keyCount -= 1;
		}

		// save nodes
		write(node);
		write(nodeB);

		// return split info
		_insertKey = leaf ? nodeB.keys[0] : node.keys[node.keyCount];
		_insertBranch = nodeB.id;
		return true;
	}

	@Override
	public void put(final int key, final int value) {
		// System.out.println("put " + key);
		_insertKey = key;
		_insertBranch = value;

		// PROCESS ROOT
		_root.next = null;
		Node node = _root;
		if (_height > 1) {
			int next_id = _root.branches[_root.find(_insertKey)];

			for (int height = 2; height < _height; height++) {
				node = new Node(node, _order);
				read(node, next_id);
				next_id = node.branches[node.find(_insertKey)];
			}

			// PROCESS LEAF
			node = new Node(node, _order);
			read(node, next_id);
		}
		final int b = node.find(_insertKey);
		if (b > 0 && _insertKey == node.keys[b - 1]) {
			// if (leaf node with key) do (update value)
			node.branches[b] = _insertBranch;
			write(node);
			return;
		}
		// else (leaf node with no key)
		if (!nodeInsert(node, b, true)) return;

		// PROCESS UP INTERNALS
		for (node = node.next; node != null; node = node.next)
			if (!nodeInsert(node, node.find(_insertKey), false)) return;

		// SPLIT ROOT
		node = new Node(null, _order);
		node.id = _alloc++;
		node.keyCount = 1;
		node.branches[0] = _root.id;
		node.keys[0] = _insertKey;
		node.branches[1] = _insertBranch;
		write(node);

		_root = node;
		_height += 1;
		writeHeader();
	}

	@Override
	public int get(final int key, final int def) {
		int b = _root.find(key);
		int id = _root.branches[b];
		if (_height == 1) return b == 0 || key != _root.keys[b - 1] ? def : id;

		final Node node = new Node(null, _order);
		for (int h = 2; h <= _height; h++) {
			read(node, id);
			b = node.find(key);
			id = node.branches[b];
		}
		return b == 0 || key != node.keys[b - 1] ? def : id;
	}

	private class Iterator implements BPlusTreeIterator {
		private Node _node;
		private int _index;

		Iterator(final Node node, final int index) {
			_node = node;
			_index = index;
		}

		@Override
		public boolean valid() {
			return _index < _node.keyCount;
		}

		@Override
		public void next() {
			_index += 1;
			if (_index == _node.keyCount) {
				final int next = _node.branches[0];
				if (next == None) return;

				_node = new Node(null, _order);
				read(_node, next);
				_index = 0;
			}
		}

		@Override
		public int key() {
			return _node.keys[_index];
		}

		@Override
		public int value() {
			return _node.branches[_index + 1];
		}
	}

	@Override
	public BPlusTreeIterator get(final int key) {
		int b = _root.find(key);
		int id = _root.branches[b];
		if (_height == 1) return b == 0 || key != _root.keys[b - 1] ? null : new Iterator(_root, b - 1);

		final Node node = new Node(null, _order);
		for (int h = 2; h <= _height; h++) {
			read(node, id);
			b = node.find(key);
			id = node.branches[b];
		}
		return b == 0 || key != node.keys[b - 1] ? null : new Iterator(node, b - 1);
	}

	@Override
	public BPlusTreeIterator iterator() {
		if (_height == 1) return new Iterator(_root, 0);

		final Node node = new Node(null, _order);
		read(node, _root.branches[0]);
		for (int h = 3; h <= _height; h++)
			read(node, node.branches[0]);
		return new Iterator(node, 0);
	}

	void debugPrint(final Node node, final int height) {
		if (height < _height) {
			System.out.print("I" + node.id + " " + node.branches[0]);
			for (int i = 0; i < node.keyCount; i++)
				System.out.print("\\" + node.keys[i] + "/" + node.branches[i + 1]);
			System.out.println();

			for (int i = 0; i <= node.keyCount; i++) {
				final Node n = new Node(null, _order);
				read(n, node.branches[i]);
				debugPrint(n, height + 1);
			}
		}
		else {
			System.out.print("L" + node.id);
			for (int i = 0; i < node.keyCount; i++)
				System.out.print(" " + node.keys[i] + ":" + node.branches[i + 1]);
			System.out.println(" => " + node.branches[0]);
		}
	}

	int debugHeight() {
		return _height;
	}
}