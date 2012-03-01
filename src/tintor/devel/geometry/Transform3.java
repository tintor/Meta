package tintor.devel.geometry;

import java.util.concurrent.atomic.AtomicInteger;

public final class Transform3 {
	private static final AtomicInteger counter = new AtomicInteger();

	/** @return number of objects constructed */
	public static int counter() {
		return counter.get();
	}

	// Constants
	/** Identity transform: I.apply(X) = X */
	public final static Transform3 Identity = new Transform3(Matrix3.Identity, Vector.Zero);

	// Factory Methods
	public static Transform3 translation(final Vector offset) {
		return new Transform3(Matrix3.Identity, offset);
	}

	public static Transform3 rotation(final Vector axis, final float angleRadians) {
		return create(Quaternion.axisAngle(axis, angleRadians), Vector.Zero);
	}

	public static Transform3 create(final Quaternion rotation, final Vector offset) {
		if (offset == null) throw new IllegalArgumentException();
		return new Transform3(rotation.matrix(), offset);
	}

	// Fields
	/** matrix describing rotational part of transform */
	public final Matrix3 rotation;
	/** vector describing translational part of transform */
	public final Vector offset;
	private final Vector invOffset; // = - offset * rotation

	// Constructors
	/** ASSUME m is rotational! */
	private Transform3(final Matrix3 rotation, final Vector offset) {
		assert rotation != null;
		assert offset != null;

		this.rotation = rotation;
		this.offset = offset;

		final float nx = offset.x * rotation.a.x + offset.y * rotation.b.x + offset.z * rotation.c.x;
		final float ny = offset.x * rotation.a.y + offset.y * rotation.b.y + offset.z * rotation.c.y;
		final float nz = offset.x * rotation.a.z + offset.y * rotation.b.z + offset.z * rotation.c.z;
		invOffset = Vector.create(-nx, -ny, -nz);

		counter.incrementAndGet();
	}

	// Direct Transformations
	public Vector applyPoint(final Vector point) {
		return offset.add(rotation, point);
	}

	public Vector applyVector(final Vector vector) {
		return rotation.mul(vector);
	}

	public Plane apply(final Plane plane) {
		final Vector normal = applyVector(plane.unitNormal);
		return Plane.create(normal, plane.offset - offset.dot(normal));
	}

	public Ray apply(final Ray ray) {
		return Ray.ray(applyPoint(ray.origin), applyVector(ray.unitDir));
	}

	public Line apply(final Line line) {
		return Line.create(applyPoint(line.a), applyPoint(line.b));
	}

	// Inverse Transformations
	public Vector iapplyPoint(final Vector point) {
		return invOffset.add(point, rotation);
	}

	public Vector iapplyVector(final Vector vector) {
		return vector.mul(rotation);
	}

	public Plane iapply(final Plane plane) {
		return Plane.create(iapplyVector(plane.unitNormal), plane.offset + offset.dot(plane.unitNormal));
	}

	public Ray iapply(final Ray ray) {
		return Ray.ray(iapplyPoint(ray.origin), iapplyVector(ray.unitDir));
	}

	public Line iapply(final Line line) {
		return Line.create(iapplyPoint(line.a), iapplyPoint(line.b));
	}

	// Misc
	public float[] columnMajorArray() {
		return new float[] { rotation.a.x, rotation.b.x, rotation.c.x, 0, rotation.a.y, rotation.b.y,
				rotation.c.y, 0, rotation.a.z, rotation.b.z, rotation.c.z, 0, offset.x, offset.y,
				offset.z, 1 };
	}

	public Transform3 combine(final Transform3 a) {
		return new Transform3(a.rotation.mul(rotation), mulAdd(a.rotation, offset, a.offset));
	}

	/** @return A * b + c */
	private static Vector mulAdd(final Matrix3 a, final Vector b, final Vector c) {
		return Vector.create(a.a.dot(b) + c.x, a.b.dot(b) + c.y, a.c.dot(b) + c.z);
	}

	public Transform3 icombine(final Transform3 a) {
		return new Transform3(a.rotation.transposedMul(rotation), subMul(offset, a.offset, a.rotation));
	}

	/** @return (a - b) * C */
	private static Vector subMul(final Vector a, final Vector b, final Matrix3 c) {
		final float x = a.x - b.x, y = a.y - b.y, z = a.z - b.z;
		final float nx = x * c.a.x + y * c.b.x + z * c.c.x;
		final float ny = x * c.a.y + y * c.b.y + z * c.c.y;
		final float nz = x * c.a.z + y * c.b.z + z * c.c.z;
		return Vector.create(nx, ny, nz);
	}

	public Transform3 inverse() {
		// for rotational matrices, inverse(M) = transpose(M)
		// inv(M,V) = (invM, -invM*V) = (transposeM, -V*M)
		return new Transform3(rotation.transpose(), invOffset);
	}
}