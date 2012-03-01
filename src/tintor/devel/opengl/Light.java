package tintor.devel.opengl;

import javax.media.opengl.GL;

import tintor.devel.geometry.Vector;

public final class Light {
	private final GL gl;
	private final int id;

	public Light(final GL gl, final int id) {
		this.gl = gl;
		this.id = GL.GL_LIGHT0 + id;
	}

	private static float[] array(final Vector a) {
		return new float[] { a.x, a.y, a.z, 1 };
	}

	public void position(final double x, final double y, final double z) {
		gl.glLightfv(id, GL.GL_POSITION, new float[] { (float) x, (float) y, (float) z }, 0);
	}

	public void ambient(final float r, final float g, final float b) {
		gl.glLightfv(id, GL.GL_AMBIENT, new float[] { r, g, b, 1 }, 0);
	}

	public void diffuse(final float r, final float g, final float b) {
		gl.glLightfv(id, GL.GL_DIFFUSE, new float[] { r, g, b, 1 }, 0);
	}

	public void specular(final float r, final float g, final float b) {
		gl.glLightfv(id, GL.GL_SPECULAR, new float[] { r, g, b, 1 }, 0);
	}

	public void position(final Vector a) {
		gl.glLightfv(id, GL.GL_POSITION, array(a), 0);
	}

	public void ambient(final Vector color) {
		gl.glLightfv(id, GL.GL_AMBIENT, array(color), 0);
	}

	public void diffuse(final Vector color) {
		gl.glLightfv(id, GL.GL_DIFFUSE, array(color), 0);
	}

	public void specular(final Vector color) {
		gl.glLightfv(id, GL.GL_SPECULAR, array(color), 0);
	}

	public void enable() {
		gl.glEnable(id);
	}

	public void disable() {
		gl.glDisable(id);
	}
}