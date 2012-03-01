package tintor.devel.deadlockgen.sokoban;

public enum Dir {
	North, East, South, West;

	public Dir opposite() {
		return opposite;
	}

	private Dir opposite;

	static {
		North.opposite = South;
		South.opposite = North;
		East.opposite = West;
		West.opposite = East;
	}
}