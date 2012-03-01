package tintor.devel.geometry;

/**
 * Various geometric operations.
 * 
 * @author Marko Tintor (tintor@gmail.com)
 */
public class Geometry {
	/** @return intersection point of three planes */
	public static Vector intersection(final Plane a, final Plane b, final Plane c) {
		final Vector ab = a.unitNormal.cross(b.unitNormal);
		final Vector bc = b.unitNormal.cross(c.unitNormal);
		final Vector ca = c.unitNormal.cross(a.unitNormal);
		return ab.mul(c.offset).add(a.offset, bc).add(b.offset, ca).div(-a.unitNormal.dot(bc));
	}

	/** @return intersection ray of two planes */
	public static Ray intersection(final Plane a, final Plane b) {
		final Vector dir = a.unitNormal.cross(b.unitNormal);
		final Vector origin = solveLinearRowSpec(dir, a.unitNormal, b.unitNormal, -a.offset, -b.offset);
		return origin.isFinite() ? Ray.ray(origin, dir) : null;
	}

	/** @return R such that (A*R, B*R, C*R) = (0, dy, dz) */
	private static Vector solveLinearRowSpec(final Vector a, final Vector b, final Vector c, final float dy,
			float dz) {
		float p = b.z * dz - dy * c.z, q = b.y * dz - dy * c.y, r = dy * c.x - b.x * dz;
		return Vector.div(a.y * p - a.z * q, a.x * p + a.z * r, a.x * q + a.y * r, a.mixed(b, c));
	}

	/** @return R such that (A*R, B*R, C*R) = D */
	public static Vector solveLinearRow(final Vector a, final Vector b, final Vector c, final Vector d) {
		float p = b.z * d.z - d.y * c.z, q = d.y * c.y - b.y * d.z, r = d.y * c.x - b.x * d.z;
		final float x = a.y * p - a.z * q + d.x * (b.y * c.z - b.z * c.y);
		final float y = a.x * p + a.z * r + d.x * (b.x * c.z - b.z * c.x);
		final float z = a.x * q + a.y * r + d.x * (b.x * c.y - b.y * c.x);
		return Vector.div(x, y, z, a.mixed(b, c));
	}

	private Geometry() {
	}
}