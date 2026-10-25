package net.dvmn2.drivenplugin;

import net.dvmn2.drivenplugin.model.DrivenData;
import net.dvmn2.drivenplugin.storage.DrivenDataStore;
import net.dvmn2.drivenplugin.util.LocalFrame;
import net.dvmn2.drivenplugin.util.Numbers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Owns the runtime state of all loaded driven entities and runs the physics.
 * <p>
 * Every physics step:
 * <pre>
 * F      = F_global + toWorld(F_local) - density * velocity
 * v     += F / mass
 * pos   += v
 * </pre>
 * Gravity is intentionally not applied; to make an entity sink or float, give it a constant global force.
 * Orientation never changes by itself; it is set explicitly with {@code /driven ... set rotation}.
 * <p>
 * A driven entity is an {@link Interaction} that carries the scoreboard tag {@value #TAG}. State lives in memory
 * while the entity is loaded and is written back to the entity's persistent data every
 * {@code settings.save-interval-seconds}, when its chunk unloads and on {@link #shutdown()}.
 * <p>
 * Driven entities are persistent by design: on load they are adopted into the state, not removed as orphans.
 * <p>
 * Threading: main server thread only.
 */
public final class DrivenManager {

    /** Scoreboard tag that marks an entity as driven; selectors such as {@code @e[tag=driven]} rely on it. */
    public static final String TAG = "driven";

    private static final long TICKS_PER_SECOND = 20L;

    private final Plugin plugin;
    private final Supplier<DrivenSettings> settings;
    private final DrivenDataStore store;
    private final Map<UUID, Tracked> tracked = new HashMap<>();
    /** Reused snapshot of {@link #tracked}, so a step can add or remove entries (chunk loads) while iterating. */
    private final List<Tracked> stepSnapshot = new ArrayList<>();

    private @Nullable BukkitTask physicsTask;
    private @Nullable BukkitTask saveTask;

    /**
     * Creates a manager. Call {@link #load()} and then {@link #start()}.
     *
     * @param plugin   owning plugin
     * @param settings supplier of the current settings snapshot
     * @param store    persistent state store
     */
    public DrivenManager(Plugin plugin, Supplier<DrivenSettings> settings, DrivenDataStore store) {
        this.plugin = plugin;
        this.settings = settings;
        this.store = store;
    }

    // ---------- Lifecycle ----------

    /** Adopts every driven entity that is already loaded in any world. */
    public void load() {
        for (World world : plugin.getServer().getWorlds()) {
            onEntitiesLoad(world.getEntitiesByClass(Interaction.class));
        }
    }

    /** Starts the physics and the periodic save tasks. */
    public void start() {
        scheduleTasks();
    }

    /** Cancels the tasks and writes all changed state synchronously. */
    public void shutdown() {
        cancelTasks();
        flushAll();
        tracked.clear();
    }

    /** Applies changed task periods after a configuration reload. */
    public void onSettingsReloaded() {
        cancelTasks();
        scheduleTasks();
    }

    // ---------- Entities ----------

    /**
     * Spawns a new driven entity at rest with the default mass and density.
     *
     * @param location spawn location; its world MUST NOT be {@code null}
     * @return the spawned entity
     */
    public Interaction create(Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "location world");
        DrivenSettings current = settings.get();

        Interaction entity = world.spawn(location, Interaction.class, spawned -> {
            spawned.addScoreboardTag(TAG);
            spawned.setPersistent(true);
        });

        DrivenData data = new DrivenData(current.defaultMass(), current.defaultDensity());
        store.save(entity, data);
        tracked.put(entity.getUniqueId(), new Tracked(entity, data));
        return entity;
    }

    /**
     * Returns the state of a driven entity.
     * <p>
     * An {@link Interaction} that got the tag {@value #TAG} by hand (for example with {@code /tag}) is adopted on
     * first access.
     *
     * @param entity any entity
     * @return its state, or {@code null} if the entity is not a driven entity
     */
    public @Nullable DrivenData find(Entity entity) {
        Tracked existing = tracked.get(entity.getUniqueId());
        if (existing != null) {
            return existing.data;
        }
        if (entity instanceof Interaction interaction && isDriven(interaction)) {
            return adopt(interaction).data;
        }
        return null;
    }

    /**
     * Records that the state of an entity changed and must be written at the next save.
     *
     * @param entity a driven entity
     */
    public void markDirty(Entity entity) {
        Tracked state = tracked.get(entity.getUniqueId());
        if (state != null) {
            state.dirty = true;
        }
    }

    /**
     * Stops tracking and removes a driven entity from the world.
     *
     * @param entity a driven entity
     */
    public void remove(Entity entity) {
        tracked.remove(entity.getUniqueId());
        entity.remove();
    }

    /**
     * Returns the number of currently tracked entities.
     *
     * @return tracked entity count
     */
    public int getTrackedCount() {
        return tracked.size();
    }

    // ---------- Events ----------

    /**
     * Adopts driven entities among freshly loaded entities.
     *
     * @param entities entities that were just loaded
     */
    public void onEntitiesLoad(Collection<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (entity instanceof Interaction interaction && isDriven(interaction)
                    && !tracked.containsKey(interaction.getUniqueId())) {
                adopt(interaction);
            }
        }
    }

    /**
     * Writes and forgets tracked entities that are about to be unloaded.
     *
     * @param entities entities that are being unloaded
     */
    public void onEntitiesUnload(Collection<? extends Entity> entities) {
        for (Entity entity : entities) {
            Tracked state = tracked.remove(entity.getUniqueId());
            if (state != null) {
                flush(state);
            }
        }
    }

    /**
     * Forgets an entity that was removed from the world.
     *
     * @param entity    removed entity
     * @param unloading true if the removal is an unload, in which case the state is still written
     */
    public void onEntityRemoved(Entity entity, boolean unloading) {
        Tracked state = tracked.remove(entity.getUniqueId());
        if (state != null && unloading) {
            flush(state);
        }
    }

    // ---------- Internals ----------

    private void scheduleTasks() {
        DrivenSettings current = settings.get();
        long stepPeriod = current.tickPeriodTicks();
        long savePeriod = current.saveIntervalSeconds() * TICKS_PER_SECOND;
        physicsTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::stepAll, stepPeriod, stepPeriod);
        saveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::flushAll, savePeriod, savePeriod);
    }

    private void cancelTasks() {
        if (physicsTask != null) {
            physicsTask.cancel();
            physicsTask = null;
        }
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    private static boolean isDriven(Interaction entity) {
        return entity.isValid() && entity.getScoreboardTags().contains(TAG);
    }

    private Tracked adopt(Interaction entity) {
        DrivenSettings current = settings.get();
        DrivenData data = store.load(entity, current.defaultMass(), current.defaultDensity());
        store.mark(entity);
        Tracked state = new Tracked(entity, data);
        tracked.put(entity.getUniqueId(), state);
        return state;
    }

    private void flushAll() {
        for (Tracked state : tracked.values()) {
            flush(state);
        }
    }

    private void flush(Tracked state) {
        if (!state.dirty) {
            return;
        }
        if (state.entity.isValid()) {
            store.save(state.entity, state.data);
        }
        state.dirty = false;
    }

    private void stepAll() {
        stepSnapshot.clear();
        stepSnapshot.addAll(tracked.values());
        for (Tracked state : stepSnapshot) {
            if (tracked.get(state.entity.getUniqueId()) != state) {
                continue;
            }
            if (!state.entity.isValid()) {
                tracked.remove(state.entity.getUniqueId());
                continue;
            }
            if (!state.data.isAtRest()) {
                step(state);
            }
        }
        stepSnapshot.clear();
    }

    private void step(Tracked state) {
        Interaction entity = state.entity;
        DrivenData data = state.data;
        Location location = entity.getLocation();
        double mass = Math.max(data.getMass(), DrivenData.MIN_MASS);

        Vector force = data.getForceGlobal().add(LocalFrame.localToGlobal(location, data.getForceLocal()));
        Vector velocity = data.getVelocity();
        force.subtract(velocity.clone().multiply(data.getDensity()));
        velocity.add(force.multiply(1.0 / mass));
        state.dirty = true;

        // Teleporting to a non-finite position throws, which would repeat every tick; reset the state instead.
        if (!Numbers.isFinite(velocity)) {
            data.reset();
            plugin.getLogger().warning("Driven entity " + entity.getUniqueId()
                    + " reached a non-finite velocity; force and velocity were reset.");
            return;
        }

        data.setVelocity(velocity);
        if (velocity.lengthSquared() > 0.0) {
            entity.teleport(location.add(velocity));
        }
    }

    private static final class Tracked {

        private final Interaction entity;
        private final DrivenData data;
        private boolean dirty;

        private Tracked(Interaction entity, DrivenData data) {
            this.entity = entity;
            this.data = data;
        }
    }
}
