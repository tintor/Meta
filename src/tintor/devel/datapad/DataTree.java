package tintor.devel.datapad;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class DocumentTree {
	private Document document;
	private Element root;
	private final Tree tree;
	private final TreeEditor editor;

	private final Color black = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);

	protected void afterModify() {

	}

	DocumentTree(final Shell shell) {
		tree = new Tree(shell, SWT.BORDER);
		editor = new TreeEditor(tree);
		new DragAndDrop();

		new TreeColumn(tree, SWT.LEFT);
		new TreeColumn(tree, SWT.LEFT);
		tree.setLinesVisible(true);
		tree.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(@SuppressWarnings("unused") final ControlEvent e) {
				final int w = tree.getClientArea().width;
				tree.getColumn(0).setWidth(w / 2);
				tree.getColumn(1).setWidth(w - w / 2);
			}
		});

		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				final TreeItem item = tree.getItem(new Point(e.x, e.y));
				if (item == null) tree.setSelection(new TreeItem[] {});
			}
		});
	}

	class DragAndDrop implements DragSourceListener, DropTargetListener {
		TreeItem dragSourceItem;

		DragAndDrop() {
			final Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
			final int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

			final DragSource source = new DragSource(tree, operations);
			source.setTransfer(types);
			source.addDragListener(this);

			final DropTarget target = new DropTarget(tree, operations);
			target.setTransfer(types);
			target.addDropListener(this);
		}

		public void dragStart(final DragSourceEvent event) {
			if (tree.getSelectionCount() > 0) {
				event.doit = true;
				dragSourceItem = tree.getSelection()[0];
			}
			else
				event.doit = false;
		}

		public void dragOver(final DropTargetEvent event) {
			event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
			if (event.item == null) return;
			final TreeItem item = (TreeItem) event.item;
			final Point p = Display.getCurrent().map(null, tree, event.x, event.y);
			final Rectangle bounds = item.getBounds();
			if (p.y < bounds.y + bounds.height / 3)
				event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
			else if (p.y > bounds.y + 2 * bounds.height / 3)
				event.feedback |= DND.FEEDBACK_INSERT_AFTER;
			else
				event.feedback |= DND.FEEDBACK_SELECT;
		}

		public void dragSetData(final DragSourceEvent event) {
			event.data = serialize(dragSourceItem);
		}

		public void drop(final DropTargetEvent event) {
			// TODO optimize internal drag and drops
			if (event.data == null) {
				event.detail = DND.DROP_NONE;
				return;
			}
			final Element element = deserialize((String) event.data);
			backup();

			if (event.item == null) {
				tree.setSelection(sync(createChild(null, element), element));
				afterModify();
				return;
			}

			final TreeItem target = (TreeItem) event.item;
			final Point p = Display.getCurrent().map(null, tree, event.x, event.y);
			final Rectangle bounds = target.getBounds();

			if (p.y < bounds.y + bounds.height / 3)
				tree.setSelection(sync(createBefore(target, element), element));
			else if (p.y > bounds.y + 2 * bounds.height / 3)
				tree.setSelection(sync(createAfter(target, element), element));
			else
				tree.setSelection(sync(createChild(target, element), element));
			afterModify();
		}

		public void dragFinished(final DragSourceEvent event) {
			if (event.detail == DND.DROP_MOVE) delete(dragSourceItem);
			dragSourceItem = null;
		}

		public void dropAccept(@SuppressWarnings("unused") final DropTargetEvent event) {
		}

		public void dragEnter(@SuppressWarnings("unused") final DropTargetEvent event) {
		}

		public void dragLeave(@SuppressWarnings("unused") final DropTargetEvent event) {
		}

		public void dragOperationChanged(@SuppressWarnings("unused") final DropTargetEvent event) {
		}
	}

	// TODO fix this 3 functions
	private TreeItem createBefore(final TreeItem item, final Element element) {
		final Node node = (Node) item.getData();
		node.getParentNode().insertBefore(element, node);
		if (item.getParentItem() == null) return new TreeItem(tree, SWT.NONE, tree.indexOf(item));
		return new TreeItem(item.getParentItem(), SWT.NONE, item.getParentItem().indexOf(item));
	}

	private TreeItem createAfter(final TreeItem item, final Element element) {
		final Node node = (Node) item.getData();
		if (node.getNextSibling() == null) return createChild(item.getParentItem(), element);
		node.getParentNode().insertBefore(element, node.getNextSibling());
		if (item.getParentItem() == null) return new TreeItem(tree, SWT.NONE, tree.indexOf(item) + 1);
		return new TreeItem(item.getParentItem(), SWT.NONE, item.getParentItem().indexOf(item) + 1);
	}

	private TreeItem createChild(final TreeItem parent, final Element element) {
		if (parent == null) {
			root.appendChild(element);
			return new TreeItem(tree, SWT.NONE);
		}
		if (!((Node) parent.getData()).getNodeName().equals("folder")) return createAfter(parent, element);

		((Node) parent.getData()).appendChild(element);
		final TreeItem item = new TreeItem(parent, SWT.NONE);
		parent.setExpanded(true);
		return item;
	}

	public void undo() {
		if (past.size() > 0) {
			future.push(new State());
			past.pop().restore();
		}
	}

	public void redo() {
		if (future.size() > 0) {
			past.push(new State());
			future.pop().restore();
		}
	}

	private void backup() {
		past.push(new State());
		future.clear();
	}

	private final Stack<State> past = new Stack<State>();
	private final Stack<State> future = new Stack<State>();

	private class State {
		final String text;

		State() {
			try {
				final StringWriter writer = new StringWriter();
				final Transformer t = TransformerFactory.newInstance().newTransformer();
				t.transform(new DOMSource(document), new StreamResult(writer));
				text = writer.toString();
			}
			catch (final TransformerException e) {
				throw new RuntimeException(e);
			}
		}

		void restore() {
			init(new ByteArrayInputStream(text.getBytes()));
		}
	}

	/**
	 * Inserts subtree at current selection.
	 */
	public void insert(final String xml) {
		final Element element = deserialize(xml);
		backup();
		tree.setRedraw(false);
		try {
			tree.setSelection(sync(createChild(selection(), element), element));
		}
		finally {
			tree.setRedraw(true);
		}
		afterModify();
	}

	public void editName() {
		final TreeItem item = selection();
		if (item == null) return;

		backup();
		final Composite composite = new Composite(tree, SWT.NONE);
		// composite.setBackground(black);
		final Text text = new Text(composite, SWT.BORDER);
		composite.addListener(SWT.Resize, new Listener() {
			public void handleEvent(@SuppressWarnings("unused") final Event e) {
				final Rectangle rect = composite.getClientArea();
				text.setBounds(rect.x, rect.y, rect.width, rect.height);
			}
		});
		final Listener textListener = new Listener() {
			public void handleEvent(final Event e) {
				switch (e.type) {
				case SWT.FocusOut:
					item.setText(text.text());
					((Element) item.getData()).setAttribute("name", text.text());
					composite.dispose();
					// text.dispose();
					break;
				case SWT.Verify:
					final String newText = text.text();
					final String leftText = newText.substring(0, e.start);
					final String rightText = newText.substring(e.end, newText.length());
					final GC gc = new GC(text);
					Point size = gc.textExtent(leftText + e.text + rightText);
					gc.dispose();
					size = text.computeSize(size.x, SWT.DEFAULT);
					editor.horizontalAlignment = SWT.LEFT;
					final Rectangle itemRect = item.getBounds(),
					rect = tree.getClientArea();
					editor.minimumWidth = Math.max(size.x, itemRect.width) + 1 * 2;
					final int left = itemRect.x,
					right = rect.x + rect.width;
					editor.minimumWidth = Math.min(editor.minimumWidth, right - left);
					editor.minimumHeight = size.y + 1 * 2;
					editor.layout();
					break;
				case SWT.Traverse:
					if (e.detail == SWT.TRAVERSE_RETURN) {
						item.setText(text.text());
						((Element) item.getData()).setAttribute("name", text.text());
						composite.dispose();
						// text.dispose();
					}
					else if (e.detail == SWT.TRAVERSE_ESCAPE) {
						composite.dispose();
						// text.dispose();
						e.doit = false;
					}
					break;
				}
			}
		};
		text.addListener(SWT.FocusOut, textListener);
		text.addListener(SWT.Traverse, textListener);
		text.addListener(SWT.Verify, textListener);
		editor.setEditor(composite, item);
		text.setText(item.text());
		text.selectAll();
		text.setFocus();
	}

	public void editValue() {
		final TreeItem item = selection();
		if (item == null || isFolder((Element) item.getData())) return;

		backup();
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		final Text text = new Text(tree, SWT.BORDER);
		final Listener textListener = new Listener() {
			public void handleEvent(final Event e) {
				switch (e.type) {
				case SWT.FocusOut:
					item.setText(1, text.text());
					text.dispose();
					break;
				case SWT.Traverse:
					switch (e.detail) {
					case SWT.TRAVERSE_RETURN:
						item.setText(1, text.text());
						// FALL THROUGH
					case SWT.TRAVERSE_ESCAPE:
						text.dispose();
						e.doit = false;
					}
					break;
				}
			}
		};
		text.addListener(SWT.FocusOut, textListener);
		text.addListener(SWT.Traverse, textListener);
		editor.setEditor(text, item, 1);
		text.setText(item.text(1));
		text.selectAll();
		text.setFocus();
	}

	public void setMenu(final Menu menu) {
		tree.setMenu(menu);
	}

	/** Delete selected item. */
	public void delete() {
		if (selection() != null) delete(selection());
	}

	private void delete(final TreeItem item) {
		backup();
		final Node node = (Node) item.getData();
		node.getParentNode().removeChild(node);
		tree.setRedraw(false);
		try {
			if (item == selection()) if (item.getParentItem() != null)
				tree.setSelection(item.getParentItem());
			else
				tree.setSelection(new TreeItem[] {});
			item.dispose();
		}
		finally {
			tree.setRedraw(true);
		}
		afterModify();
	}

	/** Serialize selected item */
	public String serialize() {
		return selection() != null ? serialize(selection()) : null;
	}

	public String serialize(final TreeItem item) {
		try {
			final StringWriter writer = new StringWriter();
			final Transformer t = TransformerFactory.newInstance().newTransformer();
			t.transform(new DOMSource((Element) item.getData()), new StreamResult(writer));
			return writer.toString();
		}
		catch (final TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	public Element deserialize(final String text) {
		try {
			final Element element = document.createElement("element");
			final Transformer t = TransformerFactory.newInstance().newTransformer();
			t.transform(new StreamSource(new StringReader(text)), new DOMResult(element));
			// TODO memory leak, need to remove element
			return (Element) element.getFirstChild();
		}
		catch (final TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	private void init(final InputStream is) {
		// TODO add document validation
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			root = null;
			for (Node a = document.getFirstChild(); a != null; a = a.getNextSibling())
				if (a.getNodeType() == Node.ELEMENT_NODE && a.getNodeName().equals("base")) {
					assert root == null;
					root = (Element) a;
				}
			assert root != null;
		}
		catch (final Exception e) {
			throw new RuntimeException(e);
		}
		tree.setRedraw(false);
		try {
			tree.removeAll();
			for (final Node node : each("/base/*"))
				if (node.getNodeType() == Node.ELEMENT_NODE)
					sync(new TreeItem(tree, SWT.NONE), (Element) node);
		}
		finally {
			tree.setRedraw(true);
		}
	}

	public void clear() {
		past.clear();
		future.clear();

		init(new ByteArrayInputStream("<base/>".getBytes()));
	}

	public void read(final File file) {
		past.clear();
		future.clear();

		try {
			init(new FileInputStream(file));
		}
		catch (final FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private Iterable<Node> each(final String xpath) {
		try {
			final NodeList list = (NodeList) XPathFactory.newInstance().newXPath().compile(xpath).evaluate(
					document, XPathConstants.NODESET);
			return new Iterable<Node>() {
				public Iterator<Node> iterator() {
					return new Iterator<Node>() {
						int index = 0;

						public boolean hasNext() {
							return index < list.getLength();
						}

						public Node next() {
							return list.item(index++);
						}

						public void remove() {
							throw new UnsupportedOperationException();
						}
					};
				}
			};
		}
		catch (final XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public void write(final File file) throws TransformerException {
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.transform(new DOMSource(document), new StreamResult(file));
	}

	private TreeItem selection() {
		return tree.getSelectionCount() > 0 ? tree.getSelection()[0] : null;
	}

	private static boolean isFolder(final Element element) {
		return element.getNodeName().equals("folder");
	}

	private static boolean isProperty(final Element element) {
		return element.getNodeName().equals("property");
	}

	private TreeItem sync(final TreeItem item, final Element element) {
		item.setData(element);
		item.setText(element.getAttribute("name"));
		item.removeAll();

		if (isFolder(element)) {
			item.setImage(SWTUtil.image("open_folder"));
			for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling())
				if (node.getNodeType() == Node.ELEMENT_NODE)
					sync(new TreeItem(item, SWT.NONE), (Element) node);
			item.setExpanded(!element.getAttribute("expanded").equals("no"));
		}
		else if (isProperty(element)) {
			item.setImage(SWTUtil.image("property"));
			item.setText(1, element.getTextContent());
		}
		else {
			item.setText(element.getNodeName());
			item.setImage(SWTUtil.image("errorwarning"));
		}
		return item;
	}
}