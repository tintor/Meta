package tintor.devel.geometry;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immutable single-precision floating-point 3d vector.
 * 
 * @author Marko Tintor (tintor@gmail.com)
 */
public final class Vector implements Comparable<Vector> {
	private static final AtomicInteger counter = new AtomicInteger();

	// Constants
	/** Vector3(0, 0, 0) */
	public final static Vector Zero = create(0, 0, 0);
	/** Vector3(1, 0, 0) */
	public final static Vector X = create(1, 0, 0);
	/** Vector3(0, 1, 0) */
	public final static Vector Y = create(0, 1, 0);
	/** Vector3(0, 0, 1) */
	public final static Vector Z = create(0, 0, 1);

	// Static fields
	/** Default formatter for toString() method */
	public static final ThreadLocal<String> defaultFormat = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "Vector(%s %s %s)";
		}
	};

	/** @return number of objects constructed */
	public static int counter() {
		return counter.get();
	}

	// Static factory methods
	/** @return Vector3(x, y, z) */
	public static Vector create(final float x, final float y, final float z) {
		return new Vector(x, y, z);
	}

	/** @return Vector3(x, y, z) */
	public static Vector create(final double x, final double y, final double z) {
		return create((float) x, (float) y, (float) z);
	}

	/** @return Vector3(x * a, y * a, z * a) */
	public static Vector mul(final float x, final float y, final float z, final float a) {
		return create(x * a, y * a, z * a);
	}

	/** @return Vector3(x / a, y / a, z / a) */
	public static Vector div(final float x, final float y, final float z, final float a) {
		return create(x / a, y / a, z / a);
	}

	/** @return Random unit vector with equal probability */
	public static Vector randomDirection(final Random rand) {
		while (true) {
			final float x = rand.nextFloat() - 0.5f;
			final float y = rand.nextFloat() - 0.5f;
			final float z = rand.nextFloat() - 0.5f;

			final float s = x * x + y * y + z * z;
			if (s < 0.25) {
				final double q = Math.sqrt(s);

				final float xq = (float) (x / q);
				if (!isFinite(xq)) continue;

				final float yq = (float) (y / q);
				if (!isFinite(yq)) continue;

				final float zq = (float) (z / q);
				if (!isFinite(zq)) continue;

				return Vector.create(xq, yq, zq);
			}
		}
	}

	/**
	 * Linear interpolation
	 * 
	 * @return a*(1-t) + b*t
	 */
	public static Vector linear(final Vector a, final Vector b, final float t) {
		final float s = 1 - t;
		final float x = a.x * s + b.x * t;
		final float y = a.y * s + b.y * t;
		final float z = a.z * s + b.z * t;
		return create(x, y, z);
	}

	/**
	 * Cubic interpolation
	 * 
	 * @return a*(1-t)^3 + b*3*(1-t)^2*t + c*3*(1-t)*t^2 + d*t^3
	 */
	public static Vector cubic(final Vector a, final Vector b, final Vector c, final Vector d, final float t) {
		final float s = 1 - t, w = t * s * 3;
		final float A = s * s * s, B = w * s, C = w * t, D = t * t * t;

		final float x = a.x * A + b.x * B + c.x * C + d.x * D;
		final float y = a.y * A + b.y * B + c.y * C + d.y * D;
		final float z = a.z * A + b.z * B + c.z * C + d.z * D;
		return create(x, y, z);
	}

	/** @return (a + b) / 2 */
	public static Vector average(final Vector a, final Vector b) {
		return create((a.x + b.x) / 2, (a.y + b.y) / 2, (a.z + b.z) / 2);
	}

	/** @return sum(array) / array.length */
	public static Vector average(final Vector... array) {
		return sum(array).div(array.length);
	}

	/** @return a + b + c */
	public static Vector sum(final Vector a, final Vector b, final Vector c) {
		return create(a.x + b.x + c.x, a.y + b.y + c.y, a.z + b.z + c.z);
	}

	/** @return sum of array elements */
	public static Vector sum(final Vector... array) {
		float x = 0, y = 0, z = 0;
		for (final Vector a : array) {
			x += a.x;
			y += a.y;
			z += a.z;
		}
		return create(x, y, z);
	}

	/** @return Vector(x y z) / |Vector(x y z)| */
	public static Vector unit(final float x, final float y, final float z) {
		return div(x, y, z, (float) Math.sqrt(x * x + y * y + z * z));
	}

	/** @return unit vector from Vector(x y z) (result is always finite) */
	public static Vector unitz(final float x, final float y, final float z) {
		final double q = 1 / Math.sqrt(x * x + y * y + z * z);
		if (Double.isInfinite(q) || Double.isNaN(q)) return Vector.Zero;

		final float xq = (float) (x * q);
		if (!isFinite(xq)) return Vector.Zero;

		final float yq = (float) (y * q);
		if (!isFinite(yq)) return Vector.Zero;

		final float zq = (float) (z * q);
		if (!isFinite(zq)) return Vector.Zero;

		return create(xq, yq, zq);
	}

	// Fields
	/** first component */
	public final float x;
	/** second component */
	public final float y;
	/** third component */
	public final float z;

	// Constructors
	private Vector(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		counter.incrementAndGet();
	}

	// Addition
	/** @return this + v */
	public Vector add(final Vector v) {
		return create(x + v.x, y + v.y, z + v.z);
	}

	/** @return this - v */
	public Vector sub(final Vector v) {
		return create(x - v.x, y - v.y, z - v.z);
	}

	/** @return -this */
	public Vector neg() {
		return create(-x, -y, -z);
	}

	// Addition with Multiplication
	/** @return this + v*a */
	public Vector add(final float a, final Vector v) {
		return create(x + v.x * a, y + v.y * a, z + v.z * a);
	}

	/** @return this - v*a */
	public Vector sub(final float a, final Vector v) {
		return create(x - v.x * a, y - v.y * a, z - v.z * a);
	}

	/** @return this + m*v */
	public Vector add(final Matrix3 m, final Vector v) {
		return create(x + m.a.dot(v), y + m.b.dot(v), z + m.c.dot(v));
	}

	/** @return this + m*v*a */
	public Vector add(final Matrix3 m, final Vector v, final float a) {
		return create(x + m.a.dot(v) * a, y + m.b.dot(v) * a, z + m.c.dot(v) * a);
	}

	/** @return this - m*v */
	public Vector sub(final Matrix3 m, final Vector v) {
		return create(x - m.a.dot(v), y - m.b.dot(v), z - m.c.dot(v));
	}

	/** @return this + transposed(v)*m */
	public Vector add(final Vector v, final Matrix3 m) {
		return create(x + m.dotX(v), y + m.dotY(v), z + m.dotZ(v));
	}

	/** @return this - transposed(v)*m */
	public Vector sub(final Vector v, final Matrix3 m) {
		return create(x - m.dotX(v), y - m.dotY(v), z - m.dotZ(v));
	}

	// Multiplication
	/** @return this * a */
	public Vector mul(final float a) {
		return create(x * a, y * a, z * a);
	}

	/** @return Quaternion(0,this) * a */
	public Quaternion mul(final Quaternion a) {
		final float iw = -x * a.x - y * a.y - z * a.z;
		final float ix = +x * a.w + y * a.z - z * a.y;
		final float iy = -x * a.z + y * a.w + z * a.x;
		final float iz = +x * a.y - y * a.x + z * a.w;
		return Quaternion.create(iw, ix, iy, iz);
	}

	/** @return transposed(this) * a */
	public Matrix3 mul(final Vector a) {
		return Matrix3.row(a.mul(x), a.mul(y), a.mul(z));
	}

	/** @return transposed(this) * m */
	public Vector mul(final Matrix3 m) {
		return mul(x, y, z, m);
	}

	/** @return transposed(Vector3(x, y, z)) * m */
	public static Vector mul(final float x, final float y, final float z, final Matrix3 m) {
		return create(x * m.a.x + y * m.b.x + z * m.c.x, x * m.a.y + y * m.b.y + z * m.c.y, x * m.a.z + y
				* m.b.z + z * m.c.z);
	}

	/** @return transposed(this) * transposed(m) */
	public Vector mulTransposed(final Matrix3 m) {
		final float nx = x * m.a.x + y * m.a.y + z * m.a.z;
		final float ny = x * m.b.x + y * m.b.y + z * m.b.z;
		final float nz = x * m.c.x + y * m.c.y + z * m.c.z;
		return create(nx, ny, nz);
	}

	// Division
	/** @return this / a */
	public Vector div(final float a) {
		return create(x / a, y / a, z / a);
	}

	/**
	 * unit length direction<br>
	 * for zero vector returns NaN vector
	 * 
	 * @return this / |this|
	 */
	public Vector unit() {
		return unit(x, y, z);
	}

	/**
	 * unit length direction<br>
	 * for zero vector returns Vector3.Zero
	 * 
	 * @return makeFinite(this / |this|)
	 */
	public Vector unitz() {
		return unitz(x, y, z);
	}

	private static boolean isFinite(final float a) {
		return !Float.isInfinite(a) && !Float.isNaN(a);
	}

	// public static Vector3 finite(final Vector3 u) {
	// return u.isFinite() ? u : Vector3.Zero;
	// }

	/** limit length of this vector to max */
	public Vector limitLength(final float max) {
		final float q = square();
		return q <= max * max ? this : mul(max / (float) Math.sqrt(q));
	}

	/** direction to a of unit length */
	public Vector direction(final Vector a) {
		final float dx = a.x - x, dy = a.y - y, dz = a.z - z;
		final float q = (float) (1 / Math.sqrt(dx * dx + dy * dy + dz * dz));
		return mul(dx, dy, dz, q);
	}

	// Dot product
	/** @return this * a (dot product) */
	public float dot(final Vector a) {
		return x * a.x + y * a.y + z * a.z;
	}

	/** @return this * (a - b) (dot product) */
	public float dot(final Vector a, final Vector b) {
		return x * (a.x - b.x) + y * (a.y - b.y) + z * (a.z - b.z);
	}

	/** @return this * this (square of length) */
	public float square() {
		return dot(this);
	}

	/** @return |this| (length of vector) */
	public double length() {
		return Math.sqrt(square());
	}

	/** @return (this - a) * (this - a) */
	public float distanceSquared(final Vector a) {
		return (x - a.x) * (x - a.x) + (y - a.y) * (y - a.y) + (z - a.z) * (z - a.z);
	}

	/** @return |this - a| */
	public double distance(final Vector a) {
		return Math.sqrt(distanceSquared(a));
	}

	// Unique
	/**
	 * mixed vector product
	 * 
	 * @return this * (a x b) = determinant |this / a / b|
	 */
	public float mixed(final Vector a, final Vector b) {
		return x * (a.y * b.z - a.z * b.y) + y * (a.z * b.x - a.x * b.z) + z * (a.x * b.y - a.y * b.x);
	}

	/** @return angle [0, PI] between this and a */
	public double angle(final Vector a) {
		return Math.acos(dot(a) / Math.sqrt(square() + a.square()));
	}

	/** @return angle [0, 2*PI] from a to b. */
	public double fullAngle(final Vector a, final Vector b) {
		return mixed(a, b) >= 0 ? a.angle(b) : 2 * Math.PI - a.angle(b);
	}

	/** @return cross product matrix: M(a) * b = a x b */
	public Matrix3 tilda() {
		return Matrix3.row(0, -z, y, z, 0, -x, -y, x, 0);
	}

	/** @return square of cross product matrix */
	public Matrix3 tildaSqr() {
		final float xx = -x * x, yy = -y * y, zz = -z * z;
		final float xy = x * y, yz = y * z, xz = x * z;
		return Matrix3.row(yy + zz, xy, xz, xy, zz + xx, yz, xz, yz, xx + yy);
	}

	/** @return this x a (cross product) */
	public Vector cross(final Vector a) {
		return create(y * a.z - z * a.y, z * a.x - x * a.z, x * a.y - y * a.x);
	}

	// /** project vector into reference system (a, b, c) */
	// public Vector3 project(final Vector3 a, final Vector3 b, final
	// Vector3 c) {
	// return create(dot(a), dot(b), dot(c));
	// }
	//
	// /** project vector into reference system (a, b) */
	// public Vector2 project(final Vector3 a, final Vector3 b) {
	// return new Vector2(dot(a.project(a, b, a)), dot(b));
	// }

	/** @return a vector that is normal to this */
	public Vector normal() {
		final float ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
		if (ax < ay) return ax < az ? create(0, z, -y) : create(y, -x, 0);
		return ay < az ? create(z, 0, -x) : create(y, -x, 0);
	}

	// Non-Math
	/** @return true if and only if every component is finite number */
	public final boolean isFinite() {
		return isFinite(x) && isFinite(y) && isFinite(z);
	}

	/** @return lexicographic comparison of vectors */
	public final int compareTo(final Vector a) {
		int c = Float.compare(x, a.x);
		if (c != 0) return c;
		c = Float.compare(y, a.y);
		return c != 0 ? c : Float.compare(z, a.z);
	}

	/** @return conversion to string using formatter */
	@SuppressWarnings("boxing")
	public final String toString(final String format) {
		return String.format(format, x, y, z);
	}

	@Override
	public final String toString() {
		return toString(defaultFormat.get());
	}

	@Override
	public final boolean equals(final Object o) {
		if (o == this) return true;
		try {
			final Vector v = (Vector) o;
			return v.x == x && v.y == y && v.z == z;
		}
		catch (final ClassCastException e) {
			return false;
		}
	}

	@Override
	public final int hashCode() {
		final int b = 769; // prime number away from powers of 2
		return (Float.floatToRawIntBits(x) * b + Float.floatToRawIntBits(y)) * b + Float.floatToRawIntBits(z);
	}
}