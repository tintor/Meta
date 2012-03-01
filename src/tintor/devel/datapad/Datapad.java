package tintor.devel.datapad;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.prefs.Preferences;

import javax.xml.transform.TransformerException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class Datapad {
	private final static boolean Debug = false;
	private final static String Title = "Datapad 1.0_alpha";

	private final Preferences prefs = Preferences.userNodeForPackage(Datapad.class);
	private final Display display = new Display();
	private final Shell shell = new Shell(display);

	private File fileName;
	private boolean fileModified;
	private final DocumentTree fileTree = new DocumentTree(shell) {
		@Override
		protected void afterModify() {
			if (!fileModified) {
				fileModified = true;
				refreshTitle();
			}
		}
	};

	private final Command newDocument = new Command("New", "Ctrl+N", "new_document") {
		@Override
		public boolean run() {
			if (fileModified) {
				final MessageBox box = new MessageBox(shell, SWT.YES | SWT.NO | SWT.CANCEL
						| SWT.ICON_WARNING);
				final String name = fileName != null ? fileName.getName() : "Untitled";
				box.setMessage("Save the changes to file '" + name + "' before closing?");
				box.setText("Closing " + name + "!");

				switch (box.open()) {
				case SWT.YES:
					if (!save.run()) return false;
					break;
				case SWT.NO:
					break;
				case SWT.CANCEL:
					return false;
				default:
					throw new IllegalStateException();
				}
			}

			fileName = null;
			fileModified = false;
			refreshTitle();
			fileTree.clear();
			return true;
		}
	};

	private final Command open = new Command("Open...", "Ctrl+O", "open_folder") {
		@Override
		public boolean run() {
			if (!newDocument.run()) return false;
			final File file = showOpenDialog();
			return file != null && openFile(file);
		}
	};

	private boolean openFile(final File file) {
		assert !fileModified && file != null;
		try {
			fileTree.read(file);
			fileName = file;
			refreshTitle();
			return true;
		}
		catch (final Exception e) {
			if (Debug) throw new RuntimeException(e);
			final MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
			box.setMessage(e.toString());
			box.setText("Error opening " + file);
			box.open();
			return false;
		}
	}

	private final Command save = new Command("Save", "Ctrl+S", "save") {
		@Override
		public boolean run() {
			return fileName == null ? saveAs.run() : saveFile();
		}
	};

	private final Command saveAs = new Command("Save As...", null, "saveas") {
		@Override
		public boolean run() {
			final File file = showSaveDialog();
			if (file == null) return false;
			fileName = file;
			return saveFile();
		}
	};

	private final Command revert = new Command("Revert", null, "refresh_nav") {
		@Override
		public boolean run() {
			final File file = fileName;
			if (!newDocument.run()) return false;
			return file != null && openFile(file);
		}
	};

	private final Command exit = new Command("Exit", null, null) {
		@Override
		public boolean run() {
			shell.close();
			return true;
		}
	};

	private final Command undo = new Command("Undo", "Ctrl+Z", "undo") {
		@Override
		public boolean run() {
			fileTree.undo();
			return true;
		}
	};

	private final Command redo = new Command("Redo", "Ctrl+Y", "redo") {
		@Override
		public boolean run() {
			fileTree.redo();
			return true;
		}
	};

	private final Command newFolder = new Command("New Folder", "F4", "new_folder") {
		@Override
		public boolean run() {
			fileTree.insert("<folder name=\"new folder\"/>");
			return true;
		}
	};

	private final Command newProperty = new Command("New Property", "Insert", "new_file") {
		@Override
		public boolean run() {
			fileTree.insert("<property name=\"new property\"/>");
			return true;
		}
	};

	private final Command editName = new Command("Edit Name", "F2", "write") {
		@Override
		public boolean run() {
			fileTree.editName();
			return true;
		}
	};

	private final Command editValue = new Command("Edit Value", "F3", "edit") {
		@Override
		public boolean run() {
			fileTree.editValue();
			return true;
		}
	};

	private final Command cut = new Command("Cut", "Ctrl+X", "cut") {
		@Override
		public boolean run() {
			return copy.run() && delete.run();
		}
	};

	private final Command copy = new Command("Copy", "Ctrl+C", "copy") {
		@Override
		public boolean run() {
			final String text = fileTree.serialize();
			if (text == null) return false;
			SWTUtil.writeClipboard(text);
			return true;
		}
	};

	private final Command paste = new Command("Paste", "Ctrl+V", "paste") {
		@Override
		public boolean run() {
			final String text = SWTUtil.readClipboard();
			if (text == null) return false;
			fileTree.insert(text);
			return true;
		}
	};

	private final Command delete = new Command("Delete", "Delete", "delete") {
		@Override
		public boolean run() {
			fileTree.delete();
			return true;
		}
	};

	// private final Command find = new Command("Find...", "Ctrl+F", "find") {
	// @Override
	// public boolean run() {
	// assert false;
	// return true;
	// }
	// };
	//
	// private final Command findNext = new Command("Find Next", "F3", "find_next") {
	// @Override
	// public boolean run() {
	// assert false;
	// return true;
	// }
	// };

	// private final ListCommand language = new ListCommand("Language", null, "English", "Ñðïñêè", "Srpski") {
	// @Override
	// public int getSelection() {
	// final int selection = prefs.getInt("language", 0);
	// return 0 <= selection && selection <= 2 ? selection : 0;
	// }
	//
	// @Override
	// public void setSelection(final int selection) {
	// final MenuItem[] items = shell.getMenuBar().getItems();
	// switch (selection) {
	// case 0:
	// items[0].setText("File");
	// // fileMenu.
	// break;
	// case 1:
	// items[0].setText("Ôà¼ë");
	// break;
	// case 2:
	// items[0].setText("Fajl");
	// break;
	// }
	// prefs.putInt("language", selection);
	// }
	// };

	private final ListCommand titleDisplay = new ListCommand("Title Display", null, "Filename only",
			"Filename and Directory", "Full Pathname") {
		@Override
		public int getSelection() {
			final int selection = prefs.getInt("title_display", 0);
			return 0 <= selection && selection <= 2 ? selection : 0;
		}

		@Override
		public void setSelection(final int selection) {
			prefs.putInt("title_display", selection);
			refreshTitle();
		}
	};

	// private final CheckCommand singleFileInstance = new CheckCommand("Single File Instance", null, null) {
	// @Override
	// public boolean getChecked() {
	// return prefs.getBoolean("single_file_instance", true);
	// }
	//
	// @Override
	// public void setChecked(final boolean checked) {
	// assert false;
	// prefs.putBoolean("single_file_instance", checked);
	// }
	// };
	//
	// private final CheckCommand fileChangeNotification = new CheckCommand("File Change Notification", null, null)
	// {
	// @Override
	// public boolean getChecked() {
	// return prefs.getBoolean("file_change_notification", true);
	// }
	//
	// @Override
	// public void setChecked(final boolean checked) {
	// assert false;
	// prefs.putBoolean("file_change_notification", checked);
	// }
	// };

	private Datapad() throws Exception {
		shell.setMenuBar(new Menu(shell, SWT.BAR));
		shell.setLayout(new FillLayout());
		shell.setMaximized(prefs.getBoolean("maximized", false));
		shell.setSize(prefs.getInt("width", 500), prefs.getInt("height", 500));
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(final ShellEvent e) {
				if (!newDocument.run()) {
					e.doit = false;
					return;
				}

				prefs.putBoolean("maximized", shell.getMaximized());
				prefs.putInt("width", shell.getSize().x);
				prefs.putInt("height", shell.getSize().y);
			}
		});
		shell.setImage(SWTUtil.image("library"));

		// Create Menus
		SWTUtil.createMenu(shell, "File", newDocument, open, save, saveAs, revert, null, exit);
		final Object[] editCommands = { undo, redo, null, cut, copy, paste, delete, null, newProperty,
				newFolder, editName, editValue /* , null, find, findNext */};
		SWTUtil.createMenu(shell, "Edit", editCommands);
		SWTUtil.createMenu(shell, "Settings", /* language, */titleDisplay /*
		 * , null, singleFileInstance,
		 * fileChangeNotification
		 */);
		fileTree.setMenu(SWTUtil.addMenuItems(new Menu(shell, SWT.POP_UP), editCommands));

		newDocument.run();
		shell.open();
	}

	private void refreshTitle() {
		final StringBuilder b = new StringBuilder();
		if (fileModified) b.append("* ");

		if (fileName == null)
			b.append("Untitled");
		else
			switch (titleDisplay.getSelection()) {
			case 0:
				b.append(fileName.getName());
				break;
			case 1:
				b.append(fileName.getName());
				b.append(" [");
				b.append(fileName.getParentFile().getAbsolutePath());
				b.append(']');
				break;
			case 2:
				b.append(fileName.getAbsolutePath());
				break;
			}

		b.append(" - ");
		b.append(Title);
		shell.setText(b.toString());
	}

	private boolean saveFile() {
		assert fileName != null;

		try {
			fileTree.write(fileName);
		}
		catch (final TransformerException e) {
			final MessageBox box = new MessageBox(shell);
			box.setText("Error saving " + fileName);
			box.setMessage(e.toString());
			box.open();
			return false;
		}

		fileModified = false;
		refreshTitle();
		return true;
	}

	/**
	 * Application entry point.
	 */
	public static void main(final String[] args) throws Exception {
		final Datapad main = new Datapad();

		if (Debug) {
			final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
					getClassPath(Datapad.class) + "/data.xml");
			final Reader reader = new InputStreamReader(is);
			while (true) {
				final int c = reader.read();
				if (c == -1) break;
				System.out.print((char) c);
			}
		}

		switch (args.length) {
		case 0:
			if (Debug) main.openFile(new File(getClassPath(Datapad.class) + "/data.xml"));
			break;
		case 1:
			main.openFile(new File(args[0]));
			break;
		default:
			final MessageBox box = new MessageBox(main.shell, SWT.ICON_WARNING);
			box.setMessage("Too many arguments!");
			box.setText("Datapad");
			break;
		}

		while (!main.shell.isDisposed())
			if (!main.display.readAndDispatch()) main.display.sleep();
	}

	private static String getClassPath(final Class<?> clazz) {
		return clazz.getPackage().getName().replace('.', '/');
	}

	/** @return null if cancel pressed */
	private File showOpenDialog() {
		final FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setText("Open");
		dialog.setFilterPath(Debug ? getClassPath(Datapad.class) : ".");
		dialog.setFilterExtensions(new String[] { "*.xml", "*" });
		dialog.setFilterNames(new String[] { "XML Tables (*.xml)", "All files (*.*)" });
		final String file = dialog.open();
		return file != null ? new File(file) : null;
	}

	/** @return null in cancel pressed */
	private File showSaveDialog() {
		final FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setText("Save");
		dialog.setFilterPath(Debug ? getClassPath(Datapad.class) : ".");
		dialog.setFilterExtensions(new String[] { "*.xml" });
		dialog.setFilterNames(new String[] { "XML Tables (*.xml)" });
		final String file = dialog.open();
		return file != null ? new File(file) : null;
	}
}