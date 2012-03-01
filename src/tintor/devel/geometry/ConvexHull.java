package tintor.devel.geometry;

import java.util.ArrayList;
import java.util.List;

import tintor.devel.Visitor;

public final class ConvexHull {
	// Constants
	private static final float ComplanarTolerance = 1e-5f;

	// Fields (not private because of synthetic accessor warning)
	Vertex ivertices;
	Face ifaces;

	// Classes
	static class Edge {
		final Vertex vertex;
		Face face;
		Edge next; // next edge on this face

		Edge(final Vertex v, final Face f, final Edge n) {
			vertex = v;
			face = f;
			next = n;
		}
	}

	private static class Vertex {
		Vector point;
		Edge edges;
		Vertex prev, next; // linked list of all vertices

		Vertex(final ConvexHull hull, final Vector point) {
			this.point = point;
			// add to linked list
			next = hull.ivertices;
			if (next != null) next.prev = this;
			hull.ivertices = this;
		}

		Face getFace(final Vertex vertex) {
			for (Edge e = edges; e != null; e = e.next)
				if (e.vertex == vertex) return e.face;
			return null;
		}

		Vertex getVertex(final Face face) {
			for (Edge e = edges; e != null; e = e.next)
				if (e.face == face) return e.vertex;
			return null;
		}

		Edge get(final Face face) {
			for (Edge e = edges; e != null; e = e.next)
				if (e.face == face) return e;
			return null;
		}

		// removes half edge
		void remove(final ConvexHull hull, final Vertex v) {
			if (edges.vertex == v) {
				edges = edges.next;
				if (edges == null) {
					// remove from linked list
					if (next != null) next.prev = prev;
					if (prev != null) prev.next = next;
					if (hull.ivertices == this) hull.ivertices = next;
				}
				return;
			}
			for (Edge e = edges; e.next != null; e = e.next)
				if (e.next.vertex == v) {
					e.next = e.next.next;
					break;
				}
		}
	}

	private static class Face {
		Vertex first;
		Face next, prev; // linked list of all faces
		Plane plane;

		Face(final ConvexHull hull, final boolean mergeComplanarFaces, final Vertex... list) {
			plane = Plane.create(list[0].point, list[1].point, list[2].point);

			// attach faces to edges
			q: for (int j = list.length - 1, i = 0; i < list.length; j = i++) {
				final Vertex a = list[j], b = list[i];
				for (Edge e = a.edges; e != null; e = e.next)
					if (e.vertex == b) {
						e.face = this;
						continue q;
					}
				a.edges = new Edge(b, this, a.edges);
			}
			first = list[0];
			// add to linked list
			next = hull.ifaces;
			prev = null;
			if (next != null) next.prev = this;
			hull.ifaces = this;

			// merge complanar faces
			if (mergeComplanarFaces)
				for (int j = list.length - 1, i = 0; i < list.length; j = i++) {
					final Vertex a = list[j], b = list[i];
					final Face f = b.getFace(a);
					if (f == null
							|| f.plane.unitNormal.dot(plane.unitNormal) < 1 - ComplanarTolerance)
						continue;

					Vertex z = b;
					while (true) {
						final Edge e = z.get(f);
						e.face = this;
						z = e.vertex;
						if (z == b) break;
					}

					a.remove(hull, b);
					b.remove(hull, a);
					// remove face from linked list
					if (f.next != null) f.next.prev = f.prev;
					if (f.prev != null) f.prev.next = f.next;
					if (hull.ifaces == f) hull.ifaces = f.next;
				}
		}

		// ! also removes free edges and free vertices left after face removal
		void remove(final ConvexHull hull) {
			Vertex a = first;
			do {
				Vertex b = null;
				assert a.edges != null;
				for (Edge e = a.edges; e != null; e = e.next)
					if (e.face == this) {
						e.face = null;
						b = e.vertex;
						break;
					}

				assert b != null;
				if (b.getFace(a) == null) {
					a.remove(hull, b);
					b.remove(hull, a);
				}
				a = b;
			}
			while (a != first);
			first = null;
			// remove from linked list
			if (next != null) next.prev = prev;
			if (prev != null) prev.next = next;
			if (hull.ifaces == this) hull.ifaces = next;
		}
	}

	public ConvexHull(final List<Vector> w, final boolean mergeComplanarFaces) {
		if (w == null || w.size() < 4) throw new IllegalArgumentException();
		try {
			initialTetrahedron(w, mergeComplanarFaces);
			for (int i = 1; i < w.size(); i++)
				addVertex(w.get(i), mergeComplanarFaces);
		}
		catch (final NullPointerException e) {
			for (final Vector v : w)
				System.out.println(v);
			throw e;
		}
	}

