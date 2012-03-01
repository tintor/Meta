package tintor.devel.rigidbody.model;

import java.util.ArrayList;
import java.util.List;

import tintor.devel.Visitor;
import tintor.devel.geometry.ConvexHull;
import tintor.devel.geometry.Polygon;
import tintor.devel.geometry.PolygonVisitor;
import tintor.devel.geometry.Vector;

// Immutable class
public class Shape {
	public static Shape box(final float xsize, final float ysize, final float zsize) {
		final Builder builder = new Builder();
		final List<Vector> vertices = new ArrayList<Vector>();
		for (int x = 0; x <= 1; x++)
			for (int y = 0; y <= 1; y++)
				for (int z = 0; z <= 1; z++)
					vertices.add(Vector.create(xsize * x, ysize * y, zsize * z));
		builder.addConvexHull(vertices);
		return builder.shape();
	}

	public static class Builder {
		private final List<Polygon[]> polyhedrons = new ArrayList<Polygon[]>();

		public void addConvexHull(final List<Vector> vertices) {
			final List<Polygon> polyhedron = new ArrayList<Polygon>();
			new ConvexHull(vertices, true).eachPolygon(new Visitor<Polygon>() {
				@Override
				public void visit(final Polygon polygon) {
					polyhedron.add(polygon);
				}
			});
			polyhedrons.add(polyhedron.toArray(new Polygon[polyhedron.size()]));
		}

		public Shape shape() {
			final PolygonVisitor<Vector> visitor = PolygonVisitor.centerOfMass();
			for (final Polygon[] polyhedron : polyhedrons)
				for (final Polygon polygon : polyhedron)
					visitor.visit(polygon);
			final Vector center = visitor.result().neg();

			for (final Polygon[] polyhedron : polyhedrons)
				for (int i = 0; i < polyhedron.length; i++)
					polyhedron[i] = polyhedron[i].translate(center);

			final Polygon[][] p = polyhedrons.toArray(new Polygon[polyhedrons.size()][]);
			polyhedrons.clear();
			return new Shape(0, p);
		}
	}

	public final float radius;
	private final Polygon[][] polyhedrons;

	private Shape(final float radius, final Polygon[][] polyhedrons) {
		this.radius = radius;
		this.polyhedrons = polyhedrons;
	}

	public Object each(final Visitor<Polygon> visitor) {
		for (final Polygon[] polyhedron : polyhedrons)
			for (final Polygon polygon : polyhedron)
				visitor.visit(polygon);
		return visitor.result();
	}
}