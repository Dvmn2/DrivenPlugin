package net.dvmn2.drivenPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.util.Vector;

/**
 * Каждый физический тик:
 * 1) считает суммарную силу (глобальная тяга + локальная тяга, переведённая в мировые координаты);
 * 2) вычитает линейное сопротивление среды: F_drag = -density * velocity;
 * 3) a = F / mass; velocity += a; position += velocity;
 * 4) телепортирует сущность в новую позицию (ориентация не меняется автоматически —
 * её явно задаёт оператор командой /driven ... set rotation).
 * <p>
 * Гравитация намеренно не применяется — если нужно, чтобы driven-сущность тонула/всплывала
 * сама по себе, задайте её через постоянную глобальную силу (set force global 0 <F> 0).
 */
public final class PhysicsEngine implements Runnable {

    private final net.dvmn2.drivenPlugin.DrivenPlugin plugin;

    public PhysicsEngine(net.dvmn2.drivenPlugin.DrivenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            for (Interaction entity : world.getEntitiesByClass(Interaction.class)) {
                if (!entity.getScoreboardTags().contains(DrivenPlugin.TAG)) {
                    continue;
                }
                tick(entity);
            }
        }
    }

    private void tick(Interaction entity) {
        DrivenDataStore store = plugin.getDataStore();
        DrivenData data = store.load(entity);
        double mass = Math.max(data.getMass(), 0.0001);

        Location location = entity.getLocation();

        Vector localForceInGlobal = LocalFrame.localToGlobal(location, data.getForceLocal());
        Vector totalForce = data.getForceGlobal().clone().add(localForceInGlobal);

        Vector velocity = data.getVelocity().clone();
        Vector drag = velocity.clone().multiply(-data.getDensity());

        Vector acceleration = totalForce.clone().add(drag).multiply(1.0 / mass);
        velocity.add(acceleration);
        data.setVelocity(velocity);
        store.save(entity, data);

        if (velocity.lengthSquared() > 0.0) {
            Location newLocation = location.clone().add(velocity);
            entity.teleport(newLocation);
        }
    }
}
