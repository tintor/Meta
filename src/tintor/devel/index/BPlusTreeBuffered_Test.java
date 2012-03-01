package tintor.devel.index;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BPlusTreeBuffered_Test {
	private File file;

	@Test
	public void misc() {
		final BPlusTree tree = BPlusTreeBuffered.create(file, 3);
		try {
			for (int i = 1; i <= 5; i++)
				tree.put(i * 2, i * 20);

			Assert.assertEquals(-1, tree.get(0, -1));
			Assert.assertEquals(20, tree.get(2, -1));
			Assert.assertEquals(100, tree.get(10, -1));
			Assert.assertEquals(-1, tree.get(12, -1));
			Assert.assertEquals(-1, tree.get(7, -1));
		}
		finally {
			tree.close();
		}
	}

	@Test
	public void listZero() {
		testList();
	}

	@Test
	public void listOne() {
		testList(1);
	}

	@Test
	public void listTwo() {
		testList(4, 3);
	}

	@Test
	public void listThree() {
		testList(-9, 10, 2);
	}

	@Test
	public void listDescending() {
		final int[] desc = new int[5000];
		for (int i = 0; i < desc.length; i++)
			desc[i] = desc.length - 1 - i;

		final int[] asc = new int[desc.length];
		for (int i = 0; i < asc.length; i++)
			asc[i] = i;

		testList(desc, asc);
	}

	@Test
	public void listSame() {
		final int[] same = new int[5000];
		for (int i = 0; i < same.length; i++)
			same[i] = 787;

		testList(same, new int[] { 787 });
	}

	@Test
	public void listRandom() {
		final int[] shuffled = new int[5000];
		for (int i = 0; i < shuffled.length; i++)
			shuffled[i] = i;

		final Random rand = new Random(Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
				+ Calendar.getInstance().get(Calendar.YEAR) * 366);
		for (int i = 0; i < shuffled.length; i++) {
			final int j = rand.nextInt(shuffled.length);
			final int t = shuffled[i];
			shuffled[i] = shuffled[j];
			shuffled[j] = t;
		}

		final int[] sorted = new int[shuffled.length];
		for (int i = 0; i < shuffled.length; i++)
			sorted[i] = i;

		testList(shuffled, sorted);
	}

	private void testList(final int... list) {
		final int[] output = Arrays.copyOf(list, list.length);
		Arrays.sort(output);
		testList(list, output);
	}

	private void testList(final int[] input, final int[] output) {
		final BPlusTree write = BPlusTreeBuffered.create(file, 3);
		try {
			for (final int a : input)
				write.put(a, ~a);
		}
		finally {
			write.close();
		}

		final BPlusTree read = BPlusTreeBuffered.open(file);
		try {
			int i = 0;
			for (final BPlusTreeIterator it = read.iterator(); it.valid(); it.next()) {
				Assert.assertEquals(output[i], it.key());
				Assert.assertEquals(~output[i], it.value());
				i += 1;
			}
			Assert.assertEquals(output.length, i);
		}
		finally {
			read.close();
		}
	}

	@Before
	public void setUp() {
		try {
			file = File.createTempFile("junit_", ".bplus.tree");
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@After
	public void tearDown() {
		file.delete();
	}
}
