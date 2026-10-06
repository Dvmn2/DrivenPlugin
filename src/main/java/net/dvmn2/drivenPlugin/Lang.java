package net.dvmn2.drivenPlugin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Простая система локализации плагина: русский (ru) и английский (en).
 * <p>
 * Язык задаётся в config.yml (settings.language):
 * - "ru"   — всегда русский;
 * - "en"   — всегда английский;
 * - "auto" (по умолчанию) — язык определяется индивидуально для каждого
 * отправителя команды: для игрока берётся его игровая локаль
 * (Player#locale()), для консоли/командных блоков — английский.
 * <p>
 * Simple RU/EN localization for the plugin.
 * Language is configured via config.yml (settings.language):
 * - "ru"   — always Russian;
 * - "en"   — always English;
 * - "auto" (default) — resolved per command sender: for a player, use their
 * client locale (Player#locale()); for console/command blocks,
 * fall back to English.
 */
public final class Lang {

    /**
     * Ключи всех локализуемых сообщений плагина. / Keys for all localizable plugin messages.
     */
    public enum Key {
        NO_PERMISSION,
        MUST_TARGET_ONE,
        ONLY_PLAYER_OR_BLOCK,
        CREATED,
        REMOVED,
        MASS_SET,
        DENSITY_SET,
        ROTATION_SET,
        YAW_SET,
        PITCH_SET,
        YAW_ADD,
        PITCH_ADD,
        FORCE_LOCAL_SET,
        FORCE_GLOBAL_SET,
        VELOCITY_LOCAL_SET,
        VELOCITY_GLOBAL_SET,
        FORCE_LOCAL_DIRECTION_SET,
        FORCE_GLOBAL_DIRECTION_SET,
        VELOCITY_LOCAL_DIRECTION_SET,
        VELOCITY_GLOBAL_DIRECTION_SET,
        MASS_ADD,
        DENSITY_ADD,
        ROTATION_ADD,
        FORCE_LOCAL_ADD,
        FORCE_GLOBAL_ADD,
        VELOCITY_LOCAL_ADD,
        VELOCITY_GLOBAL_ADD,
        WRONG_ADD_MASS,
        WRONG_ADD_DENSITY,
        WRONG_ADD_PITCH,
        STOPPED,
        VALUE_RESULT
    }

    private static final Map<Key, String> RU = new EnumMap<>(Key.class);
    private static final Map<Key, String> EN = new EnumMap<>(Key.class);

    static {
        RU.put(Key.NO_PERMISSION, "§cУ вас нет прав на использование этой команды.");
        RU.put(Key.MUST_TARGET_ONE, "§cЭта команда требует ровно одну driven-сущность в качестве цели (найдено: %d).");
        RU.put(Key.ONLY_PLAYER_OR_BLOCK, "§cЭту команду может выполнить только тот, у кого есть положение в мире (игрок или командный блок).");
        RU.put(Key.CREATED, "§aСоздана driven-сущность (UUID: %s).");
        RU.put(Key.REMOVED, "§aУдалено driven-сущностей: %d.");
        RU.put(Key.MASS_SET, "§aМасса установлена: %.3f (затронуто сущностей: %d).");
        RU.put(Key.DENSITY_SET, "§aПлотность среды установлена: %.3f (затронуто сущностей: %d).");
        RU.put(Key.ROTATION_SET, "§aПоворот установлен: yaw=%.2f, pitch=%.2f (затронуто сущностей: %d).");
        RU.put(Key.YAW_SET, "§aКурс установлен: yaw=%.2f (затронуто сущностей: %d).");
        RU.put(Key.PITCH_SET, "§aНаклон установлен: pitch=%.2f (затронуто сущностей: %d).");
        RU.put(Key.YAW_ADD, "§aКурс увеличен: yaw=%.2f (затронуто сущностей: %d).");
        RU.put(Key.PITCH_ADD, "§aНаклон увеличен: pitch=%.2f (затронуто сущностей: %d).");
        RU.put(Key.FORCE_LOCAL_SET, "§aЛокальная сила установлена: (%.3f, %.3f, %.3f) (затронуто сущностей: %d).");
        RU.put(Key.FORCE_GLOBAL_SET, "§aГлобальная сила установлена: (%.3f, %.3f, %.3f) (затронуто сущностей: %d).");
        RU.put(Key.VELOCITY_LOCAL_SET, "§aЛокальная скорость установлена: (%.3f, %.3f, %.3f) (затронуто сущностей: %d).");
        RU.put(Key.VELOCITY_GLOBAL_SET, "§aГлобальная скорость установлена: (%.3f, %.3f, %.3f) (затронуто сущностей: %d).");
        RU.put(Key.FORCE_LOCAL_DIRECTION_SET, "§aЛокальная сила в направлении установлена: %.3f (затронуто сущностей: %d).");
        RU.put(Key.FORCE_GLOBAL_DIRECTION_SET, "§aГлобальная сила в направлении установлена: %.3f (затронуто сущностей: %d).");
        RU.put(Key.VELOCITY_LOCAL_DIRECTION_SET, "§aЛокальная скорость в направлении установлена: %.3f (затронуто сущностей: %d).");
        RU.put(Key.VELOCITY_GLOBAL_DIRECTION_SET, "§aГлобальная скорость в направлении установлена: %.3f (затронуто сущностей: %d).");
        RU.put(Key.MASS_ADD, "§aМасса добавлена: %.3f (затронуто сущностей: %d).");
        RU.put(Key.DENSITY_ADD, "§aПлотность среды добавлена: %.3f (затронуто сущностей: %d).");
        RU.put(Key.ROTATION_ADD, "§aПоворот добавлен: yaw=%.2f, pitch=%.2f (затронуто сущностей: %d).");
        RU.put(Key.FORCE_LOCAL_ADD, "§aЛокальная сила добавлена: (%.3f, %.3f, %.3f) (затронуто сущностей: %d).");
        RU.put(Key.FORCE_GLOBAL_ADD, "§aГлобальная сила добавлена: (%.3f, %.3f, %.3f) (затронуто сущностей: %d).");
        RU.put(Key.VELOCITY_LOCAL_ADD, "§aЛокальная скорость добавлена: (%.3f, %.3f, %.3f) (затронуто сущностей: %d).");
        RU.put(Key.VELOCITY_GLOBAL_ADD, "§aГлобальная скорость добавлена: (%.3f, %.3f, %.3f) (затронуто сущностей: %d).");
        RU.put(Key.WRONG_ADD_MASS, "§c(UUID: %s) Неправильная масса: %.3f + %.3f = %.3f");
        RU.put(Key.WRONG_ADD_DENSITY, "§c(UUID: %s) Неправильная плотность: %.3f + %.3f = %.3f");
        RU.put(Key.WRONG_ADD_PITCH, "§c(UUID: %s) Неправильный pitch: %.3f + %.3f = %.3f");
        RU.put(Key.STOPPED, "§aСила и скорость сброшены (затронуто сущностей: %d).");
        RU.put(Key.VALUE_RESULT, "§7Значение: %d");

        EN.put(Key.NO_PERMISSION, "§cYou don't have permission to use this command.");
        EN.put(Key.MUST_TARGET_ONE, "§cThis command requires exactly one driven entity as a target (found: %d).");
        EN.put(Key.ONLY_PLAYER_OR_BLOCK, "§cThis command can only be run by something with a world position (a player or a command block).");
        EN.put(Key.CREATED, "§aCreated a driven entity (UUID: %s).");
        EN.put(Key.REMOVED, "§aRemoved driven entities: %d.");
        EN.put(Key.MASS_SET, "§aMass set to: %.3f (entities affected: %d).");
        EN.put(Key.DENSITY_SET, "§aEnvironment density set to: %.3f (entities affected: %d).");
        EN.put(Key.ROTATION_SET, "§aRotation set: yaw=%.2f, pitch=%.2f (entities affected: %d).");
        RU.put(Key.YAW_SET, "§aYaw set: %.2f (entities affected: %d).");
        RU.put(Key.PITCH_SET, "§aPitch set: %.2f (entities affected: %d).");
        RU.put(Key.YAW_ADD, "§aYaw increased: %.2f (entities affected: %d).");
        RU.put(Key.PITCH_ADD, "§aPitch increased: %.2f (entities affected: %d).");
        EN.put(Key.FORCE_LOCAL_SET, "§aLocal force set to: (%.3f, %.3f, %.3f) (entities affected: %d).");
        EN.put(Key.FORCE_GLOBAL_SET, "§aGlobal force set to: (%.3f, %.3f, %.3f) (entities affected: %d).");
        EN.put(Key.VELOCITY_LOCAL_SET, "§aLocal velocity set to: (%.3f, %.3f, %.3f) (entities affected: %d).");
        EN.put(Key.VELOCITY_GLOBAL_SET, "§aGlobal velocity set to: (%.3f, %.3f, %.3f) (entities affected: %d).");
        EN.put(Key.FORCE_LOCAL_DIRECTION_SET, "§aLocal force in direction set to: %.3f (entities affected: %d).");
        EN.put(Key.FORCE_GLOBAL_DIRECTION_SET, "§aGlobal force in direction set to: %.3f (entities affected: %d).");
        EN.put(Key.VELOCITY_LOCAL_DIRECTION_SET, "§aLocal velocity in direction set to: %.3f (entities affected: %d).");
        EN.put(Key.VELOCITY_GLOBAL_DIRECTION_SET, "§aGlobal velocity in direction set to: %.3f (entities affected: %d).");
        EN.put(Key.MASS_ADD, "§aMass added: %.3f (entities affected: %d).");
        EN.put(Key.DENSITY_ADD, "§aEnvironment density added: %.3f (entities affected: %d).");
        EN.put(Key.ROTATION_ADD, "§aRotation added: yaw=%.2f, pitch=%.2f (entities affected: %d).");
        EN.put(Key.FORCE_LOCAL_ADD, "§aLocal force added: (%.3f, %.3f, %.3f) (entities affected: %d).");
        EN.put(Key.FORCE_GLOBAL_ADD, "§aGlobal force added: (%.3f, %.3f, %.3f) (entities affected: %d).");
        EN.put(Key.VELOCITY_LOCAL_ADD, "§aLocal velocity added: (%.3f, %.3f, %.3f) (entities affected: %d).");
        EN.put(Key.VELOCITY_GLOBAL_ADD, "§aGlobal velocity added: (%.3f, %.3f, %.3f) (entities affected: %d).");
        EN.put(Key.WRONG_ADD_MASS, "§c(UUID: %s) Wrong mass: %.3f + %.3f = %.3f");
        EN.put(Key.WRONG_ADD_DENSITY, "§c(UUID: %s) Wrong density: %.3f + %.3f = %.3f");
        EN.put(Key.WRONG_ADD_PITCH, "§c(UUID: %s) Wrong pitch: %.3f + %.3f = %.3f");
        EN.put(Key.STOPPED, "§aForce and velocity reset (entities affected: %d).");
        EN.put(Key.VALUE_RESULT, "§7Value: %d");
    }

    /**
     * "ru", "en" или "auto" — значение из config.yml. / "ru", "en" or "auto" from config.yml.
     */
    private static volatile String configuredLanguage = "auto";

    private Lang() {
    }

    /**
     * Устанавливает язык плагина из конфига.
     * Setting the plugin language from the config.
     */
    public static void setLanguage(String language) {
        if (language == null || language.isBlank()) {
            configuredLanguage = "auto";
            return;
        }
        configuredLanguage = language.toLowerCase(Locale.ROOT);
    }

    /**
     * Возвращает локализованный шаблон сообщения для конкретного отправителя команды.
     * Returns the localized message template for a specific command sender.
     */
    public static String get(Key key, CommandSender sender) {
        Map<Key, String> table = resolveTable(sender);
        return table.getOrDefault(key, RU.get(key));
    }

    /**
     * Возвращает локализованное и отформатированное сообщение (String.format) для отправителя.
     * Returns the localized, formatted (String.format) message for a sender.
     */
    public static String get(Key key, CommandSender sender, Object... args) {
        String template = get(key, sender);
        return String.format(Locale.ROOT, template, args);
    }

    private static Map<Key, String> resolveTable(CommandSender sender) {
        return switch (configuredLanguage) {
            case "en" -> EN;
            case "ru" -> RU;
            default -> autoResolve(sender); // "auto" или некорректное значение
        };
    }

    private static Map<Key, String> autoResolve(CommandSender sender) {
        if (sender instanceof Player player) {
            // Player#locale() отдаёт java.util.Locale игрока, выставленную в настройках клиента.
            // Player#locale() returns the client-configured java.util.Locale.
            String langCode = player.locale().getLanguage();
            return "ru".equalsIgnoreCase(langCode) ? RU : EN;
        }
        // Консоль/командные блоки — по умолчанию английский.
        // Console/command blocks — default to English.
        return EN;
    }
}
