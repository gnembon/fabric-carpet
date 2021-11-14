package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.CarpetScriptHost;
import carpet.script.Expression;
import carpet.script.argument.FunctionArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.BooleanValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.utils.PerimeterDiagnostics;
import carpet.utils.SpawnReporter.SpawnReport;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.SpawnHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static carpet.utils.SpawnReporter.spawnReport;

public class Entities {
    public static void apply(Expression expression)
    {
        expression.addContextFunction("player", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
            {
                CarpetContext cc = (CarpetContext)c;
                if (cc.host.user != null)
                {
                    ServerPlayerEntity player = cc.s.getServer().getPlayerManager().getPlayer(cc.host.user);
                    return EntityValue.of(player);
                }
                Entity callingEntity = cc.s.getEntity();
                if (callingEntity instanceof PlayerEntity) return EntityValue.of(callingEntity);
                Vec3d pos = ((CarpetContext)c).s.getPosition();
                PlayerEntity closestPlayer = ((CarpetContext)c).s.getWorld().getClosestPlayer(pos.x, pos.y, pos.z, -1.0, EntityPredicates.VALID_ENTITY);
                return EntityValue.of(closestPlayer);
            }
            String playerName = lv.get(0).getString();
            Value retval = Value.NULL;
            if ("all".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getServer().getPlayerManager().getPlayerList().
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
                ServerPlayerEntity player = ((CarpetContext) c).s.getServer().getPlayerManager().getPlayer(playerName);
                if (player != null)
                    retval = new EntityValue(player);
            }
            return retval;
        });

        expression.addContextFunction("spawn", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 2)
                throw new InternalExpressionException("'spawn' function takes mob name, and position to spawn");
            String entityString = lv.get(0).getString();
            Identifier entityId;
            try
            {
                entityId = Identifier.fromCommandInput(new StringReader(entityString));
                EntityType<? extends Entity> type = Registry.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
                if (type == null || !type.isSummonable())
                    return Value.NULL;
            }
            catch (CommandSyntaxException exception)
            {
                 return Value.NULL;
            }

            Vector3Argument position = Vector3Argument.findIn(lv, 1);
            if (position.fromBlock)
                position.vec = position.vec.subtract(0, 0.5, 0);
            NbtCompound tag = new NbtCompound();
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
            Vec3d vec3d = position.vec;

