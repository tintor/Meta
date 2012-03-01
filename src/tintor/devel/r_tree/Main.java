package tintor.devel.r_tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import tintor.Timer;
import tintor.devel.r_tree.RTree.Entry;

//60 new RTree(10, RTreeExtra.quadraticAreaSplit, RTreeExtra.deadSpacePenalty)
//60 new RTree(10, RTreeExtra.axisPerimeterSplit, RTreeExtra.deadSpacePenalty)
//62 new RTree(20, RTreeExtra.quadraticAreaSplit, RTreeExtra.deadSpacePenalty)
//63 new RTree(30, RTreeExtra.quadraticAreaSplit, RTreeExtra.deadSpacePenalty)
//65 new RTree(20, RTreeExtra.axisPerimeterSplit, RTreeExtra.deadSpacePenalty)
//65 new RTree(10, RTreeExtra.quadraticAreaSplit, RTreeExtra.perimeterPenalty)
//66 new RTree(30, RTreeExtra.axisPerimeterSplit, RTreeExtra.deadSpacePenalty)
//68 new RTree(10, RTreeExtra.axisPerimeterSplit, RTreeExtra.perimeterPenalty)
//70 new RTree(20, RTreeExtra.quadraticAreaSplit, RTreeExtra.perimeterPenalty)
//71 new RTree(30, RTreeExtra.quadraticAreaSplit, RTreeExtra.perimeterPenalty)
//73 new RTree(20, RTreeExtra.axisPerimeterSplit, RTreeExtra.perimeterPenalty)
//75 new RTree(30, RTreeExtra.axisPerimeterSplit, RTreeExtra.perimeterPenalty)

public class Main {
	static final RTree tree = new RTree(30, 15, RTreeExtra.quadraticAreaSplit, RTreeExtra.deadSpacePenalty);
	static final RTree tree2 = new RTree(30, 15, RTreeExtra.axisPerimeterSplit, RTreeExtra.perimeterPenalty);
	static final List<RTree.Entry> list = new ArrayList<RTree.Entry>();

	static final Timer timer = new Timer();
	static final Timer timer2 = new Timer();
	static final Timer timer3 = new Timer();

	static final Random rand = new Random();
	static final double range = 1000;

	static final int n = 800000;

	public static void main(final String[] args) {
		for (int i = 0; i < n; i++) {
			final double x = rand.nextDouble() * range, y = rand.nextDouble() * range;
			final double size = 1;
			final RTree.Entry e = new Entry(new Rect(x - size / 2, x + size / 2, y - size / 2, y + size / 2), i);
			//tree.insert(e);
			tree2.insert(e);
			list.add(e);
			if (i % 1000 == 0)
				System.out.printf("left:%s height:%s:%s efficiency:%.0f:%.0f\n", n - i, tree.height(), tree2
						.height(), tree.efficiency() * 100, tree2.efficiency() * 100);
		}

		//for (int i = 0; i < 20; i++) {
		//	for (double r = 0.5; r <= range / 4; r *= 2)
		//		run(r);
		//}
	}

	static void run(final double qr) {
		final double qx = rand.nextDouble() * range, qy = rand.nextDouble() * range;
		final Rect query = new Rect(qx - qr, qx + qr, qy - qr, qy + qr);

		timer.time = 0;
		timer.restart();
		final Iterator<RTree.Entry> it = tree.search(query, RTreeExtra.intersectsQuery);
		int count = 0, tested = 0;
		while (true) {
			final RTree.Entry e = it.next();
			if (e == null) break;
			tested++;
			if (filter(e.key, qx, qy, qr)) count += 1;
		}
		timer.stop();

		//		timer2.time = 0;
		//		timer2.restart();
		//		int count2 = 0;
		//		for (final RTree.Entry e : list)
		//			if (filter(e.key, qx, qy, qr)) count2++;
		//		timer2.stop();

		timer3.time = 0;
		timer3.restart();
		final Iterator<RTree.Entry> it2 = tree.search(query, RTreeExtra.intersectsQuery);
		int count3 = 0, tested3 = 0;
		while (true) {
			final RTree.Entry e = it2.next();
			if (e == null) break;
			tested3++;
			if (filter(e.key, qx, qy, qr)) count3 += 1;
		}
		timer3.stop();

		System.out.printf("radius:%s rtree:%s:%s touched:%.2f%%:%.2f%%\n", qr, timer, timer3, 100 * (double) tested
				/ list.size(), 100 * (double) tested3 / list.size());
	}

	public static boolean filter(final Rect rect, final double qx, final double qy, final double qr) {
		final double dx = rect.xmin + 0.5 - qx;
		final double dy = rect.ymin + 0.5 - qy;
		final double dr = 0.5 + qr;
		return dx * dx + dy * dy <= dr * dr;
	}
}