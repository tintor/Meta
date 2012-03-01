package tintor.devel.arena;

import java.io.IOException;

public class Arena {
	public static void main(final String[] args) throws IOException, ClassNotFoundException {
		while (true) {

		}
	}

	static int measure(final int a) {
		final long s = a * 1000000000 + System.nanoTime();
		int i = 0;
		while (System.nanoTime() < s)
			i++;
		return i;
	}
}
// 445185