package tintor.devel.geometry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immutable single-precision floating-point 3d line represented with two points.
 * 
 * @author Marko Tintor (tintor@gmail.com)
 */
public final class Line {
	/** Default formatter for toString() method */
	public static final ThreadLocal<String> defaultFormat = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "Line(%s %s)";
		}
	};

	private static final AtomicInteger counter = new AtomicInteger();

	/** @return number of objects constructed */
	public static int counter() {
		return counter.get();
	}

	public static Line create(Vector a, Vector b) {
		return new Line(a, b);
	}

	public static float distanceSquared(final Vector a, final Vector b, final Vector p) {
		final Vector d = b.sub(a);
		final float n = d.dot(p.sub(a)) / d.square();
		return (n >= 1 ? b : n <= 0 ? a : a.add(n, d)).distanceSquared(p);
	}

	private static float square(final float a) {
		return a * a;
	}

	public static float distanceSquaredInf(final Vector a, final Vector b, final Vector p) {
		// final Vector3 d = b.sub(a);
		// final float n = d.dot(p.sub(a)) / d.square();
		// return a.add(n, d).distanceSquared(p);

		final float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
		final float mx = p.x - a.x, my = p.y - a.y, mz = p.z - a.z;
		final float n = (dx * mx + dy * my + dz * mz) / (dx * dx + dy * dy + dz * dz);
		return square(dx * n - mx) + square(dy * n - my) + square(dz * n - mz);
	}

	// Fields
	public final Vector a, b;

	// Constructor
	private Line(final Vector a, final Vector b) {
		this.a = a;
		this.b = b;
		counter.incrementAndGet();
	}

	// Operations
	public Vector point(final float t) {
		return Vector.linear(a, b, t);
	}

	// With Plane
	/** Distance along the line, line is assumed infinite. */
	public float distance(final Plane p) {
		return -p.distance(a) / p.unitNormal.dot(b.sub(a));
	}

	// With Point
	public float nearestInf(final Vector p) {
		final Vector d = b.sub(a);
		return d.dot(p.sub(a)) / d.square();
	}

	private static float clamp(float a, float min, float max) {
		if (a > max) return max;
		if (a < min) return min;
		return a;
	}

	public float nearest(final Vector p) {
		return clamp(nearestInf(p), 0, 1);
	}

	public float distanceSquaredInf(final Vector p) {
		return distanceSquaredInf(a, b, p);
	}

	public float distanceSquared(final Vector p) {
		return distanceSquared(a, b, p);
	}

	// With Line
	public static class Result {
		final float a, b;

		private Result(float a, float b) {
			this.a = a;
			this.b = b;
		}

		private boolean isFinite() {
			return Line.isFinite(a) && Line.isFinite(b);
		}
	}

	public Result closestInf(final Line q) {
		final Vector A = b.sub(a), B = q.a.sub(q.b), C = a.sub(q.a);
		final float aa = A.square(), bb = B.square(), ab = A.dot(B), ac = A.dot(C), bc = B.dot(C);
		final float det = aa * bb - ab * ab;
		return new Result((ab * bc - bb * ac) / det, (aa * bc - ab * ac) / det);
	}

	public Result nearestInf(final Line q) {
		final Result k = closestInf(q);
		return k.isFinite() ? k : new Result(nearestInf(Vector.Zero), q.nearestInf(Vector.Zero));
	}

	public Result nearest(final Line q) {
		final Result k = closestInf(q);
		if (k.isFinite()) return new Result(clamp(k.a, 0, 1), clamp(k.b, 0, 1));
		final float pa = nearest(q.a), pb = nearest(q.b);
		return point(pa).distanceSquared(q.a) < point(pb).distanceSquared(q.b) ? new Result(pa, 0)
				: new Result(pb, 1);
	}

	public float distanceSquaredInf(final Line q) {
		final Result k = nearestInf(q);
		return point(k.a).distanceSquared(q.point(k.b));
	}

	public float distanceSquared(final Line q) {
		final Result k = closestInf(q);
		if (k.isFinite()) return point(clamp(k.a, 0, 1)).distanceSquared(q.point(clamp(k.b, 0, 1)));
		return Math.min(distanceSquared(q.a), distanceSquared(q.b));
	}

	public float closestPointInf(final Line q) {
		final Vector A = b.sub(a), B = q.a.sub(q.b), C = a.sub(q.a);
		final float aa = A.square(), bb = B.square(), ab = A.dot(B), ac = A.dot(C), bc = B.dot(C);
		return (ab * bc - bb * ac) / (aa * bb - ab * ab);
	}

	public float nearestPointInf(final Line q) {
		final float x = closestPointInf(q);
		return isFinite(x) ? x : 0; // if lines are parallel every point is nearest!
	}

	public float nearestPoint(final Line q) {
		final float x = closestPointInf(q);
		return isFinite(x) ? clamp(x, 0, 1) : q.distanceSquared(a) < q.distanceSquared(b) ? 1 : 0;
	}

	private static boolean isFinite(float a) {
		return !Float.isInfinite(a) && !Float.isNaN(a);
	}

	// Misc
	/** @return b - a */
	public Vector direction() {
		return b.sub(a);
	}

	/** Returns part of line in negative side of plane */
	public Line clip(final Plane p, float eps) {
		final float da = p.distance(a), db = p.distance(b);

		if (da > eps) {
			if (db > eps) return null;
			if (db < -eps) return Line.create(Vector.linear(a, b, da / (da - db)), b);
			return Line.create(b, b);
		}
		if (da < -eps) return db > eps ? Line.create(a, Vector.linear(a, b, da / (da - db))) : this;
		return db > eps ? Line.create(a, a) : this;
	}

	/** @return conversion to string using formatter */
	public final String toString(String format) {
		return String.format(format, a, b);
	}

	@Override
	public final String toString() {
		return toString(defaultFormat.toString());
	}
}