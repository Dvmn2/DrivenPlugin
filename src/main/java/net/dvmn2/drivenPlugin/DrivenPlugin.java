package net.dvmn2.drivenPlugin;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.dvmn2.drivenPlugin.command.DrivenCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class DrivenPlugin extends JavaPlugin {

    /**
     * Тег, по которому плагин определяет "свои" сущности: /driven <selector> ...
     * работает только с сущностями, у которых есть этот scoreboard-тег.
     */
    public static final String TAG = "driven";

    private DrivenDataStore dataStore;
    private int physicsTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Lang.setLanguage(getConfig().getString("settings.language", "auto"));

        this.dataStore = new DrivenDataStore(this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(
                    DrivenCommand.build(this),
                    "Control physics of driven entities",
                    List.of()
            );
        });

        int period = Math.max(1, getConfig().getInt("physics.tick-period", 1));
        PhysicsEngine physicsEngine = new PhysicsEngine(this);
        this.physicsTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, physicsEngine, period, period);

        getLogger().info("Driven enabled.");
    }

    @Override
    public void onDisable() {
        if (physicsTaskId != -1) {
            getServer().getScheduler().cancelTask(physicsTaskId);
        }
    }

    public DrivenDataStore getDataStore() {
        return dataStore;
    }
}
