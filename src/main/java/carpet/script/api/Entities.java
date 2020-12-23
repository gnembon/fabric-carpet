package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.CarpetScriptHost;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.argument.FunctionArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.EntityValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Entities {
    public static void apply(Expression expression)
    {
        expression.addLazyFunction("player", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
            {
                CarpetContext cc = (CarpetContext)c;
                if (cc.host.user != null)
                {
                    ServerPlayerEntity player = cc.s.getMinecraftServer().getPlayerManager().getPlayer(cc.host.user);
                    if (player == null) return LazyValue.NULL;
                    Value ret = new EntityValue(player);
                    return (_c, _t) -> ret;
                }
                Entity callingEntity = cc.s.getEntity();
                if (callingEntity instanceof PlayerEntity)
                {
                    Value retval = new EntityValue(callingEntity);
                    return (_c, _t) -> retval;
                }
                Vec3d pos = ((CarpetContext)c).s.getPosition();
                PlayerEntity closestPlayer = ((CarpetContext)c).s.getWorld().getClosestPlayer(pos.x, pos.y, pos.z, -1.0, EntityPredicates.VALID_ENTITY);
                if (closestPlayer != null)
                {
                    Value retval = new EntityValue(closestPlayer);
                    return (_c, _t) -> retval;
                }
                return (_c, _t) -> Value.NULL;
            }
            String playerName = lv.get(0).evalValue(c).getString();
            Value retval = Value.NULL;
            if ("all".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getMinecraftServer().getPlayerManager().getPlayerList().
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("*".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers().
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("survival".equalsIgnoreCase(playerName))
            {
                retval =  ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers((p) -> p.interactionManager.isSurvivalLike()).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("creative".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers(PlayerEntity::isCreative).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("spectating".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers(PlayerEntity::isSpectator).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("!spectating".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers((p) -> !p.isSpectator()).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else
            {
                ServerPlayerEntity player = ((CarpetContext) c).s.getMinecraftServer().getPlayerManager().getPlayer(playerName);
                if (player != null)
                    retval = new EntityValue(player);
            }
            Value finalVar = retval;
            return (_c, _t) -> finalVar;
        });

        expression.addLazyFunction("spawn", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 2)
                throw new InternalExpressionException("'spawn' function takes mob name, and position to spawn");
            String entityString = lv.get(0).evalValue(c).getString();
            Identifier entityId;
            try
            {
                entityId = Identifier.fromCommandInput(new StringReader(entityString));
                EntityType<? extends Entity> type = Registry.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
                if (type == null || !type.isSummonable())
                    return LazyValue.NULL;
            }
            catch (CommandSyntaxException exception)
            {
                 return LazyValue.NULL;
            }

            Vector3Argument position = Vector3Argument.findIn(cc, lv, 1);
            if (position.fromBlock)
                position.vec = position.vec.subtract(0, 0.5, 0);
            CompoundTag tag = new CompoundTag();
            boolean hasTag = false;
            if (lv.size() > position.offset)
            {
                Value nbt = lv.get(position.offset).evalValue(c);
                NBTSerializableValue v = (nbt instanceof NBTSerializableValue) ? (NBTSerializableValue) nbt
                        : NBTSerializableValue.parseString(nbt.getString(), true);
                hasTag = true;
                tag = v.getCompoundTag();

            }
            tag.putString("id", entityId.toString());
            Vec3d vec3d = position.vec;

            ServerWorld serverWorld = cc.s.getWorld();
            Entity entity_1 = EntityType.loadEntityWithPassengers(tag, serverWorld, (entity_1x) -> {
                entity_1x.refreshPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, entity_1x.yaw, entity_1x.pitch);
                return !serverWorld.tryLoadEntity(entity_1x) ? null : entity_1x;
            });
            if (entity_1 == null) {
                return LazyValue.NULL;
            } else {
                if (!hasTag && entity_1 instanceof MobEntity) {
                    ((MobEntity)entity_1).initialize(serverWorld, serverWorld.getLocalDifficulty(entity_1.getBlockPos()), SpawnReason.COMMAND, null, null);
                }
                Value res = new EntityValue(entity_1);
                return (_c, _t) -> res;
            }
        });

        expression.addLazyFunction("entity_id", 1, (c, t, lv) ->
        {
            Value who = lv.get(0).evalValue(c);
            Entity e;
            if (who instanceof NumericValue)
            {
                e = ((CarpetContext)c).s.getWorld().getEntityById((int)((NumericValue) who).getLong());
            }
            else
            {
                e = ((CarpetContext)c).s.getWorld().getEntity(UUID.fromString(who.getString()));
            }
            if (e==null)
            {
                return LazyValue.NULL;
            }
            return (cc, tt) -> new EntityValue(e);
        });

        expression.addLazyFunction("entity_list", 1, (c, t, lv) ->
        {
            String who = lv.get(0).evalValue(c).getString();
            ServerCommandSource source = ((CarpetContext)c).s;
            EntityValue.EntityClassDescriptor eDesc = EntityValue.getEntityDescriptor(who, source.getMinecraftServer());
            List<Entity> entityList = source.getWorld().getEntitiesByType(eDesc.directType, eDesc.filteringPredicate);
            Value retval = ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
            return (_c, _t ) -> retval;
        });

        expression.addLazyFunction("entity_area", -1, (c, t, lv) ->
        {
            if (lv.size()<3) throw new InternalExpressionException("'entity_area' requires entity type, center and range arguments");
            String who = lv.get(0).evalValue(c).getString();
            CarpetContext cc = (CarpetContext)c;
            Vector3Argument centerLocator = Vector3Argument.findIn(cc, lv, 1, false, true);

            Box centerBox;
            if (centerLocator.entity != null)
            {
                centerBox = centerLocator.entity.getBoundingBox();
            }
            else
            {
                Vec3d center = centerLocator.vec;
                if (centerLocator.fromBlock) center.add(0.5, 0.5, 0.5);
                centerBox = new Box(center, center);
            }
            Vector3Argument rangeLocator = Vector3Argument.findIn(cc, lv, centerLocator.offset);
            if (rangeLocator.fromBlock)
                throw new InternalExpressionException("Range of 'entity_area' cannot come from a block argument");
            Vec3d range = rangeLocator.vec;
            Box area = centerBox.expand(range.x, range.y, range.z);
            EntityValue.EntityClassDescriptor eDesc = EntityValue.getEntityDescriptor(who, cc.s.getMinecraftServer());
            List<? extends Entity> entityList = cc.s.getWorld().getEntitiesByType(eDesc.directType, area,eDesc.filteringPredicate);
            Value retval = ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
            return (_c, _t ) -> retval;
        });

        expression.addLazyFunction("entity_selector", -1, (c, t, lv) ->
        {
            String selector = lv.get(0).evalValue(c).getString();
            List<Value> retlist = new ArrayList<>();
            for (Entity e: EntityValue.getEntitiesFromSelector(((CarpetContext)c).s, selector))
            {
                retlist.add(new EntityValue(e));
            }
            return (c_, t_) -> ListValue.wrap(retlist);
        });

        expression.addLazyFunction("query", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("'query' takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to query should be an entity");
            String what = lv.get(1).evalValue(c).getString().toLowerCase(Locale.ROOT);
            if (what.equals("tags"))
                c.host.issueDeprecation("'tags' for entity querying");
            Value retval;
            if (lv.size()==2)
                retval = ((EntityValue) v).get(what, null);
            else if (lv.size()==3)
                retval = ((EntityValue) v).get(what, lv.get(2).evalValue(c));
            else
                retval = ((EntityValue) v).get(what, ListValue.wrap(lv.subList(2, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList())));
            return (cc, tt) -> retval;
        });

        // or update
        expression.addLazyFunction("modify", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("'modify' takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to modify should be an entity");
            String what = lv.get(1).evalValue(c).getString();
            if (lv.size()==2)
                ((EntityValue) v).set(what, null);
            else if (lv.size()==3)
                ((EntityValue) v).set(what, lv.get(2).evalValue(c));
            else
                ((EntityValue) v).set(what, ListValue.wrap(lv.subList(2, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList())));
            return (cc, tt) -> v;
        });

        expression.addLazyFunction("entity_types", -1, (c, t, lv) ->
        {

            if (lv.size() > 1) throw new InternalExpressionException("'entity_types' requires one or no arguments");
            String desc = (lv.size() == 1)?lv.get(0).evalValue(c).getString():"*";
            Value ret = EntityValue.getEntityDescriptor(desc, ((CarpetContext) c).s.getMinecraftServer()).listValue;
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("entity_load_handler", -1, (c, t, lv) ->
        {
            if (c.host.isPerUser()) throw new InternalExpressionException("'entity_load_handler' can only be called in apps with global scope");
            if (lv.size() < 2) throw new InternalExpressionException("'entity_load_handler' required the entity type, and a function to call");
            Value entityValue = lv.get(0).evalValue(c);
            List<String> descriptors = (entityValue instanceof ListValue)
                    ? ((ListValue) entityValue).getItems().stream().map(Value::getString).collect(Collectors.toList())
                    : Collections.singletonList(entityValue.getString());
            Set<EntityType<? extends Entity>> types = new HashSet<>();
            descriptors.forEach(s -> types.addAll(EntityValue.getEntityDescriptor(s, ((CarpetContext) c).s.getMinecraftServer()).typeList));
            FunctionArgument<LazyValue> funArg = FunctionArgument.findIn(c, expression.module, lv, 1, true, false);
            CarpetEventServer events = ((CarpetScriptHost)c.host).getScriptServer().events;
            if (funArg.function == null)
            {
                types.forEach(et -> events.removeBuiltInEvent(CarpetEventServer.Event.getEntityLoadEventName(et), (CarpetScriptHost) c.host));
            }
            else
            {
                types.forEach(et -> events.addBuiltInEvent(CarpetEventServer.Event.getEntityLoadEventName(et), c.host, funArg.function, FunctionValue.resolveArgs(funArg.args, c, t)));
            }
            Value ret = new NumericValue(types.size());
            return (cc, tt) -> ret;
        });

        // or update
        expression.addLazyFunction("entity_event", -1, (c, t, lv) ->
        {
            if (lv.size()<3)
                throw new InternalExpressionException("'entity_event' requires at least 3 arguments, entity, event to be handled, and function name, with optional arguments");
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to entity_event should be an entity");
            String what = lv.get(1).evalValue(c).getString();

            FunctionArgument<LazyValue> funArg = FunctionArgument.findIn(c, expression.module, lv, 2, true, false);

            ((EntityValue) v).setEvent((CarpetContext)c, what, funArg.function, FunctionValue.resolveArgs(funArg.args, c, t));

            return LazyValue.NULL;
        });
    }
}
