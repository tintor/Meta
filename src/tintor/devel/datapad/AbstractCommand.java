package tintor.devel.datapad;

import java.util.Arrays;

import org.eclipse.swt.graphics.Image;

abstract class AbstractCommand {
	private final String text;
	private final Image image;
	private final String acc;

	public AbstractCommand(final String text, final String acc, final String image) {
		this.text = text;
		this.acc = acc;
		this.image = image != null ? SWTUtil.image(image) : null;
	}

	public final String getText() {
		return text;
	}

	public final String getAccelerator() {
		return acc;
	}

	public final Image getImage() {
		return image;
	}
}

abstract class Command extends AbstractCommand {
	public Command(final String text, final String acc, final String image) {
		super(text, acc, image);
	}

	public abstract boolean run();
}

abstract class CheckCommand extends AbstractCommand {
	public CheckCommand(final String text, final String acc, final String image) {
		super(text, acc, image);
	}

	public abstract boolean getChecked();

	public abstract void setChecked(boolean checked);
}

abstract class ListCommand extends AbstractCommand {
	private final String[] list;

	public ListCommand(final String text, final String image, final String... list) {
		super(text, null, image);
		this.list = Arrays.copyOf(list, list.length);
	}

	public String[] getList() {
		return Arrays.copyOf(list, list.length);
	}

	public abstract int getSelection();

	public abstract void setSelection(int selection);
}