package tintor.devel.deadlockgen.sokoban;

// TODO test for free goals, blocked by frozen boxes on goals
public class Deadlock {
	public final static boolean fastBlock = true;
	public final static boolean frozenBoxes = true;

	public static boolean fullTest(@SuppressWarnings("unused") final Key key) {
		if (frozenBoxes(key, key.boxTable(), key.boxes())) return true;
		return false;
	}

	public static boolean partialTest(final Key key, final Cell movedBox) {
		final boolean[] boxes = key.boxTable();

		if (fastBlock(boxes, movedBox)) return true;
		if (frozenBoxes(key, boxes, key.boxes())) return true;
		return false;
	}

	private static boolean fastBlock(final boolean[] boxes, final Cell movedBox) {
		if (!fastBlock) return false;

		// east
		final Cell e = movedBox.east();
		final boolean fe = frozen(boxes, e);

		// west
		final Cell w = movedBox.west();
		final boolean fw = frozen(boxes, w);

		// north
		final Cell n = movedBox.north();
		if (frozen(boxes, n)) {
			if (fw && frozen(boxes, n != null ? n.west() : w != null ? w.north() : null)) return true;
			if (fe && frozen(boxes, n != null ? n.east() : e != null ? e.north() : null)) return true;
		}

		// south
		final Cell s = movedBox.south();
		if (frozen(boxes, s)) {
			if (fw && frozen(boxes, s != null ? s.west() : w != null ? w.south() : null)) return true;
			if (fe && frozen(boxes, s != null ? s.east() : e != null ? e.south() : null)) return true;
		}
		return false;
	}

	// if a is wall or box (not on goal)?
	private static boolean frozen(final boolean[] boxes, final Cell a) {
		if (a == null) return true;
		return !a.hole && boxes[a.id];
	}

	// Whole level frozen boxes test
	private static boolean frozenBoxes(final Key key, final boolean[] boxes, final int boxCount) {
		if (!frozenBoxes) return false;

		// remove alive boxes
		int boxesLeft = boxCount;

		final CellSearch search = key.agent.group.cellSearch;
		search.reset(key.agent);

		for (final Cell a : search)
			for (Cell.Edge e = a.edges(); e != null; e = e.next)
				if (boxes[e.cell.id]) {
					final Cell dest = e.cell.get(e.dir);
					if (dest != null && !dest.dead() && !boxes[dest.id])
						if (true || dest.hole || !fastBlock(boxes, dest)) {
							boxes[e.cell.id] = false;
							if (--boxesLeft == 0) return false;
							search.add(e.cell);
						}
				}
				else
					search.add(e.cell);

		return true;
	}
}