package tintor.devel.geometry;

/** @see "vcg.isti.cnr.it/~ponchio/computergraphics/exercises/plucker.pdf" */
public class Plucker {
	public static Plucker create(Line line) {
		return line(line.a, line.b);
	}

	public static Plucker create(Ray ray) {
		return ray(ray.origin, ray.unitDir);
	}

	public static Plucker line(final Vector a, final Vector b) {
		return new Plucker(b.sub(a), b.cross(a));
	}

	public static Plucker ray(final Vector origin, final Vector dir) {
		return new Plucker(dir, dir.cross(origin));
	}

	private final Vector u, v;

	private Plucker(final Vector u, final Vector v) {
		this.u = u;
		this.v = v;
	}

	/**
	 * <0 Clockwise (if you look in direction of one line, other will go CW around it) =0 Intersect or Parallel >0
	 * Counterclockwise
	 */
	public float side(final Plucker p) {
		return u.dot(p.v) + v.dot(p.u);
	}

	public float side(final Vector a, final Vector b) {
		return u.mixed(a, b) + v.dot(a, b);
	}

	public float side(final Line p) {
		return u.mixed(p.a, p.b) + v.dot(p.a, p.b);
	}

	public float side(final Ray p) {
		return u.mixed(p.unitDir, p.origin) + v.dot(p.unitDir);
	}
}