            ServerWorld serverWorld = cc.s.getWorld();
            Entity entity_1 = EntityType.loadEntityWithPassengers(tag, serverWorld, (entity_1x) -> {
                entity_1x.refreshPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, entity_1x.getYaw(), entity_1x.getPitch());
                return !serverWorld.tryLoadEntity(entity_1x) ? null : entity_1x;
            });
            if (entity_1 == null) {
                return Value.NULL;
            } else {
                if (!hasTag && entity_1 instanceof MobEntity) {
                    ((MobEntity)entity_1).initialize(serverWorld, serverWorld.getLocalDifficulty(entity_1.getBlockPos()), SpawnReason.COMMAND, null, null);
                }
                return new EntityValue(entity_1);
            }
        });
        expression.addContextFunction("can_spawn", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 2)
                throw new InternalExpressionException("'can_spawn' function takes mob name, and position to check for spawning conditions");

            Value entity = lv.get(0);
            if(entity == Value.NULL) return Value.FALSE;
            Entity entity_1;
            String entityString;
            ServerWorld serverWorld = cc.s.getWorld();

            Vector3Argument position = Vector3Argument.findIn(lv, 1);
            if (position.fromBlock)
                position.vec = position.vec.subtract(0, 0.5, 0);
            Vec3d vec3d = position.vec;

            boolean startedWithEntity = entity instanceof EntityValue;

            if(startedWithEntity){
                entity_1 = ((EntityValue) entity).getEntity();
                entityString = entity_1.getEntityName();
            } else {
                entityString = lv.get(0).getString();
                Identifier entityId;
                try {
                    entityId = Identifier.fromCommandInput(new StringReader(entityString));
                    EntityType<? extends Entity> type = Registry.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
                    if (type == null || !type.isSummonable())
                        return Value.NULL;
                } catch (CommandSyntaxException exception) {
                    return Value.NULL;
                }

                NbtCompound tag = new NbtCompound();
                tag.putString("id", entityId.toString());

                entity_1 = EntityType.loadEntityWithPassengers(tag, serverWorld, (entity_1x) -> {
                    entity_1x.refreshPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, entity_1x.getYaw(), entity_1x.getPitch());
                    return !serverWorld.tryLoadEntity(entity_1x) ? null : entity_1x;
                });
                if(entity_1 instanceof MobEntity)
                    ((MobEntity)entity_1).initialize(serverWorld, serverWorld.getLocalDifficulty(entity_1.getBlockPos()), SpawnReason.COMMAND, null, null);

            }
            if (entity_1 == null) {
                return Value.FALSE;
            } else if (entity_1 instanceof MobEntity) {
                boolean canSpawn = SpawnHelper.canSpawn(SpawnRestriction.Location.ON_GROUND, serverWorld, new BlockPos(vec3d), entity_1.getType());
                if(!startedWithEntity) entity_1.discard();
                return BooleanValue.of(canSpawn);
            } else {
                throw new InternalExpressionException("Invalid entity type '" + entityString + "' to check spawning conditions for, 'can_spawn' requires a mob entity");
            }
        });

        expression.addContextFunction("will_spawn", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 2)
                throw new InternalExpressionException("'will_spawn' function takes mob name, and position to check for spawning conditions");

            Value entity = lv.get(0);
            if(entity == Value.NULL) return Value.FALSE;
            Entity entity_1;
            String entityString;
            ServerWorld serverWorld = cc.s.getWorld();

            Vector3Argument position = Vector3Argument.findIn(lv, 1);
            if (position.fromBlock)
                position.vec = position.vec.subtract(0, 0.5, 0);
            Vec3d vec3d = position.vec;

            boolean startedWithEntity = entity instanceof EntityValue;

            if(startedWithEntity){
                entity_1 = ((EntityValue) entity).getEntity();
                entityString = entity_1.getEntityName();
            } else {
                entityString = lv.get(0).getString();
                Identifier entityId;
                try {
                    entityId = Identifier.fromCommandInput(new StringReader(entityString));
                    EntityType<? extends Entity> type = Registry.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
                    if (type == null || !type.isSummonable())
                        return Value.NULL;
                } catch (CommandSyntaxException exception) {
                    return Value.NULL;
                }

                NbtCompound tag = new NbtCompound();
                tag.putString("id", entityId.toString());

                entity_1 = EntityType.loadEntityWithPassengers(tag, serverWorld, (entity_1x) -> {
                    entity_1x.refreshPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, entity_1x.getYaw(), entity_1x.getPitch());
                    return !serverWorld.tryLoadEntity(entity_1x) ? null : entity_1x;
                });
                if(entity_1 instanceof MobEntity)
                    ((MobEntity)entity_1).initialize(serverWorld, serverWorld.getLocalDifficulty(entity_1.getBlockPos()), SpawnReason.COMMAND, null, null);

            }

            int num_tries = position.offset==lv.size()? 20 : (int) lv.get(position.offset).readInteger();

            if (entity_1 == null) {
                return Value.FALSE;
            } else if (entity_1 instanceof MobEntity) {
                int will_spawn = 0;

                BlockPos pos = new BlockPos(vec3d);

                for (int i = 0; i < num_tries; ++i)
                {
                    if (
                            SpawnRestriction.canSpawn(entity_1.getType(),serverWorld, SpawnReason.NATURAL, pos, serverWorld.random) &&
                                    SpawnHelper.canSpawn(SpawnRestriction.getLocation(entity_1.getType()), serverWorld, pos, entity_1.getType()) &&
                                    ((MobEntity) entity_1).canSpawn(serverWorld, SpawnReason.NATURAL)
                        // && mob.canSpawn(worldIn) // entity collisions // mostly - except ocelots
                    )
                    {
                        if (entity_1.getType() == EntityType.OCELOT)
                        {
                            BlockState blockState = serverWorld.getBlockState(pos.down());
                            if ((pos.getY() < serverWorld.getSeaLevel()) || !(blockState.isOf(Blocks.GRASS_BLOCK) || blockState.isIn(BlockTags.LEAVES))) {
                                continue;
                            }
                        }
                        will_spawn += 1;
                    }
                }

                if(!startedWithEntity) entity_1.discard();
                return NumericValue.of(will_spawn);
            } else {
                throw new InternalExpressionException("Invalid entity type '" + entityString + "' to check spawning conditions for, 'can_spawn' requires a mob entity");
            }
        });

        expression.addContextFunction("perimeter_info", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 2)
                throw new InternalExpressionException("'perimeter_info' function takes mob name, and position to check for spawning conditions");

            String entityString = lv.get(0).getString();

            Vector3Argument position = Vector3Argument.findIn(lv, 1);
            if (position.fromBlock)
                position.vec = position.vec.subtract(0, 0.5, 0);

            NbtCompound nbttagcompound = new NbtCompound();
            MobEntity entityliving = null;
            if (!entityString.equals("null")) {
                nbttagcompound.putString("id", entityString);
                Entity baseEntity = EntityType.loadEntityWithPassengers(nbttagcompound, cc.s.getWorld(), (entity_1x) -> {
                    entity_1x.refreshPositionAndAngles(new BlockPos(position.vec.getX(), -10, position.vec.getZ()), entity_1x.getYaw(), entity_1x. getPitch());
                    return !cc.s.getWorld().tryLoadEntity(entity_1x) ? null : entity_1x;
                });
                if (!(baseEntity instanceof  MobEntity)) {
                    if (baseEntity != null) baseEntity.discard();
                    throw new InternalExpressionException("'perimeter_info' function requires a mob entity to test against");
                }
                entityliving = (MobEntity) baseEntity;
            }
            PerimeterDiagnostics.Result res = PerimeterDiagnostics.countSpots(cc.s.getWorld(), new BlockPos(position.vec), entityliving);

            return ListValue.of(
                    new NumericValue(res.ground),
                    new NumericValue(res.liquid),
                    new NumericValue(res.specific),
                    new ListValue(res.samples.stream().map(ValueConversions::of).collect(Collectors.toList()))
            );
        }); //todo decide whether or not to remove this

        expression.addContextFunction("mob_spawns", -1, (c, t, lv)->{
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 1)
                throw new InternalExpressionException("'mob_spawns' function takes position to check spawns");

            Vector3Argument position = Vector3Argument.findIn(lv, 0);

            Map<SpawnGroup,List<SpawnReport>> spawnReport = spawnReport(new BlockPos(position.vec), cc.s.getWorld());

            Map<Value, Value> finalReport = new HashMap<>();
            List<Value> groupReport = new ArrayList<>();

            for (Map.Entry<SpawnGroup, List<SpawnReport>> spawnGroupListEntry : spawnReport.entrySet()) {
                groupReport.clear();
                for (SpawnReport report : spawnGroupListEntry.getValue()) {
                    groupReport.add(ListValue.of(
                        BooleanValue.of(report.canSpawn),//boolean can spawn
                        BooleanValue.of(report.fitsTrue),//boolean can fit
                        BooleanValue.of(report.fitsFalse),//boolean can not fit (slimes)
                        new NumericValue(report.willSpawn),//integer percentage will spawn
                        new NumericValue(report.chunkSpawnLimit),//max spawns per chunk
                        new NumericValue(report.spawnEntry.getWeight().getValue())//spawn weight
                    ));
                }
                finalReport.put(StringValue.of(spawnGroupListEntry.getKey().getName()), ListValue.wrap(groupReport));
            }

            return MapValue.wrap(finalReport);
        });

        expression.addContextFunction("entity_id", 1, (c, t, lv) ->
        {
            Value who = lv.get(0);
            if (who instanceof NumericValue)
                return EntityValue.of(((CarpetContext)c).s.getWorld().getEntityById((int)((NumericValue) who).getLong()));
            return EntityValue.of(((CarpetContext)c).s.getWorld().getEntity(UUID.fromString(who.getString())));
        });

        expression.addContextFunction("entity_list", 1, (c, t, lv) ->
        {
            String who = lv.get(0).getString();
            ServerCommandSource source = ((CarpetContext)c).s;
            EntityValue.EntityClassDescriptor eDesc = EntityValue.getEntityDescriptor(who, source.getServer());
            List<? extends Entity> entityList = source.getWorld().getEntitiesByType(eDesc.directType, eDesc.filteringPredicate);
            return ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
        });

        expression.addContextFunction("entity_area", -1, (c, t, lv) ->
        {
            if (lv.size()<3) throw new InternalExpressionException("'entity_area' requires entity type, center and range arguments");
            String who = lv.get(0).getString();
            CarpetContext cc = (CarpetContext)c;
            Vector3Argument centerLocator = Vector3Argument.findIn(lv, 1, false, true);

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
            Vector3Argument rangeLocator = Vector3Argument.findIn(lv, centerLocator.offset);
            if (rangeLocator.fromBlock)
                throw new InternalExpressionException("Range of 'entity_area' cannot come from a block argument");
            Vec3d range = rangeLocator.vec;
            Box area = centerBox.expand(range.x, range.y, range.z);
            EntityValue.EntityClassDescriptor eDesc = EntityValue.getEntityDescriptor(who, cc.s.getServer());
            List<? extends Entity> entityList = cc.s.getWorld().getEntitiesByType(eDesc.directType, area,eDesc.filteringPredicate);
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
            return EntityValue.getEntityDescriptor(desc, ((CarpetContext) c).s.getServer()).listValue;
        });

        expression.addContextFunction("entity_load_handler", -1, (c, t, lv) ->
        {
            if (lv.size() < 2) throw new InternalExpressionException("'entity_load_handler' required the entity type, and a function to call");
            Value entityValue = lv.get(0);
            List<String> descriptors = (entityValue instanceof ListValue)
                    ? ((ListValue) entityValue).getItems().stream().map(Value::getString).collect(Collectors.toList())
                    : Collections.singletonList(entityValue.getString());
            Set<EntityType<? extends Entity>> types = new HashSet<>();
            descriptors.forEach(s -> types.addAll(EntityValue.getEntityDescriptor(s, ((CarpetContext) c).s.getServer()).typeList));
            FunctionArgument funArg = FunctionArgument.findIn(c, expression.module, lv, 1, true, false);
            CarpetEventServer events = ((CarpetScriptHost)c.host).getScriptServer().events;
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
