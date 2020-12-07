package carpet.script.utils;

import carpet.CarpetSettings;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.Value;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.BinaryOperator;

public class FixedCommandSource extends ServerCommandSource
{
    private Vec3d position;
    private ServerWorld world;
    private final String simpleName;
    private final Text name;
    private final MinecraftServer server;
    private Entity entity;
    private ResultConsumer<ServerCommandSource> resultConsumer;
    private EntityAnchorArgumentType.EntityAnchor entityAnchor;
    private Vec2f rotation;
    private final Value[] error;
    private final List<Value> chatOutput;

    public FixedCommandSource(ServerCommandSource original, Vec3d pos, Value[] error, List<Value> chatOutput)
    {
        super(CommandOutput.DUMMY, pos, Vec2f.ZERO, original.getWorld(), CarpetSettings.runPermissionLevel,
                original.getName(), original.getDisplayName(), original.getMinecraftServer(), original.getEntity(), true,
                (ctx, succ, res) -> { }, EntityAnchorArgumentType.EntityAnchor.FEET);
        position = pos;
        world = original.getWorld();
        simpleName = original.getName();
        name = original.getDisplayName();
        server = original.getMinecraftServer();
        entity = original.getEntity();
        resultConsumer = (ctx, succ, res) -> { };
        entityAnchor = original.getEntityAnchor();
        rotation = Vec2f.ZERO;
        this.error = error;
        this.chatOutput = chatOutput;
    }

    @Override
    public ServerCommandSource withEntity(Entity entity)
    {
        this.entity = entity;
        return this;
    }

    @Override
    public ServerCommandSource withPosition(Vec3d position)
    {
        this.position = position;
        return this;
    }

    @Override
    public ServerCommandSource withRotation(Vec2f rotation)
    {
        this.rotation = rotation;
        return this;
    }

    @Override
    public ServerCommandSource withConsumer(ResultConsumer<ServerCommandSource> consumer)
    {
        this.resultConsumer = consumer;
        return this;
    }

    @Override
    public ServerCommandSource mergeConsumers(ResultConsumer<ServerCommandSource> consumer, BinaryOperator<ResultConsumer<ServerCommandSource>> binaryOperator)
    {
        return this.withConsumer(binaryOperator.apply(this.resultConsumer, consumer));
    }

    @Override
    public ServerCommandSource withSilent()
    {
        return this;
    }

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
        entityAnchor = anchor;
        return this;
    }

    @Override
    public ServerCommandSource withWorld(ServerWorld world)
    {
        this.world = world;
        return this;
    }

    @Override
    public Text getDisplayName()
    {
        return name;
    }

    @Override
    public String getName()
    {
        return simpleName;
    }

    @Override
    public Vec3d getPosition()
    {
        return position;
    }

    @Override
    public ServerWorld getWorld()
    {
        return world;
    }

    @Override
    public Entity getEntity()
    {
        return entity;
    }

    @Override
    public Entity getEntityOrThrow() throws CommandSyntaxException
    {
        if (entity == null) {
            throw REQUIRES_ENTITY_EXCEPTION.create();
        } else {
            return entity;
        }
    }

    @Override
    public ServerPlayerEntity getPlayer() throws CommandSyntaxException
    {
        if (!(this.entity instanceof ServerPlayerEntity)) {
            throw REQUIRES_PLAYER_EXCEPTION.create();
        } else {
            return (ServerPlayerEntity)this.entity;
        }
    }

    @Override
    public Vec2f getRotation()
    {
        return rotation;
    }

    @Override
    public MinecraftServer getMinecraftServer()
    {
        return server;
    }

    @Override
    public EntityAnchorArgumentType.EntityAnchor getEntityAnchor()
    {
        return entityAnchor;
    }


    @Override
    public void onCommandComplete(CommandContext<ServerCommandSource> context, boolean success, int result)
    {
    }

    @Override
    public void sendError(Text message)
    {
        error[0] = new FormattedTextValue(message);
    }
    @Override
    public void sendFeedback(Text message, boolean broadcastToOps)
    {
        chatOutput.add(new FormattedTextValue(message));
    }

}
