package net.dvmn2.drivenplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Localisation of all player-visible texts (Russian and English, Adventure MiniMessage).
 * <p>
 * The language comes from {@code settings.language}: {@code ru} and {@code en} are fixed; {@code auto} uses the
 * client locale of a player and English for the console and command blocks.
 * <p>
 * Dynamic values MUST be passed as {@link TagResolver}s, never concatenated into a template. This is the only
 * class of the plugin that holds static mutable state (the current language, {@code volatile}).
 */
public final class Lang {

    /** All message keys. Every key has a Russian and an English text; completeness is checked on class load. */
    public enum Key {
        NO_WORLD_POSITION,
        SELECTOR_FAILED,
        MUST_TARGET_ONE,
        RELOADED,
        CREATED,
        REMOVED,
        STOPPED,
        VALUE_RESULT,
        MASS_SET,
        MASS_ADD,
        DENSITY_SET,
        DENSITY_ADD,
        ROTATION_SET,
        ROTATION_ADD,
        YAW_SET,
        YAW_ADD,
        PITCH_SET,
        PITCH_ADD,
        VECTOR_SET,
        VECTOR_ADD,
        AXIS_SET,
        AXIS_ADD,
        WRONG_MASS,
        WRONG_DENSITY,
        WRONG_PITCH,
        FRAME_LOCAL,
        FRAME_GLOBAL,
        QUANTITY_FORCE,
        QUANTITY_VELOCITY
    }

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Map<Key, String> RU = new EnumMap<>(Key.class);
    private static final Map<Key, String> EN = new EnumMap<>(Key.class);

    private static volatile String language = "auto";

