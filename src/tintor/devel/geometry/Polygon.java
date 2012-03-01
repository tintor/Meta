package tintor.devel.geometry;

import java.util.List;

import tintor.devel.Visitor2;

/** Immutable convex 3d polygon. */
public class Polygon {
	private final Vector[] vertices;
	public final Plane plane;

	public Polygon(final List<Vector> vertices) {
		if (vertices == null || vertices.size() < 3)
			throw new IllegalArgumentException("Less than 3 vertices. " + vertices);
		// TODO check if planar
		// TODO check if polygon
		// TODO check if convex polygon
		this.vertices = vertices.toArray(new Vector[vertices.size()]);
		this.plane = Plane.create(this.vertices);
	}

	private Polygon(final Vector[] vertices, final Plane plane) {
		this.vertices = vertices;
		this.plane = plane;
	}

	public Object each(final Visitor2<Vector, Vector> visitor) {
		for (int j = vertices.length - 1, i = 0; i < vertices.length; j = i++)
			visitor.visit(vertices[j], vertices[i]);
		return visitor.result();
	}

	public int size() {
		return vertices.length;
	}

	public Vector get(final int index) {
		return vertices[index];
	}

	public Polygon translate(final Vector a) {
		final Vector[] w = new Vector[vertices.length];
		for (int i = 0; i < w.length; i++)
			w[i] = vertices[i].add(a);
		return new Polygon(w, plane.move(plane.unitNormal.dot(a)));
	}
}