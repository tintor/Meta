package tintor.devel.deadlockgen;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import tintor.devel.deadlockgen.sokoban.Cell;
import tintor.devel.deadlockgen.sokoban.CellGroup;
import tintor.devel.deadlockgen.sokoban.CellSearch;
import tintor.devel.deadlockgen.sokoban.Code;
import tintor.devel.deadlockgen.sokoban.Deadlock;
import tintor.devel.deadlockgen.sokoban.Dir;
import tintor.devel.deadlockgen.sokoban.Key;
import tintor.devel.deadlockgen.sokoban.KeySet;
import tintor.devel.deadlockgen.util.IntHashSet;
import tintor.devel.deadlockgen.util.Statistics;

public final class Map implements Serializable {
	final int width, height;
	final char[] cells;
	int boxes;

	long start;
	long order;
	final long total;

	private final int[] transH, transV, transHV, transT, transHT, transVT, transHVT;

	Map(final int width, final int height) {
		this.width = width;
		this.height = height;
		total = power3[width * height];

		cells = new char[width * height];
		for (int i = 0; i < cells.length; i++)
			cells[i] = Code.Space;

		transH = transform(true, false, false);
		transV = transform(false, true, false);
		transHV = transform(true, true, false);
		transT = transform(false, false, true);
		transHT = transform(true, false, true);
		transVT = transform(false, true, true);
		transHVT = transform(true, true, true);
	}

	void load(final String map) {
		if (map.length() != cells.length) throw new IllegalArgumentException();
		for (int i = 0; i < cells.length; i++)
			cells[i] = map.charAt(i);

		start = order = order();
		boxes = 0;
		for (final char c : cells)
			if (c == Code.Box) boxes++;
	}

	void load(final long map) {
		assert map < total;

		start = order = map;
		boxes = 0;
		long m = map;
		for (int i = 0; i < cells.length; i++) {
			switch ((int) (m % 3)) {
			case 0:
				cells[i] = Code.Space;
				break;
			case 1:
				cells[i] = Code.Box;
				boxes++;
				break;
			case 2:
				cells[i] = Code.Wall;
				break;
			}
			m /= 3;
		}
	}

	boolean next() {
		order += 1;
		for (int i = 0; i < cells.length; i++)
			switch (cells[i]) {
			case Code.Space:
				cells[i] = Code.Box;
				boxes++;
				return true;
			case Code.Box:
				cells[i] = Code.Wall;
				boxes--;
				return true;
			case Code.Wall:
				cells[i] = Code.Space;
				break;
			}
		return false;
	}

	boolean unique() {
		// remove symmetric duplicates
		if (order > order(transH)) return false;
		if (order > order(transV)) return false;
		if (order > order(transHV)) return false;
		if (width == height) {
			if (order > order(transT)) return false;
			if (order > order(transHT)) return false;
			if (order > order(transVT)) return false;
			if (order > order(transHVT)) return false;
		}
		return true;
	}

	boolean containsPattern(final int w, final int h, final IntHashSet patterns) {
		for (int y = 0; y < height - h + 1; y++)
			for (int x = 0; x < width - w + 1; x++) {
				int a = 0;
				for (int sy = h - 1; sy >= 0; sy--)
					for (int sx = w - 1; sx >= 0; sx--)
						a = a * 3 + code(cells[x + sx + (y + sy) * width]);
				if (patterns.contains(a)) return true;
			}

		if (w != h && h <= width) for (int y = 0; y < height - w + 1; y++)
			for (int x = 0; x < width - h + 1; x++) {
				int a = 0;
				for (int sx = h - 1; sx >= 0; sx--)
					for (int sy = w - 1; sy >= 0; sy--)
						a = a * 3 + code(cells[x + sx + (y + sy) * width]);
				if (patterns.contains(a)) return true;
			}

		return false;
	}

	static int code(final char c) {
		switch (c) {
		case Code.Space:
			return 0;
		case Code.Box:
			return 1;
		case Code.Wall:
			return 2;
		}
		throw new RuntimeException();
	}

	private final Statistics keysOpenedStats = new Statistics("keysOpened");
	private final Statistics stackCapacityStats = new Statistics("stackCapacity");
	private final Statistics setSizeStats = new Statistics("setSize");

	boolean isDeadlock() {
		for (int i = 0; i < 5; i++)
			while (true)
				try {
					return realIsDeadlock();
				}
				catch (final OutOfMemoryError e) {
					System.gc();
				}
		throw new RuntimeException("could not recover from out of memory");
	}

