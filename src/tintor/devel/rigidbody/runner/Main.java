package tintor.devel.rigidbody.runner;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import tintor.devel.rigidbody.renderer.WorldView;
import tintor.devel.rigidbody.scenario.Dominoes;

public class Main {
	public static void main(final String[] args) {
		final Display display = new Display();
		final Shell shell = new Shell(display);
		final WorldView view = new WorldView(shell);
		view.attachWorld(new Dominoes());

		shell.setLayout(new FillLayout());
		shell.setSize(800, 600);
		shell.open();

		while (!shell.isDisposed())
			if (!display.readAndDispatch()) display.sleep();
	}
}