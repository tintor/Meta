package tintor.devel;

public abstract class Visitor2<A, B> {
	public void begin() {
	}

	public abstract void visit(A first, B second);

	public void end() {
	}

	public Object result() {
		return null;
	}
}