package tintor.devel.deadlockgen.sokoban;

public final class CellGroup {
	public final Cell zero;
	public final int size;
	public final Key start;

	final CellSearch cellSearch;

	public CellGroup(final int width, final int height, final char[] map) {
		final Cell[] cmap = new Cell[width * height];

		size = createCells(width, height, map, cmap, this);
		zero = createHoles(width, height, cmap, this);
		markDeadCells(cmap);

		start = new Key(zero, collectBoxes(map, cmap));
		cellSearch = new CellSearch(this);
	}

	private static int createCells(final int width, final int height, final char[] map, final Cell[] cmap,
			final CellGroup group) {
		int id = width + height + 2 << 1;
		int leftEdge = 0;
		for (int i = 0; i < cmap.length; i++) {
			if (map[i] == Code.Wall) {
				if (i == leftEdge) leftEdge += width;
				continue;
			}
			final Cell cell = cmap[i] = new Cell(group, id++, false);
			if (i != leftEdge)
				cell.attach(cmap[i - 1], Dir.West);
			else
				leftEdge += width;
			if (i >= width) cell.attach(cmap[i - width], Dir.North);
		}

		for (int x = 0; x < width; x++) {
			final Cell a = cmap[x];
			if (a != null) a.outer = true;
			final Cell b = cmap[x + (height - 1) * width];
			if (b != null) b.outer = true;
		}

		for (int y = 1; y < height - 1; y++) {
			final Cell a = cmap[y * width];
			if (a != null) a.outer = true;
			final Cell b = cmap[width - 1 + y * width];
			if (b != null) b.outer = true;
		}

		return id;
	}

	private static void markDeadCells(final Cell[] cmap) {
		for (final Cell c : cmap) {
			if (c == null) continue;
			final Cell.Edge e = c.edges();
			if (e == null || e.next == null || e.next.next == null && e.dir.opposite() != e.next.dir)
				c.dead = true;
		}
	}

	private static Cell createHoles(final int width, final int height, final Cell[] cmap, final CellGroup group) {
		int id = 0;

		// west side
		final Cell aa = new Cell(group, id++, true);
		aa.outer = true;
		Cell prev = aa;
		for (int y = 0; y < height; y++) {
			final Cell cell = new Cell(group, id++, true);
			cell.outer = true;
			cell.attach(prev, Dir.North);
			cell.attach(cmap[y * width], Dir.East);
			prev = cell;
		}

		// south side
		final Cell bb = new Cell(group, id++, true);
		bb.outer = true;
		bb.attach(prev, Dir.North);
		prev = bb;
		for (int x = 0; x < width; x++) {
			final Cell cell = new Cell(group, id++, true);
			cell.outer = true;
			cell.attach(prev, Dir.West);
			cell.attach(cmap[x + (height - 1) * width], Dir.North);
			prev = cell;
		}

		// east side
		final Cell cc = new Cell(group, id++, true);
		cc.outer = true;
		cc.attach(prev, Dir.West);
		prev = cc;
		for (int y = height - 1; y >= 0; y--) {
			final Cell cell = new Cell(group, id++, true);
			cell.outer = true;
			cell.attach(prev, Dir.South);
			cell.attach(cmap[width - 1 + y * width], Dir.West);
			prev = cell;
		}

		// north side
		final Cell dd = new Cell(group, id++, true);
		dd.outer = true;
		dd.attach(prev, Dir.South);
		prev = bb;
		for (int x = width - 1; x >= 0; x--) {
			final Cell cell = new Cell(group, id++, true);
			cell.outer = true;
			cell.attach(prev, Dir.East);
			cell.attach(cmap[x], Dir.South);
			prev = cell;
		}

		aa.attach(prev, Dir.East);
		return aa;
	}

	private static Cell[] collectBoxes(final char[] map, final Cell[] cmap) {
		int count = 0;
		for (final char c : map)
			if (c == Code.Box) count++;

		int box = 0;
		final Cell[] boxes = new Cell[count];
		for (int i = 0; i < map.length; i++)
			if (map[i] == Code.Box) boxes[box++] = cmap[i];
		return boxes;
	}
}