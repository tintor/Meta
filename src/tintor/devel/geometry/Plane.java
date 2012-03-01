package tintor.devel.geometry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immutable 3d plane with unit normal.
 * 
 * @author Marko Tintor (tintor@gmail.com)
 */
public final class Plane {
	private static final AtomicInteger counter = new AtomicInteger();

	/** @return number of objects constructed */
	public static int counter() {
		return counter.get();
	}

	// Static fields
	/** Default formatter for toString() method */
	public static final ThreadLocal<String> defaultFormat = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "Plane(%s %s %s %s)";
		}
	};

	// Static factory methods
	/** @return plane from plane function parameters: a*x + b*y + c*z + d */
	public static Plane create(final float a, final float b, final float c, final float d) {
		return new Plane(Vector.unit(a, b, c), d);
	}

	/** @return plane from normal and point on plane */
	public static Plane create(final Vector normal, final Vector point) {
		final Vector unitNormal = normal.unit();
		return new Plane(unitNormal, -unitNormal.dot(point));
	}

	/** @return plane from normal and offset */
	public static Plane create(final Vector normal, final float offset) {
		return new Plane(normal, offset);
	}

	/** @return plane as bisection of line AB (normal points from B to A) */
	public static Plane bisection(final Vector a, final Vector b) {
		return create(a.sub(b), Vector.average(a, b));
	}

	/** @return plane from three points (points are counterclockwise around normal) */
	public static Plane create(final Vector a, final Vector b, final Vector c) {
		return create(b.sub(a).cross(c.sub(a)), a);
	}

	/** @return plane from array of complanar points in counterclockwise orientation */
	public static Plane create(final Vector[] vertices) {
		if (vertices.length < 3) throw new IllegalArgumentException("Less than 3 points.");
		// if (vertices.length == 3)
		return create(vertices[0], vertices[1], vertices[2]);
		//
		// // calculate sums
		// double ix = 0, iy = 0, iz = 0, ixx = 0, iyy = 0, izz = 0, ixy = 0, ixz = 0, iyz = 0;
		// for (final Vector v : vertices) {
		// ix += v.x;
		// iy += v.y;
		// iz += v.z;
		// ixx += v.x * v.x;
		// iyy += v.y * v.y;
		// izz += v.z * v.z;
		// ixy += v.x * v.y;
		// ixz += v.x * v.z;
		// iyz += v.y * v.z;
		// }
		//
		// // calculate matrix terms
		// final double ax = ix / vertices.length;
		// final double ay = iy / vertices.length;
		// final double az = iy / vertices.length;
		//
		// final double aa = ixx - ax * ix, bb = iyy - ay * iy, cc = izz - az * iz;
		// final double ab = ixy - ax * iy, ac = ixz - ax * iz, bc = iyz - ay * iz;
		//
		// // calculate best normal
		// // TODO could use decreasing entropy algorithm instead
		// Vector nmin = Vector.X;
		// double smin = aa * aa + ab * ab + ac * ac;
		//
		// final Random rand = new Random();
		// for (int i = 1000; i > 0; i--) {
		// final Vector n = Vector.create(rand.nextFloat() - 0.5f, rand.nextFloat() - 0.5f,
		// rand.nextFloat() - 0.5f).unitz();
		// if (n == Vector.Zero) continue;
		//
		// final double na = n.x * aa + n.y * ab + n.z * ac;
		// final double nb = n.x * ab + n.y * bb + n.z * bc;
		// final double nc = n.x * ac + n.y * bc + n.z * cc;
		// final double s = na * na + nb * nb + nc * nc;
		//
		// if (s < smin) {
		// smin = s;
		// nmin = n;
		// }
		// }
		//
		// // calculate projection vectors
		// final Vector dx = nmin.normal();
		// final Vector dy = nmin.cross(dx);
		//
		// // calculate projected area
		// final Vector w = vertices[vertices.length - 1];
		// double area = 0;
		// double wx = w.dot(dx);
		// double wy = w.dot(dy);
		//
		// for (final Vector v : vertices) {
		// final double vx = v.dot(dx);
		// final double vy = v.dot(dy);
		// area += (vx - wx) * (vy + wy);
		// wx = vx;
		// wy = vy;
		// }
		//
		// // calculate correct orientation
		// final Vector n = area > 0 ? nmin : nmin.neg();
		//
		// // construct plane
		// final Plane plane = new Plane(n, (float) (-n.x * ax - n.y * ay - n.z * az));
		//
		// // check if result is good enough
		// assert plane.maxDistance(vertices) <= 1e-7;
		//
		// // return plane
		// return plane;
	}

	//	private double maxDistance(final Vector[] vertices) {
	//		double dmax = 0;
	//		for (final Vector v : vertices)
	//			dmax = Math.max(dmax, Math.abs(distance(v)));
	//		return dmax;
	//	}

	// Static methods
	/** @return Plane(a, b, c).distance(p) */
	public static float distance(final Vector p, final Vector a, final Vector b, final Vector c) {
		// final Vector3 n = b.sub(a).cross(c.sub(a));
		// return n.dot(p) - n.dot(a);
		final float cax = c.x - a.x, cay = c.y - a.y, caz = c.z - a.z;
		final float bax = b.x - a.x, bay = b.y - a.y, baz = b.z - a.z;
		return (bay * caz - baz * cay) * (p.x - a.x) + (baz * cax - bax * caz) * (p.y - a.y)
				+ (bax * cay - bay * cax) * (p.z - a.z);
	}

	/** @return Plane(a, b, c).closest(p) */
	public static Vector closest(final Vector p, final Vector a, final Vector b, final Vector c) {
		// final Vector3 n = b.sub(a).cross(c.sub(a));
		// return p.sub(n, n.dot(p, a));
		final float cax = c.x - a.x, cay = c.y - a.y, caz = c.z - a.z;
		final float bax = b.x - a.x, bay = b.y - a.y, baz = b.z - a.z;

		final float nx = bay * caz - baz * cay;
		final float ny = baz * cax - bax * caz;
		final float nz = bax * cay - bay * cax;

		final float d = nx * (p.x - a.x) + ny * (p.y - a.y) + nz * (p.z - a.z);
		return Vector.create(p.x - nx * d, p.y - ny * d, p.z - nz * d);
	}

	// Fields
	/** unit normal of the plane */
	public final Vector unitNormal;
	/** offset = plane_function(Vector3.Zero) */
	public final float offset;

	// Constructors
	private Plane(final Vector unitNormal, final float offset) {
		if (unitNormal == null) throw new NullPointerException();
		this.unitNormal = unitNormal;
		this.offset = offset;
		counter.incrementAndGet();
	}

	// Operations
	/** @return signed distance between point and plane */
	public float distance(final Vector a) {
		return unitNormal.dot(a) + offset;
	}

	/** @return point on plane closest to A */
	public Vector closest(final Vector a) {
		return a.sub(distance(a), unitNormal);
	}

	// public Side side(final Vector3 a) {
	// return Side.classify(distance(a));
	// }

	public Plane move(final float a) {
		return new Plane(unitNormal, offset - a);
	}

	/** @return plane with reversed sides */
	public Plane flip() {
		return new Plane(unitNormal.neg(), -offset);
	}

	/** @return conversion to string using formatter */
	public String toString(final String format) {
		return String.format(format, unitNormal.x, unitNormal.y, unitNormal.z, offset);
	}

	@Override
	public String toString() {
		return toString(defaultFormat.get());
	}
}