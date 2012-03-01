package tintor.devel.index;

public interface BPlusTree {
	void put(final int key, final int value);

	int get(final int key, final int def);

	BPlusTreeIterator get(final int key);

	BPlusTreeIterator iterator();

	void close();
}

interface BPlusTreeIterator {
	boolean valid();

	void next();

	int key();

	int value();
}