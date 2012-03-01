package tintor.devel.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.glu.GLU;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public abstract class View extends GLCanvas {
	public final GL gl;
	public final GLU glu;

	public View(final Composite parent, final boolean doubleBuffer, final int depthBits) {
		super(parent, SWT.NO_BACKGROUND, gldata(doubleBuffer, depthBits));
		setCurrent();

		final GLContext context = GLDrawableFactory.getFactory().createExternalGLContext();
		context.makeCurrent();
		gl = context.getGL();
		glu = new GLU();

		addListener(SWT.Resize, new Listener() {
			public void handleEvent(final Event event) {
				final Rectangle bounds = getBounds();
				reshape(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		});

		final Display display = Display.getCurrent();
		display.asyncExec(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					display();
					swapBuffers();
					display.asyncExec(this);
				}
			}
		});
	}

	private static GLData gldata(final boolean doubleBuffer, final int depthBits) {
		final GLData data = new GLData();
		data.doubleBuffer = doubleBuffer;
		data.depthSize = depthBits;
		return data;
	}

	public abstract void display();

	public abstract void reshape(final int x, final int y, final int width, final int height);
}