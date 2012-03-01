package tintor.devel.deadlockgen.sokoban;

import java.util.Iterator;

/** Class for visiting Cells in BFS order */
public class CellSearch implements Iterable<Cell>, Iterator<Cell> {
	private int head, tail;
	private final Cell[] queue;
	private final boolean[] reached;

	public CellSearch(final CellGroup group) {
		queue = new Cell[group.size];
		reached = new boolean[group.size];
	}

	public CellSearch(final Cell a) {
		this(a.group);
		reached[a.id] = true;
		queue[0] = a;
		tail = 1;
	}

	public void reset(final Cell a) {
		for (int i = 0; i < reached.length; i++)
			reached[i] = false;

		reached[a.id] = true;
		queue[0] = a;
		head = 0;
		tail = 1;
	}

	public void reached(final Cell a) {
		reached[a.id] = true;
	}

	public final boolean add(final Cell a) {
		if (reached[a.id]) return false;
		reached[a.id] = true;
		queue[tail++] = a;
		return true;
	}

	@Override
	public Iterator<Cell> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return head < tail;
	}

	@Override
	public Cell next() {
		return queue[head++];
	}

	@Override
	public void remove() {
		throw new RuntimeException();
	}
}