package tintor.devel.rigidbody.renderer;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import tintor.devel.geometry.Vector;

public interface Camera {
	void zoom(float factor);

	void setProjectionMatrix(GL gl, int width, int height);

	void setModelViewMatrix(GL gl);
}

class OrbitingCamera implements Camera {
	public float distance = 10;
	public float pitch, yaw; // in degrees
	public Vector center = Vector.Zero;

	@Override
	public void zoom(final float factor) {
		distance *= factor;
	}

	@Override
	public void setModelViewMatrix(final GL gl) {
		gl.glLoadIdentity();
		gl.glTranslated(0, 0, -distance);
		gl.glRotated(-pitch, 1, 0, 0);
		gl.glRotated(-yaw, 0, 1, 0);
		gl.glTranslated(-center.x, -center.y, -center.z);
	}

	@Override
	public void setProjectionMatrix(final GL gl, final int width, final int height) {
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		new GLU().gluPerspective(45, (float) width / height, 1, 100);
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}
}