package tintor.devel.deadlockgen.sokoban;

public final class Cell {
	public final static class Edge {
		public final Cell cell;
		public final Dir dir;
		public final Edge next;

		public Edge(final Cell cell, final Dir dir, final Edge next) {
			this.cell = cell;
			this.dir = dir;
			this.next = next;
		}
	}

	private final Cell[] map = new Cell[Dir.values().length];
	private Edge edges;

	public final CellGroup group;
	public final int id;
	public final boolean hole;
	boolean outer;
	boolean dead;

	public Cell(final CellGroup group, final int id, final boolean hole) {
		this.group = group;
		this.id = id;
		this.hole = hole;
	}

	public boolean dead() {
		return dead;
	}

	public Edge edges() {
		return edges;
	}

	public Cell north() {
		return get(Dir.North);
	}

	public Cell east() {
		return get(Dir.East);
	}

	public Cell south() {
		return get(Dir.South);
	}

	public Cell west() {
		return get(Dir.West);
	}

	public Cell get(final Dir d) {
		return map[d.ordinal()];
	}

	public void attach(final Cell b, final Dir d) {
		if (b == null) return;

		assert get(d) == null;
		assert b.get(d.opposite()) == null;

		map[d.ordinal()] = b;
		b.map[d.opposite().ordinal()] = this;

		edges = new Edge(b, d, edges);
		b.edges = new Edge(this, d.opposite(), b.edges);
	}
}