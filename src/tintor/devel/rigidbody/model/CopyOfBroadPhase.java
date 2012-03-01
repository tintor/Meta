package tintor.devel.rigidbody.model;

import java.util.List;

import tintor.devel.geometry.Interval;
import tintor.devel.geometry.Vector;

interface CopyOfBroadPhase {
	void updateFixedBodies(List<Body> fixedBodies);

	void run(List<Body> staticBodies, List<Body> dynamicBodies, NarrowPhase narrowPhase, List<Contact> contacts);
}

interface Colideable {
	Interval getSpan(Vector dir);
}

class BruteForceBroadPhase implements CopyOfBroadPhase {
	private List<Body> fixedBodies;

	@Override
	public void updateFixedBodies(final List<Body> fixedBodies) {
		this.fixedBodies = fixedBodies;
	}

	@Override
	public void run(final List<Body> staticBodies, final List<Body> dynamicBodies, final NarrowPhase narrowPhase,
			final List<Contact> contacts) {
		for (int ai = 0; ai < dynamicBodies.size(); ai++) {
			final Body a = dynamicBodies.get(ai);

			for (final Body b : fixedBodies)
				narrowPhase.run(a, b, contacts);

			for (final Body b : staticBodies)
				narrowPhase.run(a, b, contacts);

			for (int bi = 0; bi < ai; bi++)
				narrowPhase.run(a, dynamicBodies.get(bi), contacts);
		}
	}
}

class SpatialHashingBroadPhase implements CopyOfBroadPhase {
	private final int SIZE = 1511; // prime number

	private final Entry[] bucket = new Entry[SIZE]; // static and dynamic bodies go here
	private final Entry[] init = new Entry[SIZE]; // fixed bodies go here
	private final int[] freshness = new int[SIZE];
	private int frame = 0;

	static class Entry {
		final Body body;
		final Entry next;

		Entry(final Body b, final Entry n) {
			body = b;
			next = n;
		}
	}

	@Override
	public void updateFixedBodies(final List<Body> fixedBodies) {
		frame += 1;

		for (final Body body : fixedBodies) {
			final int index = -1; // FIXME
			init[index] = new Entry(body, empty(index) ? null : init[index]);
		}
	}

	@Override
	public void run(final List<Body> staticBodies, final List<Body> dynamicBodies, final NarrowPhase narrowPhase,
			final List<Contact> contacts) {
		frame += 1;

		for (final Body body : staticBodies) {
			final int index = -1; // FIXME
			insert(body, index);
		}

		for (final Body body : dynamicBodies) {
			final int index = -1; // FIXME
			final Entry list = insert(body, index);

			// this can be run safely (safely for broadphase) in parallel
			for (Entry e = list.next; e != null; e = e.next)
				narrowPhase.run(body, e.body, contacts);
		}
	}

	private boolean empty(final int index) {
		if (freshness[index] != frame) {
			freshness[index] = frame;
			return true;
		}
		return false;
	}

	private Entry insert(final Body body, final int index) {
		return bucket[index] = new Entry(body, empty(index) ? init[index] : bucket[index]);
	}

	private int index(final int x, final int y, final int z) {
		return Math.abs((x * 73856093 ^ y * 19349663 ^ z * 83492791) % bucket.length);
	}
}