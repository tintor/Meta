package tintor.devel.sokoban.keyset;

import tintor.devel.deadlockgen.Key;

public interface KeySet {
	boolean add(final Key a);

	int size();

	int arraysize();
}