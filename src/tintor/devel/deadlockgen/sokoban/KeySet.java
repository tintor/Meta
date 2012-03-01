package tintor.devel.deadlockgen.sokoban;

// TODO find best initial table size
public final class KeySet {
	private static final float LoadFactor = 0.75f;
	private Key[] table = new Key[1 << 7];
	private int size, threshold = (int) (table.length * LoadFactor);

	public int size() {
		return size;
	}

	public int arraySize() {
		return table.length;
	}

	public void clear() {
		for (int i = 0; i < table.length; i++)
			table[i] = null;
		size = 0;
	}

	public boolean contains(final Key key) {
		for (Key e = table[key.hashCode() & table.length - 1]; e != null; e = e.setNext)
			if (e.equals(key)) return true;
		return false;
	}

	public boolean add(final Key key) {
		final int entry = key.hashCode() & table.length - 1;
		for (Key e = table[entry]; e != null; e = e.setNext)
			if (e.equals(key)) return false;

		if (size >= threshold) grow();
		size += 1;
		insert(key, entry);
		return true;
	}

	public void addUnsafe(final Key key) {
		if (size >= threshold) grow();
		size += 1;
		insert(key, key.hashCode() & table.length - 1);
	}

	private void grow() {
		final Key[] old = table;
		table = new Key[old.length * 2];

		for (Key e : old)
			while (e != null) {
				final Key a = e.setNext;
				insert(e, e.hashCode() & table.length - 1);
				e = a;
			}

		threshold = (int) (table.length * LoadFactor);
	}

	private void insert(final Key key, final int entry) {
		key.setNext = table[entry];
		table[entry] = key;
	}
}