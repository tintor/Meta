package tintor.devel.sokoban.common;

import tintor.devel.deadlockgen.Cell;

public class CellSetB {
	private final boolean[] reached = new boolean[Cell.count()];

	public boolean add(final Cell a) {
		if (reached[a.id()]) return false;
		reached[a.id()] = true;
		return true;
	}
}