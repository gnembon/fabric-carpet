package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.CarpetScriptHost;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.argument.FunctionArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Entities
{
    private static ListValue getPlayersFromWorldMatching(Context c, Predicate<ServerPlayer> condition)
    {
        List<Value> ret = new ArrayList<>();
        for (ServerPlayer player : ((CarpetContext) c).level().players())
        {
            if (condition.test(player))
            {
                ret.add(new EntityValue(player));
            }
        }
        return ListValue.wrap(ret);
    }

    public static void apply(Expression expression)
    {
        expression.addContextFunction("player", -1, (c, t, lv) ->
        {
            if (lv.isEmpty())
            {
                CarpetContext cc = (CarpetContext) c;
                if (cc.host.user != null)
                {
                    ServerPlayer player = cc.server().getPlayerList().getPlayerByName(cc.host.user);
                    return EntityValue.of(player);
                }
                Entity callingEntity = cc.source().getEntity();
                if (callingEntity instanceof Player)
                {
                    return EntityValue.of(callingEntity);
                }
                Vec3 pos = ((CarpetContext) c).source().getPosition();
                Player closestPlayer = ((CarpetContext) c).level().getNearestPlayer(pos.x, pos.y, pos.z, -1.0, EntitySelector.ENTITY_STILL_ALIVE);
                return EntityValue.of(closestPlayer);
            }
            String playerName = lv.get(0).getString();
            return switch (playerName)
                    {
                        case "all" -> {
                            List<Value> ret = new ArrayList<>();
                            for (ServerPlayer player : ((CarpetContext) c).server().getPlayerList().getPlayers())
                            {
                                ret.add(new EntityValue(player));
                            }
                            yield ListValue.wrap(ret);
                        }
                        case "*" -> getPlayersFromWorldMatching(c, p -> true);
                        case "survival" -> getPlayersFromWorldMatching(c, p -> p.gameMode.isSurvival()); // includes adventure
                        case "creative" -> getPlayersFromWorldMatching(c, ServerPlayer::isCreative);
                        case "spectating" -> getPlayersFromWorldMatching(c, ServerPlayer::isSpectator);
                        case "!spectating" -> getPlayersFromWorldMatching(c, p -> !p.isSpectator());
                        default -> {
                            ServerPlayer player = ((CarpetContext) c).server().getPlayerList().getPlayerByName(playerName);
                            yield player != null ? new EntityValue(player) : Value.NULL;
                        }
                    };
        });

        expression.addContextFunction("spawn", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'spawn' function takes mob name, and position to spawn");
            }
            String entityString = lv.get(0).getString();
            ResourceLocation entityId;
            try
            {
                entityId = ResourceLocation.read(new StringReader(entityString));
                EntityType<? extends Entity> type = cc.registry(Registries.ENTITY_TYPE).getOptional(entityId).orElse(null);
                if (type == null || !type.canSummon())
                {
                    return Value.NULL;
                }
            }
            catch (CommandSyntaxException ignored)
            {
                return Value.NULL;
            }

            Vector3Argument position = Vector3Argument.findIn(lv, 1);
            if (position.fromBlock)
            {
                position.vec = position.vec.subtract(0, 0.5, 0);
            }
            CompoundTag tag = new CompoundTag();
            boolean hasTag = false;
            if (lv.size() > position.offset)
            {
                Value nbt = lv.get(position.offset);
                NBTSerializableValue v = (nbt instanceof final NBTSerializableValue nbtsv)
                        ? nbtsv
                        : NBTSerializableValue.parseStringOrFail(nbt.getString());
                hasTag = true;
                tag = v.getCompoundTag();
            }
            tag.putString("id", entityId.toString());
            Vec3 vec3d = position.vec;

            ServerLevel serverWorld = cc.level();
            Entity entity = EntityType.loadEntityRecursive(tag, serverWorld, EntitySpawnReason.COMMAND, e -> {
                e.moveTo(vec3d.x, vec3d.y, vec3d.z, e.getYRot(), e.getXRot());
                return e;
            });
            if (entity == null)
            {
                return Value.NULL;
            }
            if (!hasTag && entity instanceof Mob mob)
            {
                mob.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.COMMAND, null);
            }
            if (!serverWorld.tryAddFreshEntityWithPassengers(entity))
            {
                entity.discard();
                return Value.NULL;
            }
            return new EntityValue(entity);
        });

        expression.addContextFunction("entity_id", 1, (c, t, lv) ->
        {
            Value who = lv.get(0);
            if (who instanceof final NumericValue numericValue)
            {
                return EntityValue.of(((CarpetContext) c).level().getEntity((int) numericValue.getLong()));
            }
            return EntityValue.of(((CarpetContext) c).level().getEntity(UUID.fromString(who.getString())));
        });

        expression.addContextFunction("entity_list", 1, (c, t, lv) ->
        {
            String who = lv.get(0).getString();
            CommandSourceStack source = ((CarpetContext) c).source();
            EntityValue.EntityClassDescriptor eDesc = EntityValue.getEntityDescriptor(who, source.getServer());
            List<? extends Entity> entityList = source.getLevel().getEntities(eDesc.directType, eDesc.filteringPredicate);
            return ListValue.wrap(entityList.stream().map(EntityValue::new));
        });

        expression.addContextFunction("entity_area", -1, (c, t, lv) ->
        {
            if (lv.size() < 3)
            {
                throw new InternalExpressionException("'entity_area' requires entity type, center and range arguments");
            }
            String who = lv.get(0).getString();
            CarpetContext cc = (CarpetContext) c;
            Vector3Argument centerLocator = Vector3Argument.findIn(lv, 1, false, true);

            AABB centerBox;
            if (centerLocator.entity != null)
            {
                centerBox = centerLocator.entity.getBoundingBox();
            }
            else
            {
                Vec3 center = centerLocator.vec;
                if (centerLocator.fromBlock)
                {
                    center.add(0.5, 0.5, 0.5);
                }
                centerBox = new AABB(center, center);
            }
            Vector3Argument rangeLocator = Vector3Argument.findIn(lv, centerLocator.offset);
            if (rangeLocator.fromBlock)
            {
                throw new InternalExpressionException("Range of 'entity_area' cannot come from a block argument");
            }
            Vec3 range = rangeLocator.vec;
            AABB area = centerBox.inflate(range.x, range.y, range.z);
            EntityValue.EntityClassDescriptor eDesc = EntityValue.getEntityDescriptor(who, cc.server());
            List<? extends Entity> entityList = cc.level().getEntities(eDesc.directType, area, eDesc.filteringPredicate);
            return ListValue.wrap(entityList.stream().map(EntityValue::new));
        });

        expression.addContextFunction("entity_selector", -1, (c, t, lv) ->
        {
            String selector = lv.get(0).getString();
            List<Value> retlist = new ArrayList<>();
            for (Entity e : EntityValue.getEntitiesFromSelector(((CarpetContext) c).source(), selector))
            {
                retlist.add(new EntityValue(e));
            }
            return ListValue.wrap(retlist);
        });

        expression.addContextFunction("query", -1, (c, t, lv) ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'query' takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0);
            if (!(v instanceof final EntityValue ev))
            {
                throw new InternalExpressionException("First argument to query should be an entity");
            }
            String what = lv.get(1).getString().toLowerCase(Locale.ROOT);
            if (what.equals("tags"))
            {
                c.host.issueDeprecation("'tags' for entity querying");
            }
            return switch (lv.size())
                    {
                        case 2 -> ev.get(what, null);
                        case 3 -> ev.get(what, lv.get(2));
                        default -> ev.get(what, ListValue.wrap(lv.subList(2, lv.size())));
                    };
        });

        // or update
        expression.addContextFunction("modify", -1, (c, t, lv) ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'modify' takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0);
            if (!(v instanceof final EntityValue ev))
            {
                throw new InternalExpressionException("First argument to modify should be an entity");
            }
            String what = lv.get(1).getString();
            switch (lv.size())
            {
                case 2 -> ev.set(what, null);
                case 3 -> ev.set(what, lv.get(2));
                default -> ev.set(what, ListValue.wrap(lv.subList(2, lv.size())));
            }
            return v;
        });

        expression.addContextFunction("entity_types", -1, (c, t, lv) ->
        {
            if (lv.size() > 1)
            {
                throw new InternalExpressionException("'entity_types' requires one or no arguments");
            }
            String desc = (lv.size() == 1) ? lv.get(0).getString() : "*";
            return EntityValue.getEntityDescriptor(desc, ((CarpetContext) c).server()).listValue(((CarpetContext) c).registryAccess());
        });

        expression.addContextFunction("entity_load_handler", -1, (c, t, lv) ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'entity_load_handler' required the entity type, and a function to call");
            }
            Value entityValue = lv.get(0);
            List<String> descriptors = (entityValue instanceof final ListValue list)
                    ? list.getItems().stream().map(Value::getString).toList()
                    : Collections.singletonList(entityValue.getString());
            Set<EntityType<? extends Entity>> types = new HashSet<>();
            descriptors.forEach(s -> types.addAll(EntityValue.getEntityDescriptor(s, ((CarpetContext) c).server()).types));
            FunctionArgument funArg = FunctionArgument.findIn(c, expression.module, lv, 1, true, false);
            CarpetEventServer events = ((CarpetScriptHost) c.host).scriptServer().events;
            if (funArg.function == null)
            {
                types.forEach(et -> events.removeBuiltInEvent(CarpetEventServer.Event.getEntityLoadEventName(et), (CarpetScriptHost) c.host));
                types.forEach(et -> events.removeBuiltInEvent(CarpetEventServer.Event.getEntityHandlerEventName(et), (CarpetScriptHost) c.host));
            }
            else
            {
                ///compat
                int numberOfArguments = funArg.function.getArguments().size() - funArg.args.size();
                if (numberOfArguments == 1)
                {
                    c.host.issueDeprecation("entity_load_handler() with single argument callback");
                    types.forEach(et -> events.addBuiltInEvent(CarpetEventServer.Event.getEntityLoadEventName(et), c.host, funArg.function, funArg.args));
                }
                else
                {
                    types.forEach(et -> events.addBuiltInEvent(CarpetEventServer.Event.getEntityHandlerEventName(et), c.host, funArg.function, funArg.args));
                }
            }
            return new NumericValue(types.size());
        });

        // or update
        expression.addContextFunction("entity_event", -1, (c, t, lv) ->
        {
            if (lv.size() < 3)
            {
                throw new InternalExpressionException("'entity_event' requires at least 3 arguments, entity, event to be handled, and function name, with optional arguments");
            }
            Value v = lv.get(0);
            if (!(v instanceof final EntityValue ev))
            {
                throw new InternalExpressionException("First argument to entity_event should be an entity");
            }
            String what = lv.get(1).getString();

            FunctionArgument funArg = FunctionArgument.findIn(c, expression.module, lv, 2, true, false);

            ev.setEvent((CarpetContext) c, what, funArg.function, funArg.args);

            return Value.NULL;
        });
    }
}
