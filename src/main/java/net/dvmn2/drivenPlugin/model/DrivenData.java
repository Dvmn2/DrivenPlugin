package net.dvmn2.drivenplugin.model;

import org.bukkit.util.Vector;

/**
 * Physical state of one driven entity.
 * <p>
 * Forces are constant thrust applied on every physics step until changed or reset. Velocity is stored in world
 * coordinates, blocks per physics step; this is the only true velocity state. "Local velocity" is always the world
 * velocity projected onto the entity's local basis at the moment of the request.
 * <p>
 * Vectors are copied on the way in and on the way out, so callers cannot alias the internal state.
 * Not thread-safe; used on the server main thread only.
 */
public final class DrivenData {

    /** Smallest allowed mass; keeps the acceleration finite. */
    public static final double MIN_MASS = 0.0001;

    /** Smallest allowed medium density; a negative density would accelerate entities instead of slowing them. */
    public static final double MIN_DENSITY = 0.0;

    private double mass;
    private double density;
    private Vector forceLocal;
    private Vector forceGlobal;
    private Vector velocity;

    /**
     * Creates a state at rest.
     *
     * @param mass    initial mass
     * @param density initial medium density
     */
    public DrivenData(double mass, double density) {
        this(mass, density, new Vector(), new Vector(), new Vector());
    }

    /**
     * Creates a state with explicit vectors.
     *
     * @param mass        mass
     * @param density     medium density
     * @param forceLocal  thrust in entity-local coordinates
     * @param forceGlobal thrust in world coordinates
     * @param velocity    velocity in world coordinates, blocks per step
     */
    public DrivenData(double mass, double density, Vector forceLocal, Vector forceGlobal, Vector velocity) {
        this.mass = mass;
        this.density = density;
        this.forceLocal = forceLocal.clone();
        this.forceGlobal = forceGlobal.clone();
        this.velocity = velocity.clone();
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) {
        this.density = density;
    }

    /** Returns a copy of the local thrust. */
    public Vector getForceLocal() {
        return forceLocal.clone();
    }

    public void setForceLocal(Vector forceLocal) {
        this.forceLocal = forceLocal.clone();
    }

    /** Returns a copy of the world thrust. */
    public Vector getForceGlobal() {
        return forceGlobal.clone();
    }

    public void setForceGlobal(Vector forceGlobal) {
        this.forceGlobal = forceGlobal.clone();
    }

    /** Returns a copy of the world velocity, blocks per physics step. */
    public Vector getVelocity() {
        return velocity.clone();
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity.clone();
    }

    /**
     * Returns true if both forces and the velocity are exactly zero, so a physics step would change nothing.
     *
     * @return true if the entity has nothing to simulate
     */
    public boolean isAtRest() {
        // Exact comparison on purpose: only a state that is bit-for-bit zero is guaranteed to stay unchanged.
        return forceLocal.lengthSquared() == 0.0
                && forceGlobal.lengthSquared() == 0.0
                && velocity.lengthSquared() == 0.0;
    }

    /** Zeroes both forces and the velocity; mass and density are kept. */
    public void reset() {
        forceLocal = new Vector();
        forceGlobal = new Vector();
        velocity = new Vector();
    }
}