    static {
        put(Key.NO_WORLD_POSITION,
                "<red>Эту команду может выполнить только тот, у кого есть положение в мире "
                        + "(игрок или командный блок).",
                "<red>This command can only be run by something with a world position "
                        + "(a player or a command block).");
        put(Key.SELECTOR_FAILED,
                "<red>Не удалось разобрать селектор: <reason>",
                "<red>Could not resolve the selector: <reason>");
        put(Key.MUST_TARGET_ONE,
                "<red>Эта команда требует ровно одну driven-сущность в качестве цели (найдено: <count>).",
                "<red>This command requires exactly one driven entity as a target (found: <count>).");
        put(Key.RELOADED,
                "<green>Конфигурация перезагружена.",
                "<green>Configuration reloaded.");
        put(Key.CREATED,
                "<green>Создана driven-сущность (UUID: <uuid>).",
                "<green>Created a driven entity (UUID: <uuid>).");
        put(Key.REMOVED,
                "<green>Удалено driven-сущностей: <count>.",
                "<green>Removed driven entities: <count>.");
        put(Key.STOPPED,
                "<green>Сила и скорость сброшены (затронуто сущностей: <count>).",
                "<green>Force and velocity reset (entities affected: <count>).");
        put(Key.VALUE_RESULT,
                "<gray>Значение: <value>",
                "<gray>Value: <value>");
        put(Key.MASS_SET,
                "<green>Масса установлена: <value> (затронуто сущностей: <count>).",
                "<green>Mass set to: <value> (entities affected: <count>).");
        put(Key.MASS_ADD,
                "<green>Масса добавлена: <value> (затронуто сущностей: <count>).",
                "<green>Mass added: <value> (entities affected: <count>).");
        put(Key.DENSITY_SET,
                "<green>Плотность среды установлена: <value> (затронуто сущностей: <count>).",
                "<green>Environment density set to: <value> (entities affected: <count>).");
        put(Key.DENSITY_ADD,
                "<green>Плотность среды добавлена: <value> (затронуто сущностей: <count>).",
                "<green>Environment density added: <value> (entities affected: <count>).");
        put(Key.ROTATION_SET,
                "<green>Поворот установлен: yaw=<yaw>, pitch=<pitch> (затронуто сущностей: <count>).",
                "<green>Rotation set: yaw=<yaw>, pitch=<pitch> (entities affected: <count>).");
        put(Key.ROTATION_ADD,
                "<green>Поворот добавлен: yaw=<yaw>, pitch=<pitch> (затронуто сущностей: <count>).",
                "<green>Rotation added: yaw=<yaw>, pitch=<pitch> (entities affected: <count>).");
        put(Key.YAW_SET,
                "<green>Курс установлен: yaw=<value> (затронуто сущностей: <count>).",
                "<green>Yaw set: <value> (entities affected: <count>).");
        put(Key.YAW_ADD,
                "<green>Курс увеличен: yaw=<value> (затронуто сущностей: <count>).",
                "<green>Yaw increased: <value> (entities affected: <count>).");
        put(Key.PITCH_SET,
                "<green>Наклон установлен: pitch=<value> (затронуто сущностей: <count>).",
                "<green>Pitch set: <value> (entities affected: <count>).");
        put(Key.PITCH_ADD,
                "<green>Наклон увеличен: pitch=<value> (затронуто сущностей: <count>).",
                "<green>Pitch increased: <value> (entities affected: <count>).");
        put(Key.VECTOR_SET,
                "<green><frame> <quantity> установлена: (<x>, <y>, <z>) (затронуто сущностей: <count>).",
                "<green><frame> <quantity> set to: (<x>, <y>, <z>) (entities affected: <count>).");
        put(Key.VECTOR_ADD,
                "<green><frame> <quantity> добавлена: (<x>, <y>, <z>) (затронуто сущностей: <count>).",
                "<green><frame> <quantity> added: (<x>, <y>, <z>) (entities affected: <count>).");
        put(Key.AXIS_SET,
                "<green><frame> <quantity> по оси <axis> установлена: <value> (затронуто сущностей: <count>).",
                "<green><frame> <quantity> on axis <axis> set to: <value> (entities affected: <count>).");
        put(Key.AXIS_ADD,
                "<green><frame> <quantity> по оси <axis> добавлена: <value> (затронуто сущностей: <count>).",
                "<green><frame> <quantity> on axis <axis> added: <value> (entities affected: <count>).");
        put(Key.WRONG_MASS,
                "<red>(UUID: <uuid>) Неправильная масса: <old> + <delta> = <result>",
                "<red>(UUID: <uuid>) Wrong mass: <old> + <delta> = <result>");
        put(Key.WRONG_DENSITY,
                "<red>(UUID: <uuid>) Неправильная плотность: <old> + <delta> = <result>",
                "<red>(UUID: <uuid>) Wrong density: <old> + <delta> = <result>");
        put(Key.WRONG_PITCH,
                "<red>(UUID: <uuid>) Неправильный pitch: <old> + <delta> = <result>",
                "<red>(UUID: <uuid>) Wrong pitch: <old> + <delta> = <result>");
        // The four fragments below are inserted into VECTOR_* and AXIS_* messages; both Russian nouns are feminine.
        put(Key.FRAME_LOCAL, "Локальная", "Local");
        put(Key.FRAME_GLOBAL, "Глобальная", "Global");
        put(Key.QUANTITY_FORCE, "сила", "force");
        put(Key.QUANTITY_VELOCITY, "скорость", "velocity");
        verify();
    }

    private Lang() {
    }

    /**
     * Sets the configured language.
     *
     * @param value {@code ru}, {@code en} or {@code auto}; anything else behaves like {@code auto}
     */
    public static void setLanguage(String value) {
        language = value == null || value.isBlank() ? "auto" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Renders a message for a recipient.
     *
     * @param key       message key
     * @param sender    recipient, selects the language in {@code auto} mode
     * @param resolvers placeholder values
     * @return rendered component
     */
    public static Component get(Key key, CommandSender sender, TagResolver... resolvers) {
        return MM.deserialize(table(sender).get(key), resolvers);
    }

    /**
     * Returns the untouched template of a key, for short fragments that are passed as placeholder values.
     *
     * @param key    message key
     * @param sender recipient, selects the language in {@code auto} mode
     * @return template text
     */
    public static String raw(Key key, CommandSender sender) {
        return table(sender).get(key);
    }

    private static void put(Key key, String ru, String en) {
        RU.put(key, ru);
        EN.put(key, en);
    }

    private static void verify() {
        for (Key key : Key.values()) {
            if (!RU.containsKey(key) || !EN.containsKey(key)) {
                throw new IllegalStateException("Missing translation for " + key);
            }
        }
    }

    private static Map<Key, String> table(CommandSender sender) {
        return switch (language) {
            case "ru" -> RU;
            case "en" -> EN;
            default -> sender instanceof Player player
                    && "ru".equalsIgnoreCase(player.locale().getLanguage()) ? RU : EN;
        };
    }
}
