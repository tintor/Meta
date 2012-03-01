package tintor.devel.deadlockgen.util;

import java.util.Random;

public class IntHashSet_OpenAddresing {
	private static final float loadFactor = 0.6667f;

	private int[] table;
	private int threshold;
	private int size;
	private boolean containsZero;

	public IntHashSet_OpenAddresing() {
		this(32);
	}

	public IntHashSet_OpenAddresing(final int initialCapacity) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);

		int capacity = 1;
		while (capacity < initialCapacity)
			capacity <<= 1;

		table = new int[capacity];
		threshold = (int) (capacity * loadFactor);
	}

	public int size() {
		return containsZero ? size + 1 : size;
	}

	public boolean contains(final int key) {
		int slot = key, perturb = key;
		final int mask = table.length - 1;
		while (true) {
			final int a = table[slot & mask];
			if (a == 0) {
				if (key == 0) return containsZero;
				return false;
			}
			if (a == key) return true;
			slot = slot * 5 + 1 + perturb;
			perturb >>= 5;
		}
	}

	public boolean add(final int key) {
		int slot = key, perturb = key;
		final int mask = table.length - 1;

		int i = 0;
		while (true) {
			final int a = table[slot & mask];
			if (a == 0) {
				if (key == 0) {
					if (containsZero) return false;
					containsZero = true;
					return true;
				}
				table[slot & mask] = key;
				if (++size > threshold) resize();
				return true;
			}
			if (a == key) return false;
			slot = slot * 5 + 1 + perturb & mask;
			perturb >>= 5;

			if (++i > table.length) throw new IllegalStateException("" + key);
		}
	}

	private void resize() {
		final int capacity = table.length <= 50000 ? table.length * 4 : table.length * 2;

		final int[] newTable = new int[capacity];
		threshold = (int) (capacity * loadFactor);
		final int mask = capacity - 1;

		for (final int key : table) {
			if (key == 0) continue;
			int slot = key, perturb = key;
			while (newTable[slot & mask] != 0) {
				slot = slot * 5 + 1 + perturb;
				perturb >>= 5;
			}
			newTable[slot & mask] = key;
		}

		table = newTable;
	}

	public static void main(final String[] args) {
		final int[] table = new int[1000000];
		final Random rand = new Random();

		final int x = 0;
		while (true) {
			for (int i = 0; i < table.length; i++)
				table[i] = rand.nextInt();

			final IntHashSet h1 = new IntHashSet();
			final IntHashSet_OpenAddresing h2 = new IntHashSet_OpenAddresing();
			for (final int e : table)
				if (h1.add(e) != h2.add(e)) throw new RuntimeException("" + e);

			//System.out.println(++x);
		}
	}

	public static void main2(final String[] args) {
		final Timer timer = new Timer();
		final int[] table = new int[1000000];
		final Random rand = new Random();

		while (true) {
			for (int i = 0; i < table.length; i++)
				table[i] = rand.nextInt(table.length);

			timer.restart();
			IntHashSet_OpenAddresing h2 = new IntHashSet_OpenAddresing();
			for (final int e : table)
				h2.add(e);
			timer.stop();
			final long a = timer.time;
			timer.time = 0;

			h2 = null;

			timer.restart();
			final IntHashSet h1 = new IntHashSet();
			for (final int e : table)
				h1.add(e);
			timer.stop();
			final long b = timer.time;
			timer.time = 0;

			System.out.println((double) a / b);
		}
	}
}