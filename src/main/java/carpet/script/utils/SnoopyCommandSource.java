package carpet.script.utils;

import carpet.script.external.Vanilla;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.TaskChainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

public class SnoopyCommandSource extends CommandSourceStack
{
    private final CommandSource output;
    private final Vec3 position;
    private final ServerLevel world;
    private final int level;
    private final String simpleName;
    private final Component name;
    private final MinecraftServer server;
    // skipping silent since snooper is never silent
    private final Entity entity;
    private final CommandResultCallback resultConsumer;
    private final EntityAnchorArgument.Anchor entityAnchor;
    private final Vec2 rotation;
    // good stuff
    private final Component[] error;
    private final List<Component> chatOutput;
    private final CommandSigningContext signingContext;

    private final TaskChainer taskChainer;

    public SnoopyCommandSource(CommandSourceStack original, Component[] error, List<Component> chatOutput)
    {
        super(CommandSource.NULL, original.getPosition(), original.getRotation(), original.getLevel(), Vanilla.MinecraftServer_getRunPermissionLevel(original.getServer()),
                original.getTextName(), original.getDisplayName(), original.getServer(), original.getEntity(), false,
                CommandResultCallback.EMPTY, EntityAnchorArgument.Anchor.FEET, CommandSigningContext.ANONYMOUS, TaskChainer.immediate(original.getServer()));
        this.output = CommandSource.NULL;
        this.position = original.getPosition();
        this.world = original.getLevel();
        this.level = Vanilla.MinecraftServer_getRunPermissionLevel(original.getServer());
        this.simpleName = original.getTextName();
        this.name = original.getDisplayName();
        this.server = original.getServer();
        this.entity = original.getEntity();
        this.resultConsumer = CommandResultCallback.EMPTY;
        this.entityAnchor = original.getAnchor();
        this.rotation = original.getRotation();
        this.error = error;
        this.chatOutput = chatOutput;
        this.signingContext = original.getSigningContext();
        this.taskChainer = TaskChainer.immediate(original.getServer());
    }

    public SnoopyCommandSource(ServerPlayer player, Component[] error, List<Component> output)
    {
        super(player.commandSource(), player.position(), player.getRotationVector(),
                player.level() instanceof final ServerLevel serverLevel ? serverLevel : null,
                player.server.getProfilePermissions(player.getGameProfile()), player.getName().getString(), player.getDisplayName(),
                player.level().getServer(), player);
        this.output = player.commandSource();
        this.position = player.position();
        this.world = player.level() instanceof final ServerLevel serverLevel ? serverLevel : null;
        this.level = player.server.getProfilePermissions(player.getGameProfile());
        this.simpleName = player.getName().getString();
        this.name = player.getDisplayName();
        this.server = player.level().getServer();
        this.entity = player;
        this.resultConsumer = CommandResultCallback.EMPTY;
        this.entityAnchor = EntityAnchorArgument.Anchor.FEET;
        this.rotation = player.getRotationVector(); // not a client call really
        this.error = error;
        this.chatOutput = output;
        this.signingContext = CommandSigningContext.ANONYMOUS;
        this.taskChainer = TaskChainer.immediate(player.server);
    }

    private SnoopyCommandSource(CommandSource output, Vec3 pos, Vec2 rot, ServerLevel world, int level, String simpleName, Component name, MinecraftServer server, @Nullable Entity entity, CommandResultCallback consumer, EntityAnchorArgument.Anchor entityAnchor, CommandSigningContext context, TaskChainer chainer,
                                Component[] error, List<Component> chatOutput
    )
    {
        super(output, pos, rot, world, level,
                simpleName, name, server, entity, false,
                consumer, entityAnchor, context, chainer);
        this.output = output;
        this.position = pos;
        this.rotation = rot;
        this.world = world;
        this.level = level;
        this.simpleName = simpleName;
        this.name = name;
        this.server = server;
        this.entity = entity;
        this.resultConsumer = consumer;
        this.entityAnchor = entityAnchor;
        this.error = error;
        this.chatOutput = chatOutput;
        this.signingContext = context;
        this.taskChainer = chainer;
    }

    @Override
    public CommandSourceStack withEntity(Entity entity)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, entity.getName().getString(), entity.getDisplayName(), server, entity, resultConsumer, entityAnchor, signingContext, taskChainer, error, chatOutput);
    }

    @Override
    public CommandSourceStack withPosition(Vec3 position)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, resultConsumer, entityAnchor, signingContext, taskChainer, error, chatOutput);
    }

    @Override
    public CommandSourceStack withRotation(Vec2 rotation)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, resultConsumer, entityAnchor, signingContext, taskChainer, error, chatOutput);
    }

    @Override
    public CommandSourceStack withCallback(CommandResultCallback consumer)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, consumer, entityAnchor, signingContext, taskChainer, error, chatOutput);
    }

    @Override
    public CommandSourceStack withCallback(CommandResultCallback consumer, BinaryOperator<CommandResultCallback> binaryOperator)
    {
        CommandResultCallback resultConsumer = binaryOperator.apply(this.resultConsumer, consumer);
        return this.withCallback(resultConsumer);
    }

    //@Override // only used in fuctions and we really don't care to track these actually, besides the basic output
    // also other overrides target ONLY execute command, which withSilent doesn't care bout.
    //public ServerCommandSource withSilent() { return this; }

    @Override
    public CommandSourceStack withPermission(int level)
    {
        return this;
    }

    @Override
    public CommandSourceStack withMaximumPermission(int level)
    {
        return this;
    }

    @Override
    public CommandSourceStack withAnchor(EntityAnchorArgument.Anchor anchor)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, resultConsumer, anchor, signingContext, taskChainer, error, chatOutput);
    }

    @Override
    public CommandSourceStack withSigningContext(CommandSigningContext commandSigningContext, TaskChainer taskChainer)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, resultConsumer, entityAnchor, commandSigningContext, taskChainer, error, chatOutput);
    }

    @Override
    public CommandSourceStack withLevel(ServerLevel world)
    {
        double d = DimensionType.getTeleportationScale(this.world.dimensionType(), world.dimensionType());
        Vec3 position = new Vec3(this.position.x * d, this.position.y, this.position.z * d);
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, resultConsumer, entityAnchor, signingContext, taskChainer, error, chatOutput);
    }

    @Override
    public CommandSourceStack facing(Vec3 position)
    {
        Vec3 vec3d = this.entityAnchor.apply(this);
        double d = position.x - vec3d.x;
        double e = position.y - vec3d.y;
        double f = position.z - vec3d.z;
        double g = Math.sqrt(d * d + f * f);
        float h = Mth.wrapDegrees((float) (-(Mth.atan2(e, g) * 57.2957763671875D)));
        float i = Mth.wrapDegrees((float) (Mth.atan2(f, d) * 57.2957763671875D) - 90.0F);
        return this.withRotation(new Vec2(h, i));
    }

    @Override
    public void sendFailure(Component message)
    {
        error[0] = message;
    }

    @Override
    public void sendSuccess(Supplier<Component> message, boolean broadcastToOps)
    {
        chatOutput.add(message.get());
    }

}
