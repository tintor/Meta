package tintor.devel.deadlockgen.sokoban;

public final class Key {
	public final Cell agent;
	private final Cell[] boxes;

	Key setNext;

	public int boxes() {
		return boxes.length;
	}

	public Cell box(final int i) {
		return boxes[i];
	}

	public Key(final Cell agent, final Cell[] boxes) {
		this.agent = agent;
		this.boxes = boxes;
	}

	public Key removeBox(final Cell box) {
		final Cell[] nboxes = new Cell[boxes.length - 1];
		for (int i = 0; i < boxes.length; i++)
			if (boxes[i] == box) {
				System.arraycopy(boxes, 0, nboxes, 0, i);
				System.arraycopy(boxes, i + 1, nboxes, i, nboxes.length - i);
				return new Key(agent, nboxes);
			}
		throw new RuntimeException();
	}

	public Key pushBox(final Cell box, final Cell dest) {
		final Cell[] nboxes = new Cell[boxes.length];
		System.arraycopy(boxes, 0, nboxes, 0, boxes.length);
		for (int i = 0; i < boxes.length; i++)
			if (boxes[i] == box) {
				nboxes[i] = dest;
				return new Key(normalizeAgent(box, nboxes), nboxes);
			}
		throw new RuntimeException();
	}

	private static Cell normalizeAgent(final Cell agent, final Cell[] boxes) {
		if (agent.outer) return agent.group.zero;

		final CellSearch search = agent.group.cellSearch;
		search.reset(agent);

		for (final Cell b : boxes)
			search.reached(b);

		Cell result = agent;
		for (final Cell a : search) {
			if (a.id < result.id) result = a;
			for (Cell.Edge e = a.edges(); e != null; e = e.next)
				if (search.add(e.cell) && e.cell.outer) return agent.group.zero;
		}
		return result;
	}

	public boolean hasBox(final Cell a) {
		for (final Cell box : boxes)
			if (box == a) return true;
		return false;
	}

	public boolean[] boxTable() {
		final boolean[] table = new boolean[agent.group.size];
		for (final Cell box : boxes)
			table[box.id] = true;
		return table;
	}

	public boolean equals(final Key key) {
		if (agent != key.agent || boxes.length != key.boxes.length) return false;
		for (int i = 0; i < boxes.length; i++)
			if (boxes[i] != key.boxes[i]) return false;
		return true;
	}

	@Override
	public int hashCode() {
		int h = agent.id;
		for (final Cell box : boxes)
			h = h * 953 + box.id;
		h ^= h >>> 20 ^ h >>> 12;
		return h ^ h >>> 7 ^ h >>> 4;
	}
}