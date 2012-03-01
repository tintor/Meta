package tintor.devel.compiler;

class Match {
	final String text;
	final int start, count;

	Match(final String text, final int start, final int count) {
		if (start < 0 || count < 0 || start + count > text.length()) throw new IllegalArgumentException();
		this.text = text;
		this.start = start;
		this.count = count;
	}

	@Override
	public String toString() {
		return String.format("start=%s, count=%s, match='%s', prematch='%s', postmatch='%s'", start, count, text
				.substring(start, start + count), text.substring(0, start), text.substring(start + count));
	}
}