	private boolean realIsDeadlock() {
		final CellGroup group = new CellGroup(width, height, cells);
		if (group.start == null) return true;
		if (group.start.boxes() == 0) return false;

		// TODO specialize solver for sub-maze
		// TODO deadlock pattern matching (can I do it per push?)

		//if (Deadlock.fullTest(group.start)) return true;

		final KeySet set = new KeySet();
		final CellSearch reachable = new CellSearch(group);

		final Stack<Key> stack = new Stack<Key>();
		final Stack<Cell.Edge> edgeStack = new Stack<Cell.Edge>();

		set.add(group.start);
		stack.push(group.start);

		int keysOpened = 0;

		try {
			loop: while (stack.size > 0) {
				final Key a = stack.pop();

				// TODO create box table and move all has box questions here

				try {
					reachable.reset(a.agent);
					for (final Cell c : reachable)
						for (Cell.Edge e = c.edges(); e != null; e = e.next) {
							if (!a.hasBox(e.cell)) {
								reachable.add(e.cell);
								continue;
							}

							final Cell dest = e.cell.get(e.dir);
							if (dest == null || dest.dead() || a.hasBox(dest)) continue;

							if (hasHole(dest, e.dir, a)) {
								if (a.boxes() == 1) return false;
								final Key b = a.removeBox(e.cell);
								if (!set.add(b)) continue;

								stack.push(b);
								continue loop;
							}

							edgeStack.push(e);
						}

					while (edgeStack.size > 0) {
						final Cell.Edge e = edgeStack.pop();
						final Cell dest = e.cell.get(e.dir);

						final Key b = a.pushBox(e.cell, dest);
						if (set.contains(b) || Deadlock.partialTest(b, dest)) continue;

						set.addUnsafe(b);
						stack.push(b);
					}
				}
				finally {
					keysOpened += 1;
				}
			}
			return true;
		}
		finally {
			keysOpenedStats.add(keysOpened);
			stackCapacityStats.add(stack.array.length);
			setSizeStats.add(set.size());
		}
	}

	private static class Stack<T> {
		Object[] array = new Object[1 << 7];
		int size = 0;

		void push(final T a) {
			if (array.length == size) {
				final Object[] ns = new Object[size * 2];
				System.arraycopy(array, 0, ns, 0, size);
				array = ns;
			}
			array[size++] = a;
		}

		@SuppressWarnings("unchecked")
		T pop() {
			return (T) array[--size];
		}
	}

	private static boolean hasHole(Cell a, final Dir d, final Key key) {
		while (true) {
			if (a.hole) return true;
			a = a.get(d);
			if (a == null || a.dead() || key.hasBox(a)) return false;
		}
	}

	boolean isMinimal() {
		for (int i = 0; i < cells.length; i++)
			switch (cells[i]) {
			case Code.Box:
				try {
					cells[i] = Code.Space;
					if (isDeadlock()) return false;
				}
				finally {
					cells[i] = Code.Box;
				}
				break;
			case Code.Wall:
				try {
					cells[i] = Code.Space;
					if (isDeadlock()) return false;

					cells[i] = Code.Box;
					if (isDeadlock()) return false;
				}
				finally {
					cells[i] = Code.Wall;
				}
			}
		return true;
	}

	void print(final Writer out) {
		synchronized (out) {
			try {
				for (int y = 0; y < height; y++) {
					out.write(cells, y * width, width);
					out.write('\n');
				}
				out.write("===\n");
				out.flush();
			}
			catch (final IOException e) {
				throw new RuntimeException();
			}
		}
	}

	private boolean freeTopEdge() {
		for (int i = 0; i < width; i++)
			if (cells[i] != Code.Space) return false;
		return true;
	}

	private boolean freeBottomEdge() {
		for (int i = 0; i < width; i++)
			if (cells[(height - 1) * width + i] != Code.Space) return false;
		return true;
	}

	private boolean freeLeftEdge() {
		for (int i = 0; i < height; i++)
			if (cells[i * width] != Code.Space) return false;
		return true;
	}

	private boolean freeRightEdge() {
		for (int i = 0; i < height; i++)
			if (cells[i * width + width - 1] != Code.Space) return false;
		return true;
	}

	boolean freeEdge() {
		return freeBottomEdge() || freeRightEdge() || freeTopEdge() && freeLeftEdge();
	}

	private long order() {
		long p = 0, m = 1;
		for (final char c : cells) {
			switch (c) {
			case Code.Box:
				p += m;
				break;

			case Code.Wall:
				p += m << 1;
				break;

			}
			m *= 3;
		}
		return p;
	}

	private long order(final int[] transform) {
		long p = 0, m = 1;

		final char[] c = cells;
		for (int i = 0; i < c.length; i++) {
			switch (c[transform[i]]) {
			case Code.Box:
				p += m;
				break;

			case Code.Wall:
				p += m << 1;
				break;

			}
			m *= 3;
		}
		return p;
	}

	private int[] transform(final boolean flipHor, final boolean flipVer, final boolean transpose) {
		final int[] transform = new int[cells.length];
		for (int i = 0; i < cells.length; i++) {
			int x = i % width, y = i / width;

			if (flipHor) x = width - 1 - x;
			if (flipVer) y = height - 1 - y;
			if (transpose) {
				final int a = x;
				x = y;
				y = a;
			}

			transform[i] = y * width + x;
		}
		return transform;

	}

	private long order(final boolean flipHor, final boolean flipVer, final boolean transpose) {
		long p = 0, m = 1;

		for (int i = 0; i < cells.length; i++) {
			int x = i % width, y = i / width;

			if (flipHor) x = width - 1 - x;
			if (flipVer) y = height - 1 - y;
			if (transpose) {
				assert width == height;
				final int a = x;
				x = y;
				y = a;
			}

			switch (cells[y * width + x]) {
			case Code.Box:
				p += m;
				break;

			case Code.Wall:
				p += m << 1;
				break;

			}
			m *= 3;
		}
		return p;
	}

	static final long power3[] = new long[50];

	static {
		long m = 1;
		for (int i = 0; i < power3.length; i++) {
			power3[i] = m;
			m *= 3;
		}
	}
}