package tintor.devel.random;

import java.util.HashSet;
import java.util.Set;

public class PseudoTest {
	public static void main(final String[] args) {
		final Set<Integer> set = new HashSet<Integer>();
		int a = 2;
		while (!set.contains(a)) {
			if (set.size() % 100000 == 0) System.out.println(set.size());
			set.add(a);
			a = xorShift(a);
		}
		System.out.println("size = " + set.size());
	}

	static int xorShift(int y) {
		y ^= y << 6;
		y ^= y >>> 21;
		y ^= y << 7;
		return y;
	}
}
