package tintor.devel.rigidbody.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import tintor.devel.geometry.Matrix3;
import tintor.devel.geometry.PolygonVisitor;
import tintor.devel.geometry.Quaternion;
import tintor.devel.geometry.Transform3;
import tintor.devel.geometry.Vector;

public final class Body {
	/** mass of body [kg] */
	public final float mass;
	/** inverted mass of body [1/kg] */
	public final float imass;

	private final Matrix3 Ibody;
	private Matrix3 I;
	private Matrix3 invI;
	private Transform3 transform;

	// Misc fields
	public final Shape shape;
	public String name;

	// State variables
	private Vector linPos = Vector.Zero; // position
	private Quaternion angPos = Quaternion.Identity;
	private Vector linVel = Vector.Zero, angVel = Vector.Zero;
	private Vector biasLinVel = Vector.Zero, biasAngVel = Vector.Zero;

	// Externally computed quantities
	private Vector force = Vector.Zero, torque = Vector.Zero;

	// Material properties
	public float elasticity = 0.25f;
	public float drag = 0.003f;
	public float sfriction = 0.25f, dfriction = 0.2f;

	// Constructor
	public Body(final Vector position, final Quaternion orientation, final Shape shape, final float density) {
		if (!position.isFinite()) throw new IllegalArgumentException("position");
		if (!orientation.isFinite()) throw new IllegalArgumentException("orientation");
		if (shape == null) throw new NullPointerException("shape");
		if (density <= 0) throw new IllegalArgumentException("density");

		// position
		linPos = position;
		angPos = orientation;

		// shape
		this.shape = shape;

		// init mass
		final float volume = Math.abs((Float) shape.each(PolygonVisitor.signedVolume()));
		mass = density * volume;
		imass = 1 / mass;

		// init body inertial moment
		final Matrix3 inertiaTensor = (Matrix3) shape.each(PolygonVisitor.inertiaTensor());
		Ibody = inertiaTensor.mul(density);
		updateTransform();

		// TODO check invariants
	}

	public float kinetic() {
		return (mass * linVel.square() + angVel.dot(I.mul(angVel))) / 2;
	}

	public void integrateVel(final float dt) {
		// integrate
		linVel = linVel.add(dt * imass, force);
		angVel = angVel.add(dt, invI.mul(torque.sub(angVel.cross(I.mul(angVel)))));

		// reset accumulators
		force = torque = Vector.Zero;
	}

	public void integratePos(final float dt) {
		// integrate
		linPos = linPos.add(dt, linVel.add(biasLinVel));
		// dq/dt = w*q/2 => q' = q + (w*q)*(dt/2)
		angPos = angPos.add(angVel.add(biasAngVel).mul(angPos).mul(dt / 2)).unit();

		// reset bias velocities
		biasLinVel = biasAngVel = Vector.Zero;

		// update matrices
		updateTransform();
	}

	private void updateTransform() {
		transform = Transform3.create(angPos, linPos);
		I = transform.rotation.mul(Ibody).mul(transform.rotation.transpose());
		invI = I.inv();
	}

	// public void advanceTransforms(final float dt) {
	// transform = new Transform3(angPos.add(angVel.mul(angPos).mul(dt / 2)), linPos.add(dt, linVel));
	// }

	public void addForce(final Vector f) {
		assert force.isFinite() : force;
		assert f.isFinite() : f;
		force = force.add(f);
		assert force.isFinite();
	}

	public void addTorque(final Vector t) {
		assert torque.isFinite() : torque;
		assert t.isFinite() : t;
		torque = torque.add(t);
		assert torque.isFinite() : torque;
	}

	public void addLinAcc(final Vector acc) {
		assert force.isFinite() : force;
		assert acc.isFinite() : acc;
		force = force.add(mass, acc);
		assert force.isFinite() : force;
	}

	private final AtomicReference<Vector> linImpulse = new AtomicReference<Vector>();
	private final AtomicReference<Vector> angImpulse = new AtomicReference<Vector>();
	private final AtomicInteger impulses = new AtomicInteger();

	private static void atomicAdd(final AtomicReference<Vector> a, final Vector b) {
		while (true) {
			final Vector v = a.get();
			if (a.compareAndSet(v, v.add(b))) break;
		}
	}

	private static void atomicSub(final AtomicReference<Vector> a, final Vector b) {
		while (true) {
			final Vector v = a.get();
			if (a.compareAndSet(v, v.sub(b))) break;
		}
	}

