package net.dvmn2.drivenPlugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import net.dvmn2.drivenPlugin.*;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Дерево команды /driven, зарегистрированное через Paper Commands (Brigadier) API,
 * чтобы "get"-ветка могла возвращать целочисленный результат, читаемый через
 * /execute store result ... run driven ... get ...
 */
public final class DrivenCommand {

    // Было "driven.admin" — узел не зарегистрирован в plugin.yml, из-за чего
    // Bukkit по умолчанию разрешал бы команду ВСЕМ, а не только оператору.
    private static final String PERMISSION = "driven.use";
    private static final String TARGETS_ARG = "targets";

    private enum VectorField {
        LOCAL_FORCE, GLOBAL_FORCE, LOCAL_VELOCITY, GLOBAL_VELOCITY
    }

    private enum GetProperty {
        LOCAL_VX, LOCAL_VY, LOCAL_VZ,
        GLOBAL_VX, GLOBAL_VY, GLOBAL_VZ,
        LOCAL_FX, LOCAL_FY, LOCAL_FZ,
        GLOBAL_FX, GLOBAL_FY, GLOBAL_FZ,
        SPEED, MASS, DENSITY, YAW, PITCH, AZIMUTH, SLANT
    }

    private DrivenCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(DrivenPlugin plugin) {
        return Commands.literal("driven")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("create")
                        .executes(ctx -> executeCreate(plugin, ctx)))
                .then(Commands.argument(TARGETS_ARG, ArgumentTypes.entities())
                        .then(Commands.literal("remove")
                                .executes(ctx -> executeRemove(plugin, ctx)))
                        .then(Commands.literal("stop")
                                .executes(ctx -> executeStop(plugin, ctx)))
                        .then(Commands.literal("set")
                                .then(Commands.literal("mass")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0001))
                                                .executes(ctx -> executeSetMass(plugin, ctx))))
                                .then(Commands.literal("density")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                                .executes(ctx -> executeSetDensity(plugin, ctx))))
                                .then(Commands.literal("rotation")
                                        .then(Commands.literal("vector")
                                                .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90.0f, 90.0f))
                                                                .executes(ctx -> executeSetRotation(plugin, ctx)))))
                                        .then(Commands.literal("yaw")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg())
                                                        .executes(ctx -> executeSetYaw(plugin, ctx))))
                                        .then(Commands.literal("pitch")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(-90.0f, 90.0f))
                                                        .executes(ctx -> executeSetPitch(plugin, ctx)))))
                                .then(Commands.literal("force")
                                        .then(Commands.literal("local")
                                                .then(Commands.literal("vector")
                                                        .then(vectorArgs(ctx -> executeSetVector(plugin, ctx, VectorField.LOCAL_FORCE))))
                                                .then(Commands.literal("x")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.LOCAL_FX))))
                                                .then(Commands.literal("y")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.LOCAL_FY))))
                                                .then(Commands.literal("z")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.LOCAL_FZ)))))
                                        .then(Commands.literal("global")
                                                .then(Commands.literal("vector")
                                                        .then(vectorArgs(ctx -> executeSetVector(plugin, ctx, VectorField.GLOBAL_FORCE))))
                                                .then(Commands.literal("x")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.GLOBAL_FX))))
                                                .then(Commands.literal("y")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.GLOBAL_FY))))
                                                .then(Commands.literal("z")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.GLOBAL_FZ))))))
                                .then(Commands.literal("velocity")
                                        .then(Commands.literal("local")
                                                .then(Commands.literal("vector")
                                                        .then(vectorArgs(ctx -> executeSetVector(plugin, ctx, VectorField.LOCAL_VELOCITY))))
                                                .then(Commands.literal("x")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.LOCAL_VX))))
                                                .then(Commands.literal("y")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.LOCAL_VY))))
                                                .then(Commands.literal("z")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.LOCAL_VZ)))))
                                        .then(Commands.literal("global")
                                                .then(Commands.literal("vector")
                                                        .then(vectorArgs(ctx -> executeSetVector(plugin, ctx, VectorField.GLOBAL_VELOCITY))))
                                                .then(Commands.literal("x")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.GLOBAL_VX))))
                                                .then(Commands.literal("y")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.GLOBAL_VY))))
                                                .then(Commands.literal("z")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeSetDirection(plugin, ctx, GetProperty.GLOBAL_VZ)))))))
                        .then(Commands.literal("add")
                                .then(Commands.literal("mass")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> executeAddMass(plugin, ctx))))
                                .then(Commands.literal("density")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> executeAddDensity(plugin, ctx)))).then(Commands.literal("rotation")
                                        .then(Commands.literal("vector")
                                                .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90.0f, 90.0f))
                                                                .executes(ctx -> executeAddRotation(plugin, ctx)))))
                                        .then(Commands.literal("yaw")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg())
                                                        .executes(ctx -> executeAddYaw(plugin, ctx))))
                                        .then(Commands.literal("pitch")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(-90.0f, 90.0f))
                                                        .executes(ctx -> executeAddPitch(plugin, ctx)))))
                                .then(Commands.literal("force")
                                        .then(Commands.literal("local")
                                                .then(Commands.literal("vector")
                                                        .then(vectorArgs(ctx -> executeAddVector(plugin, ctx, VectorField.LOCAL_FORCE))))
                                                .then(Commands.literal("x")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.LOCAL_FX))))
                                                .then(Commands.literal("y")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.LOCAL_FY))))
                                                .then(Commands.literal("z")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.LOCAL_FZ)))))
                                        .then(Commands.literal("global")
                                                .then(Commands.literal("vector")
                                                        .then(vectorArgs(ctx -> executeAddVector(plugin, ctx, VectorField.GLOBAL_FORCE))))
                                                .then(Commands.literal("x")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.GLOBAL_FX))))
                                                .then(Commands.literal("y")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.GLOBAL_FY))))
                                                .then(Commands.literal("z")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.GLOBAL_FZ))))))
                                .then(Commands.literal("velocity")
                                        .then(Commands.literal("local")
                                                .then(Commands.literal("vector")
                                                        .then(vectorArgs(ctx -> executeAddVector(plugin, ctx, VectorField.LOCAL_VELOCITY))))
                                                .then(Commands.literal("x")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.LOCAL_VX))))
                                                .then(Commands.literal("y")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.LOCAL_VY))))
                                                .then(Commands.literal("z")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.LOCAL_VZ)))))
                                        .then(Commands.literal("global")
                                                .then(Commands.literal("vector")
                                                        .then(vectorArgs(ctx -> executeAddVector(plugin, ctx, VectorField.GLOBAL_VELOCITY))))
                                                .then(Commands.literal("x")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.GLOBAL_VX))))
                                                .then(Commands.literal("y")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.GLOBAL_VY))))
                                                .then(Commands.literal("z")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> executeAddDirection(plugin, ctx, GetProperty.GLOBAL_VZ)))))))
                        .then(Commands.literal("get")
                                .then(Commands.literal("local_velocity_x").executes(ctx -> executeGet(plugin, ctx, GetProperty.LOCAL_VX)))
                                .then(Commands.literal("local_velocity_y").executes(ctx -> executeGet(plugin, ctx, GetProperty.LOCAL_VY)))
                                .then(Commands.literal("local_velocity_z").executes(ctx -> executeGet(plugin, ctx, GetProperty.LOCAL_VZ)))
                                .then(Commands.literal("global_velocity_x").executes(ctx -> executeGet(plugin, ctx, GetProperty.GLOBAL_VX)))
                                .then(Commands.literal("global_velocity_y").executes(ctx -> executeGet(plugin, ctx, GetProperty.GLOBAL_VY)))
                                .then(Commands.literal("global_velocity_z").executes(ctx -> executeGet(plugin, ctx, GetProperty.GLOBAL_VZ)))
                                .then(Commands.literal("local_force_x").executes(ctx -> executeGet(plugin, ctx, GetProperty.LOCAL_FX)))
                                .then(Commands.literal("local_force_y").executes(ctx -> executeGet(plugin, ctx, GetProperty.LOCAL_FY)))
                                .then(Commands.literal("local_force_z").executes(ctx -> executeGet(plugin, ctx, GetProperty.LOCAL_FZ)))
                                .then(Commands.literal("global_force_x").executes(ctx -> executeGet(plugin, ctx, GetProperty.GLOBAL_FX)))
                                .then(Commands.literal("global_force_y").executes(ctx -> executeGet(plugin, ctx, GetProperty.GLOBAL_FY)))
                                .then(Commands.literal("global_force_z").executes(ctx -> executeGet(plugin, ctx, GetProperty.GLOBAL_FZ)))
                                .then(Commands.literal("speed").executes(ctx -> executeGet(plugin, ctx, GetProperty.SPEED)))
                                .then(Commands.literal("mass").executes(ctx -> executeGet(plugin, ctx, GetProperty.MASS)))
                                .then(Commands.literal("density").executes(ctx -> executeGet(plugin, ctx, GetProperty.DENSITY)))
                                .then(Commands.literal("yaw").executes(ctx -> executeGet(plugin, ctx, GetProperty.YAW)))
                                .then(Commands.literal("pitch").executes(ctx -> executeGet(plugin, ctx, GetProperty.PITCH)))
                                .then(Commands.literal("azimuth").executes(ctx -> executeGet(plugin, ctx, GetProperty.AZIMUTH)))
                                .then(Commands.literal("slant").executes(ctx -> executeGet(plugin, ctx, GetProperty.SLANT)))))
                .build();
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Double> vectorArgs(Command<CommandSourceStack> command) {
        return Commands.argument("x", DoubleArgumentType.doubleArg())
                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                .executes(command)));
    }

    // ---------------------------------------------------------------- create / remove / stop

    private static int executeCreate(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Location location = ctx.getSource().getLocation();
        if (location == null || location.getWorld() == null) {
            sender.sendMessage(Lang.get(Lang.Key.ONLY_PLAYER_OR_BLOCK, sender));
            return 0;
        }

        Interaction interaction = location.getWorld().spawn(location, Interaction.class, entity -> {
            entity.addScoreboardTag(DrivenPlugin.TAG);
            entity.setPersistent(true);
        });

        DrivenDataStore store = plugin.getDataStore();
        DrivenData data = new DrivenData(
                store.getDefaultMass(),
                store.getDefaultDensity(),
                new Vector(), new Vector(), new Vector()
        );
        store.save(interaction, data);

        sender.sendMessage(Lang.get(Lang.Key.CREATED, sender, interaction.getUniqueId().toString()));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeRemove(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            entity.remove();
        }
        sender.sendMessage(Lang.get(Lang.Key.REMOVED, sender, driven.size()));
        return driven.size();
    }

    private static int executeStop(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        DrivenDataStore store = plugin.getDataStore();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            DrivenData data = store.load(entity);
            data.setForceLocal(new Vector());
            data.setForceGlobal(new Vector());
            data.setVelocity(new Vector());
            store.save(entity, data);
        }
        sender.sendMessage(Lang.get(Lang.Key.STOPPED, sender, driven.size()));
        return driven.size();
    }

    // ---------------------------------------------------------------- set

    private static int executeSetMass(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        double value = DoubleArgumentType.getDouble(ctx, "value");
        DrivenDataStore store = plugin.getDataStore();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            DrivenData data = store.load(entity);
            data.setMass(value);
            store.save(entity, data);
        }
        sender.sendMessage(Lang.get(Lang.Key.MASS_SET, sender, value, driven.size()));
        return driven.size();
    }

    private static int executeSetDensity(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        double value = DoubleArgumentType.getDouble(ctx, "value");
        DrivenDataStore store = plugin.getDataStore();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            DrivenData data = store.load(entity);
            data.setDensity(value);
            store.save(entity, data);
        }
        sender.sendMessage(Lang.get(Lang.Key.DENSITY_SET, sender, value, driven.size()));
        return driven.size();
    }

    private static int executeSetRotation(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        float yaw = FloatArgumentType.getFloat(ctx, "yaw");
        float pitch = FloatArgumentType.getFloat(ctx, "pitch");
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            Location location = entity.getLocation();
            location.setYaw(yaw);
            location.setPitch(pitch);
            entity.teleport(location);
        }
        sender.sendMessage(Lang.get(Lang.Key.ROTATION_SET, sender, yaw, pitch, driven.size()));
        return driven.size();
    }

    private static int executeSetYaw(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        float yaw = FloatArgumentType.getFloat(ctx, "value");
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            Location location = entity.getLocation();
            location.setYaw(yaw);
            entity.teleport(location);
        }
        sender.sendMessage(Lang.get(Lang.Key.YAW_SET, sender, yaw, driven.size()));
        return driven.size();
    }

    private static int executeSetPitch(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        float pitch = FloatArgumentType.getFloat(ctx, "value");
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            Location location = entity.getLocation();
            location.setPitch(pitch);
            entity.teleport(location);
        }
        sender.sendMessage(Lang.get(Lang.Key.PITCH_SET, sender, pitch, driven.size()));
        return driven.size();
    }

    private static int executeSetVector(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx, VectorField field) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double y = DoubleArgumentType.getDouble(ctx, "y");
        double z = DoubleArgumentType.getDouble(ctx, "z");
        Vector vector = new Vector(x, y, z);

        DrivenDataStore store = plugin.getDataStore();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            DrivenData data = store.load(entity);
            switch (field) {
                case LOCAL_FORCE -> data.setForceLocal(vector.clone());
                case GLOBAL_FORCE -> data.setForceGlobal(vector.clone());
                case LOCAL_VELOCITY -> data.setVelocity(LocalFrame.localToGlobal(entity.getLocation(), vector));
                case GLOBAL_VELOCITY -> data.setVelocity(vector.clone());
            }
            store.save(entity, data);
        }

        Lang.Key key = switch (field) {
            case LOCAL_FORCE -> Lang.Key.FORCE_LOCAL_SET;
            case GLOBAL_FORCE -> Lang.Key.FORCE_GLOBAL_SET;
            case LOCAL_VELOCITY -> Lang.Key.VELOCITY_LOCAL_SET;
            case GLOBAL_VELOCITY -> Lang.Key.VELOCITY_GLOBAL_SET;
        };
        sender.sendMessage(Lang.get(key, sender, x, y, z, driven.size()));
        return driven.size();
    }

    private static int executeSetDirection(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx, GetProperty field) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        double value = DoubleArgumentType.getDouble(ctx, "value");


        DrivenDataStore store = plugin.getDataStore();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            DrivenData data = store.load(entity);
            switch (field) {
                case LOCAL_FX -> data.getForceLocal().setX(value);
                case LOCAL_FY -> data.getForceLocal().setY(value);
                case LOCAL_FZ -> data.getForceLocal().setZ(value);
                case GLOBAL_FX -> data.getForceGlobal().setX(value);
                case GLOBAL_FY -> data.getForceGlobal().setY(value);
                case GLOBAL_FZ -> data.getForceGlobal().setZ(value);
                case LOCAL_VX -> data.setVelocity(LocalFrame.localToGlobal(entity.getLocation(),
                        LocalFrame.globalToLocal(entity.getLocation(), data.getVelocity()).setX(value)));
                case LOCAL_VY -> data.setVelocity(LocalFrame.localToGlobal(entity.getLocation(),
                        LocalFrame.globalToLocal(entity.getLocation(), data.getVelocity()).setY(value)));
                case LOCAL_VZ -> data.setVelocity(LocalFrame.localToGlobal(entity.getLocation(),
                        LocalFrame.globalToLocal(entity.getLocation(), data.getVelocity()).setZ(value)));
                case GLOBAL_VX -> data.getVelocity().setX(value);
                case GLOBAL_VY -> data.getVelocity().setY(value);
                case GLOBAL_VZ -> data.getVelocity().setZ(value);
            }
            store.save(entity, data);
        }

        Lang.Key key = switch (field) {
            case LOCAL_FX, LOCAL_FY, LOCAL_FZ -> Lang.Key.FORCE_LOCAL_DIRECTION_SET;
            case GLOBAL_FX, GLOBAL_FY, GLOBAL_FZ -> Lang.Key.FORCE_GLOBAL_DIRECTION_SET;
            case LOCAL_VX, LOCAL_VY, LOCAL_VZ -> Lang.Key.VELOCITY_LOCAL_DIRECTION_SET;
            case GLOBAL_VX, GLOBAL_VY, GLOBAL_VZ -> Lang.Key.VELOCITY_GLOBAL_DIRECTION_SET;
            case SPEED, MASS, DENSITY, YAW, PITCH, AZIMUTH, SLANT -> null;
        };
        sender.sendMessage(Lang.get(key, sender, value, driven.size()));
        return driven.size();
    }

    // ---------------------------------------------------------------- add

    private static int executeAddMass(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        double value = DoubleArgumentType.getDouble(ctx, "value");
        DrivenDataStore store = plugin.getDataStore();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        int edited = driven.size();
        for (Entity entity : driven) {
            DrivenData data = store.load(entity);
            double mass = data.getMass();
            double newMass = mass + value;
            if (newMass <= 0) {
                String uuid = entity.getUniqueId().toString();
                sender.sendMessage(Lang.get(Lang.Key.WRONG_ADD_MASS, sender, uuid, mass, value, newMass));
                edited--;
                continue;
            }
            data.setMass(newMass);
            store.save(entity, data);
        }
        sender.sendMessage(Lang.get(Lang.Key.MASS_ADD, sender, value, edited));
        return edited;
    }

    private static int executeAddDensity(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        double value = DoubleArgumentType.getDouble(ctx, "value");
        DrivenDataStore store = plugin.getDataStore();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        int edited = driven.size();
        for (Entity entity : driven) {
            DrivenData data = store.load(entity);
            double density = data.getDensity();
            double newDensity = density + value;
            if (newDensity <= 0) {
                String uuid = entity.getUniqueId().toString();
                sender.sendMessage(Lang.get(Lang.Key.WRONG_ADD_DENSITY, sender, uuid, density, value, newDensity));
                edited--;
                continue;
            }
            // Раньше здесь было data.setDensity(value) — сырой инкремент затирал
            // предыдущую плотность вместо того, чтобы прибавляться к ней.
            data.setDensity(newDensity);
            store.save(entity, data);
        }
        sender.sendMessage(Lang.get(Lang.Key.DENSITY_ADD, sender, value, edited));
        return edited;
    }

    private static int executeAddRotation(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        float yaw = FloatArgumentType.getFloat(ctx, "yaw");
        float pitch = FloatArgumentType.getFloat(ctx, "pitch");
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        int edited = driven.size();
        for (Entity entity : driven) {
            Location location = entity.getLocation();
            float oldPitch = location.getPitch();
            float newPitch = oldPitch + pitch;
            if (newPitch <= -90 || newPitch >= 90) {
                String uuid = entity.getUniqueId().toString();
                sender.sendMessage(Lang.get(Lang.Key.WRONG_ADD_PITCH, sender, uuid, oldPitch, pitch, newPitch));
                edited--;
                continue;
            }
            float oldYaw = location.getYaw();
            float newYaw = oldYaw + yaw;
            location.setYaw(newYaw);
            location.setPitch(newPitch);
            entity.teleport(location);
        }
        sender.sendMessage(Lang.get(Lang.Key.ROTATION_ADD, sender, yaw, pitch, edited));
        return edited;
    }

    private static int executeAddYaw(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        // Аргумент в дереве команд называется "value", а не "yaw" — раньше здесь
        // был IllegalArgumentException при попытке прочитать несуществующий аргумент.
        float yaw = FloatArgumentType.getFloat(ctx, "value");
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        int edited = driven.size();
        for (Entity entity : driven) {
            Location location = entity.getLocation();
            float oldYaw = location.getYaw();
            float newYaw = oldYaw + yaw;
            location.setYaw(newYaw);
            entity.teleport(location);
        }
        sender.sendMessage(Lang.get(Lang.Key.YAW_ADD, sender, yaw, edited));
        return edited;
    }

    private static int executeAddPitch(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        // Аргумент в дереве команд называется "value", а не "pitch".
        float pitch = FloatArgumentType.getFloat(ctx, "value");
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        int edited = driven.size();
        for (Entity entity : driven) {
            Location location = entity.getLocation();
            float oldPitch = location.getPitch();
            float newPitch = oldPitch + pitch;
            if (newPitch <= -90 || newPitch >= 90) {
                String uuid = entity.getUniqueId().toString();
                sender.sendMessage(Lang.get(Lang.Key.WRONG_ADD_PITCH, sender, uuid, oldPitch, pitch, newPitch));
                edited--;
                continue;
            }
            location.setPitch(newPitch);
            entity.teleport(location);
        }
        sender.sendMessage(Lang.get(Lang.Key.PITCH_ADD, sender, pitch, edited));
        return edited;
    }

    private static int executeAddVector(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx, VectorField field) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        double x = DoubleArgumentType.getDouble(ctx, "x");
        double y = DoubleArgumentType.getDouble(ctx, "y");
        double z = DoubleArgumentType.getDouble(ctx, "z");
        Vector vector = new Vector(x, y, z);

        DrivenDataStore store = plugin.getDataStore();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            DrivenData data = store.load(entity);
            switch (field) {
                case LOCAL_FORCE -> data.setForceLocal(data.getForceLocal().add(vector.clone()));
                case GLOBAL_FORCE -> data.setForceGlobal(data.getForceGlobal().add(vector.clone()));
                case LOCAL_VELOCITY ->
                        data.setVelocity(data.getVelocity().add(LocalFrame.localToGlobal(entity.getLocation(), vector)));
                case GLOBAL_VELOCITY -> data.setVelocity(data.getVelocity().add(vector.clone()));
            }
            store.save(entity, data);
        }

        Lang.Key key = switch (field) {
            case LOCAL_FORCE -> Lang.Key.FORCE_LOCAL_ADD;
            case GLOBAL_FORCE -> Lang.Key.FORCE_GLOBAL_ADD;
            case LOCAL_VELOCITY -> Lang.Key.VELOCITY_LOCAL_ADD;
            case GLOBAL_VELOCITY -> Lang.Key.VELOCITY_GLOBAL_ADD;
        };
        sender.sendMessage(Lang.get(key, sender, x, y, z, driven.size()));
        return driven.size();
    }

    private static int executeAddDirection(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx, GetProperty field) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        double value = DoubleArgumentType.getDouble(ctx, "value");
        Vector vector = new Vector(0, 0, 0);

        switch (field) {
            case LOCAL_FX, GLOBAL_FX, LOCAL_VX, GLOBAL_VX -> vector.setX(value);
            case LOCAL_FY, GLOBAL_FY, LOCAL_VY, GLOBAL_VY -> vector.setY(value);
            case LOCAL_FZ, GLOBAL_FZ, LOCAL_VZ, GLOBAL_VZ -> vector.setZ(value);
        }

        DrivenDataStore store = plugin.getDataStore();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        for (Entity entity : driven) {
            DrivenData data = store.load(entity);
            switch (field) {
                case LOCAL_FX, LOCAL_FY, LOCAL_FZ -> data.getForceLocal().add(vector);
                case GLOBAL_FX, GLOBAL_FY, GLOBAL_FZ -> data.getForceGlobal().add(vector);
                case LOCAL_VX, LOCAL_VY, LOCAL_VZ -> data.setVelocity(LocalFrame.localToGlobal(entity.getLocation(),
                        LocalFrame.globalToLocal(entity.getLocation(), data.getVelocity()).add(vector)));
                case GLOBAL_VX, GLOBAL_VY, GLOBAL_VZ -> data.getVelocity().add(vector);
            }
            store.save(entity, data);
        }

        Lang.Key key = switch (field) {
            case LOCAL_FX, LOCAL_FY, LOCAL_FZ -> Lang.Key.FORCE_LOCAL_DIRECTION_SET;
            case GLOBAL_FX, GLOBAL_FY, GLOBAL_FZ -> Lang.Key.FORCE_GLOBAL_DIRECTION_SET;
            case LOCAL_VX, LOCAL_VY, LOCAL_VZ -> Lang.Key.VELOCITY_LOCAL_DIRECTION_SET;
            case GLOBAL_VX, GLOBAL_VY, GLOBAL_VZ -> Lang.Key.VELOCITY_GLOBAL_DIRECTION_SET;
            case SPEED, MASS, DENSITY, YAW, PITCH, AZIMUTH, SLANT -> null;
        };
        sender.sendMessage(Lang.get(key, sender, value, driven.size()));
        return driven.size();
    }

    // ---------------------------------------------------------------- get

    private static int executeGet(DrivenPlugin plugin, CommandContext<CommandSourceStack> ctx, GetProperty property) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        List<Entity> driven = filterDriven(resolveTargets(ctx));
        if (driven.size() != 1) {
            sender.sendMessage(Lang.get(Lang.Key.MUST_TARGET_ONE, sender, driven.size()));
            return 0;
        }

        Entity entity = driven.getFirst();
        DrivenData data = plugin.getDataStore().load(entity);
        Vector velocity = data.getVelocity();
        Vector localForce = data.getForceLocal();
        Vector globalForce = data.getForceGlobal();
        Location location = entity.getLocation();

        double scale = plugin.getConfig().getDouble("physics.get-scale", 1.0);

        int result = (int) Math.round(switch (property) {
            case LOCAL_VX -> LocalFrame.globalToLocal(location, velocity).getX() * scale;
            case LOCAL_VY -> LocalFrame.globalToLocal(location, velocity).getY() * scale;
            case LOCAL_VZ -> LocalFrame.globalToLocal(location, velocity).getZ() * scale;
            case GLOBAL_VX -> velocity.getX() * scale;
            case GLOBAL_VY -> velocity.getY() * scale;
            case GLOBAL_VZ -> velocity.getZ() * scale;
            case LOCAL_FX -> localForce.getX();
            case LOCAL_FY -> localForce.getY();
            case LOCAL_FZ -> localForce.getZ();
            case GLOBAL_FX -> globalForce.getX();
            case GLOBAL_FY -> globalForce.getY();
            case GLOBAL_FZ -> globalForce.getZ();
            case SPEED -> velocity.length() * scale;
            case MASS -> data.getMass();
            case DENSITY -> data.getDensity();
            case YAW -> entity.getYaw();
            case PITCH -> entity.getPitch();
            case AZIMUTH -> mod(180 + Math.round(entity.getYaw()), 360);
            case SLANT -> -entity.getPitch();
        });

        sender.sendMessage(Lang.get(Lang.Key.VALUE_RESULT, sender, result));
        return result;
    }

    // ---------------------------------------------------------------- helpers

    private static List<Entity> resolveTargets(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        EntitySelectorArgumentResolver resolver = ctx.getArgument(TARGETS_ARG, EntitySelectorArgumentResolver.class);
        return resolver.resolve(ctx.getSource());
    }

    private static List<Entity> filterDriven(List<Entity> entities) {
        return entities.stream()
                .filter(entity -> entity instanceof Interaction)
                .filter(entity -> entity.getScoreboardTags().contains(DrivenPlugin.TAG))
                .toList();
    }

    private static int mod(int a, int b) {
        return (a % b + b) % b;
    }
}