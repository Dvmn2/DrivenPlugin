package net.dvmn2.drivenplugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import net.dvmn2.drivenplugin.DrivenManager;
import net.dvmn2.drivenplugin.DrivenPlugin;
import net.dvmn2.drivenplugin.DrivenSettings;
import net.dvmn2.drivenplugin.Lang;
import net.dvmn2.drivenplugin.Permissions;
import net.dvmn2.drivenplugin.model.DrivenData;
import net.dvmn2.drivenplugin.util.LocalFrame;
import net.dvmn2.drivenplugin.util.Numbers;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The {@code /driven} command tree (Brigadier).
 * <pre>
 * /driven create
 * /driven reload
 * /driven &lt;targets&gt; remove
 * /driven &lt;targets&gt; stop
 * /driven &lt;targets&gt; &lt;mode&gt; mass &lt;value&gt;
 * /driven &lt;targets&gt; &lt;mode&gt; density &lt;value&gt;
 * /driven &lt;targets&gt; &lt;mode&gt; rotation vector &lt;yaw&gt; &lt;pitch&gt;
 * /driven &lt;targets&gt; &lt;mode&gt; rotation yaw &lt;value&gt;
 * /driven &lt;targets&gt; &lt;mode&gt; rotation pitch &lt;value&gt;
 * /driven &lt;targets&gt; &lt;mode&gt; &lt;quantity&gt; &lt;frame&gt; vector &lt;x&gt; &lt;y&gt; &lt;z&gt;
 * /driven &lt;targets&gt; &lt;mode&gt; &lt;quantity&gt; &lt;frame&gt; &lt;axis&gt; &lt;value&gt;
 * /driven &lt;targets&gt; get &lt;property&gt;
 *
 * mode     = set | add
 * quantity = force | velocity
 * frame    = local | global
 * axis     = x | y | z
 * </pre>
 * {@code get} returns an integer so it can be read with {@code /execute store result}. Selectors may match any
 * entities; only driven ones are affected.
 * <p>
 * Runs on the main thread.
 */
public final class DrivenCommand {

    private static final String ARG_TARGETS = "targets";
    private static final String ARG_VALUE = "value";
    private static final String ARG_X = "x";
    private static final String ARG_Y = "y";
    private static final String ARG_Z = "z";
    private static final String ARG_YAW = "yaw";
    private static final String ARG_PITCH = "pitch";

    /** Largest absolute pitch, degrees. */
    private static final float MAX_PITCH_DEGREES = 90.0f;
    private static final int HALF_TURN_DEGREES = 180;
    private static final int FULL_TURN_DEGREES = 360;

    private static final int VALUE_DECIMALS = 3;
    private static final int ANGLE_DECIMALS = 2;

    private static final List<Property> PROPERTIES = buildProperties();

    private enum Mode {
        SET, ADD;

