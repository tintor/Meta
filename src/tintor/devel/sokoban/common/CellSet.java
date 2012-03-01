package tintor.devel.sokoban.common;

import tintor.devel.deadlockgen.Cell;

public class CellSet {
	private final int[] reached;
	private int timestamp = 1;

	public CellSet() {
		reached = new int[Cell.count()];
	}

	public void clear() {
		// NOTE this can overflow
		timestamp++;
	}

	public boolean add(final Cell a) {
		if (reached[a.id()] == timestamp) return false;
		reached[a.id()] = timestamp;
		return true;
	}
}