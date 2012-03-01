package tintor.devel.deadlockgen.util;

import java.util.HashMap;
import java.util.Map;

public class HitRatio {
	private static final boolean Enabled = false;

	private int nom, den;

	private static Map<String, HitRatio> map = new HashMap<String, HitRatio>();

	public HitRatio(final String name) {
		map.put(name, this);
	}

	public boolean hit(final boolean cond) {
		if (Enabled) {
			if (cond) nom++;
			den++;
		}
		return cond;
	}

	public static void dump() {
		if (Enabled) for (final Map.Entry<String, HitRatio> e : map.entrySet())
			System.out.println("" + e.getKey() + " ratio " + 100.0 * e.getValue().nom / e.getValue().den);
	}
}