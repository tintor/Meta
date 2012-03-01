package tintor.devel.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Matcher {
	abstract public Result match(int position);

	public final Matcher optional() {
		return new Matcher() {
			@Override
			public Result match(final int position) {
				final Result p = Matcher.this.match(position);
				return p == null ? new Result(position) : p;
			}
		};
	}

	public final Matcher any() {
		return new Matcher() {
			@Override
			public Result match(final int position) {
				final List<Object> list = new ArrayList<Object>();
				int p = position;

				while (true) {
					final Result a = Matcher.this.match(p);
					if (a == null) break;

					p = a.position;
					a.copyTo(list);
				}
				return new Result(p, list);
			}
		};
	}

	public final Matcher repeat() {
		return new Matcher() {
			@Override
			public Result match(final int position) {
				final Result x = Matcher.this.match(position);
				if (x == null) return null;

				int p = x.position;
				final List<Object> list = new ArrayList<Object>();
				x.copyTo(list);

				while (true) {
					final Result a = Matcher.this.match(p);
					if (a == null) break;

					p = a.position;
					a.copyTo(list);
				}
				return new Result(p, list);
			}
		};
	}

	public final Matcher and() {
		return new Matcher() {
			@Override
			public Result match(final int position) {
				return Matcher.this.match(position) != null ? new Result(position) : null;
			}
		};
	}

	public final Matcher not() {
		return new Matcher() {
			@Override
			public Result match(final int position) {
				return Matcher.this.match(position) != null ? null : new Result(position);
			}
		};
	}

	public final Matcher then(final Matcher second) {
		return new Matcher() {
			@Override
			public Result match(final int position) {
				final Result p = Matcher.this.match(position);
				if (p == null) return null;

				final Result a = second.match(p.position);
				if (a == null) return null;

				if (p.size() == 0) return a;

				final List<Object> list = new ArrayList<Object>();
				p.copyTo(list);
				a.copyTo(list);
				return new Result(a.position, list);
			}
		};
	}

	public final Matcher or(final Matcher second) {
		return new Matcher() {
			@Override
			public Result match(final int position) {
				final Result p = Matcher.this.match(position);
				return p != null ? p : second.match(position);
			}
		};
	}

	public static class Result {
		public final int position;
		private final Object[] list;

		public Result(final int position) {
			this.position = position;
			this.list = new Object[0];
		}

		public Result(final int position, final Object obj) {
			this.position = position;
			this.list = new Object[] { obj };
		}

		public Result(final int position, final List<Object> list) {
			this.position = position;
			this.list = list.toArray();
		}

		public void copyTo(final List<Object> a) {
			for (final Object e : list)
				a.add(e);
		}

		public Object[] toArray() {
			return Arrays.copyOf(list, list.length);
		}

		public int size() {
			return list.length;
		}

		public Object get(final int i) {
			return list[i];
		}
	}
}