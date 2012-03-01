package tintor.devel.geometry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immutable single-precision floating-point 3d ray represented with origin point and unit direction.
 * 
 * @author Marko Tintor (tintor@gmail.com)
 */
public final class Ray {
	// Static fields
	/** Default formatter for toString() method */
	public static final ThreadLocal<String> defaultFormat = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "Ray(%s %s)";
		}
	};

	private static final AtomicInteger counter = new AtomicInteger();

	/** @return number of objects constructed */
	public static int counter() {
		return counter.get();
	}

	// Static factory methods
	public static Ray ray(Vector origin, Vector dir) {
		return new Ray(origin, dir);
	}

	public static Ray line(Vector a, Vector b) {
		return new Ray(a, b.sub(a));
	}

	/** ASSUME |dir| = 1 AND |normal| = 1 */
	public static Ray reflect(final Vector dir, final Vector point, final Vector normal) {
		return new Ray(point, dir.sub(2 * normal.dot(dir), normal));
	}

	// Fields
	/** origin of ray */
	public final Vector origin;
	/** unit direction of ray */
	public final Vector unitDir;

	// Constructor
	private Ray(final Vector origin, final Vector dir) {
		this.origin = origin;
		this.unitDir = dir.unit();
		counter.incrementAndGet();
	}

	/** @return point on the ray. */
	public Vector point(final float t) {
		return origin.add(t, unitDir);
	}

	// With Plane
	/**
	 * @return distance along the ray<br>
	 *         Returns +-Inf if ray is parallel to plane<br>
	 *         Returns NaN if ray is normal to plane
	 */
	public float distance(final Plane p) {
		return -p.distance(origin) / p.unitNormal.dot(unitDir);
	}

	// With Point
	public float nearest(final Vector p) {
		return unitDir.dot(p, origin);
	}

	public float distanceSquared(final Vector p) {
		return point(nearest(p)).distanceSquared(p);
	}

	// With Ray
	public static class Result {
		final float a, b;

		private Result(float a, float b) {
			this.a = a;
			this.b = b;
		}

		private boolean isFinite() {
			return !Float.isInfinite(a) && !Float.isNaN(a) && !Float.isInfinite(b) && !Float.isNaN(b);
		}
	}

	/**
	 * @return coordinates of closest points beetwen this and q.<BR>
	 *         Returns non-finite Vector2 if this.dir is colinear with q.dir.
	 */
	public Result closest(final Ray q) {
		final Vector r = origin.sub(q.origin);
		final float a = unitDir.dot(q.unitDir), b = unitDir.dot(r), c = q.unitDir.dot(r), d = 1 - a * a;
		return new Result((a * c - b) / d, (a * b - c) / d);
	}

	public Result nearest(final Ray q) {
		final Result k = closest(q);
		return k.isFinite() ? k : new Result(-origin.dot(unitDir), -q.origin.dot(q.unitDir));
	}

	public float distanceSquared(final Ray q) {
		final Result k = nearest(q);
		return point(k.a).distanceSquared(point(k.b));
	}

	/** @return conversion to string using formatter */
	public final String toString(String format) {
		return String.format(format, origin, unitDir);
	}

	@Override
	public final String toString() {
		return toString(defaultFormat.toString());
	}
}