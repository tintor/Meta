package tintor.devel;

public abstract class Visitor<T> {
	public void begin() {
	}

	public abstract void visit(T object);

	public void end() {
	}

	public Object result() {
		return null;
	}
}