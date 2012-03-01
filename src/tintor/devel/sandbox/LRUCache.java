package tintor.devel.sandbox;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
	public LRUCache(final int maxsize) {
		super(maxsize * 4 / 3 + 1, 0.75f, true);
		_maxsize = maxsize;
	}

	protected int _maxsize;

	@Override
	protected boolean removeEldestEntry(@SuppressWarnings("unused")
	Map.Entry<K, V> eldest) {
		return size() > _maxsize;
	}
}