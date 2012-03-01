package tintor.devel.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class Grammar {
	private final Map<String, Matcher> matchers = new LinkedHashMap<String, Matcher>();
	private final Map<String, Matcher> references = new HashMap<String, Matcher>();
	private final Set<String> tokens = new HashSet<String>();
	private final Set<String> wrappers = new HashSet<String>();

	private final static MetaGrammar meta = new MetaGrammar();
	public static boolean trace = false;

	public Matcher optional(final Object... matchers) {
		return sequence(matchers).optional();
	}

	public Matcher any(final Object... matchers) {
		return sequence(matchers).any();
	}

	public Matcher repeat(final Object... matchers) {
		return sequence(matchers).repeat();
	}

	public Matcher and(final Object... matchers) {
		return sequence(matchers).and();
	}

	public Matcher not(final Object... matchers) {
		return sequence(matchers).not();
	}

	public Matcher sequence(final Object... matchers) {
		if (matchers.length == 0) throw new IllegalArgumentException();
		return sequence(matchers, 0);
	}

	public Matcher choice(final Object... matchers) {
		if (matchers.length == 0) throw new IllegalArgumentException();
		return choice(matchers, 0);
	}

	private Matcher sequence(final Object[] matchers, final int start) {
		assert start < matchers.length;
		if (start + 1 == matchers.length) return valueOf(matchers[start]);
		return valueOf(matchers[start]).then(sequence(matchers, start + 1));
	}

	private Matcher choice(final Object[] matchers, final int start) {
		assert start < matchers.length;
		if (start + 1 == matchers.length) return valueOf(matchers[start]);
		return valueOf(matchers[start]).or(choice(matchers, start + 1));
	}

	public Matcher valueOf(final Object m) {
		return (Matcher) m;
	}

	public Grammar() {
	}

	public Grammar(final File file) {
		try {
			load(new BufferedReader(new FileReader(file)));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void load(final String text) {
		load(new BufferedReader(new StringReader(text)));
	}

	public void load(final BufferedReader reader) {
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#") || line.length() == 0) continue;

				meta.text.set(line);
				final XNode tree = (XNode) meta.parse();
				put(tree.node("rname").text(0), meta.convert(tree.node(1)));
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void put(String name, final Object... m) {
		if (name.charAt(0) == '@') {
			name = name.substring(1);
			tokens.add(name);
		} else if (name.charAt(0) == '!') {
			name = name.substring(1);
			wrappers.add(name);
		}
		final Matcher a = sequence(m);
		matchers.put(name, matchers.containsKey(name) ? matchers.get(name).or(a) : a);
	}

	private static Stack<String> errors = new Stack<String>();

	public Matcher get(final String name) {
		if (!references.containsKey(name)) references.put(name, new Matcher() {
			@Override
			public Result match(final int position) {
				final Matcher matcher = runtimeGet(name);

				final int startErrors = errors.size();
				final Result p = matcher.match(position);
				if (trace) if (p == null) {
					System.out.printf("%s@%s\tfailed\n", name, position);
				} else {
					System.out.printf("%s@%s\t%s\n", name, position, p.position);
				}

				if (p == null) {
					if (startErrors == errors.size()) errors.push(String.format("%s at %s", name, position));
					return null;
				} else {
					while (errors.size() > startErrors)
						errors.pop();
				}

				return format(name, position, p);
			}
		});
		return references.get(name);
	}

	private Matcher runtimeGet(final String name) {
		final Matcher matcher = matchers.get(name);
		if (matcher != null) return matcher;

		final Matcher matcher2 = getGeneric(name);
		if (matcher2 == null) throw new RuntimeException("unknown rule: " + name);

		put(name, matcher2);
		return matcher2;
	}

	public Matcher getGeneric(final String name) {
		return null;
	}

	public Matcher.Result format(final String name, final int position, final Matcher.Result p) {
		if (tokens.contains(name)) return new Matcher.Result(p.position, new XNode(name, position, p.position));

		if (wrappers.contains(name) && p.size() == 1) return p;

		final Object[] list = new Object[p.size()];
		for (int i = 0; i < p.size(); i++)
			list[i] = p.get(i);

		return new Matcher.Result(p.position, new XNode(name, list));
	}

	public Object parse(final String name) {
		final Matcher.Result p = get(name).match(0);
		if (p != null) return p.get(0);

		final StringBuilder b = new StringBuilder("syntax error: expected ");
		for (int i = 0; i < errors.size(); i++) {
			if (i > 0) b.append(" OR ");
			b.append(errors.get(i));
		}
		errors.clear();
		throw new RuntimeException(b.toString());
	}

	public Object parse() {
		final Iterator<String> it = matchers.keySet().iterator();
		if (!it.hasNext()) throw new RuntimeException("grammar is empty");
		return parse(it.next());
	}

	public static void main(final String[] args) {
		final String text = "1*3*4";

		final Grammar g = new Grammar();
		g.load("!expr := expr2 {add expr2}");
		g.load("!expr2 := number {mul number} / '(' expr ')'");
		g.load("mul := '*' / '/'");
		g.load("add := '+' / '-'");
		g.load("number := [0-9]+");
		System.out.println(g.parse());
	}
}

class MetaGrammar extends Grammar {
	MetaGrammar() {
		final Matcher head = choice(range('a', 'z'), range('A', 'Z'), string("_"));
		final Matcher tail = choice(range('a', 'z'), range('A', 'Z'), range('0', '9'), "_");

		put("rule", get("rname"), " := ", get("choice"), get("end"));
		put("@rname", choice("@", "!").optional(), head, tail.any());
		put("end", new Matcher() {
			@Override
			public Result match(final int position) {
				return position == text.get().length() ? new Result(position) : null;
			}
		});

		put("!choice", get("sequence"), any(" / ", get("sequence")));
		put("!sequence", get("logical").then(any(" ", get("logical"))));

		put("!logical", get("repeat"), choice(get("and"), get("not")).optional());
		put("!repeat", get("unary"), get("plus").optional());
		put("!unary", choice(get("optional"), get("any"), sequence("(", get("choice"), ")"), get("atom")));
		put("and", "&");
		put("not", "!");
		put("plus", "+");

		put("optional", "[", get("choice"), "]");
		put("any", "{", get("choice"), "}");

		put("!atom", get("name"));
		//put("!atom", choice(get("name"), get("string"), get("range")));

		put("@name", head, tail.any());
		//final Matcher anyChar = range('\0', '\uFFFF');
		//put("@string", "'", repeat(string("'").not(), anyChar, "'"));
		//put("@range", "[", anyChar, "-", anyChar, "]");
	}

	@Override
	public Matcher valueOf(final Object m) {
		return m instanceof String ? string((String) m) : (Matcher) m;
	}

	final ThreadLocal<String> text = new ThreadLocal<String>();

	private Matcher string(final String a) {
		return new Matcher() {
			@Override
			public Result match(final int position) {
				return text.get().startsWith(a, position) ? new Result(position + a.length()) : null;
			}
		};
	}

	private Matcher range(final char low, final char high) {
		return new Matcher() {
			@Override
			public Result match(final int position) {
				final String t = text.get();
				return position < t.length() && low <= t.charAt(position) && t.charAt(position) <= high ? new Result(
						position + 1)
						: null;
			}
		};
	}

	public Matcher convert(final XNode a) {
		if (a.name == "choice") {
			assert a.size() > 1;
			Matcher m = convert(a.node(0));
			for (int i = 1; i < a.size(); i++)
				m = m.or(convert(a.node(i)));
			return m;
		}
		if (a.name == "sequence") {
			assert a.size() > 1;
			Matcher m = convert(a.node(0));
			for (int i = 1; i < a.size(); i++)
				m = m.then(convert(a.node(i)));
			return m;
		}
		if (a.name == "repeat") {
			assert a.size() == 2;
			return convert(a.node(0)).repeat();
		}
		if (a.name == "logical") {
			assert a.size() == 2;
			if (a.node(1).name == "and") return convert(a.node(0)).and();
			if (a.node(1).name == "not") return convert(a.node(0)).not();
			throw new RuntimeException();
		}
		if (a.name == "any") {
			assert a.size() == 1;
			return convert(a.node(0)).any();
		}
		if (a.name == "repeat") {
			assert a.size() == 1;
			return convert(a.node(0)).repeat();
		}
		if (a.name == "optional") {
			assert a.size() == 1;
			return convert(a.node(0)).optional();
		}
		if (a.name == "name") {
			assert a.size() == 1;
			return get(a.text(0));
		}
		throw new RuntimeException("unrecognized '" + a + "'");
	}
}