        String literal() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private enum Quantity {
        FORCE(Lang.Key.QUANTITY_FORCE),
        VELOCITY(Lang.Key.QUANTITY_VELOCITY);

        private final Lang.Key key;

        Quantity(Lang.Key key) {
            this.key = key;
        }

        String literal() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private enum Frame {
        LOCAL(Lang.Key.FRAME_LOCAL),
        GLOBAL(Lang.Key.FRAME_GLOBAL);

        private final Lang.Key key;

        Frame(Lang.Key key) {
            this.key = key;
        }

        String literal() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private enum Axis {
        X, Y, Z;

        String literal() {
            return name().toLowerCase(Locale.ROOT);
        }

        double get(Vector vector) {
            return switch (this) {
                case X -> vector.getX();
                case Y -> vector.getY();
                case Z -> vector.getZ();
            };
        }

        void set(Vector vector, double value) {
            switch (this) {
                case X -> vector.setX(value);
                case Y -> vector.setY(value);
                case Z -> vector.setZ(value);
            }
        }
    }

    /** Scalar physical parameters that share the same set/add rules. */
    private enum Scalar {
        MASS(DrivenData.MIN_MASS, Lang.Key.MASS_SET, Lang.Key.MASS_ADD, Lang.Key.WRONG_MASS),
        DENSITY(DrivenData.MIN_DENSITY, Lang.Key.DENSITY_SET, Lang.Key.DENSITY_ADD, Lang.Key.WRONG_DENSITY);

        private final double min;
        private final Lang.Key setKey;
        private final Lang.Key addKey;
        private final Lang.Key wrongKey;

        Scalar(double min, Lang.Key setKey, Lang.Key addKey, Lang.Key wrongKey) {
            this.min = min;
            this.setKey = setKey;
            this.addKey = addKey;
            this.wrongKey = wrongKey;
        }

        String literal() {
            return name().toLowerCase(Locale.ROOT);
        }

        double get(DrivenData data) {
            return this == MASS ? data.getMass() : data.getDensity();
        }

        void set(DrivenData data, double value) {
            if (this == MASS) {
                data.setMass(value);
            } else {
                data.setDensity(value);
            }
        }
    }

    private record Target(Entity entity, DrivenData data) {
    }

    @FunctionalInterface
    private interface Reader {

        double read(Entity entity, DrivenData data, double scale);
    }

    private record Property(String literal, Reader reader) {
    }

    private final DrivenPlugin plugin;
    private final DrivenManager manager;
    private final Supplier<DrivenSettings> settings;

    /**
     * Creates the command.
     *
     * @param plugin   plugin, used for {@code reload}
     * @param manager  runtime state
     * @param settings supplier of the current settings snapshot
     */
    public DrivenCommand(DrivenPlugin plugin, DrivenManager manager, Supplier<DrivenSettings> settings) {
        this.plugin = plugin;
        this.manager = manager;
        this.settings = settings;
    }

    /**
     * Builds the command tree.
     *
     * @return root node of {@code /driven}
     */
    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("driven")
                .requires(src -> src.getSender().hasPermission(Permissions.USE))
                .then(Commands.literal("create").executes(this::create))
                .then(Commands.literal("reload")
                        .requires(src -> src.getSender().hasPermission(Permissions.RELOAD))
                        .executes(this::reload))
                .then(Commands.argument(ARG_TARGETS, ArgumentTypes.entities())
                        .then(Commands.literal("remove").executes(this::remove))
                        .then(Commands.literal("stop").executes(this::stop))
                        .then(modeBranch(Mode.SET))
                        .then(modeBranch(Mode.ADD))
                        .then(getBranch()))
                .build();
    }

    // ---------- Tree ----------

    private LiteralArgumentBuilder<CommandSourceStack> modeBranch(Mode mode) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(mode.literal());
        for (Scalar scalar : Scalar.values()) {
            node.then(Commands.literal(scalar.literal())
                    .then(Commands.argument(ARG_VALUE, scalarArgument(mode, scalar))
                            .executes(ctx -> editScalar(ctx, mode, scalar))));
        }
        node.then(rotationBranch(mode));
        for (Quantity quantity : Quantity.values()) {
            node.then(quantityBranch(mode, quantity));
        }
        return node;
    }

    private static ArgumentType<Double> scalarArgument(Mode mode, Scalar scalar) {
        // "set" can reject bad values at parse time; "add" depends on the current value and is checked per entity.
        return mode == Mode.SET ? DoubleArgumentType.doubleArg(scalar.min) : DoubleArgumentType.doubleArg();
    }

