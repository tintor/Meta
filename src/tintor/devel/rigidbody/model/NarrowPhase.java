package tintor.devel.rigidbody.model;

import java.util.List;

public interface NarrowPhase {
	void run(Body a, Body b, List<Contact> contacts);
}

class BruteForceNarrowPhase implements NarrowPhase {
	@Override
	public void run(final Body a, final Body b, final List<Contact> contacts) {
		assert false;
	}
}

class SATNarrowPhase implements NarrowPhase {
	@Override
	public void run(final Body a, final Body b, final List<Contact> contacts) {
		assert false;
	}
}