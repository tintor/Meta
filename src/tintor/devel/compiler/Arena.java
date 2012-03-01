package tintor.devel.compiler;

public class Arena {
	public static int sum(final int a) {
		return a > 0 ? a + sum(a - 1) : 30;
	}

	public static int xsum(final int a) {
		return xsum(a, 30);
	}

	private static int xsum(final int a, final int s) {
		return a > 0 ? xsum(a - 1, s + a) : s;
	}

	public static int isum(int a) {
		int s = 0;
		while (a > 0) {
			s = s + a;
			a = a - 1;
		}
		return s;
	}

	public static int ixsum(int a, int s) {
		while (a > 0) {
			s = s + a;
			a = a - 1;
		}
		return s;
	}
}