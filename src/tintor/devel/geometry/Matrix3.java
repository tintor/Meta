package tintor.devel.geometry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immutable single-precision floating-point 3x3 matrix.
 * see Matrix and Quaternion FAQ, "http://mccammon.ucsd.edu/~adcock/matrixfaq.html"
 * 
 * @author Marko Tintor (tintor@gmail.com)
 */
public final class Matrix3 {
	private static final AtomicInteger counter = new AtomicInteger();

	/** @return number of objects constructed */
	public static int counter() {
		return counter.get();
	}

	// Constants
	/** Zero matrix: Z * A = A * Z = Z */
	public final static Matrix3 Zero = row(Vector.Zero, Vector.Zero, Vector.Zero);
	/** Identity matrix: I * A = A * I = A */
	public final static Matrix3 Identity = row(Vector.X, Vector.Y, Vector.Z);

	// Static fields
	/** Default formatter for toString() method */
	public static final ThreadLocal<String> defaultFormat = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "Matrix[%s %s %s / %s %s %s / %s %s %s]";
		}
	};

	// Factory Methods
	/** @return row matrix */
	public static Matrix3 row(final Vector a, final Vector b, final Vector c) {
		return new Matrix3(a, b, c);
	}

	/** @return column matrix */
	public static Matrix3 column(final Vector a, final Vector b, final Vector c) {
		return new Matrix3(Vector.create(a.x, b.x, c.x), Vector.create(a.y, b.y, c.y), Vector.create(a.z, b.z,
				c.z));
	}

	/** @return row matrix */
	public static Matrix3 row(final float ax, final float ay, final float az, final float bx, final float by,
			final float bz, final float cx, final float cy, final float cz) {
		return new Matrix3(Vector.create(ax, ay, az), Vector.create(bx, by, bz), Vector.create(cx, cy, cz));
	}

	public static Matrix3 rotation(final Vector unitAxis, final float angleRadians) {
		final float c = (float) Math.cos(angleRadians), s = (float) Math.sin(angleRadians);
		return unitAxis.mul(1 - c).mul(unitAxis).add(Identity, c).add(unitAxis.mul(s).tilda());
	}

	/** @return diagonal matrix */
	public static Matrix3 diagonal(final float d) {
		return diagonal(d, d, d);
	}

	/** @return diagonal matrix */
	public static Matrix3 diagonal(final float ax, final float by, final float cz) {
		return new Matrix3(Vector.create(ax, 0, 0), Vector.create(0, by, 0), Vector.create(0, 0, cz));
	}

	// Fields
	/** first row */
	public final Vector a;
	/** second row */
	public final Vector b;
	/** third row */
	public final Vector c;

	// Constructors
	private Matrix3(final Vector a, final Vector b, final Vector c) {
		if (a == null || b == null || c == null) throw new NullPointerException();
		this.a = a;
		this.b = b;
		this.c = c;
		counter.incrementAndGet();
	}

	// Addition
	/** @return this + m */
	public Matrix3 add(final Matrix3 m) {
		return new Matrix3(a.add(m.a), b.add(m.b), c.add(m.c));
	}

	/** @return this - m */
	public Matrix3 sub(final Matrix3 m) {
		return new Matrix3(a.sub(m.a), b.sub(m.b), c.sub(m.c));
	}

	/** @return this + m*s */
	public Matrix3 add(final Matrix3 m, final float s) {
		return new Matrix3(a.add(s, m.a), b.add(s, m.b), c.add(s, m.c));
	}

	// Multiplication
	/** @return this * s */
	public Matrix3 mul(final float s) {
		return new Matrix3(a.mul(s), b.mul(s), c.mul(s));
	}

	/** @return this * v */
	public Vector mul(final Vector v) {
		return Vector.create(a.dot(v), b.dot(v), c.dot(v));
	}

	/** @return (this * v) . p */
	public float mulDot(final Vector v, final Vector p) {
		return a.dot(v) * p.x + b.dot(v) * p.y + c.dot(v) * p.z;
	}

	/** @return this * m (27 multiplications) */
	public Matrix3 mul(final Matrix3 m) {
		return new Matrix3(a.mul(m), b.mul(m), c.mul(m));
	}

	/** @return this * m */
	public Matrix3 mulDiagonal(final Matrix3 diagonalMatrix) {
		return new Matrix3(mulDiagonal(a, diagonalMatrix), mulDiagonal(b, diagonalMatrix), mulDiagonal(c,
				diagonalMatrix));
	}

	private static Vector mulDiagonal(final Vector a, final Matrix3 m) {
		return Vector.create(a.x * m.a.x, a.y * m.b.y, a.z * m.c.z);
	}

	/** @return this * m.transpose */
	public Matrix3 mulTransposed(final Matrix3 m) {
		return new Matrix3(a.mulTransposed(m), b.mulTransposed(m), c.mulTransposed(m));
	}

	/** @return this.transpose * m */
	public Matrix3 transposedMul(final Matrix3 m) {
		return new Matrix3(Vector.mul(a.x, b.x, c.x, m), Vector.mul(a.y, b.y, c.y, m), Vector.mul(a.z, b.z,
				c.z, m));
	}

	/** @return this * this */
	public Matrix3 square() {
		return mul(this);
	}

	/** @return inverted matrix (36 multiplications, 1 divisions) */
	public Matrix3 inv() {
		final float d = 1 / det();

		final Vector p = Vector.mul(b.y * c.z - b.z * c.y, c.y * a.z - a.y * c.z, a.y * b.z - b.y * a.z, d);
		if (!p.isFinite()) return Matrix3.Zero;

		final Vector q = Vector.mul(b.z * c.x - b.x * c.z, a.x * c.z - c.x * a.z, b.x * a.z - a.x * b.z, d);
		if (!q.isFinite()) return Matrix3.Zero;

		final Vector r = Vector.mul(b.x * c.y - c.x * b.y, c.x * a.y - a.x * c.y, a.x * b.y - a.y * b.x, d);
		if (!r.isFinite()) return Matrix3.Zero;

		return new Matrix3(p, q, r);
	}

	/** @return transposed matrix */
	public Matrix3 transpose() {
		return new Matrix3(colX(), colY(), colZ());
	}

	/** @return determinant of matrix */
	public float det() {
		return a.mixed(b, c);
	}

	/** @return a.x + b.y + c.z */
	public float trace() {
		return a.x + b.y + c.z;
	}

	/** @return conversion to quaternion */
	public Quaternion quaternion() {
		if (a.x + b.y + c.z >= 0) {
			final double s = Math.sqrt(1 + a.x + b.y + c.z) * 2;
			return Quaternion.create(s / 4, (c.y - b.z) / s, (a.z - c.x) / s, (b.x - a.y) / s);
		}
		if (a.x >= b.y && a.x >= c.z) {
			final double s = Math.sqrt(1 + a.x - b.y - c.z) * 2;
			return Quaternion.create((c.y - b.z) / s, s / 4, (a.y + b.x) / s, (c.x + a.z) / s);
		}
		if (b.y >= c.z) {
			final double s = Math.sqrt(1 - a.x + b.y - c.z) * 2;
			return Quaternion.create((a.z - c.x) / s, (a.y + b.x) / s, s / 4, (b.z + c.y) / s);
		}

		final double s = Math.sqrt(1 - a.x - b.y + c.z) * 2;
		return Quaternion.create((b.x - a.y) / s, (c.x + a.z) / s, (b.z + c.y) / s, s / 4);
	}

	/** @return first column */
	public Vector colX() {
		return Vector.create(a.x, b.x, c.x);
	}

	/** @return second column */
	public Vector colY() {
		return Vector.create(a.y, b.y, c.y);
	}

	/** @return third column */
	public Vector colZ() {
		return Vector.create(a.z, b.z, c.z);
	}

	/** @return colX() dot v */
	public float dotX(final Vector v) {
		return a.x * v.x + b.x * v.y + c.x * v.z;
	}

	/** @return colY() dot v */
	public float dotY(final Vector v) {
		return a.y * v.x + b.y * v.y + c.y * v.z;
	}

	/** @return colZ() dot v */
	public float dotZ(final Vector v) {
		return a.z * v.x + b.z * v.y + c.z * v.z;
	}

	/** @return conversion to string using formatter */
	@SuppressWarnings("boxing")
	public String toString(final String format) {
		return String.format(format, a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z);
	}

	/** @return conversion to string using formatter */
	@Override
	public String toString() {
		return toString(defaultFormat.get());
	}
}