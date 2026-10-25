package net.dvmn2.drivenplugin.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Local basis of an entity without roll (roll = 0), built from its yaw and pitch.
 * <pre>
 * forward (local X) - view direction of the entity, includes yaw and pitch
 * up      (local Y) - right x forward, normalised
 * right   (local Z) - forward x worldUp, normalised
 * </pre>
 * Because "up" is derived from the tilted forward vector instead of being the world Y axis, a push along local Y
 * (for example ballast) moves the entity "up along its nose", that is diagonally when the entity is pitched.
 * <p>
 * Special case: when forward is almost vertical, "right" degenerates and the world X axis is used instead
 * (the same situation as gimbal lock).
 * <p>
 * Stateless and thread-safe.
 */
public final class LocalFrame {

    /** Squared length below which the right vector is considered degenerate. */
    private static final double DEGENERATE_THRESHOLD_SQ = 1.0E-6;

    private LocalFrame() {
    }

    /**
     * Returns the forward vector (local X axis) of a location.
     *
     * @param location entity location, its yaw and pitch are used
     * @return unit vector in world coordinates
     */
    public static Vector forward(Location location) {
        return location.getDirection();
    }

    /**
     * Returns the right vector (local Z axis) for a forward vector.
     *
     * @param forward unit forward vector
     * @return unit vector in world coordinates
     */
    public static Vector right(Vector forward) {
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < DEGENERATE_THRESHOLD_SQ) {
            return new Vector(1, 0, 0);
        }
        return right.normalize();
    }

    /**
     * Returns the up vector (local Y axis).
     *
     * @param forward unit forward vector
     * @param right   unit right vector
     * @return unit vector in world coordinates
     */
    public static Vector up(Vector forward, Vector right) {
        return right.clone().crossProduct(forward).normalize();
    }

    /**
     * Converts a vector from entity-local coordinates (x = forward, y = up, z = right) to world coordinates.
     *
     * @param location entity location
     * @param local    vector in local coordinates
     * @return new vector in world coordinates
     */
    public static Vector localToGlobal(Location location, Vector local) {
        Vector forward = forward(location);
        Vector right = right(forward);
        Vector up = up(forward, right);

        return forward.multiply(local.getX())
                .add(up.multiply(local.getY()))
                .add(right.multiply(local.getZ()));
    }

    /**
     * Converts a vector from world coordinates to entity-local coordinates (x = forward, y = up, z = right).
     *
     * @param location entity location
     * @param global   vector in world coordinates
     * @return new vector in local coordinates
     */
    public static Vector globalToLocal(Location location, Vector global) {
        Vector forward = forward(location);
        Vector right = right(forward);
        Vector up = up(forward, right);

        return new Vector(global.dot(forward), global.dot(up), global.dot(right));
    }
}
