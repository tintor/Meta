package tintor.devel.compiler;

import java.util.Arrays;

public class XNode {
	public final String name;
	private final Object[] list;

	public XNode(final String name, final Object... list) {
		if (name == null) throw new IllegalArgumentException("name");
		if (list == null) throw new IllegalArgumentException("list");

		this.name = name.intern();
		this.list = Arrays.copyOf(list, list.length);
	}

	public int size() {
		return list.length;
	}

	public XNode node(final int i) {
		return (XNode) list[i];
	}

	public XNode node(final String name) {
		for (final Object o : list)
			if (o instanceof XNode) {
				final XNode a = (XNode) o;
				if (a.name == name) return a;
			}
		throw new RuntimeException("unknown node '" + name + "'");
	}

	public String text() {
		if (list.length != 1) throw new RuntimeException();
		return text(0);
	}

	public String text(final int i) {
		if (list[i] instanceof XNode) throw new RuntimeException("element is node, not text");
		return list[i].toString();
	}

	public void printAsTree(final StringBuilder b, final int indent) {
		if (isSimpleList()) {
			for (int i = 0; i < indent; i++)
				b.append('\t');
			printInline(b);
			b.append('\n');
		} else {
			for (int i = 0; i < indent; i++)
				b.append('\t');
			b.append('(');
			b.append(escape(name));
			b.append('\n');
			for (final Object x : list) {
				if (x instanceof XNode) {
					((XNode) x).printAsTree(b, indent + 1);
				} else {
					for (int i = 0; i < indent + 1; i++)
						b.append('\t');
					b.append(escape(x.toString()));
					b.append('\n');
				}
			}
			for (int i = 0; i < indent; i++)
				b.append('\t');
			b.append(")\n");
		}
	}

	public void printInline(final StringBuilder b) {
		b.append('(');
		b.append(escape(name));
		for (final Object x : list) {
			b.append(' ');
			if (x instanceof XNode)
				((XNode) x).printInline(b);
			else
				b.append(escape(x.toString()));
		}
		b.append(')');
	}

	private boolean isSimpleList() {
		if (list.length == 1 && (!(list[0] instanceof XNode) || ((XNode) list[0]).isSimpleList())) return true;
		for (final Object x : list)
			if (x instanceof XNode) return false;
		return true;
	}

	private String escape(final String text) {
		if (isSimple(text)) return text;
		final StringBuilder b = new StringBuilder();
		b.append('"');
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			if (c == '\"')
				b.append("\\\"");
			else if (c == '\n')
				b.append("\\n");
			else
				b.append(c);
		}
		b.append('"');
		return b.toString();
	}

	private static boolean isSimple(final String text) {
		if (text.length() == 0) return false;
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '_' && c != '@' && c != '!' && c != '?') return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		printAsTree(b, 0);
		return b.toString();
	}

	public static void main(final String[] args) {
		System.out.println(new XNode("marko", new XNode("pera", 10, "mare dss")));
	}
}