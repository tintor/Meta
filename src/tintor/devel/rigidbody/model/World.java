package tintor.devel.rigidbody.model;

import java.util.ArrayList;
import java.util.List;

import tintor.devel.Visitor;

public class World {
	public int collisionIterations = 10;
	public int contactIterations = 20;

	public float timeStep = 0.01f;

	public NarrowPhase narrowPhase = new BruteForceNarrowPhase();
	public BroadPhase broadPhase = new BruteForceBroadPhase();

	private final List<Body> fixedBodies = new ArrayList<Body>();
	private final List<Body> staticBodies = new ArrayList<Body>();
	private final List<Body> dynamicBodies = new ArrayList<Body>();

	private final List<Contact> contacts = new ArrayList<Contact>();
	private final List<Constraint> joints = new ArrayList<Constraint>();

	/** Advances simulation by number of steps. */
	public void step(final int steps) {
		for (int i = 0; i < steps; i++)
			step();
	}

	public Object eachBody(final Visitor<Body> visitor) {
		for (final Body body : fixedBodies)
			visitor.visit(body);
		for (final Body body : staticBodies)
			visitor.visit(body);
		for (final Body body : dynamicBodies)
			visitor.visit(body);
		return visitor.result();
	}

	private void eachConstraintConcurrent(final Visitor<Constraint> visitor) {
		// TODO parallelize loop
		for (final Constraint joint : joints)
			visitor.visit(joint);
		// TODO parallelize loop
		for (final Contact contact : contacts)
			visitor.visit(contact);
	}

	private void eachDynamicBodyConcurrent(final Visitor<Body> visitor) {
		// TODO parallelize loop
		for (final Body body : dynamicBodies)
			visitor.visit(body);
	}

	private boolean contains(final Body body) {
		return fixedBodies.contains(body) || staticBodies.contains(body) || dynamicBodies.contains(body);
	}

	public void add(final Body body) {
		if (contains(body)) throw new IllegalArgumentException("already added");
		dynamicBodies.add(body);
	}

	public void addFixed(final Body body) {
		if (contains(body)) throw new IllegalArgumentException("already added");
		fixedBodies.add(body);
		broadPhase.updateFixedBodies(fixedBodies);
	}

	private void step() {
		assert contacts.size() == 0;
		broadPhase.run(staticBodies, dynamicBodies, narrowPhase, contacts);

		eachConstraintConcurrent(prepare);

		// resolve collisions
		for (int i = 0; i < collisionIterations; i++) {
			eachConstraintConcurrent(processCollision);
			eachDynamicBodyConcurrent(applyConcurrentImpuses);
		}

		// calculate external forces/torques for each body
		for (final Effector m : effectors)
			m.apply(this);

		eachDynamicBodyConcurrent(integrateVelocities);

		// resolve contacts
		for (int i = 0; i < contactIterations; i++) {
			eachConstraintConcurrent(processContact);
			eachDynamicBodyConcurrent(updateVelocity);
		}

		eachDynamicBodyConcurrent(integratePositions);

		contacts.clear();
		assert false;
	}
}