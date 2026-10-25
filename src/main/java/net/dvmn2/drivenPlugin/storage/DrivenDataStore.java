package net.dvmn2.drivenplugin.storage;

import net.dvmn2.drivenplugin.PluginKeys;
import net.dvmn2.drivenplugin.model.DrivenData;
import net.dvmn2.drivenplugin.util.Numbers;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reads and writes the state of a driven entity from and to its {@code PersistentDataContainer}, so the state
 * survives chunk unloads and server restarts.
 * <p>
 * Layout, data version {@value #DATA_VERSION}: doubles {@code mass}, {@code density}, {@code force_local_x|y|z},
 * {@code force_global_x|y|z}, {@code velocity_x|y|z}, a byte marker {@code driven} and an integer
 * {@code data_version}. Missing values get defaults; corrupt values (non-finite or out of range) are replaced by
 * defaults with a warning. Entities written before {@code data_version} existed use the same layout.
 * <p>
 * Main thread only: persistent data belongs to a live entity.
 */
public final class DrivenDataStore {

    /** Version of the persisted layout. */
    public static final int DATA_VERSION = 1;

    private static final byte MARKER_VALUE = 1;

    private final PluginKeys keys;
    private final Logger log;

    /**
     * Creates a store.
     *
     * @param keys persistent data keys
     * @param log  plugin logger
     */
    public DrivenDataStore(PluginKeys keys, Logger log) {
        this.keys = keys;
        this.log = log;
    }

    /**
     * Reads the state of an entity.
     *
     * @param entity         entity to read
     * @param defaultMass    mass used when none is stored or the stored one is invalid
     * @param defaultDensity density used when none is stored or the stored one is invalid
     * @return state; never {@code null}
     */
    public DrivenData load(Entity entity, double defaultMass, double defaultDensity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        UUID id = entity.getUniqueId();

        double mass = readScalar(pdc, keys.mass(), defaultMass, DrivenData.MIN_MASS, "mass", id);
        double density = readScalar(pdc, keys.density(), defaultDensity, DrivenData.MIN_DENSITY, "density", id);

        return new DrivenData(
                mass,
                density,
                readVector(pdc, keys.forceLocal(), "force_local", id),
                readVector(pdc, keys.forceGlobal(), "force_global", id),
                readVector(pdc, keys.velocity(), "velocity", id));
    }

    /**
     * Writes the state of an entity and marks it as owned by the plugin.
     *
     * @param entity entity to write
     * @param data   state to store
     */
    public void save(Entity entity, DrivenData data) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        mark(entity);
        pdc.set(keys.mass(), PersistentDataType.DOUBLE, data.getMass());
        pdc.set(keys.density(), PersistentDataType.DOUBLE, data.getDensity());
        writeVector(pdc, keys.forceLocal(), data.getForceLocal());
        writeVector(pdc, keys.forceGlobal(), data.getForceGlobal());
        writeVector(pdc, keys.velocity(), data.getVelocity());
    }

    /**
     * Marks an entity as owned by the plugin without touching its physical state.
     *
     * @param entity entity to mark
     */
    public void mark(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(keys.marker(), PersistentDataType.BYTE, MARKER_VALUE);
        pdc.set(keys.dataVersion(), PersistentDataType.INTEGER, DATA_VERSION);
    }

    private double readScalar(PersistentDataContainer pdc, NamespacedKey key, double fallback, double min,
                              String name, UUID id) {
        double value = pdc.getOrDefault(key, PersistentDataType.DOUBLE, fallback);
        if (!Double.isFinite(value) || value < min) {
            log.warning("Entity " + id + " has invalid " + name + " " + value + "; using " + fallback);
            return fallback;
        }
        return value;
    }

    private Vector readVector(PersistentDataContainer pdc, PluginKeys.VectorKeys vectorKeys, String name, UUID id) {
        Vector vector = new Vector(
                pdc.getOrDefault(vectorKeys.x(), PersistentDataType.DOUBLE, 0.0),
                pdc.getOrDefault(vectorKeys.y(), PersistentDataType.DOUBLE, 0.0),
                pdc.getOrDefault(vectorKeys.z(), PersistentDataType.DOUBLE, 0.0));
        if (!Numbers.isFinite(vector)) {
            log.warning("Entity " + id + " has a non-finite " + name + "; using zero");
            return new Vector();
        }
        return vector;
    }

    private static void writeVector(PersistentDataContainer pdc, PluginKeys.VectorKeys vectorKeys, Vector vector) {
        pdc.set(vectorKeys.x(), PersistentDataType.DOUBLE, vector.getX());
        pdc.set(vectorKeys.y(), PersistentDataType.DOUBLE, vector.getY());
        pdc.set(vectorKeys.z(), PersistentDataType.DOUBLE, vector.getZ());
    }
}
