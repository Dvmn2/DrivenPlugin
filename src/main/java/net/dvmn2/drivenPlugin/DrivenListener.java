package net.dvmn2.drivenplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

/**
 * Keeps {@link DrivenManager} in sync with entities loading, unloading and being removed.
 * <p>
 * Runs on the main thread.
 */
public final class DrivenListener implements Listener {

    private final DrivenManager manager;

    /**
     * Creates the listener.
     *
     * @param manager manager to notify
     */
    public DrivenListener(DrivenManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        manager.onEntitiesLoad(event.getEntities());
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        manager.onEntitiesUnload(event.getEntities());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        manager.onEntityRemoved(event.getEntity(), event.getCause() == EntityRemoveEvent.Cause.UNLOAD);
    }
}
