package net.dvmn2.drivenPlugin;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Локальный базис сущности без крена (roll = 0), построенный из её yaw и pitch:
 * <p>
 * forward (локальная X) — направление взгляда сущности (учитывает yaw и pitch);
 * up      (локальная Y) — forward × right, нормализованный;
 * right   (локальная Z) — worldUp × forward, нормализованный.
 * <p>
 * Благодаря тому, что "up" вычисляется из наклонённого forward, а не берётся как мировой (0,1,0),
 * толчок по локальной оси Y (например, балласт) реально толкает сущность "вверх по носу" —
 * то есть под углом, если сущность наклонена по pitch. Это и даёт движение наискосок.
 * <p>
 * Особый случай: если forward почти вертикален (взгляд строго вверх/вниз), right вырождается —
 * в этом случае берётся мировая ось X как запасной вариант (аналог gimbal lock).
 */
public final class LocalFrame {

    private static final double DEGENERATE_THRESHOLD_SQ = 1.0E-6;

    private LocalFrame() {
    }

    public static Vector forward(Location location) {
        return location.getDirection();
    }

    public static Vector right(Vector forward) {
        Vector worldUp = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(worldUp);
        if (right.lengthSquared() < DEGENERATE_THRESHOLD_SQ) {
            return new Vector(1, 0, 0);
        }
        return right.normalize();
    }

    public static Vector up(Vector forward, Vector right) {
        return right.clone().crossProduct(forward).normalize();
    }

    /**
     * Переводит вектор из локальных координат сущности (x=forward, y=up, z=right) в мировые.
     */
    public static Vector localToGlobal(Location location, Vector local) {
        Vector forward = forward(location);
        Vector right = right(forward);
        Vector up = up(forward, right);

        return forward.clone().multiply(local.getX())
                .add(up.clone().multiply(local.getY()))
                .add(right.clone().multiply(local.getZ()));
    }

    /**
     * Переводит вектор из мировых координат в локальные координаты сущности (x=forward, y=up, z=right).
     */
    public static Vector globalToLocal(Location location, Vector global) {
        Vector forward = forward(location);
        Vector right = right(forward);
        Vector up = up(forward, right);

        return new Vector(global.dot(forward), global.dot(up), global.dot(right));
    }
}