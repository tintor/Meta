package tintor.devel.compiler.scanner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tintor.devel.compiler.XNode;

public class Scanner {
	private void emit(final XNode token) {
		tokens.add(token);
	}

	public static void main(final String[] args) throws IOException {
		final String file = "src/" + Scanner.class.getPackage().getName().replace(".", "/") + "/sample.txt";
		final Scanner scanner = new Scanner(new FileInputStream(file));
		scanner.scan();
	}

	private final Map<String, XNode> symbols = new HashMap<String, XNode>();
	private final XNode[] operators = new XNode[256];
	public final List<XNode> tokens = new ArrayList<XNode>();

	private static final XNode Begin = new XNode("begin"), End = new XNode("end"), Separator = new XNode("endl"),
			Space = new XNode("space");

	private final byte[] buffer = new byte[1 << 16];
	private int cursor; // index of next character to read from buffer 
	private int buffer_size; // number of bytes stored in buffer
	private final static byte EOF = 0;
	private final InputStream is;

	public Scanner(final InputStream is) {
		this.is = is;
	}

	public void symbols(final String... list) {
		for (final String a : list)
			symbols.put(a, new XNode(a));
	}

	public void operators(final String list) {
		for (final byte c : list.getBytes()) {
			final String a = new String(new byte[] { c }, 0, 1).intern();
			operators[c] = new XNode(a, new XNode(a));
		}
	}

	private byte read() throws IOException {
		try {
			return buffer[cursor];
		} catch (final ArrayIndexOutOfBoundsException e) {
			buffer_size = is.read(buffer, 0, buffer.length);
			if (buffer_size <= 0) return EOF;
			cursor = 0;
			return buffer[0];
		} finally {
			cursor++;
		}
	}

	private int line = 0;

	public void scan() throws IOException {
		int depth = 0;
		byte c = read();

		loop: while (true) {
			line += 1;

			// calculate indent
			int indent = 0;
			while (c == '\t') {
				indent += 1;
				c = read();
			}

			// update depth
			while (depth < indent) {
				emit(Begin);
				depth += 1;
			}
			while (depth > indent) {
				emit(End);
				depth -= 1;
			}

			// comment line
			if (c == '#') {
				c = read();
				while (true)
					if (c == '\n') {
						emit(Separator);
						c = read();
						continue loop;
					} else if (c == EOF)
						break loop;
					else
						c = read();
			}

			// command line
			while (true)
				if (isLetter(c) || c == '_') { // symbol
					data_size = 0;
					do {
						append(c);
						c = read();
					} while (isLetter(c) || c == '_' || isDigit(c));

					final String s = new String(data, 0, data_size);
					final XNode node = symbols.get(s);
					emit(node == null ? new XNode("symbol", s) : node);
				} else if (isDigit(c)) { // integer
					data_size = 0;
					do {
						append(c);
						c = read();
					} while (isDigit(c));
					emit(new XNode("int", new String(data, 0, data_size)));
					data_size = 0;
				} else if (operators[c] != null) {
					emit(operators[c]);
					c = read();
				} else
					switch (c) {
					case EOF:
						break loop;

					case '\n':
						emit(Separator);
						c = read();
						continue loop;

					case ' ':
						emit(Space);
						c = read();
						if (c == ' ') error("multiple space characters");
						break;

					case '\'':
						c = read();
						data_size = 0;
						while (c != '\'') {
							if (c == '\n' || c == EOF) error("expected ' before end of line");
							append(c);
							c = read();
						}
						emit(new XNode("string", new String(data, 0, data_size)));
						c = read();
						break;

					case '\t':
						do
							c = read();
						while (c == '\t');

						if (c != '#') error("expected comment after tab");

						while (true) {
							c = read();
							if (c == '\n') {
								emit(Separator);
								c = read();
								continue loop;
							}
							if (c == EOF) break loop;
						}

					default:
						error("unexpected char '" + (char) c + "'");
					}
		}

		while (depth > 0) {
			emit(End);
			depth -= 1;
		}
	}

	private void error(final String msg) {
		throw new RuntimeException("line:" + line + " " + msg);
	}

	private static boolean isLetter(final int c) {
		assert 'Z' < 'a';
		if (c <= 'Z') return 'A' <= c;
		return 'a' <= c && c <= 'z';
	}

	private static boolean isDigit(final int c) {
		return '0' <= c && c <= '9';
	}

	private byte[] data = new byte[1024];
	private int data_size = 0;

	private void append(final byte c) {
		try {
			data[data_size] = c;
		} catch (final ArrayIndexOutOfBoundsException e) {
			data = Arrays.copyOf(data, data.length * 2);
			data[data_size] = c;
		}
		data_size++;
	}
}