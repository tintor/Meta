package tintor.devel.deadlockgen;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tintor.devel.deadlockgen.util.IntHashSet;

class Patterns {
	static class Entry {
		int width, height;
		IntHashSet patterns;
	}

	private final List<Entry> list = new ArrayList<Entry>();

	void load(final int width, final int height) {
		final Entry e = new Entry();
		e.width = width;
		e.height = height;
		e.patterns = new IntHashSet(128);
		list.add(e);

		try {
			final DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(
					width + "x" + height + ".bin")));
			while (true)
				e.patterns.add(in.readInt());
		}
		catch (final EOFException ee) {
		}
		catch (final IOException ee) {
			throw new RuntimeException(ee);
		}
	}

	boolean matches(final Map map) {
		for (final Entry e : list)
			if (map.containsPattern(e.width, e.height, e.patterns)) return true;
		return false;
	}
}