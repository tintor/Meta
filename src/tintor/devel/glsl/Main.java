package tintor.devel.glsl;

import javax.media.opengl.GL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import tintor.devel.Path;
import tintor.devel.opengl.Light;
import tintor.devel.opengl.Program;
import tintor.devel.opengl.View;

public class Main extends View {
	static String vertex_source = "hello";

	private final Program program;

	Main() {
		super(shell, true, 16);

		System.out.println("Version: " + gl.glGetString(GL.GL_VERSION));
		System.out.println("Vendor: " + gl.glGetString(GL.GL_VENDOR));
		System.out.println("GLSL Version: " + gl.glGetString(GL.GL_SHADING_LANGUAGE_VERSION));
		System.out.println("Extensions: " + gl.glGetString(GL.GL_EXTENSIONS));

		gl.glClearColor(.3f, .5f, .8f, 1.0f);
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		gl.glClearDepth(1.0);
		gl.glLineWidth(2);
		gl.glEnable(GL.GL_DEPTH_TEST);

		final Light light = new Light(gl, 0);
		light.position(5, 5, 5);
		light.ambient(0.1f, 0.1f, 0.1f);
		light.diffuse(0.9f, 0.9f, 0.9f);
		light.specular(1, 1, 1);
		light.enable();

		gl.glEnable(GL.GL_LIGHTING);

		program = new Program(gl);
		// program.attachVertexShader(pwd + "brick.vert");
		// program.attachFragmentShader(pwd + "brick.frag");
		program.attachVertexShader(Path.get(Main.class, "proc3d.vert"));
		program.attachFragmentShader(Path.get(Main.class, "brick.frag"));
		program.link();
		program.use();

		gl.glUniform3f(program.uniform("LightPosition"), 5f, 5f, 5f);
		gl.glUniform3f(program.uniform("BrickColor"), 1f, 0.3f, 0.2f);
		gl.glUniform2f(program.uniform("BrickPct"), 0.9f, 0.85f);
		gl.glUniform2f(program.uniform("BrickSize"), 0.3f, 0.15f);
		gl.glUniform3f(program.uniform("MortarColor"), 0.85f, 0.86f, 0.84f);
	}

	int frames = 0;
	long start;

	int rot;

	@Override
	public void display() {
		if (++frames == 100) {
			final long now = System.nanoTime();
			final long time = now - start;
			start = now;
			shell.setText(String.valueOf((int) (frames * 1e9 / time)));
			frames = 0;
		}

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		gl.glTranslatef(0.0f, 0.0f, -10.0f);

		gl.glRotatef(0.075f * rot, 1.0f * rot, 5.0f * rot, 1.0f);
		gl.glRotatef(0.15f * rot, 1.5f * rot, 0.5f * rot, 1.0f);
		rot++;

		// gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
		gl.glColor3f(0.9f, 0.9f, 0.9f);

		drawTorus(1, 1.9f + (float) Math.sin((0.004f * rot)), 30, 60);
	}

	@Override
	public void reshape(final int x, final int y, final int width, final int height) {
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0f, (float) width / (float) height, 0.5f, 400.0f);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

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

	static final Display display = new Display();
	static final Shell shell = new Shell(display);

	public static void main(final String[] args) {
		shell.setLayout(new FillLayout());
		shell.setSize(640, 480);
		shell.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(final Event event) {
				display.dispose();
			}
		});
		new Main();

		shell.open();
		while (!display.isDisposed())
			if (!display.readAndDispatch()) display.sleep();
	}
}