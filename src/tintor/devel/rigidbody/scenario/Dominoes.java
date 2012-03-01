package tintor.devel.rigidbody.scenario;

import tintor.devel.geometry.Quaternion;
import tintor.devel.geometry.Vector;
import tintor.devel.rigidbody.model.Body;
import tintor.devel.rigidbody.model.Shape;
import tintor.devel.rigidbody.model.World;

public class Dominoes extends World {
	public Dominoes() {
		float x = 20, z = -8;
		final float a = 0;
		final int n = 10;

		for (int i = 0; i < n; i++) {
			dominoe(x, z, a);
			x -= 3.5;
		}
		for (int i = 0; i <= 8; i++)
			dominoe((float) (x - 8 * Math.sin(i * Math.PI / 8)),
					(float) (z + 8 * Math.cos(i * Math.PI / 8) + 8), (float) (-i * Math.PI / 8));
		z += 16;
		for (int i = 0; i < n; i++) {
			x += 3.5;
			dominoe(x, z, a);
		}

		final Body impulse = new Body(Vector.create(24, 1.5, z), Quaternion.Identity, Shape.box(0.5f, 0.5f,
				0.5f), 8);
		// impulse.linVel = Vector.create(-5, 0, 0);
		impulse.elasticity = 0.5f;
		add(impulse);

		// surface(-3, 2);
	}

	final Shape box = Shape.box(1, 5, 2.5f);

	void dominoe(final float x, final float z, final float a) {
		final Body b = new Body(Vector.create(x, -0.5, z), Quaternion.axisY(a), box, 1);
		b.elasticity = 0;
		b.sfriction = 0.5f;
		b.dfriction = 0.5f;
		add(b);
	}
}