package net.dvmn2.drivenPlugin;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/**
 * Читает/пишет физическое состояние driven-сущности из/в её PersistentDataContainer,
 * чтобы оно переживало перезагрузку чанка и рестарт сервера.
 */
public final class DrivenDataStore {

    private final NamespacedKey keyMass;
    private final NamespacedKey keyDensity;
    private final NamespacedKey keyForceLocalX;
    private final NamespacedKey keyForceLocalY;
    private final NamespacedKey keyForceLocalZ;
    private final NamespacedKey keyForceGlobalX;
    private final NamespacedKey keyForceGlobalY;
    private final NamespacedKey keyForceGlobalZ;
    private final NamespacedKey keyVelocityX;
    private final NamespacedKey keyVelocityY;
    private final NamespacedKey keyVelocityZ;

    private final double defaultMass;
    private final double defaultDensity;

    public DrivenDataStore(net.dvmn2.drivenPlugin.DrivenPlugin plugin) {
        this.keyMass = new NamespacedKey(plugin, "mass");
        this.keyDensity = new NamespacedKey(plugin, "density");
        this.keyForceLocalX = new NamespacedKey(plugin, "force_local_x");
        this.keyForceLocalY = new NamespacedKey(plugin, "force_local_y");
        this.keyForceLocalZ = new NamespacedKey(plugin, "force_local_z");
        this.keyForceGlobalX = new NamespacedKey(plugin, "force_global_x");
        this.keyForceGlobalY = new NamespacedKey(plugin, "force_global_y");
        this.keyForceGlobalZ = new NamespacedKey(plugin, "force_global_z");
        this.keyVelocityX = new NamespacedKey(plugin, "velocity_x");
        this.keyVelocityY = new NamespacedKey(plugin, "velocity_y");
        this.keyVelocityZ = new NamespacedKey(plugin, "velocity_z");

        this.defaultMass = plugin.getConfig().getDouble("physics.default-mass", 1000.0);
        this.defaultDensity = plugin.getConfig().getDouble("physics.default-density", 1.0);
    }

    public DrivenData load(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        double mass = pdc.getOrDefault(keyMass, PersistentDataType.DOUBLE, defaultMass);
        double density = pdc.getOrDefault(keyDensity, PersistentDataType.DOUBLE, defaultDensity);

        Vector forceLocal = new Vector(
                pdc.getOrDefault(keyForceLocalX, PersistentDataType.DOUBLE, 0.0),
                pdc.getOrDefault(keyForceLocalY, PersistentDataType.DOUBLE, 0.0),
                pdc.getOrDefault(keyForceLocalZ, PersistentDataType.DOUBLE, 0.0)
        );
        Vector forceGlobal = new Vector(
                pdc.getOrDefault(keyForceGlobalX, PersistentDataType.DOUBLE, 0.0),
                pdc.getOrDefault(keyForceGlobalY, PersistentDataType.DOUBLE, 0.0),
                pdc.getOrDefault(keyForceGlobalZ, PersistentDataType.DOUBLE, 0.0)
        );
        Vector velocity = new Vector(
                pdc.getOrDefault(keyVelocityX, PersistentDataType.DOUBLE, 0.0),
                pdc.getOrDefault(keyVelocityY, PersistentDataType.DOUBLE, 0.0),
                pdc.getOrDefault(keyVelocityZ, PersistentDataType.DOUBLE, 0.0)
        );

        return new DrivenData(mass, density, forceLocal, forceGlobal, velocity);
    }

    public void save(Entity entity, DrivenData data) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(keyMass, PersistentDataType.DOUBLE, data.getMass());
        pdc.set(keyDensity, PersistentDataType.DOUBLE, data.getDensity());

        pdc.set(keyForceLocalX, PersistentDataType.DOUBLE, data.getForceLocal().getX());
        pdc.set(keyForceLocalY, PersistentDataType.DOUBLE, data.getForceLocal().getY());
        pdc.set(keyForceLocalZ, PersistentDataType.DOUBLE, data.getForceLocal().getZ());

        pdc.set(keyForceGlobalX, PersistentDataType.DOUBLE, data.getForceGlobal().getX());
        pdc.set(keyForceGlobalY, PersistentDataType.DOUBLE, data.getForceGlobal().getY());
        pdc.set(keyForceGlobalZ, PersistentDataType.DOUBLE, data.getForceGlobal().getZ());

        pdc.set(keyVelocityX, PersistentDataType.DOUBLE, data.getVelocity().getX());
        pdc.set(keyVelocityY, PersistentDataType.DOUBLE, data.getVelocity().getY());
        pdc.set(keyVelocityZ, PersistentDataType.DOUBLE, data.getVelocity().getZ());
    }

    public double getDefaultMass() {
        return defaultMass;
    }

    public double getDefaultDensity() {
        return defaultDensity;
    }
}
