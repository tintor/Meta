package tintor.devel.r_tree;

class Rect {
	public final double xmin, xmax, ymin, ymax;

	public Rect(final double xmin, final double xmax, final double ymin, final double ymax) {
		if (xmax < xmin || ymax < ymin) throw new IllegalArgumentException();
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
	}

	@Override
	public String toString() {
		return String.format("[x(%s:%s) y(%s:%s)]", xmin, xmax, ymin, ymax);
	}

	public Rect union(final Rect a) {
		return new Rect(Math.min(xmin, a.xmin), Math.max(xmax, a.xmax), Math.min(ymin, a.ymin), Math.max(ymax,
				a.ymax));
	}

	public double unionArea(final Rect a) {
		return (Math.max(xmax, a.xmax) - Math.min(xmin, a.xmin))
				* (Math.max(ymax, a.ymax) - Math.min(ymin, a.ymin));
	}

	public double unionPerimeter(final Rect a) {
		return (Math.max(xmax, a.xmax) - Math.min(xmin, a.xmin))
				+ (Math.max(ymax, a.ymax) - Math.min(ymin, a.ymin));
	}

	public Rect intersection(final Rect a) {
		return new Rect(Math.max(xmin, a.xmin), Math.min(xmax, a.xmax), Math.max(ymin, a.ymin), Math.min(ymax,
				a.ymax));
	}

	public double intersectionArea(final Rect a) {
		return (Math.min(xmax, a.xmax) - Math.max(xmin, a.xmin))
				* (Math.min(ymax, a.ymax) - Math.max(ymin, a.ymin));
	}

	public double area() {
		return (xmax - xmin) * (ymax - ymin);
	}

	public double perimeter() {
		return (xmax - xmin) + (ymax - ymin);
	}

	public boolean intersects(final Rect a) {
		return xmin <= a.xmax && a.xmin <= xmax && ymin <= a.ymax && a.ymin <= ymax;
	}

	public boolean contains(final Rect a) {
		return xmin <= a.xmin && a.xmax <= xmax && ymin <= a.ymin && a.ymax <= ymax;
	}

	public boolean equals(final Rect a) {
		return xmin == a.xmin && a.xmax == xmax && ymin == a.ymin && a.ymax == ymax;
	}

	public double overlapArea(final Rect a) {
		return intersects(a) ? intersectionArea(a) : 0;
	}

	public double centerDistanceSquared4(final Rect a) {
		return dist(xmin + xmax - a.xmin - a.xmax, ymin + ymax - a.ymin - a.ymax);
	}

	public double distanceSquared(final double x, final double y) {
		if (xmin <= x && x <= xmax && ymin <= y && y <= ymax) return 0;
		// TODO optimize
		// TODO incorrect

		final double a = dist(x - xmin, y - ymin);
		final double b = dist(x - xmin, y - ymax);
		final double c = dist(x - xmax, y - ymin);
		final double d = dist(x - xmax, y - ymax);

		return Math.min(Math.min(a, b), Math.min(c, d));
	}

	private static double dist(final double dx, final double dy) {
		return dx * dx + dy * dy;
	}
}

class MutableRect {
	public double xmin = Double.POSITIVE_INFINITY, xmax = Double.NEGATIVE_INFINITY;
	public double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;

	public void reset() {
		xmin = Double.POSITIVE_INFINITY;
		xmax = Double.NEGATIVE_INFINITY;
		ymin = Double.POSITIVE_INFINITY;
		ymax = Double.NEGATIVE_INFINITY;
	}

	public void union(final Rect rect) {
		if (rect.xmin < xmin) xmin = rect.xmin;
		if (rect.xmax > xmax) xmax = rect.xmax;
		if (rect.ymin < ymin) ymin = rect.ymin;
		if (rect.ymax > ymax) ymax = rect.ymax;
	}

	public double perimeter() {
		return (xmax - xmin) + (ymax - ymin);
	}

	public double area() {
		return (xmax - xmin) * (ymax - ymin);
	}

	public Rect toRect() {
		return new Rect(xmin, xmax, ymin, ymax);
	}
}