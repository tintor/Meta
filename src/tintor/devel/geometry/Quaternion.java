package tintor.devel.geometry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immutable single-precision floating-point quaternion.
 * see Matrix and Quaternion FAQ, "http://mccammon.ucsd.edu/~adcock/matrixfaq.html"
 * 
 * @author Marko Tintor (tintor@gmail.com)
 */
public final class Quaternion {
	private static final AtomicInteger counter = new AtomicInteger();

	/** @return number of objects constructed */
	public static int counter() {
		return counter.get();
	}

	// Constants
	/** Zero quaternion: Z * A = A * Z = Z */
	public final static Quaternion Zero = create(0, 0, 0, 0);
	/** Identity quaternion: I * A = A * I = A */
	public final static Quaternion Identity = create(1, 0, 0, 0);

	// Static fields
	/** Default formatter for toString() method */
	public static final ThreadLocal<String> defaultFormat = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "Quaternion(%s %s %s %s)";
		}
	};

	// Fields
	public final float w, x, y, z;

	// Constructors
	private Quaternion(final float w, final float x, final float y, final float z) {
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;
		counter.incrementAndGet();
	}

	// Factory Methods
	/** @return Quaternion(w, Vector3(x, y, z)) */
	public static Quaternion create(final float w, final float x, final float y, final float z) {
		return new Quaternion(w, x, y, z);
	}

	/** @return Quaternion(w, Vector3(x, y, z)) */
	public static Quaternion create(final double w, final double x, final double y, final double z) {
		return create((float) w, (float) x, (float) y, (float) z);
	}

	/**
	 * Creates quaternion that represents rotation around axis by angle (in radians).<br>
	 * Right-hand rule is used for rotation direction.<br>
	 * Returns Identity quaternion if angle = 0 or axis = 0.
	 */
	public static Quaternion axisAngle(final Vector axis, final float angleRadians) {
		final Vector v = axis.mul((float) (Math.sin(angleRadians / 2) / axis.length()));
		return Quaternion.create((float) Math.cos(angleRadians / 2), v.x, v.y, v.z);
	}

	/** Creates quaternion that rotates vector a to vector b. */
	public static Quaternion create(final Vector a, final Vector b) {
		Vector axis = a.cross(b);
		if (axis.square() < 1e-10) axis = a.normal(); // FIXME
		return axisAngle(axis, (float) Math.acos(a.dot(b) / Math.sqrt(a.square() * b.square())));
	}

	/** @return create(Vector3.X, angle) */
	public static Quaternion axisX(final float angle) {
		return Quaternion.create((float) Math.cos(angle / 2), (float) Math.sin(angle / 2), 0, 0);
	}

	/** @return create(Vector3.Y, angle) */
	public static Quaternion axisY(final float angle) {
		return Quaternion.create((float) Math.cos(angle / 2), 0, (float) Math.sin(angle / 2), 0);
	}

	/** @return create(Vector3.Z, angle) */
	public static Quaternion axisZ(final float angle) {
		return Quaternion.create((float) Math.cos(angle / 2), 0, 0, (float) Math.sin(angle / 2));
	}

	/** @return linear interpolation */
	public static Quaternion lerp(final Quaternion p, final Quaternion q, final float t) {
		return combine(p, 1 - t, q, t);
	}

	/** @return spherical linear interpolation of unit quaternions */
	public static Quaternion slerp(final Quaternion p, final Quaternion q, final float t) {
		// a + quat(axis(b + neg(a))*t)
		final float d = p.dot(q);
		if (d >= 0) {
			final float a = (float) Math.acos(d), k = (float) (1 / Math.sin(a));
			return combine(p, (float) (Math.sin(a - t * a) * k), q, (float) (Math.sin(t * a) * k));
		}
		final float a = (float) Math.acos(-d), k = (float) (1 / Math.sin(a));
		return combine(p, (float) (Math.sin(a - t * a) * k), q, (float) (Math.sin(t * a) * -k));
	}

	private static Quaternion combine(final Quaternion p, final float a, final Quaternion q, final float b) {
		return Quaternion.create(p.w * a + q.w * b, p.x * a + q.x * b, p.y * a + q.y * b, p.z * a + q.z * b);
	}

	// Addition
	/** @return this + q */
	public Quaternion add(final Quaternion q) {
		return Quaternion.create(w + q.w, x + q.x, y + q.y, z + q.z);
	}

	/** @return this + q */
	public Quaternion sub(final Quaternion q) {
		return Quaternion.create(w - q.w, x - q.x, y - q.x, z - q.z);
	}

	/** @return this + q*a */
	public Quaternion add(final Quaternion q, final float a) {
		return Quaternion.create(w + q.w * a, x + q.x * a, y + q.y * a, z + q.z * a);
	}

	/** @return this - q*a */
	public Quaternion sub(final Quaternion q, final float a) {
		return Quaternion.create(w - q.w * a, x - q.x * a, y - q.y * a, z - q.z * a);
	}

	// Multiplication
	/** @return this * a */
	public Quaternion mul(final float a) {
		return Quaternion.create(w * a, x * a, y * a, z * a);
	}

	/** @return this / a */
	public Quaternion div(final float a) {
		return mul(1 / a);
	}

	/** @return this * q (dot product) */
	public float dot(final Quaternion q) {
		return w * q.w + x * q.x + y * q.y + z * q.z;
	}

	/** @return this * q (combination of two quaternions) */
	public Quaternion mul(final Quaternion q) {
		if (q == Identity) return this;
		if (this == Identity) return q;

		final float iw = w * q.w - x * q.x - y * q.y - z * q.z;
		final float ix = w * q.x + x * q.w + y * q.z - z * q.y;
		final float iy = w * q.y - x * q.z + y * q.w + z * q.x;
		final float iz = w * q.z + x * q.y - y * q.x + z * q.w;
		return Quaternion.create(iw, ix, iy, iz);
	}

	/** @return inv(this) * q */
	public Quaternion ldiv(final Quaternion q) {
		if (this == Identity) return q;
		if (q == Identity) return Quaternion.create(w, -x, -y, -z);

		final float iw = w * q.w + x * q.x + y * q.y + z * q.z;
		final float ix = w * q.x - x * q.w - y * q.z + z * q.y;
		final float iy = w * q.y + x * q.z - y * q.w - z * q.x;
		final float iz = w * q.z - x * q.y + y * q.x - z * q.w;
		return Quaternion.create(iw, ix, iy, iz);
	}

	/** @return this * inv(q) */
	public Quaternion rdiv(final Quaternion q) {
		if (q == Identity) return this;
		if (this == Identity) return Quaternion.create(q.w, -q.x, -q.y, -q.z);

		final float iw = w * q.w + x * q.x + y * q.y + z * q.z;
		final float ix = -w * q.x + x * q.w - y * q.z + z * q.y;
		final float iy = -w * q.y + x * q.z + y * q.w - z * q.x;
		final float iz = -w * q.z - x * q.y + y * q.x + z * q.w;
		return Quaternion.create(iw, ix, iy, iz);
	}

	/** @return q such that this * q = q * this = Identity (for unit quaternions only!) */
	public Quaternion inv() {
		return Quaternion.create(w, -x, -y, -z);
	}

	/** @return this / |this| */
	public Quaternion unit() {
		return div((float) length());
	}

	/** @return |this| */
	public double length() {
		return w * w + x * x + y * y + z * z;
	}

	/** @return unit quaternion */
	public Quaternion unitz() {
		return unitz(w, x, y, z);
	}

	/** @return unit quaternion */
	public static Quaternion unitz(final float w, final float x, final float y, final float z) {
		final double q = 1 / Math.sqrt(w * w + x * x + y * y + z * z);
		if (Double.isInfinite(q) || Double.isNaN(q)) return Quaternion.Zero;

		final float wq = (float) (w * q);
		if (!isFinite(wq)) return Quaternion.Zero;
		final float xq = (float) (x * q);
		if (!isFinite(xq)) return Quaternion.Zero;
		final float yq = (float) (y * q);
		if (!isFinite(yq)) return Quaternion.Zero;
		final float zq = (float) (z * q);
		if (!isFinite(zq)) return Quaternion.Zero;

		return Quaternion.create(wq, xq, yq, zq);
	}

	private static boolean isFinite(final float a) {
		return !Float.isInfinite(a) && !Float.isNaN(a);
	}

	// Conversion
	/** @return axis from (axis, angle) */
	public Vector axis() {
		return Vector.create(x, y, z);
	}

	/** @return angle from (axis, angle) */
	public float angle() {
		return (float) Math.acos(w) * 2;
	}

	/** @return rotation matrix (assumes unit quaternion) */
	public Matrix3 matrix() {
		if (this == Identity) return Matrix3.Identity;
		final float x2 = x * x, y2 = y * y, z2 = z * z;
		final float xy = x * y, xz = x * z, yz = y * z;
		final float wx = w * x, wy = w * y, wz = w * z;

		final Vector a = Vector.create(1 - 2 * (y2 + z2), 2 * (xy - wz), 2 * (xz + wy));
		final Vector b = Vector.create(2 * (xy + wz), 1 - 2 * (x2 + z2), 2 * (yz - wx));
		final Vector c = Vector.create(2 * (xz - wy), 2 * (yz + wx), 1 - 2 * (x2 + y2));
		return Matrix3.row(a, b, c);
	}

	/** @return first row of rotation matrix (assumes unit quaternion) */
	public Vector dirX() {
		return Vector.create(1 - 2 * (y * y + z * z), 2 * (x * y - w * z), 2 * (x * z + w * y));
	}

	/** @return second row of rotation matrix (assumes unit quaternion) */
	public Vector dirY() {
		return Vector.create(2 * (x * y + w * z), 1 - 2 * (x * x + z * z), 2 * (y * z - w * x));
	}

	/** @return third row of rotation matrix (assumes unit quaternion) */
	public Vector dirZ() {
		return Vector.create(2 * (x * z - w * y), 2 * (y * z + w * x), 1 - 2 * (x * x + y * y));
	}

	/** @return first column of rotation matrix (assumes unit quaternion) */
	public Vector idirX() {
		return Vector.create(1 - 2 * (y * y + z * z), 2 * (x * y + w * z), 2 * (x * z - w * y));
	}

	/** @return second column of rotation matrix (assumes unit quaternion) */
	public Vector idirY() {
		return Vector.create(2 * (x * y - w * z), 1 - 2 * (x * x + z * z), 2 * (y * z + w * x));
	}

	/** @return third column of rotation matrix (assumes unit quaternion) */
	public Vector idirZ() {
		return Vector.create(2 * (x * z + w * y), 2 * (y * z - w * x), 1 - 2 * (x * x + y * y));
	}

	/** @return true if and only if every component is finite number */
	public boolean isFinite() {
		return isFinite(w) && isFinite(x) && isFinite(y) && isFinite(z);
	}

	/** @return rotate vector by quaternion, returns this * Quaternion4(0, a) * inv(this) */
	public Vector rotate(final Vector a) {
		// BUGY! return a.mul(1 - w * w).add(v, 2 * v.dot(a)).add(v.cross(a), 2 * w);
		if (this == Identity) return a;

		// 24 multiplications, 17 additions
		final float iw = x * a.x + y * a.y + z * a.z;
		final float ix = w * a.x + y * a.z - z * a.y;
		final float iy = w * a.y - x * a.z + z * a.x;
		final float iz = w * a.z + x * a.y - y * a.x;

		final float vx = iw * x + ix * w - iy * z + iz * y;
		final float vy = iw * y + ix * z + iy * w - iz * x;
		final float vz = iw * z - ix * y + iy * x + iz * w;
		return Vector.create(vx, vy, vz);
	}

	/** @return vector rotated by inverse quaternion (inv(this) * Quaternion4(0, a) * this) */
	public Vector irotate(final Vector a) {
		// BUGY! return a.mul(1 - w * w).add(v, 2 * v.dot(a)).add(a.cross(v), 2 * w);
		if (this == Identity) return a;

		// 24 multiplications, 17 additions
		final float iw = x * a.x + y * a.y + z * a.z;
		final float ix = w * a.x - y * a.z + z * a.y;
		final float iy = w * a.y + x * a.z - z * a.x;
		final float iz = w * a.z - x * a.y + y * a.x;

		final float vx = iw * x + ix * w + iy * z - iz * y;
		final float vy = iw * y - ix * z + iy * w + iz * x;
		final float vz = iw * z + ix * y - iy * x + iz * w;
		return Vector.create(vx, vy, vz);
	}

	/** @return conversion to string using formatter */
	@SuppressWarnings("boxing")
	public String toString(final String format) {
		return String.format(format, w, x, y, z);
	}

	@Override
	public String toString() {
		return toString(defaultFormat.get());
	}
}