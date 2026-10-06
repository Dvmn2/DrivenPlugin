package net.dvmn2.drivenPlugin;

import org.bukkit.util.Vector;

/**
 * Физическое состояние одной driven-сущности.
 * <p>
 * force* — постоянная тяга (сила), применяется каждый физический тик, пока не изменена/сброшена.
 * velocity хранится в мировых (глобальных) координатах — это единственное "истинное" состояние скорости;
 * "локальная скорость" — это всегда velocity, спроецированная на локальный базис сущности в момент запроса.
 */
public final class DrivenData {

    private double mass;
    private double density;
    private Vector forceLocal;
    private Vector forceGlobal;
    private Vector velocity;

    public DrivenData(double mass, double density, Vector forceLocal, Vector forceGlobal, Vector velocity) {
        this.mass = mass;
        this.density = density;
        this.forceLocal = forceLocal;
        this.forceGlobal = forceGlobal;
        this.velocity = velocity;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) {
        this.density = density;
    }

    public Vector getForceLocal() {
        return forceLocal;
    }

    public void setForceLocal(Vector forceLocal) {
        this.forceLocal = forceLocal;
    }

    public Vector getForceGlobal() {
        return forceGlobal;
    }

    public void setForceGlobal(Vector forceGlobal) {
        this.forceGlobal = forceGlobal;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }
}
