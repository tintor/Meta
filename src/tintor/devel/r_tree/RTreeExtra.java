package tintor.devel.r_tree;

import java.util.Arrays;
import java.util.List;

import tintor.devel.r_tree.RTree.Consistent;
import tintor.devel.r_tree.RTree.Entry;
import tintor.devel.r_tree.RTree.Penalty;
import tintor.devel.r_tree.RTree.PickSplit;

public class RTreeExtra {
	/** O(N^2) */
	public static final PickSplit quadraticAreaSplit = new PickSplit() {
		@Override
		public void pickSplit(final List<Entry> entries, final int min, final List<Entry> partitionA,
				final List<Entry> partitionB) {
			final int degree = entries.size() - 1;
			assert degree >= 2;

			// PickSeeds
			double maxWaste = Double.NEGATIVE_INFINITY;
			int bestA = -1, bestB = -1;
			for (int a = 1; a < entries.size(); a++)
				for (int b = 0; b < a; b++) {
					final Rect keyA = entries.get(a).key;
					final Rect keyB = entries.get(b).key;
					final double waste = keyA.unionArea(keyB) - keyA.area() - keyB.area();
					if (waste > maxWaste) {
						maxWaste = waste;
						bestA = a;
						bestB = b;
					}
				}

			final Entry entryA = entries.get(bestA);
			entries.remove(bestA);
			Rect keyA = entryA.key;
			partitionA.add(entryA);

			final Entry entryB = entries.get(bestB);
			entries.remove(bestB);
			Rect keyB = entryB.key;
			partitionB.add(entryB);

			// TODO can be improved
			for (int i = 0; i < entries.size(); i++) {
				final Entry e = entries.get(i);

				final int entriesLeft = entries.size() - i;

				if (partitionA.size() == min - entriesLeft) {
					partitionA.add(e);
					continue;
				}
				if (partitionB.size() == min - entriesLeft) {
					partitionB.add(e);
					continue;
				}

				final Rect keyAx = keyA.union(e.key);
				final Rect keyBx = keyB.union(e.key);

				if (keyAx.area() - keyA.area() < keyBx.area() - keyB.area()) {
					keyA = keyAx;
					partitionA.add(e);
				} else {
					keyB = keyBx;
					partitionB.add(e);
				}
			}
		}
	};

	/** O(N*log(N)) */
	public static final PickSplit axisPerimeterSplit = new PickSplit() {
		@Override
		public void pickSplit(final List<Entry> list, final int min, final List<Entry> partitionA,
				final List<Entry> partitionB) {
			final int size = list.size();
			final double[] buffer = new double[size];

			final Entry[] xsorted = list.toArray(new Entry[size]);
			Arrays.sort(xsorted, Entry.compareX);
			final int xsplit = split(xsorted, min, buffer);
			final double xbest = buffer[0];

			final Entry[] ysorted = list.toArray(new Entry[size]);
			Arrays.sort(ysorted, Entry.compareY);
			final int ysplit = split(ysorted, min, buffer);
			final double ybest = buffer[0];

			if (xbest < ybest) {
				fill(xsorted, 0, xsplit, partitionA);
				fill(xsorted, xsplit, size, partitionB);
			} else {
				fill(ysorted, 0, ysplit, partitionA);
				fill(ysorted, ysplit, size, partitionB);
			}
		}

		void fill(final Entry[] array, int start, final int end, final List<Entry> list) {
			while (start < end)
				list.add(array[start++]);
		}

		/** splits to sorted[0..result) and sorted[result..sorted.length) 
		 *  also puts minimal penalty in buffer[0]*/
		int split(final Entry[] sorted, final int min, final double[] buffer) {
			final int max = sorted.length - min;

			final MutableRect box = new MutableRect();
			for (int i = 0; i < max; i++) {
				box.union(sorted[i].key);
				buffer[i] = box.perimeter();
			}

			box.reset();

			double best = Double.POSITIVE_INFINITY;
			int count = 0;

			for (int i = sorted.length - 1; i >= min; i--) {
				box.union(sorted[i].key);
				if (i - 1 < max) {
					final double p = box.perimeter() + buffer[i - 1];
					if (p < best) {
						best = p;
						count = i;
					}
				}
			}

			buffer[0] = best;
			assert min <= count && count <= max;
			return count;
		}
	};

	public static final Penalty deadSpacePenalty = new Penalty() {
		@Override
		public double penalty(final Rect key, final Rect newKey) {
			return key.unionArea(newKey) - key.area();
		}
	};

	public static final Penalty perimeterPenalty = new Penalty() {
		@Override
		public double penalty(final Rect key, final Rect newKey) {
			return key.unionPerimeter(newKey);
		}
	};

	public static final Consistent<Rect> intersectsQuery = new Consistent<Rect>() {
		@Override
		public boolean consistent(final Rect key, final Rect query) {
			return key.intersects(query);
		}
	};

	public static final Consistent<Rect> containsQuery = new Consistent<Rect>() {
		@Override
		public boolean consistent(final Rect key, final Rect query) {
			return key.contains(query);
		}
	};
}