	/**
	 * impulse is transfered from B to A ra and rb = point - body.position
	 */
	public static void transferConcurrentImpulse(final Vector impulse, final Body bodyA, final Body bodyB,
			final Vector ra, final Vector rb) {
		assert impulse.isFinite() : impulse;
		assert impulse.isFinite() : impulse;
		assert ra.cross(impulse).isFinite() : ra + " " + impulse;
		assert rb.cross(impulse).isFinite();

		atomicAdd(bodyA.linImpulse, impulse);
		atomicAdd(bodyA.angImpulse, ra.cross(impulse));
		bodyA.impulses.incrementAndGet();

		atomicSub(bodyB.linImpulse, impulse);
		atomicSub(bodyB.angImpulse, rb.cross(impulse));
		bodyB.impulses.incrementAndGet();
	}

	public void applyConcurrentImpulses() {
		if (impulses.get() > 0) {
			final float d = 1 / impulses.get();
			linVel = linVel.add(imass * d, linImpulse.get());
			angVel = angVel.add(invI, angImpulse.get(), d);
			linImpulse.set(Vector.Zero);
			angImpulse.set(Vector.Zero);
			impulses.set(0);
		}
	}

	/**
	 * impulse is transfered from B to A ra and rb = point - body.position
	 */
	public static void transferImpulse(final Vector impulse, final Body bodyA, final Body bodyB, final Vector ra,
			final Vector rb) {
		assert impulse.isFinite() : impulse;
		assert impulse.isFinite() : impulse;
		assert ra.cross(impulse).isFinite() : ra + " " + impulse;
		assert rb.cross(impulse).isFinite();

		bodyA.linVel = bodyA.linVel.add(bodyA.imass, impulse);
		bodyA.angVel = bodyA.angVel.add(bodyA.invI, ra.cross(impulse));
		bodyB.linVel = bodyB.linVel.sub(bodyB.imass, impulse);
		bodyB.angVel = bodyB.angVel.sub(bodyB.invI, rb.cross(impulse));

		// TODO check post conditions
	}

	public static void transferBiasImpulse(final Vector impulse, final Body bodyA, final Body bodyB,
			final Vector ra, final Vector rb) {
		assert impulse.isFinite();
		assert ra.cross(impulse).isFinite() : ra + " " + impulse;
		assert rb.cross(impulse).isFinite();

		bodyA.biasLinVel = bodyA.biasLinVel.add(bodyA.imass, impulse);
		bodyA.biasAngVel = bodyA.biasAngVel.add(bodyA.invI, ra.cross(impulse));
		bodyB.biasLinVel = bodyB.biasLinVel.sub(bodyB.imass, impulse);
		bodyB.biasAngVel = bodyB.biasAngVel.sub(bodyB.invI, rb.cross(impulse));

		// TODO check post conditions
	}

	/** @return Total absolute velocity at point relative to the center of mass. (r = point - position) */
	public Vector velAt(final Vector r) {
		// assert r.isFinite();
		return linVelocity().add(angVelocity().cross(r));
	}

	public Vector biasVelAt(final Vector r) {
		assert r.isFinite();
		return biasLinVel.add(biasAngVel.cross(r));
	}

	public Matrix3 imassAt(final Vector r) {
		final Matrix3 rt = r.tilda();
		// NOTE this can be simplified for special bodies
		return Matrix3.diagonal(imass).sub(rt.mul(invI).mul(rt));
	}

	public static Matrix3 imassAt(final Body bodyA, final Body bodyB, final Vector ra, final Vector rb) {
		final Matrix3 rat = ra.tilda(), rbt = rb.tilda();
		final Matrix3 Ma = rat.mul(bodyA.invI).mul(rat);
		final Matrix3 Mb = rbt.mul(bodyB.invI).mul(rbt);
		return Matrix3.diagonal(bodyA.imass + bodyB.imass).sub(Ma).sub(Mb);
	}

	// Getters/Setters
	public Vector position() {
		return linPos;
	}

	public Quaternion orientation() {
		return angPos;
	}

	public Vector linVelocity() {
		return linVel;
	}

	public Vector angVelocity() {
		return angVel;
	}

	public void setLinVelocity(final Vector a) {
		linVel = a;
	}

	public void setAngVelocity(final Vector a) {
		angVel = a;
	}

	public Transform3 transform() {
		return transform;
	}

	public Matrix3 invI() {
		return invI;
	}

	private static final AtomicInteger lastID = new AtomicInteger();
	public int id = lastID.incrementAndGet(); // NOTE can wrap

	// From Object
	@Override
	public String toString() {
		return name != null ? name : super.toString();
	}
}