package tintor.devel.rigidbody.renderer;

import java.nio.IntBuffer;

import javax.media.opengl.GL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import tintor.devel.Path;
import tintor.devel.Visitor;
import tintor.devel.geometry.Polygon;
import tintor.devel.geometry.Quaternion;
import tintor.devel.geometry.Vector;
import tintor.devel.glsl.Main;
import tintor.devel.opengl.Program;
import tintor.devel.opengl.View;
import tintor.devel.rigidbody.model.Body;
import tintor.devel.rigidbody.model.World;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.j2d.TextRenderer;

/** Panel for rendering World using OpenGL. */
public class WorldView extends View {
	public OrbitingCamera camera = new OrbitingCamera();

	private final TextRenderer renderer = new TextRenderer(new java.awt.Font("Courier New", java.awt.Font.BOLD, 20));
	private final Program program;
	private World world;
	private Body selectedBody;

	public void attachWorld(final World world) {
		this.world = world;
		selectedBody = null;
	}

	public WorldView(final Composite parent) {
		super(parent, true, 16);
		gl.glClearColor(0, 0, 0, 1);
		gl.glClearDepth(1.0);
		gl.glLineWidth(2);
		gl.glEnable(GL.GL_DEPTH_TEST);

		program = new Program(gl);
		program.attachVertexShader(Path.get(Main.class, "proc3d.vert"));
		program.attachFragmentShader(Path.get(Main.class, "brick.frag"));
		program.link();
		program.use();

		gl.glUniform3f(program.uniform("LightPosition"), 5f, 5f, 5f);
		gl.glUniform3f(program.uniform("BrickColor"), 1f, 0.3f, 0.2f);
		gl.glUniform3f(program.uniform("BrickPct"), 0.9f, 0.85f, 0.9f);
		gl.glUniform3f(program.uniform("BrickSize"), 0.3f, 0.15f, 0.3f);
		gl.glUniform3f(program.uniform("MortarColor"), 0.85f, 0.86f, 0.84f);

		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent e) {
				camera.zoom((float) Math.pow(0.97, e.count));
			}
		});

		addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(final Event e) {
				if (e.button == 3) {
					startX = e.x;
					startY = e.y;
					startPitch = camera.pitch;
					startYaw = camera.yaw;
				}
				if (e.button == 1) pick(e.x, e.y, 2);
			}
		});

		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent e) {
				if ((e.stateMask & SWT.BUTTON3) != 0) {
					final int x = e.x - startX;
					final int y = e.y - startY;

					camera.pitch = clamp(startPitch - (float) y / 2, -90, 90);
					camera.yaw = startYaw - (float) x / 2;
				}
			}
		});
	}

	private static float clamp(final float a, final float min, final float max) {
		return a < min ? min : a > max ? max : a;
	}

	private float startYaw, startPitch;
	private int startX, startY;

	private void pick(final int x, final int y, final int offset) {
		assert offset >= 0;

		final int[] viewport = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		assert 0 <= x && x < viewport[2] : x;
		assert 0 <= y && y < viewport[3] : x;

		// Setup selection buffer
		final int capacity = 200;
		final IntBuffer buffer = BufferUtil.newIntBuffer(capacity);
		gl.glSelectBuffer(capacity, buffer);

		// Change render mode
		gl.glRenderMode(GL.GL_SELECT);

		final double[] projection = new double[16];
		gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, projection, 0);

		// Init pick projection matrix
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluPickMatrix(x, viewport[3] - y, 2, 2, viewport, 0);
		gl.glMultMatrixd(projection, 0);
		gl.glMatrixMode(GL.GL_MODELVIEW);

		// Draw the scene
		gl.glInitNames();
		gl.glPushName(0);
		gl.glUseProgram(0);
		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
		camera.setModelViewMatrix(gl);
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
		gl.glEnable(GL.GL_DEPTH_TEST);

		world.eachBody(new Visitor<Body>() {
			int index;

			@Override
			public void visit(final Body body) {
				gl.glLoadName(index++);
				renderBody(body);
			}
		});

		// Restore projection matrix
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_MODELVIEW);

		// Find selected body ID
		final int hits = gl.glRenderMode(GL.GL_RENDER);

		int pickID = -1;
		int bestMinZ = Integer.MAX_VALUE;
		for (int i = 0; i < hits; i++) {
			System.out.println("hit: " + i);
			System.out.println(buffer.get(i * 4));
			System.out.println(buffer.get(i * 4 + 1));
			System.out.println(buffer.get(i * 4 + 2));
			System.out.println(buffer.get(i * 4 + 3));

			if (buffer.get(i * 4 + 1) <= bestMinZ) {
				pickID = buffer.get(i * 4 + 3);
				bestMinZ = buffer.get(i * 4 + 1);
			}
		}

		// Find body by ID
		final int selectedBodyIndex = pickID;
		selectedBody = null;

		world.eachBody(new Visitor<Body>() {
			int index;

			@Override
			public void visit(final Body body) {
				if (index++ == selectedBodyIndex) selectedBody = body;
			}
		});
	}

	private void renderBody(final Body body) {
		// transform into body reference frame
		gl.glPushMatrix();
		glTranslate(body.position());
		glRotate(body.orientation());

		// render body
		body.shape.each(polygonRenderer);

		// restore reference frame
		gl.glPopMatrix();
	}

	@Override
	public void display() {
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		// if (selectedBody != null) camera.center = selectedBody.position();
		camera.setModelViewMatrix(gl);

		program.use();
		drawTorus(1, 1.9f + (float) Math.sin((0.004f * ++rot)), 30, 60);
		world.eachBody(bodyRenderer);
	}

	int rot;

	void drawTorus(final float r, final float R, final int nsides, final int rings) {
		final float ringDelta = 2.0f * (float) Math.PI / rings;
		final float sideDelta = 2.0f * (float) Math.PI / nsides;
		float theta = 0.0f, cosTheta = 1.0f, sinTheta = 0.0f;
		for (int i = rings - 1; i >= 0; i--) {
			final float theta1 = theta + ringDelta;
			final float cosTheta1 = (float) Math.cos(theta1);
			final float sinTheta1 = (float) Math.sin(theta1);
			gl.glBegin(GL.GL_QUAD_STRIP);
			float phi = 0.0f;
			for (int j = nsides; j >= 0; j--) {
				phi += sideDelta;
				final float cosPhi = (float) Math.cos(phi);
				final float sinPhi = (float) Math.sin(phi);
				final float dist = R + r * cosPhi;
				gl.glNormal3f(cosTheta1 * cosPhi, -sinTheta1 * cosPhi, sinPhi);
				gl.glVertex3f(cosTheta1 * dist, -sinTheta1 * dist, r * sinPhi);
				gl.glNormal3f(cosTheta * cosPhi, -sinTheta * cosPhi, sinPhi);
				gl.glVertex3f(cosTheta * dist, -sinTheta * dist, r * sinPhi);
			}
			gl.glEnd();
			theta = theta1;
			cosTheta = cosTheta1;
			sinTheta = sinTheta1;
		}
	}

	private final Visitor<Body> bodyRenderer = new Visitor<Body>() {
		@Override
		public void visit(final Body body) {
			if (selectedBody == body) gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
			renderBody(body);
			if (selectedBody == body) gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
		}
	};

	private final Visitor<Polygon> polygonRenderer = new Visitor<Polygon>() {
		@Override
		public void visit(final Polygon polygon) {
			gl.glBegin(GL.GL_POLYGON);
			glNormal(polygon.plane.unitNormal);
			for (int i = 0; i < polygon.size(); i++)
				glVertex(polygon.get(i));
			gl.glEnd();
		}
	};

	@Override
	public void reshape(final int x, final int y, final int width, final int height) {
		gl.glViewport(x, y, width, height);
		camera.setProjectionMatrix(gl, width, height);
	}

	private void glColor(final Vector color) {
		gl.glColor3f(color.x, color.y, color.z);
	}

	private void glVertex(final Vector vertex) {
		gl.glVertex3f(vertex.x, vertex.y, vertex.z);
	}

	private void glNormal(final Vector normal) {
		gl.glNormal3f(normal.x, normal.y, normal.z);
	}

	private void glTranslate(final Vector v) {
		gl.glTranslatef(v.x, v.y, v.z);
	}

	private void glRotate(final Quaternion q) {
		final Vector axis = q.axis();
		gl.glRotatef(q.angle() * (float) (180 / Math.PI), axis.x, axis.y, axis.z);
	}
}