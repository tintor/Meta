package tintor.util;

public final class UnionFind {
	private UnionFind group = this;
	private int count = 1;

	public void union(final UnionFind a) {
		union(this, a);
	}

	public static void union(UnionFind a, UnionFind b) {
		if (a.group == b.group) return;

		a = a.group();
		b = b.group();

		if (a.count > b.count)
			b.group = a;
		else if (a.count < b.count)
			a.group = b;
		else {
			a.group = b;
			b.count += a.count;
		}
	}

	public UnionFind group() {
		UnionFind a = this;
		while (a.group != a)
			a = a.group;

		UnionFind p = this;
		while (p.group != p) {
			final UnionFind t = p.group;
			p.group = a;
			p = t;
		}

		return a;
	}
}