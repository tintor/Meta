package tintor.devel.datapad;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

class SWTUtil {
	private static final Map<String, Image> images = new HashMap<String, Image>();

	public static Image image(final String name) {
		if (images.containsKey(name)) return images.get(name);

		final URL url = Datapad.class.getResource(name + ".gif");
		try {
			final Image image = new Image(null, url.openStream());
			images.put(name, image);
			return image;
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
		// final Image image = new Image(null, "tintor/devel/datapad/" + name + ".gif");
	}

	private static void addMenuItem(final Menu menu, final Object o) {
		if (o == null)
			new MenuItem(menu, SWT.SEPARATOR);
		else if (o instanceof Command) {
			final MenuItem item = new MenuItem(menu, SWT.PUSH);
			final Command command = (Command) o;

			if (command.getAccelerator() != null) {
				item.setText(command.getText() + "\t" + command.getAccelerator());
				item.setAccelerator(SWTUtil.accelerator(command.getAccelerator()));
			}
			else
				item.setText(command.getText());

			if (command.getImage() != null) item.setImage(command.getImage());
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					command.run();
				}
			});
		}
		else if (o instanceof CheckCommand) {
			final MenuItem item = new MenuItem(menu, SWT.CHECK);
			final CheckCommand command = (CheckCommand) o;

			if (command.getAccelerator() != null) {
				item.setText(command.getText() + "\t" + command.getAccelerator());
				item.setAccelerator(SWTUtil.accelerator(command.getAccelerator()));
			}
			else
				item.setText(command.getText());

			item.setSelection(command.getChecked());
			if (command.getImage() != null) item.setImage(command.getImage());
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					command.setChecked(item.getSelection());
				}
			});
		}
		else if (o instanceof ListCommand) {
			final MenuItem item = new MenuItem(menu, SWT.CASCADE);
			final ListCommand command = (ListCommand) o;

			final Menu m = new Menu(item);
			item.setText(command.getText());
			item.setMenu(m);
			if (command.getImage() != null) item.setImage(command.getImage());

			final int selection = command.getSelection();
			int i = 0;
			for (final String option : command.getList()) {
				final MenuItem mi = new MenuItem(m, SWT.RADIO);
				final int id = i++;

				if (id == selection) mi.setSelection(true);
				mi.setText(option);
				mi.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						if (mi.getSelection()) command.setSelection(id);
					}
				});
			}
		}
	}

	public static Menu addMenuItems(final Menu menu, final Object... items) {
		for (final Object o : items)
			addMenuItem(menu, o);
		return menu;
	}

	static Menu createMenu(final Shell shell, final String text, final Object... items) {
		final Menu menu = new Menu(shell, SWT.DROP_DOWN);

		addMenuItems(menu, items);

		final MenuItem item = new MenuItem(shell.getMenuBar(), SWT.CASCADE);
		item.setText(text);
		item.setMenu(menu);
		return menu;
	}

	private static int accelerator(final String a) {
		if (a.startsWith("Ctrl+")) return SWT.CONTROL | accelerator(a.substring(5));
		if (a.startsWith("Shift+")) return SWT.SHIFT | accelerator(a.substring(6));
		if (a.startsWith("Alt+")) return SWT.ALT | accelerator(a.substring(4));

		final String k = a.intern();
		if (k == "Esc") return SWT.ESC;
		if (k == "PageUp") return SWT.PAGE_UP;
		if (k == "Insert") return SWT.INSERT;
		if (k == "Delete") return SWT.DEL;
		if (k == "Home") return SWT.HOME;
		if (k == "End") return SWT.END;
		if (k == "Add") return SWT.KEYPAD_ADD;
		if (k == "Subtract") return SWT.KEYPAD_SUBTRACT;
		if (k == "Multiply") return SWT.KEYPAD_MULTIPLY;
		if (k == "PageDown") return SWT.PAGE_DOWN;
		if (k == "Space") return ' ';
		if (k == "Enter") return '\n';
		if (k.length() > 1 && k.charAt(0) == 'F') return SWT.F1 - 1 + Integer.parseInt(k.substring(1));
		if (k.length() != 1) throw new RuntimeException("Invalid key: " + k);
		return k.charAt(0);
	}

	public static void writeClipboard(final String text) {
		final Clipboard clipboard = new Clipboard(null);
		try {
			clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
		}
		finally {
			clipboard.dispose();
		}
	}

	public static String readClipboard() {
		final Clipboard clipboard = new Clipboard(null);
		try {
			return (String) clipboard.getContents(TextTransfer.getInstance());
		}
		finally {
			clipboard.dispose();
		}
	}
}