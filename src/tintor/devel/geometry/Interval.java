package tintor.devel.geometry;

import java.util.concurrent.atomic.AtomicInteger;

public final class Interval {
	private static final AtomicInteger counter = new AtomicInteger();

	public static int counter() {
		return counter.get();
	}

	// Fields
	public final float min;
	public final float max;

	// Factory methods
	public static Interval create(final float min, final float max) {
		return new Interval(min, max);
	}

	// Constructors
	private Interval(final float min, final float max) {
		this.min = min;
		this.max = max;
		counter.incrementAndGet();
	}

	// Operations
	public Interval include(final float value) {
		if (value < min) return create(value, max);
		if (value > max) return create(min, value);
		return this;
	}

	public Interval shift(final float a) {
		return create(min + a, max + a);
	}

	public float width() {
		return max - min;
	}

	public float center() {
		return (max + min) / 2;
	}

	public Interval union(final Interval a) {
		return create(Math.min(min, a.min), Math.max(max, a.max));
	}

	public float distance(final Interval b) {
		return (Math.abs(min - b.min + max - b.max) + min - max + b.min - b.max) / 2;
	}

	// From Object
	@Override
	public String toString() {
		return String.format("Interval[%s %s]", min, max);
	}
}