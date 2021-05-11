package carpet.script.utils;

import carpet.CarpetSettings;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BinaryOperator;

public class SnoopyCommandSource extends ServerCommandSource
{
    private final CommandOutput output;
    private final Vec3d position;
    private final ServerWorld world;
    private final int level;
    private final String simpleName;
    private final Text name;
    private final MinecraftServer server;
    // skipping silent since snooper is never silent
    private final Entity entity;
    private final ResultConsumer<ServerCommandSource> resultConsumer;
    private final EntityAnchorArgumentType.EntityAnchor entityAnchor;
    private final Vec2f rotation;
    // good stuff
    private final Text[] error;
    private final List<Text> chatOutput;

    public SnoopyCommandSource(ServerCommandSource original, Vec3d pos, Text[] error, List<Text> chatOutput)
    {
        super(CommandOutput.DUMMY, pos, Vec2f.ZERO, original.getWorld(), CarpetSettings.runPermissionLevel,
                original.getName(), original.getDisplayName(), original.getMinecraftServer(), original.getEntity(), false,
                (ctx, succ, res) -> { }, EntityAnchorArgumentType.EntityAnchor.FEET);
        this.output = CommandOutput.DUMMY;
        this.position = pos;
        this.world = original.getWorld();
        this.level = CarpetSettings.runPermissionLevel;
        this.simpleName = original.getName();
        this.name = original.getDisplayName();
        this.server = original.getMinecraftServer();
        this.entity = original.getEntity();
        this.resultConsumer = (ctx, succ, res) -> { };
        this.entityAnchor = original.getEntityAnchor();
        this.rotation = Vec2f.ZERO;
        this.error = error;
        this.chatOutput = chatOutput;
    }

    public SnoopyCommandSource(ServerPlayerEntity player, Text[] error, List<Text> output) {
        super(player, player.getPos(), player.getRotationClient(),
                player.world instanceof ServerWorld ? (ServerWorld) player.world : null,
                player.server.getPermissionLevel(player.getGameProfile()), player.getName().getString(), player.getDisplayName(),
                player.world.getServer(), player);
        this.output = player;
        this.position = player.getPos();
        this.world = player.world instanceof ServerWorld ? (ServerWorld) player.world : null;
        this.level = player.server.getPermissionLevel(player.getGameProfile());
        this.simpleName = player.getName().getString();
        this.name = player.getDisplayName();
        this.server = player.world.getServer();
        this.entity = player;
        this.resultConsumer = (ctx, succ, res) -> { };
        this.entityAnchor = EntityAnchorArgumentType.EntityAnchor.FEET;
        this.rotation = Vec2f.ZERO;
        this.error = error;
        this.chatOutput = output;
    }

    private SnoopyCommandSource(CommandOutput output, Vec3d pos, Vec2f rot, ServerWorld world, int level, String simpleName, Text name, MinecraftServer server, @Nullable Entity entity, ResultConsumer<ServerCommandSource> consumer, EntityAnchorArgumentType.EntityAnchor entityAnchor,
             Text[] error, List<Text> chatOutput
    ) {
        super(output, pos, rot, world, level,
                simpleName, name, server, entity, false,
                consumer, entityAnchor);
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
    }

    @Override
    public ServerCommandSource withEntity(Entity entity)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, entity.getName().getString(), entity.getDisplayName(), server, entity, resultConsumer, entityAnchor, error, chatOutput);
    }

    @Override
    public ServerCommandSource withPosition(Vec3d position)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, resultConsumer, entityAnchor, error, chatOutput);
    }

    @Override
    public ServerCommandSource withRotation(Vec2f rotation)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, resultConsumer, entityAnchor, error, chatOutput);
    }

    @Override
    public ServerCommandSource withConsumer(ResultConsumer<ServerCommandSource> consumer)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, consumer, entityAnchor, error, chatOutput);
    }

    public ServerCommandSource mergeConsumers(ResultConsumer<ServerCommandSource> consumer, BinaryOperator<ResultConsumer<ServerCommandSource>> binaryOperator) {
        ResultConsumer<ServerCommandSource> resultConsumer = binaryOperator.apply(this.resultConsumer, consumer);
        return this.withConsumer(resultConsumer);
    }

    //@Override // only used in fuctions and we really don't care to track these actually, besides the basic output
    // also other overrides target ONLY execute command, which withSilent doesn't care bout.
    //public ServerCommandSource withSilent() { return this; }

    @Override
    public ServerCommandSource withLevel(int level)
    {
        return this;
    }

    @Override
    public ServerCommandSource withMaxLevel(int level)
    {
        return this;
    }

    @Override
    public ServerCommandSource withEntityAnchor(EntityAnchorArgumentType.EntityAnchor anchor)
    {
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, resultConsumer, anchor, error, chatOutput);
    }

    @Override
    public ServerCommandSource withWorld(ServerWorld world)
    {
        double d = DimensionType.method_31109(this.world.getDimension(), world.getDimension());
        Vec3d position = new Vec3d(this.position.x * d, this.position.y, this.position.z * d);
        return new SnoopyCommandSource(output, position, rotation, world, level, simpleName, name, server, entity, resultConsumer, entityAnchor, error, chatOutput);
    }

    public ServerCommandSource withLookingAt(Vec3d position) throws CommandSyntaxException {
        Vec3d vec3d = this.entityAnchor.positionAt(this);
        double d = position.x - vec3d.x;
        double e = position.y - vec3d.y;
        double f = position.z - vec3d.z;
        double g = (double) MathHelper.sqrt(d * d + f * f);
        float h = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875D)));
        float i = MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875D) - 90.0F);
        return this.withRotation(new Vec2f(h, i));
    }

    @Override
    public void sendError(Text message)
    {
        error[0] = message;
    }
    @Override
    public void sendFeedback(Text message, boolean broadcastToOps)
    {
        chatOutput.add(message);
    }

}
