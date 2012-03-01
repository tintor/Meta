package tintor.devel;

import tintor.devel.geometry.Polygon;
import tintor.devel.geometry.Quaternion;
import tintor.devel.geometry.Vector;

public abstract class OBB {
	private static final class Branch extends OBB {
		final OBB a, b;
		final Vector pa, pb;
	}

	private static final class Leaf extends OBB {
		final Polygon[] polyhedron;
	}

	public static OBB create(Polygon[] polyhedron) {
		// TODO move to center of mass
		// TODO find OBB
		// TODO if large split along largest axis
	}

	public static boolean intersects(OBB a, OBB b, Quaternion q, Vector v) {
		// TODO sphere test
		// TODO box test
		if (!testBoxes(a, b, a.orientation.mul(q).rdiv(b.orientation), ?)) return false;
		
		if (a instanceof Branch) {
			if (b instanceof Branch) {
				// branch - branch
				if (a.volume > b.volume) {
					return intersects(((Branch) a).a, b, q?, v?) || intersects(((Branch)a).b, b, q, v);
				} else {
					return intersects(a, ((Branch) b).a, q, v) || intersects(a, ((Branch)b).b, q, v);
				}
			} else {
				// branch - leaf
				return intersects(((Branch) a).a, b, q, v) || intersects(((Branch)a).b, b, q, v);
			}
		} else {
			if (b instanceof Branch) {
				// leaf - branch
			} else {
				// leaf - leaf
			}
		}
	}

	private static boolean testBoxes(OBB a, OBB b, Quaternion q, Vector v) {

	}

	private final float volume; // volume of box
	private final float radius;
	private final float xmin, xmax, ymin, ymax, zmin, zmax;
	private final Quaternion orientation; // absolute orientation of box relative to root
}