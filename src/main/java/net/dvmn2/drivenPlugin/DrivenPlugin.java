package net.dvmn2.drivenplugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.dvmn2.drivenplugin.command.DrivenCommand;
import net.dvmn2.drivenplugin.storage.DrivenDataStore;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Composition root of the Driven plugin: wires objects together and manages enabling and disabling.
 */
public final class DrivenPlugin extends JavaPlugin {

    private volatile DrivenSettings settings;
    private DrivenManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        DrivenDataStore store = new DrivenDataStore(PluginKeys.create(this), getLogger());
        manager = new DrivenManager(this, () -> settings, store);
        manager.load();

        getServer().getPluginManager().registerEvents(new DrivenListener(manager), this);

        DrivenCommand command = new DrivenCommand(this, manager, () -> settings);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.build(), "Control physics of driven entities"));

        manager.start();

        getLogger().info("Enabled; tracking " + manager.getTrackedCount() + " driven entities.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    /** Re-reads config.yml and rebuilds everything derived from it. */
    public void reloadAll() {
        reloadConfig();
        reloadSettings();
        manager.onSettingsReloaded();
    }

    private void reloadSettings() {
        settings = DrivenSettings.parse(getConfig(), getLogger());
        Lang.setLanguage(settings.language());
    }
}
