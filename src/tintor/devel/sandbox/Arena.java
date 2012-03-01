package tintor.devel.sandbox;

import java.util.Map;

public class Arena {
	public static void main(final String[] args) {
		final Map<String, String> map = new LRUCache<String, String>(3);
		map.put("marko", "");
		map.put("nikola", "");
		map.put("pera", "");
		map.get("marko");
		map.put("zvaja", "");

		// WeakHashMap<String, String> wmap;

		for (final String a : map.keySet())
			System.out.println(a);
	}
}