    private LiteralArgumentBuilder<CommandSourceStack> rotationBranch(Mode mode) {
        return Commands.literal("rotation")
                .then(Commands.literal("vector")
                        .then(Commands.argument(ARG_YAW, FloatArgumentType.floatArg())
                                .then(Commands.argument(ARG_PITCH,
                                                FloatArgumentType.floatArg(-MAX_PITCH_DEGREES, MAX_PITCH_DEGREES))
                                        .executes(ctx -> rotate(ctx, mode,
                                                FloatArgumentType.getFloat(ctx, ARG_YAW),
                                                FloatArgumentType.getFloat(ctx, ARG_PITCH))))))
                .then(Commands.literal("yaw")
                        .then(Commands.argument(ARG_VALUE, FloatArgumentType.floatArg())
                                .executes(ctx -> rotate(ctx, mode,
                                        FloatArgumentType.getFloat(ctx, ARG_VALUE), null))))
                .then(Commands.literal("pitch")
                        .then(Commands.argument(ARG_VALUE,
                                        FloatArgumentType.floatArg(-MAX_PITCH_DEGREES, MAX_PITCH_DEGREES))
                                .executes(ctx -> rotate(ctx, mode,
                                        null, FloatArgumentType.getFloat(ctx, ARG_VALUE)))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> quantityBranch(Mode mode, Quantity quantity) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(quantity.literal());
        for (Frame frame : Frame.values()) {
            LiteralArgumentBuilder<CommandSourceStack> frameNode = Commands.literal(frame.literal())
                    .then(Commands.literal("vector").then(vectorArguments(ctx ->
                            editVector(ctx, mode, quantity, frame))));
            for (Axis axis : Axis.values()) {
                frameNode.then(Commands.literal(axis.literal())
                        .then(Commands.argument(ARG_VALUE, DoubleArgumentType.doubleArg())
                                .executes(ctx -> editAxis(ctx, mode, quantity, frame, axis))));
            }
            node.then(frameNode);
        }
        return node;
    }

    private LiteralArgumentBuilder<CommandSourceStack> getBranch() {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("get");
        for (Property property : PROPERTIES) {
            node.then(Commands.literal(property.literal()).executes(ctx -> get(ctx, property)));
        }
        return node;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Double> vectorArguments(
            Command<CommandSourceStack> command) {
        return Commands.argument(ARG_X, DoubleArgumentType.doubleArg())
                .then(Commands.argument(ARG_Y, DoubleArgumentType.doubleArg())
                        .then(Commands.argument(ARG_Z, DoubleArgumentType.doubleArg())
                                .executes(command)));
    }

    private static List<Property> buildProperties() {
        List<Property> list = new ArrayList<>();
        for (Frame frame : Frame.values()) {
            for (Quantity quantity : Quantity.values()) {
                for (Axis axis : Axis.values()) {
                    String literal = frame.literal() + "_" + quantity.literal() + "_" + axis.literal();
                    // Only velocity is scaled; forces are returned as stored.
                    list.add(new Property(literal, (entity, data, scale) ->
                            axis.get(current(quantity, frame, entity, data))
                                    * (quantity == Quantity.VELOCITY ? scale : 1.0)));
                }
            }
        }
        list.add(new Property("speed", (entity, data, scale) -> data.getVelocity().length() * scale));
        list.add(new Property("mass", (entity, data, scale) -> data.getMass()));
        list.add(new Property("density", (entity, data, scale) -> data.getDensity()));
        list.add(new Property("yaw", (entity, data, scale) -> entity.getYaw()));
        list.add(new Property("pitch", (entity, data, scale) -> entity.getPitch()));
        list.add(new Property("azimuth", (entity, data, scale) ->
                Numbers.mod(HALF_TURN_DEGREES + Math.round(entity.getYaw()), FULL_TURN_DEGREES)));
        list.add(new Property("slant", (entity, data, scale) -> -entity.getPitch()));
        return List.copyOf(list);
    }

    // ---------- Executors ----------

    private int create(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Location location = ctx.getSource().getLocation();
        if (location.getWorld() == null) {
            send(sender, Lang.Key.NO_WORLD_POSITION);
            return 0;
        }

        Interaction entity = manager.create(location);
        send(sender, Lang.Key.CREATED, Placeholder.unparsed("uuid", entity.getUniqueId().toString()));
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        plugin.reloadAll();
        send(sender, Lang.Key.RELOADED);
        return Command.SINGLE_SUCCESS;
    }

    private int remove(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<Target> targets = resolve(ctx);
        for (Target target : targets) {
            manager.remove(target.entity());
        }
        send(sender, Lang.Key.REMOVED, count(targets.size()));
        return targets.size();
    }

    private int stop(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<Target> targets = resolve(ctx);
        for (Target target : targets) {
            target.data().reset();
            manager.markDirty(target.entity());
        }
        send(sender, Lang.Key.STOPPED, count(targets.size()));
        return targets.size();
    }

    private int editScalar(CommandContext<CommandSourceStack> ctx, Mode mode, Scalar scalar) {
        CommandSender sender = ctx.getSource().getSender();
        double value = DoubleArgumentType.getDouble(ctx, ARG_VALUE);
        int done = 0;
        for (Target target : resolve(ctx)) {
            double current = scalar.get(target.data());
            double result = mode == Mode.SET ? value : current + value;
            if (!Double.isFinite(result) || result < scalar.min) {
                send(sender, scalar.wrongKey, uuid(target), number("old", current), number("delta", value),
                        number("result", result));
                continue;
            }
            scalar.set(target.data(), result);
            manager.markDirty(target.entity());
            done++;
        }
        send(sender, mode == Mode.SET ? scalar.setKey : scalar.addKey, number(ARG_VALUE, value), count(done));
        return done;
    }

    private int rotate(CommandContext<CommandSourceStack> ctx, Mode mode, @Nullable Float yaw,
                       @Nullable Float pitch) {
        CommandSender sender = ctx.getSource().getSender();
        int done = 0;
        for (Target target : resolve(ctx)) {
            Location location = target.entity().getLocation();
            if (pitch != null) {
                float newPitch = mode == Mode.ADD ? location.getPitch() + pitch : pitch;
                if (newPitch < -MAX_PITCH_DEGREES || newPitch > MAX_PITCH_DEGREES) {
                    send(sender, Lang.Key.WRONG_PITCH, uuid(target), number("old", location.getPitch()),
                            number("delta", pitch), number("result", newPitch));
                    continue;
                }
                location.setPitch(newPitch);
            }
            if (yaw != null) {
                location.setYaw(mode == Mode.ADD ? location.getYaw() + yaw : yaw);
            }
            target.entity().teleport(location);
            done++;
        }

        boolean add = mode == Mode.ADD;
        if (yaw != null && pitch != null) {
            send(sender, add ? Lang.Key.ROTATION_ADD : Lang.Key.ROTATION_SET,
                    angle(ARG_YAW, yaw), angle(ARG_PITCH, pitch), count(done));
        } else if (yaw != null) {
            send(sender, add ? Lang.Key.YAW_ADD : Lang.Key.YAW_SET, angle(ARG_VALUE, yaw), count(done));
        } else if (pitch != null) {
            send(sender, add ? Lang.Key.PITCH_ADD : Lang.Key.PITCH_SET, angle(ARG_VALUE, pitch), count(done));
        }
        return done;
    }

    private int editVector(CommandContext<CommandSourceStack> ctx, Mode mode, Quantity quantity, Frame frame) {
        CommandSender sender = ctx.getSource().getSender();
        double x = DoubleArgumentType.getDouble(ctx, ARG_X);
        double y = DoubleArgumentType.getDouble(ctx, ARG_Y);
        double z = DoubleArgumentType.getDouble(ctx, ARG_Z);
        Vector vector = new Vector(x, y, z);

        List<Target> targets = resolve(ctx);
        for (Target target : targets) {
            Vector result = mode == Mode.SET
                    ? vector.clone()
                    : current(quantity, frame, target.entity(), target.data()).add(vector);
            store(quantity, frame, target.entity(), target.data(), result);
            manager.markDirty(target.entity());
        }

        send(sender, mode == Mode.SET ? Lang.Key.VECTOR_SET : Lang.Key.VECTOR_ADD,
                frameWord(sender, frame), quantityWord(sender, quantity),
                number(ARG_X, x), number(ARG_Y, y), number(ARG_Z, z), count(targets.size()));
        return targets.size();
    }

    private int editAxis(CommandContext<CommandSourceStack> ctx, Mode mode, Quantity quantity, Frame frame,
                         Axis axis) {
        CommandSender sender = ctx.getSource().getSender();
        double value = DoubleArgumentType.getDouble(ctx, ARG_VALUE);

        List<Target> targets = resolve(ctx);
        for (Target target : targets) {
            Vector vector = current(quantity, frame, target.entity(), target.data());
            axis.set(vector, mode == Mode.SET ? value : axis.get(vector) + value);
            store(quantity, frame, target.entity(), target.data(), vector);
            manager.markDirty(target.entity());
        }

        send(sender, mode == Mode.SET ? Lang.Key.AXIS_SET : Lang.Key.AXIS_ADD,
                frameWord(sender, frame), quantityWord(sender, quantity),
                Placeholder.unparsed("axis", axis.literal()), number(ARG_VALUE, value), count(targets.size()));
        return targets.size();
    }

    private int get(CommandContext<CommandSourceStack> ctx, Property property) {
        CommandSender sender = ctx.getSource().getSender();
        List<Target> targets = resolve(ctx);
        if (targets.size() != 1) {
            send(sender, Lang.Key.MUST_TARGET_ONE, count(targets.size()));
            return 0;
        }

        Target target = targets.getFirst();
        int result = Numbers.toInt(property.reader().read(target.entity(), target.data(),
                settings.get().queryScale()));
        send(sender, Lang.Key.VALUE_RESULT, Placeholder.unparsed(ARG_VALUE, String.valueOf(result)));
        return result;
    }

    // ---------- Helpers ----------

    /** Returns the vector of a quantity expressed in the given frame (a copy). */
    private static Vector current(Quantity quantity, Frame frame, Entity entity, DrivenData data) {
        return switch (quantity) {
            case FORCE -> frame == Frame.LOCAL ? data.getForceLocal() : data.getForceGlobal();
            // Velocity is stored in world coordinates; the local view is a projection.
            case VELOCITY -> frame == Frame.LOCAL
                    ? LocalFrame.globalToLocal(entity.getLocation(), data.getVelocity())
                    : data.getVelocity();
        };
    }

    /** Stores a vector that is expressed in the given frame. */
    private static void store(Quantity quantity, Frame frame, Entity entity, DrivenData data, Vector vector) {
        switch (quantity) {
            case FORCE -> {
                if (frame == Frame.LOCAL) {
                    data.setForceLocal(vector);
                } else {
                    data.setForceGlobal(vector);
                }
            }
            case VELOCITY -> data.setVelocity(frame == Frame.LOCAL
                    ? LocalFrame.localToGlobal(entity.getLocation(), vector)
                    : vector);
        }
    }

    /** Resolves the selector and keeps only driven entities; reports a selector failure to the sender. */
    private List<Target> resolve(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<Entity> entities;
        try {
            entities = ctx.getArgument(ARG_TARGETS, EntitySelectorArgumentResolver.class)
                    .resolve(ctx.getSource());
        } catch (CommandSyntaxException ex) {
            send(sender, Lang.Key.SELECTOR_FAILED, Placeholder.unparsed("reason", String.valueOf(ex.getMessage())));
            return List.of();
        }

        List<Target> targets = new ArrayList<>();
        for (Entity entity : entities) {
            DrivenData data = manager.find(entity);
            if (data != null) {
                targets.add(new Target(entity, data));
            }
        }
        return targets;
    }

    private static void send(CommandSender sender, Lang.Key key, TagResolver... resolvers) {
        sender.sendMessage(Lang.get(key, sender, resolvers));
    }

    private static TagResolver number(String name, double value) {
        return Placeholder.unparsed(name, Numbers.format(value, VALUE_DECIMALS));
    }

    private static TagResolver angle(String name, double value) {
        return Placeholder.unparsed(name, Numbers.format(value, ANGLE_DECIMALS));
    }

    private static TagResolver count(int value) {
        return Placeholder.unparsed("count", String.valueOf(value));
    }

    private static TagResolver uuid(Target target) {
        return Placeholder.unparsed("uuid", target.entity().getUniqueId().toString());
    }

    private static TagResolver frameWord(CommandSender sender, Frame frame) {
        return Placeholder.unparsed("frame", Lang.raw(frame.key, sender));
    }

    private static TagResolver quantityWord(CommandSender sender, Quantity quantity) {
        return Placeholder.unparsed("quantity", Lang.raw(quantity.key, sender));
    }
}
