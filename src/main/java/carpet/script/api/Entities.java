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
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Entities {
    private static ListValue getPlayersFromWorldMatching(Context c, Predicate<ServerPlayer> condition) {
        List<Value> ret = new ArrayList<>();
        for (ServerPlayer player: ((CarpetContext) c).s.getLevel().players()) {
            if (condition.test(player)) {
                ret.add(new EntityValue(player));
            }
        }
        return ListValue.wrap(ret);
    }

    public static void apply(Expression expression)
    {
        expression.addContextFunction("player", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
            {
                CarpetContext cc = (CarpetContext)c;
                if (cc.host.user != null)
                {
                    ServerPlayer player = cc.s.getServer().getPlayerList().getPlayerByName(cc.host.user);
                    return EntityValue.of(player);
                }
                Entity callingEntity = cc.s.getEntity();
                if (callingEntity instanceof Player) return EntityValue.of(callingEntity);
                Vec3 pos = ((CarpetContext)c).s.getPosition();
                Player closestPlayer = ((CarpetContext)c).s.getLevel().getNearestPlayer(pos.x, pos.y, pos.z, -1.0, EntitySelector.ENTITY_STILL_ALIVE);
                return EntityValue.of(closestPlayer);
            }
            String playerName = lv.get(0).getString();
            return switch (playerName) {
                case "all" -> {
                    List<Value> ret = new ArrayList<>();
                    for (ServerPlayer player: ((CarpetContext)c).s.getServer().getPlayerList().getPlayers())
                        ret.add(new EntityValue(player));
                    yield ListValue.wrap(ret);
                }
                case "*" -> getPlayersFromWorldMatching(c, p -> true);
                case "survival" -> getPlayersFromWorldMatching(c, p -> p.gameMode.isSurvival()); // todo assert correct
                case "creative" -> getPlayersFromWorldMatching(c, ServerPlayer::isCreative);
                case "spectating" -> getPlayersFromWorldMatching(c, ServerPlayer::isSpectator);
                case "!spectating" -> getPlayersFromWorldMatching(c, p -> !p.isSpectator());
                default -> {
                    ServerPlayer player = ((CarpetContext) c).s.getServer().getPlayerList().getPlayerByName(playerName);
                    if (player != null) {
                        yield new EntityValue(player);
                    }
                    yield Value.NULL;
                }
            };
        });

        expression.addContextFunction("spawn", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 2)
                throw new InternalExpressionException("'spawn' function takes mob name, and position to spawn");
            String entityString = lv.get(0).getString();
            ResourceLocation entityId;
            try
            {
                entityId = ResourceLocation.read(new StringReader(entityString));
                EntityType<? extends Entity> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
                if (type == null || !type.canSummon())
                    return Value.NULL;
            }
            catch (CommandSyntaxException exception)
            {
                 return Value.NULL;
            }

            Vector3Argument position = Vector3Argument.findIn(lv, 1);
            if (position.fromBlock)
                position.vec = position.vec.subtract(0, 0.5, 0);
            CompoundTag tag = new CompoundTag();
            boolean hasTag = false;
            if (lv.size() > position.offset)
            {
                Value nbt = lv.get(position.offset);
                NBTSerializableValue v = (nbt instanceof NBTSerializableValue) ? (NBTSerializableValue) nbt
                        : NBTSerializableValue.parseString(nbt.getString(), true);
                hasTag = true;
                tag = v.getCompoundTag();

            }
            tag.putString("id", entityId.toString());
            Vec3 vec3d = position.vec;

            ServerLevel serverWorld = cc.s.getLevel();
            Entity entity_1 = EntityType.loadEntityRecursive(tag, serverWorld, (entity_1x) -> {
                entity_1x.moveTo(vec3d.x, vec3d.y, vec3d.z, entity_1x.getYRot(), entity_1x.getXRot());
                return !serverWorld.addWithUUID(entity_1x) ? null : entity_1x;
            });
            if (entity_1 == null) {
                return Value.NULL;
            } else {
                if (!hasTag && entity_1 instanceof Mob) {
                    ((Mob)entity_1).finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(entity_1.blockPosition()), MobSpawnType.COMMAND, null, null);
                }
                return new EntityValue(entity_1);
            }
        });

        expression.addContextFunction("entity_id", 1, (c, t, lv) ->
        {
            Value who = lv.get(0);
            if (who instanceof NumericValue)
                return EntityValue.of(((CarpetContext)c).s.getLevel().getEntity((int)((NumericValue) who).getLong()));
            return EntityValue.of(((CarpetContext)c).s.getLevel().getEntity(UUID.fromString(who.getString())));
        });

        expression.addContextFunction("entity_list", 1, (c, t, lv) ->
        {
            String who = lv.get(0).getString();
            CommandSourceStack source = ((CarpetContext)c).s;
            EntityValue.EntityClassDescriptor eDesc = EntityValue.getEntityDescriptor(who, source.getServer());
            List<? extends Entity> entityList = source.getLevel().getEntities(eDesc.directType, eDesc.filteringPredicate);
            return ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
        });

        expression.addContextFunction("entity_area", -1, (c, t, lv) ->
        {
            if (lv.size()<3) throw new InternalExpressionException("'entity_area' requires entity type, center and range arguments");
            String who = lv.get(0).getString();
            CarpetContext cc = (CarpetContext)c;
            Vector3Argument centerLocator = Vector3Argument.findIn(lv, 1, false, true);

            AABB centerBox;
            if (centerLocator.entity != null)
            {
                centerBox = centerLocator.entity.getBoundingBox();
            }
            else
            {
                Vec3 center = centerLocator.vec;
                if (centerLocator.fromBlock) center.add(0.5, 0.5, 0.5);
                centerBox = new AABB(center, center);
            }
            Vector3Argument rangeLocator = Vector3Argument.findIn(lv, centerLocator.offset);
            if (rangeLocator.fromBlock)
                throw new InternalExpressionException("Range of 'entity_area' cannot come from a block argument");
            Vec3 range = rangeLocator.vec;
            AABB area = centerBox.inflate(range.x, range.y, range.z);
            EntityValue.EntityClassDescriptor eDesc = EntityValue.getEntityDescriptor(who, cc.s.getServer());
            List<? extends Entity> entityList = cc.s.getLevel().getEntities(eDesc.directType, area,eDesc.filteringPredicate);
            return ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
        });

        expression.addContextFunction("entity_selector", -1, (c, t, lv) ->
        {
            String selector = lv.get(0).getString();
            List<Value> retlist = new ArrayList<>();
            for (Entity e: EntityValue.getEntitiesFromSelector(((CarpetContext)c).s, selector))
            {
                retlist.add(new EntityValue(e));
            }
            return ListValue.wrap(retlist);
        });

        expression.addContextFunction("query", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("'query' takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to query should be an entity");
            String what = lv.get(1).getString().toLowerCase(Locale.ROOT);
            if (what.equals("tags"))
                c.host.issueDeprecation("'tags' for entity querying");
            Value retval;
            if (lv.size()==2)
                retval = ((EntityValue) v).get(what, null);
            else if (lv.size()==3)
                retval = ((EntityValue) v).get(what, lv.get(2));
            else
                retval = ((EntityValue) v).get(what, ListValue.wrap(lv.subList(2, lv.size())));
            return retval;
        });

        // or update
        expression.addContextFunction("modify", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("'modify' takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to modify should be an entity");
            String what = lv.get(1).getString();
            if (lv.size()==2)
                ((EntityValue) v).set(what, null);
            else if (lv.size()==3)
                ((EntityValue) v).set(what, lv.get(2));
            else
                ((EntityValue) v).set(what, ListValue.wrap(lv.subList(2, lv.size())));
            return v;
        });

        expression.addContextFunction("entity_types", -1, (c, t, lv) ->
        {
            if (lv.size() > 1) throw new InternalExpressionException("'entity_types' requires one or no arguments");
            String desc = (lv.size() == 1)?lv.get(0).getString():"*";
            return EntityValue.getEntityDescriptor(desc, ((CarpetContext) c).s.getServer()).listValue();
        });

        expression.addContextFunction("entity_load_handler", -1, (c, t, lv) ->
        {
            if (lv.size() < 2) throw new InternalExpressionException("'entity_load_handler' required the entity type, and a function to call");
            Value entityValue = lv.get(0);
            List<String> descriptors = (entityValue instanceof ListValue)
                    ? ((ListValue) entityValue).getItems().stream().map(Value::getString).collect(Collectors.toList())
                    : Collections.singletonList(entityValue.getString());
            Set<EntityType<? extends Entity>> types = new HashSet<>();
            descriptors.forEach(s -> types.addAll(EntityValue.getEntityDescriptor(s, ((CarpetContext) c).s.getServer()).types));
            FunctionArgument funArg = FunctionArgument.findIn(c, expression.module, lv, 1, true, false);
            CarpetEventServer events = ((CarpetScriptHost)c.host).scriptServer().events;
            if (funArg.function == null)
            {
                types.forEach(et -> events.removeBuiltInEvent(CarpetEventServer.Event.getEntityLoadEventName(et), (CarpetScriptHost) c.host));
                types.forEach(et -> events.removeBuiltInEvent(CarpetEventServer.Event.getEntityHandlerEventName(et), (CarpetScriptHost) c.host));
            }
            else
            {
                ///compat
                int argno = funArg.function.getArguments().size() - funArg.args.size();
                if (argno == 1)
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
            if (lv.size()<3)
                throw new InternalExpressionException("'entity_event' requires at least 3 arguments, entity, event to be handled, and function name, with optional arguments");
            Value v = lv.get(0);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to entity_event should be an entity");
            String what = lv.get(1).getString();

            FunctionArgument funArg = FunctionArgument.findIn(c, expression.module, lv, 2, true, false);

            ((EntityValue) v).setEvent((CarpetContext)c, what, funArg.function, funArg.args);

            return Value.NULL;
        });
    }
}
