package tintor.devel.rigidbody.runner.swing;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.opengl.GLCanvas;
import javax.swing.JFrame;

import tintor.devel.rigidbody.model.World;
import tintor.devel.rigidbody.renderer.Renderer;
import tintor.devel.rigidbody.scenario.Dominoes;

import com.sun.opengl.util.Animator;

public class MainFrame extends JFrame {
	final World world = new Dominoes();
	final Renderer renderer = new Renderer();
	final CustomGLCanvas canvas = new CustomGLCanvas(false);

	MainFrame() {
		renderer.world = world;
		canvas.addGLEventListener(renderer);
		add(canvas);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				canvas.stopAnimating();
				System.exit(0); // TODO workaround for now
			}
		});
		setSize(800, 600);
	}

	public static void main(final String[] args) {
		final MainFrame mainFrame = new MainFrame();
		mainFrame.setVisible(true);
		mainFrame.canvas.startAnimating();
	}
}

class CustomGLCanvas extends GLCanvas {
	final Animator animator = new Animator(this);

	public CustomGLCanvas(final boolean runAsFastAsPossible) {
		animator.setRunAsFastAsPossible(runAsFastAsPossible);
	}

	public void startAnimating() {
		animator.start();
	}

	public void stopAnimating() {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				animator.stop();
			}
		};
		thread.start();
		try {
			thread.join();
		}
		catch (final InterruptedException e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	public void saveScreen(final String format, final File output) {
		final Dimension size = getSize();
		final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g2 = image.createGraphics();
		paintAll(g2);
		try {
			if (!ImageIO.write(image, format, output))
				throw new RuntimeException("No appropiate writer found for format: " + format + ".");
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}