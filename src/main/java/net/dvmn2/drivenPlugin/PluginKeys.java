package net.dvmn2.drivenplugin;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Persistent data keys used on driven entities.
 * <p>
 * The key names are part of the on-disk format; renaming one orphans existing entities' data.
 *
 * @param marker      byte flag set on every entity owned by the plugin
 * @param dataVersion integer version of the persisted layout
 * @param mass        double
 * @param density     double
 * @param forceLocal  doubles, entity-local thrust
 * @param forceGlobal doubles, world thrust
 * @param velocity    doubles, world velocity
 */
public record PluginKeys(
        NamespacedKey marker,
        NamespacedKey dataVersion,
        NamespacedKey mass,
        NamespacedKey density,
        VectorKeys forceLocal,
        VectorKeys forceGlobal,
        VectorKeys velocity) {

    /**
     * Keys of the three components of one stored vector.
     *
     * @param x x component key
     * @param y y component key
     * @param z z component key
     */
    public record VectorKeys(NamespacedKey x, NamespacedKey y, NamespacedKey z) {

        static VectorKeys of(Plugin plugin, String prefix) {
            return new VectorKeys(
                    new NamespacedKey(plugin, prefix + "_x"),
                    new NamespacedKey(plugin, prefix + "_y"),
                    new NamespacedKey(plugin, prefix + "_z"));
        }
    }

    /**
     * Creates all keys for a plugin.
     *
     * @param plugin owning plugin, supplies the namespace
     * @return key set
     */
    public static PluginKeys create(Plugin plugin) {
        return new PluginKeys(
                new NamespacedKey(plugin, "driven"),
                new NamespacedKey(plugin, "data_version"),
                new NamespacedKey(plugin, "mass"),
                new NamespacedKey(plugin, "density"),
                VectorKeys.of(plugin, "force_local"),
                VectorKeys.of(plugin, "force_global"),
                VectorKeys.of(plugin, "velocity"));
    }
}