	private void initialTetrahedron(final List<Vector> w, final boolean mergeComplanarFaces) {
		final Vertex a = new Vertex(this, w.get(0));
		final Vertex b = new Vertex(this, maxPointDistance(w));
		final Vertex c = new Vertex(this, maxRayDistance(b.point, w));

		// maxPlaneDistance from plane(a, b, c)
		final Plane plane = Plane.create(w.get(0), b.point, c.point);
		int max = 1;
		float maxDist = plane.distance(w.get(1));
		for (int i = 2; i < w.size(); i++) {
			final float dist = plane.distance(w.get(i));
			if (Math.abs(dist) > Math.abs(maxDist)) {
				maxDist = dist;
				max = i;
			}
		}
		final Vertex d = new Vertex(this, w.get(max));

		if (maxDist < 0) {
			new Face(this, mergeComplanarFaces, a, b, c);
			new Face(this, mergeComplanarFaces, b, a, d);
			new Face(this, mergeComplanarFaces, c, b, d);
			new Face(this, mergeComplanarFaces, a, c, d);
		}
		else {
			new Face(this, mergeComplanarFaces, c, b, a);
			new Face(this, mergeComplanarFaces, a, b, d);
			new Face(this, mergeComplanarFaces, b, c, d);
			new Face(this, mergeComplanarFaces, c, a, d);
		}
	}

	public void eachVertex(final Visitor<Vector> visitor) {
		for (Vertex v = ivertices; v != null; v = v.next)
			visitor.visit(v.point);
	}

	public void eachPolygon(final Visitor<Polygon> visitor) {
		final List<Vector> list = new ArrayList<Vector>();
		eachPolygonVertex(new Visitor<Vector>() {
			@Override
			public void begin() {
				list.clear();
			}

			@Override
			public void visit(final Vector vertex) {
				list.add(vertex);
			}

			@Override
			public void end() {
				visitor.visit(new Polygon(list));
			}
		});
	}

	public void eachPolygonVertex(final Visitor<Vector> visitor) {
		for (Face face = ifaces; face != null; face = face.next) {
			visitor.begin();
			Vertex vertex = face.first;
			do {
				visitor.visit(vertex.point);
				vertex = vertex.getVertex(face);
			}
			while (vertex != face.first);
			visitor.end();
		}
	}

	public final static class Center {
		private float x = 0, y = 0, z = 0, v = 0;
		private Vector a, b;

		public void begin(final Vector a, final Vector b) {
			this.a = a;
			this.b = b;
		}

		public void add(final Vector c) {
			final float m = a.mixed(b, c);
			x += m * (a.x + b.x + c.x);
			y += m * (a.y + b.y + c.y);
			z += m * (a.z + b.z + c.z);
			v += m;
			b = c;
		}

		public Vector center() {
			return Vector.div(x, y, z, v * 4);
		}
	}

	/** @return center of mass of convex hull */
	public Vector centerOfMass() {
		final Center center = new Center();
		for (Face face = ifaces; face != null; face = face.next) {
			Vertex v = face.first;
			final Vector a = v.point;
			v = v.getVertex(face);
			final Vector b = v.point;

			center.begin(a, b);
			v = v.getVertex(face);

			do {
				center.add(v.point);
				v = v.getVertex(face);
			}
			while (v != face.first);
		}
		return center.center();
	}

	public void addVertex(final Vector w, final boolean mergeComplanarFaces) {
		if (inside(w)) return;
		final Vertex c = new Vertex(this, w);
		for (Face face = ifaces; face != null; face = face.next)
			if (face.plane.distance(w) > -ComplanarTolerance) face.remove(this);
		for (Vertex a = ivertices; a != null; a = a.next)
			if (a != c)
				for (Edge e = a.edges; e != null; e = e.next)
					if (e.vertex != c && e.face == null)
						new Face(this, mergeComplanarFaces, a, e.vertex, c);
	}

	private boolean inside(final Vector w) {
		for (Face face = ifaces; face != null; face = face.next)
			if (face.plane.distance(w) > ComplanarTolerance) return false;
		return true;
	}

	// from w[0]
	private static Vector maxPointDistance(final List<Vector> w) {
		int max = 1;
		float maxDist = w.get(1).distanceSquared(w.get(0));
		for (int i = 2; i < w.size(); i++) {
			final float dist = w.get(i).distanceSquared(w.get(0));
			if (dist > maxDist) {
				maxDist = dist;
				max = i;
			}
		}
		return w.get(max);
	}

	// from ray(w[0], b-w[0])
	private static Vector maxRayDistance(final Vector b, final List<Vector> w) {
		final Ray ray = Ray.ray(w.get(0), b.sub(w.get(0)));
		int max = 1;
		float maxDist = ray.distanceSquared(w.get(1));
		for (int i = 2; i < w.size(); i++) {
			final float dist = ray.distanceSquared(w.get(i));
			if (dist > maxDist) {
				maxDist = dist;
				max = i;
			}
		}
		return w.get(max);
	}
}