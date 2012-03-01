package tintor.devel.geometry;

import tintor.devel.Visitor;
import tintor.devel.Visitor2;

public abstract class PolygonVisitor<T> extends Visitor<Polygon> {
	@Override
	public abstract T result();

	public static PolygonVisitor<Float> radius() {
		return new PolygonVisitor<Float>() {
			private float squareRadius = 0;

			@Override
			public void visit(final Polygon poly) {
				for (int i = 0; i < poly.size(); i++)
					squareRadius = Math.max(squareRadius, poly.get(i).square());
			}

			@Override
			public Float result() {
				return (float) Math.sqrt(squareRadius);
			}
		};
	}

	public static PolygonVisitor<Float> signedVolume() {
		return new PolygonVisitor<Float>() {
			private float volume = 0;
			private float s, z;

			@Override
			public void visit(final Polygon poly) {
				s = z = 0;
				poly.each(new Visitor2<Vector, Vector>() {
					@Override
					public void visit(final Vector a, final Vector b) {
						z += b.z;
						s += (a.y + b.y) * (a.x - b.x);
					}
				});
				volume += z * s / poly.size();
			}

			@Override
			public Float result() {
				return volume / 2;
			}
		};
	}

	public static PolygonVisitor<Vector> centerOfMass() {
		return new PolygonVisitor<Vector>() {
			private Vector P = Vector.Zero;
			private float volume = 0;

			@Override
			public void visit(final Polygon poly) {
				final Vector a = poly.get(0);
				for (int i = 2; i < poly.size(); i++) {
					final Vector b = poly.get(i - 1), c = poly.get(i);
					// TODO could we use function from signedVolume integral?
					final float v = a.mixed(b, c);
					P = P.add(v, a.add(b).add(c));
					volume += v;
				}
			}

			@Override
			public Vector result() {
				return P.div(volume * 4);
			}
		};
	}

	/** Origin is assumed to be in the center of mass of Polyhedron. Density is 1. */
	public static PolygonVisitor<Matrix3> inertiaTensor() {
		return new PolygonVisitor<Matrix3>() {
			private Matrix3 covariance = Matrix3.Zero;

			@Override
			public void visit(final Polygon poly) {
				for (int i = 2; i < poly.size(); i++) {
					final Matrix3 A = Matrix3.row(poly.get(0), poly.get(i - 1), poly.get(i));
					covariance = covariance.add(A.transpose().mul(canonical).mul(A), A.det());
				}
			}

			@Override
			public Matrix3 result() {
				return Matrix3.diagonal(covariance.trace()).sub(covariance);
			}

			private static final float a = 1 / 60.f, b = 1 / 120.f;
			private final Matrix3 canonical = Matrix3.row(a, b, b, b, a, b, b, b, a);
		};
	}
}