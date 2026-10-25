package net.dvmn2.drivenplugin;

import net.dvmn2.drivenplugin.model.DrivenData;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Immutable snapshot of {@code config.yml}.
 *
 * @param language            {@code ru}, {@code en} or {@code auto}
 * @param saveIntervalSeconds seconds between writes of changed state to persistent data
 * @param defaultMass         mass of newly created entities
 * @param defaultDensity      medium density of newly created entities
 * @param tickPeriodTicks     ticks between physics steps
 * @param queryScale          multiplier for velocity results of {@code get}
 */
public record DrivenSettings(
        String language,
        int saveIntervalSeconds,
        double defaultMass,
        double defaultDensity,
        int tickPeriodTicks,
        double queryScale) {

    private static final String SETTINGS = "settings.";
    private static final String PHYSICS = "physics.";

    private static final String DEFAULT_LANGUAGE = "auto";
    private static final Set<String> LANGUAGES = Set.of("ru", "en", "auto");

    private static final int DEFAULT_SAVE_INTERVAL_SECONDS = 30;
    private static final int MIN_SAVE_INTERVAL_SECONDS = 5;
    private static final int MAX_SAVE_INTERVAL_SECONDS = 3600;

    private static final double DEFAULT_MASS = 1000.0;
    private static final double DEFAULT_DENSITY = 1.0;
    private static final double MAX_MASS_OR_DENSITY = 1.0E9;

    private static final int DEFAULT_TICK_PERIOD_TICKS = 1;
    private static final int MAX_TICK_PERIOD_TICKS = 20;

    private static final double DEFAULT_QUERY_SCALE = 20.0;
    private static final double MIN_QUERY_SCALE = 0.001;
    private static final double MAX_QUERY_SCALE = 1.0E6;

    /**
     * Reads and validates the configuration. Invalid values are logged and replaced by their defaults.
     *
     * @param config config.yml root
     * @param log    plugin logger
     * @return validated snapshot
     */
    public static DrivenSettings parse(ConfigurationSection config, Logger log) {
        String language = config.getString(SETTINGS + "language", DEFAULT_LANGUAGE).toLowerCase(Locale.ROOT);
        if (!LANGUAGES.contains(language)) {
            log.warning("settings.language must be one of ru, en, auto, got " + language
                    + "; using " + DEFAULT_LANGUAGE);
            language = DEFAULT_LANGUAGE;
        }

        return new DrivenSettings(
                language,
                readInt(config, log, SETTINGS + "save-interval-seconds", DEFAULT_SAVE_INTERVAL_SECONDS,
                        MIN_SAVE_INTERVAL_SECONDS, MAX_SAVE_INTERVAL_SECONDS),
                readDouble(config, log, PHYSICS + "default-mass", DEFAULT_MASS,
                        DrivenData.MIN_MASS, MAX_MASS_OR_DENSITY),
                readDouble(config, log, PHYSICS + "default-density", DEFAULT_DENSITY,
                        DrivenData.MIN_DENSITY, MAX_MASS_OR_DENSITY),
                readInt(config, log, PHYSICS + "tick-period-ticks", DEFAULT_TICK_PERIOD_TICKS,
                        1, MAX_TICK_PERIOD_TICKS),
                readDouble(config, log, PHYSICS + "get-scale", DEFAULT_QUERY_SCALE,
                        MIN_QUERY_SCALE, MAX_QUERY_SCALE));
    }

    private static int readInt(ConfigurationSection config, Logger log, String path, int fallback, int min, int max) {
        int value = config.getInt(path, fallback);
        if (value < min || value > max) {
            log.warning(path + " must be in " + min + ".." + max + ", got " + value + "; using " + fallback);
            return fallback;
        }
        return value;
    }

    private static double readDouble(ConfigurationSection config, Logger log, String path, double fallback,
                                     double min, double max) {
        double value = config.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < min || value > max) {
            log.warning(path + " must be in " + min + ".." + max + ", got " + value + "; using " + fallback);
            return fallback;
        }
        return value;
    }
}
