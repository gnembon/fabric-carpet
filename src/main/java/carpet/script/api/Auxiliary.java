package carpet.script.api;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.fakes.MinecraftServerInterface;
import carpet.fakes.StatTypeInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.helpers.FeatureGenerator;
import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.CarpetScriptHost;
import carpet.script.CarpetScriptServer;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.FunctionArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.InternalExpressionException;
import carpet.script.utils.ShapeDispatcher;
import carpet.script.utils.WorldTools;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.utils.Messenger;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Auxiliary {
    public static final String MARKER_STRING = "__scarpet_marker";
    private static final Map<String, SoundCategory> mixerMap = Arrays.stream(SoundCategory.values()).collect(Collectors.toMap(SoundCategory::getName, k -> k));

    public static String recognizeResource(Value value)
    {
        String origfile = value.getString();
        String file = origfile.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9\\-+_/]", "");
        file = Arrays.stream(file.split("/+")).filter(s -> !s.isEmpty()).collect(Collectors.joining("/"));
        if (file.isEmpty())
        {
            throw new InternalExpressionException("Cannot use "+origfile+" as resource name - must have some letters and numbers");
        }
        return file;
    }

    public static void apply(Expression expression)
    {
        expression.addLazyFunction("sound", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            Identifier soundName = new Identifier(lv.get(0).evalValue(c).getString());
            Vector3Argument locator = Vector3Argument.findIn(cc, lv, 1);
            if (Registry.SOUND_EVENT.get(soundName) == null)
                throw new InternalExpressionException("No such sound: "+soundName.getPath());
            float volume = 1.0F;
            float pitch = 1.0F;
            SoundCategory mixer = SoundCategory.MASTER;
            if (lv.size() > 0+locator.offset)
            {
                volume = (float) NumericValue.asNumber(lv.get(0+locator.offset).evalValue(c)).getDouble();
                if (lv.size() > 1+locator.offset)
                {
                    pitch = (float) NumericValue.asNumber(lv.get(1+locator.offset).evalValue(c)).getDouble();
                    if (lv.size() > 2+locator.offset)
                    {
                        String mixerName = lv.get(2+locator.offset).evalValue(c).getString();
                        mixer = mixerMap.get(mixerName.toLowerCase(Locale.ROOT));
                        if (mixer == null) throw  new InternalExpressionException(mixerName +" is not a valid mixer name");
                    }
                }
            }
            Vec3d vec = locator.vec;
            double d0 = Math.pow(volume > 1.0F ? (double)(volume * 16.0F) : 16.0D, 2.0D);
            int count = 0;
            for (ServerPlayerEntity player : cc.s.getWorld().getPlayers( (p) -> p.squaredDistanceTo(vec) < d0))
            {
                count++;
                player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(soundName, mixer, vec, volume, pitch));
            }
            int totalPlayed = count;
            return (_c, _t) -> new NumericValue(totalPlayed);
        });

        expression.addLazyFunction("particle", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            MinecraftServer ms = cc.s.getMinecraftServer();
            ServerWorld world = cc.s.getWorld();
            Vector3Argument locator = Vector3Argument.findIn(cc, lv, 1);
            String particleName = lv.get(0).evalValue(c).getString();
            int count = 10;
            double speed = 0;
            float spread = 0.5f;
            ServerPlayerEntity player = null;
            if (lv.size() > locator.offset)
            {
                count = (int) NumericValue.asNumber(lv.get(locator.offset).evalValue(c)).getLong();
                if (lv.size() > 1+locator.offset)
                {
                    spread = (float) NumericValue.asNumber(lv.get(1+locator.offset).evalValue(c)).getDouble();
                    if (lv.size() > 2+locator.offset)
                    {
                        speed = NumericValue.asNumber(lv.get(2 + locator.offset).evalValue(c)).getDouble();
                        if (lv.size() > 3 + locator.offset) // should accept entity as well as long as it is player
                        {
                            player = ms.getPlayerManager().getPlayer(lv.get(3 + locator.offset).evalValue(c).getString());
                        }
                    }
                }
            }
            ParticleEffect particle = ShapeDispatcher.getParticleData(particleName);
            Vec3d vec = locator.vec;
            if (player == null)
            {
                for (PlayerEntity p : (world.getPlayers()))
                {
                    world.spawnParticles((ServerPlayerEntity)p, particle, true, vec.x, vec.y, vec.z, count,
                            spread, spread, spread, speed);
                }
            }
            else
            {
                world.spawnParticles(player,
                        particle, true, vec.x, vec.y, vec.z, count,
                        spread, spread, spread, speed);
            }

            return (c_, t_) -> Value.TRUE;
        });

        expression.addLazyFunction("particle_line", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            String particleName = lv.get(0).evalValue(c).getString();
            ParticleEffect particle = ShapeDispatcher.getParticleData(particleName);
            Vector3Argument pos1 = Vector3Argument.findIn(cc, lv, 1);
            Vector3Argument pos2 = Vector3Argument.findIn(cc, lv, pos1.offset);
            double density = 1.0;
            ServerPlayerEntity player = null;
            if (lv.size() > pos2.offset+0 )
            {
                density = NumericValue.asNumber(lv.get(pos2.offset+0).evalValue(c)).getDouble();
                if (density <= 0)
                {
                    throw new InternalExpressionException("Particle density should be positive");
                }
                if (lv.size() > pos2.offset+1)
                {
                    Value playerValue = lv.get(pos2.offset+1).evalValue(c);
                    if (playerValue instanceof EntityValue)
                    {
                        Entity e = ((EntityValue) playerValue).getEntity();
                        if (!(e instanceof ServerPlayerEntity)) throw new InternalExpressionException("'particle_line' player argument has to be a player");
                        player = (ServerPlayerEntity) e;
                    }
                    else
                    {
                        player = cc.s.getMinecraftServer().getPlayerManager().getPlayer(playerValue.getString());
                    }
                }
            }

            Value retval = new NumericValue(ShapeDispatcher.drawParticleLine(
                    (player == null)?world.getPlayers(): Collections.singletonList(player),
                    particle, pos1.vec, pos2.vec, density));

            return (c_, t_) -> retval;
        });

        expression.addLazyFunction("particle_box", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            String particleName = lv.get(0).evalValue(c).getString();
            ParticleEffect particle = ShapeDispatcher.getParticleData(particleName);
            Vector3Argument pos1 = Vector3Argument.findIn(cc, lv, 1);
            Vector3Argument pos2 = Vector3Argument.findIn(cc, lv, pos1.offset);

            double density = 1.0;
            ServerPlayerEntity player = null;
            if (lv.size() > pos2.offset+0 )
            {
                density = NumericValue.asNumber(lv.get(pos2.offset+0).evalValue(c)).getDouble();
                if (density <= 0)
                {
                    throw new InternalExpressionException("Particle density should be positive");
                }
                if (lv.size() > pos2.offset+1)
                {
                    Value playerValue = lv.get(pos2.offset+1).evalValue(c);
                    if (playerValue instanceof EntityValue)
                    {
                        Entity e = ((EntityValue) playerValue).getEntity();
                        if (!(e instanceof ServerPlayerEntity)) throw new InternalExpressionException("'particle_box' player argument has to be a player");
                        player = (ServerPlayerEntity) e;
                    }
                    else
                    {
                        player = cc.s.getMinecraftServer().getPlayerManager().getPlayer(playerValue.getString());
                    }
                }
            }
            Vec3d a = pos1.vec;
            Vec3d b = pos2.vec;
            Vec3d from = new Vec3d(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z));
            Vec3d to = new Vec3d(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z));
            int particleCount = ShapeDispatcher.Box.particleMesh(
                    player==null?world.getPlayers():Collections.singletonList(player),
                    particle, density, from, to
            );
            return (c_, t_) -> new NumericValue(particleCount);
        });
        // deprecated
        expression.alias("particle_rect", "particle_box");


        expression.addLazyFunction("draw_shape", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerPlayerEntity player[] = {null};
            List<Pair<ShapeDispatcher.ExpiringShape, Map<String,Value>>> shapes = new ArrayList<>();
            if (lv.size() == 1) // bulk
            {
                Value specLoad = lv.get(0).evalValue(c);
                if (!(specLoad instanceof ListValue)) throw new InternalExpressionException("In bulk mode - shapes need to be provided as a list of shape specs");
                for (Value list : ((ListValue) specLoad).getItems())
                {
                    if (!(list instanceof ListValue))  throw new InternalExpressionException("In bulk mode - shapes need to be provided as a list of shape specs");
                    shapes.add( ShapeDispatcher.fromFunctionArgs(cc, ((ListValue) list).getItems(), player));
                }
            }
            else
            {
                List<Value> params = new ArrayList<>();
                for (LazyValue v : lv) params.add(v.evalValue(c));
                shapes.add(ShapeDispatcher.fromFunctionArgs(cc, params, player));
            }

            ShapeDispatcher.sendShape(
                    (player[0]==null)?cc.s.getWorld().getPlayers():Collections.singletonList(player[0]),
                    shapes
            );
            return LazyValue.TRUE;
        });

        expression.addLazyFunction("create_marker", -1, (c, t, lv) ->{
            CarpetContext cc = (CarpetContext)c;
            BlockState targetBlock = null;
            Vector3Argument pointLocator;
            boolean interactable = true;
            String name;
            try
            {
                Value nameValue = lv.get(0).evalValue(c);
                name = nameValue instanceof NullValue ? "" : nameValue.getString();
                pointLocator = Vector3Argument.findIn(cc, lv, 1, true);
                if (lv.size()>pointLocator.offset)
                {
                    BlockArgument blockLocator = BlockArgument.findIn(cc, lv, pointLocator.offset, true, true);
                    if (blockLocator.block != null) targetBlock = blockLocator.block.getBlockState();
                    if (lv.size() > blockLocator.offset)
                    {
                        interactable = lv.get(blockLocator.offset).evalValue(c, Context.BOOLEAN).getBoolean();
                    }
                }
            }
            catch (IndexOutOfBoundsException e)
            {
                throw new InternalExpressionException("'create_marker' requires a name and three coordinates, with optional direction, and optional block on its head");
            }

            ArmorStandEntity armorstand = new ArmorStandEntity(EntityType.ARMOR_STAND, cc.s.getWorld());
            double yoffset;
            if (targetBlock == null && name.isEmpty())
            {
                yoffset = 0.0;
            }
            else if (!interactable && targetBlock == null)
            {
                yoffset = -0.41;
            }
            else
            {
                if (targetBlock==null)
                {
                    yoffset = -armorstand.getHeight()-0.41;
                }
                else
                {
                    yoffset = -armorstand.getHeight()+0.3;
                }
            }
            armorstand.refreshPositionAndAngles(
                    pointLocator.vec.x,
                    //pointLocator.vec.y - ((!interactable && targetBlock == null)?0.41f:((targetBlock==null)?(armorstand.getHeight()+0.41):(armorstand.getHeight()-0.3))),
                    pointLocator.vec.y + yoffset,
                    pointLocator.vec.z,
                    (float)pointLocator.yaw,
                    (float) pointLocator.pitch
            );
            armorstand.addScoreboardTag(MARKER_STRING+"_"+((cc.host.getName()==null)?"":cc.host.getName()));
            armorstand.addScoreboardTag(MARKER_STRING);
            if (targetBlock != null)
                armorstand.equipStack(EquipmentSlot.HEAD, new ItemStack(targetBlock.getBlock().asItem()));
            if (!name.isEmpty())
            {
                armorstand.setCustomName(new LiteralText(name));
                armorstand.setCustomNameVisible(true);
            }
            armorstand.setHeadRotation(new EulerAngle((int)pointLocator.pitch,0,0));
            armorstand.setNoGravity(true);
            armorstand.setInvisible(true);
            armorstand.setInvulnerable(true);
            armorstand.getDataTracker().set(ArmorStandEntity.ARMOR_STAND_FLAGS, (byte)(interactable?8 : 16|8));
            cc.s.getWorld().spawnEntity(armorstand);
            EntityValue result = new EntityValue(armorstand);
            return (_c, _t) -> result;
        });

        expression.addLazyFunction("remove_all_markers", 0, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            int total = 0;
            String markerName = MARKER_STRING+"_"+((cc.host.getName()==null)?"":cc.host.getName());
            for (Entity e : cc.s.getWorld().getEntitiesByType(EntityType.ARMOR_STAND, (as) -> as.getScoreboardTags().contains(markerName)))
            {
                total ++;
                e.discard(); // discard // remove();
            }
            int finalTotal = total;
            return (_cc, _tt) -> new NumericValue(finalTotal);
        });

        expression.addLazyFunction("nbt", 1, (c, t, lv) -> {
            Value ret = NBTSerializableValue.fromValue(lv.get(0).evalValue(c));
            return (cc, tt) -> ret;
        });

        expression.addLazyFunction("escape_nbt", 1, (c, t, lv) -> {
            Value v = lv.get(0).evalValue(c);
            String string = v.getString();
            Value ret = new StringValue(StringTag.escape(string));
            return (cc, tt) -> ret;
        });

        expression.addLazyFunction("parse_nbt", 1, (c, t, lv) -> {
            Value v = lv.get(0).evalValue(c);
            if (v instanceof NBTSerializableValue)
            {
                Value parsed = ((NBTSerializableValue) v).toValue();
                return (cc, tt) -> parsed;
            }
            NBTSerializableValue ret = NBTSerializableValue.parseString(v.getString());
            if (ret == null)
                return LazyValue.NULL;
            Value parsed = ret.toValue();
            return (cc, tt) -> parsed;
        });

        expression.addLazyFunction("encode_nbt", -1, (c, t, lv) -> {
            int argSize = lv.size();
            if (argSize==0 || argSize > 2) throw new InternalExpressionException("'encode_nbt' requires 1 or 2 parameters");
            Value v = lv.get(0).evalValue(c);
            boolean force = (argSize > 1) && lv.get(1).evalValue(c).getBoolean();
            Tag tag;
            try
            {
                tag = v.toTag(force);
            }
            catch (NBTSerializableValue.IncompatibleTypeException ignored)
            {
                throw new InternalExpressionException("cannot reliably encode to a tag the value of '"+ignored.val.getPrettyString()+"'");
            }
            Value tagValue = new NBTSerializableValue(tag);
            return (cc, tt) -> tagValue;
        });

        //"overridden" native call that prints to stderr
        expression.addLazyFunction("print", -1, (c, t, lv) ->
        {
            if (lv.size() == 0 || lv.size() > 2) throw new InternalExpressionException("'print' takes one or two arguments");
            ServerCommandSource s = ((CarpetContext)c).s;
            Value res = lv.get(0).evalValue(c);
            if (lv.size() == 2)
            {
                ServerPlayerEntity player = EntityValue.getPlayerByValue(s.getMinecraftServer(), res);
                s = player.getCommandSource();
                res = lv.get(1).evalValue(c);
            }
            if (res instanceof FormattedTextValue)
            {
                s.sendFeedback(((FormattedTextValue) res).getText(), false);
            }
            else
            {
                if (s.getEntity() instanceof PlayerEntity)
                {
                    Messenger.m((PlayerEntity) s.getEntity(), "w " + res.getString());
                }
                else
                {
                    Messenger.m(s, "w " + res.getString());
                }
            }
            Value finalRes = res;
            return (_c, _t) -> finalRes; // pass through for variables
        });

        expression.addLazyFunction("format", -1, (c, t, lv) -> {
            if (lv.size() == 0 ) throw new InternalExpressionException("'format' requires at least one component");
            List<Value> values = lv.stream().map(lazy -> lazy.evalValue(c)).collect(Collectors.toList());
            if (values.get(0) instanceof ListValue && values.size()==1)
                values = ((ListValue) values.get(0)).getItems();
            Value ret = new FormattedTextValue(Messenger.c(values.stream().map(Value::getString).toArray()));
            return (cc, tt) -> ret;
        });

        expression.addLazyFunction("run", 1, (c, t, lv) -> {
            BlockPos target = ((CarpetContext)c).origin;
            Vec3d posf = new Vec3d((double)target.getX()+0.5D,(double)target.getY(),(double)target.getZ()+0.5D);
            ServerCommandSource s = ((CarpetContext)c).s;
            try
            {
                Value[] error = {Value.NULL};
                List<Value> output = new ArrayList<>();
                Value retval = new NumericValue(s.getMinecraftServer().getCommandManager().execute(
                        new ServerCommandSource(
                                CommandOutput.DUMMY, posf, Vec2f.ZERO, s.getWorld(), CarpetSettings.runPermissionLevel,
                                s.getName(), s.getDisplayName(), s.getMinecraftServer(), s.getEntity(), true,
                                (ctx, succ, res) -> { }, EntityAnchorArgumentType.EntityAnchor.FEET)
                        {
                            @Override
                            public void sendError(Text message)
                            {
                                error[0] = new FormattedTextValue(message);
                            }
                            @Override
                            public void sendFeedback(Text message, boolean broadcastToOps)
                            {
                                output.add(new FormattedTextValue(message));
                            }
                        },
                        lv.get(0).evalValue(c).getString())
                );
                Value ret = ListValue.of(retval, ListValue.wrap(output), error[0]);
                return (c_, t_) -> ret;
            }
            catch (Exception exc)
            {
                Value ret = ListValue.of(Value.NULL, ListValue.of(), new FormattedTextValue(new LiteralText(exc.getMessage())));
                return (c_, t_) -> ret;
            }
        });

        expression.addLazyFunction("save", 0, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            s.getMinecraftServer().getPlayerManager().saveAllPlayerData();
            s.getMinecraftServer().save(true,true,true);
            s.getWorld().getChunkManager().tick(() -> true);
            CarpetSettings.LOG.warn("Saved chunks");
            return (cc, tt) -> Value.TRUE;
        });

        expression.addLazyFunction("tick_time", 0, (c, t, lv) ->
        {
            Value time = new NumericValue(((CarpetContext) c).s.getMinecraftServer().getTicks());
            return (cc, tt) -> time;
        });

        expression.addLazyFunction("world_time", 0, (c, t, lv) ->
        {
            Value time = new NumericValue(((CarpetContext) c).s.getWorld().getTime());
            return (cc, tt) -> time;
        });

        expression.addLazyFunction("day_time", -1, (c, t, lv) ->
        {
            Value time = new NumericValue(((CarpetContext) c).s.getWorld().getTimeOfDay());
            if (lv.size() > 0)
            {
                long newTime = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
                if (newTime < 0) newTime = 0;
                ((CarpetContext) c).s.getWorld().setTimeOfDay(newTime);// setTimeOfDay(newTime);
            }
            return (cc, tt) -> time;
        });

        expression.addLazyFunction("last_tick_times", -1, (c, t, lv) ->
        {
            //assuming we are in the tick world section
            // might be off one tick when run in the off tasks or asynchronously.
            int currentReportedTick = ((CarpetContext) c).s.getMinecraftServer().getTicks()-1;
            List<Value> ticks = new ArrayList<>(100);
            final long[] tickArray = ((CarpetContext) c).s.getMinecraftServer().lastTickLengths;
            for (int i=currentReportedTick+100; i > currentReportedTick; i--)
            {
                ticks.add(new NumericValue(((double)tickArray[i % 100])/1000000.0));
            }
            Value ret = ListValue.wrap(ticks);
            return (cc, tt) -> ret;
        });


        expression.addLazyFunction("game_tick", -1, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            if (!s.getMinecraftServer().isOnThread()) throw new InternalExpressionException("Unable to run ticks from threads");
            ((MinecraftServerInterface)s.getMinecraftServer()).forceTick( () -> System.nanoTime()- CarpetServer.scriptServer.tickStart<50000000L);
            if (lv.size()>0)
            {
                long ms_total = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
                long end_expected = CarpetServer.scriptServer.tickStart+ms_total*1000000L;
                long wait = end_expected-System.nanoTime();
                if (wait > 0L)
                {
                    try
                    {
                        Thread.sleep(wait/1000000L);
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }
            }
            CarpetServer.scriptServer.tickStart = System.nanoTime(); // for the next tick
            Thread.yield();
            if(CarpetServer.scriptServer.stopAll)
                throw new ExitStatement(Value.NULL);
            return (cc, tt) -> Value.TRUE;
        });

        expression.addLazyFunction("seed", -1, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value ret = new NumericValue(s.getWorld().getSeed());
            return (cc, tt) -> ret;
        });

        expression.addLazyFunction("relight", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            ServerWorld world = cc.s.getWorld();
            ((ThreadedAnvilChunkStorageInterface) world.getChunkManager().threadedAnvilChunkStorage).relightChunk(new ChunkPos(pos));
            WorldTools.forceChunkUpdate(pos, world);
            return LazyValue.TRUE;
        });

        expression.addLazyFunction("current_dimension", 0, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value retval = ValueConversions.of(s.getWorld());
            return (cc, tt) -> retval;
        });

        expression.addLazyFunction("view_distance", 0, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value retval = new NumericValue(s.getMinecraftServer().getPlayerManager().getViewDistance());
            return (cc, tt) -> retval;
        });

        expression.addLazyFunction("in_dimension", 2, (c, t, lv) -> {
            ServerCommandSource outerSource = ((CarpetContext)c).s;
            Value dimensionValue = lv.get(0).evalValue(c);
            World world = ValueConversions.dimFromValue(dimensionValue, outerSource.getMinecraftServer());
            if (world == outerSource.getWorld()) return lv.get(1);
            ServerCommandSource innerSource = outerSource.withWorld((ServerWorld)world);
            Context newCtx = c.recreate();
            ((CarpetContext) newCtx).s = innerSource;
            newCtx.variables = c.variables;
            Value retval = lv.get(1).evalValue(newCtx);
            return (cc, tt) -> retval;
        });

        expression.addLazyFunction("plop", -1, (c, t, lv) ->{
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 0);
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("'plop' needs extra argument indicating what to plop");
            String what = lv.get(locator.offset).evalValue(c).getString();
            Value [] result = new Value[]{Value.NULL};
            ((CarpetContext)c).s.getMinecraftServer().submitAndJoin( () ->
            {
                Boolean res = FeatureGenerator.plop(what, ((CarpetContext) c).s.getWorld(), locator.block.getPos());

                if (res == null)
                    return;
                if (what.equalsIgnoreCase("boulder"))  // there might be more of those
                    WorldTools.forceChunkUpdate(locator.block.getPos(), ((CarpetContext) c).s.getWorld());
                result[0] = new NumericValue(res);
            });
            Value ret = result[0]; // preventing from lazy evaluating of the result in case a future completes later
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("schedule", -1, (c, t, lv) -> {
            if (lv.size()<2)
                throw new InternalExpressionException("'schedule' should have at least 2 arguments, delay and call name");
            long delay = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();

            FunctionArgument<LazyValue> functionArgument = FunctionArgument.findIn(c, expression.module, lv, 1, false, true);

            CarpetServer.scriptServer.events.scheduleCall(
                    (CarpetContext) c,
                    functionArgument.function,
                    FunctionValue.resolveArgs(functionArgument.args, c, t),
                    delay
            );
            return (c_, t_) -> Value.TRUE;
        });

        expression.addLazyFunction("logger", -1, (c, t, lv) ->
        {
            //CarpetSettings.LOG.error("Standard Structures:");
            //CarpetSettings.LOG.error(Registry.STRUCTURE_FEATURE.getIds().stream().map(i -> "`'"+NBTSerializableValue.nameFromRegistryId(i)+"'`").sorted(). collect(Collectors.joining(", ")));
            //CarpetSettings.LOG.error("");
            //CarpetSettings.LOG.error("Configured Structures:");
            //CarpetSettings.LOG.error(BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.getIds().stream().map(i -> "`'"+NBTSerializableValue.nameFromRegistryId(i)+"'`").sorted().collect(Collectors.joining(", ")));
            //CarpetSettings.LOG.error("");
            //CarpetSettings.LOG.error("Features:");
            //CarpetSettings.LOG.error(Registry.FEATURE.getIds().stream().map(i -> "`'"+NBTSerializableValue.nameFromRegistryId(i)+"'`").sorted().collect(Collectors.joining(", ")));
            //CarpetSettings.LOG.error("");
            //CarpetSettings.LOG.error("Configured Features:");
            //CarpetSettings.LOG.error(BuiltinRegistries.CONFIGURED_FEATURE.getIds().stream().map(i -> "`'"+NBTSerializableValue.nameFromRegistryId(i)+"'`").sorted().collect(Collectors.joining(", ")));
            //CarpetSettings.LOG.error("");
            //CarpetSettings.LOG.error("Custom:");
            //CarpetSettings.LOG.error(FeatureGenerator.featureMap.keySet().stream().map(i -> "`'"+i+"'`").sorted().collect(Collectors.joining(", ")));

            //CarpetSettings.LOG.error(Registry.ENTITY_TYPE.getIds().stream().sorted().map(ValueConversions::simplify).collect(Collectors.joining("`, `")));

            Value res;

            if(lv.size()==1)
            {
                res = lv.get(0).evalValue(c);
                CarpetSettings.LOG.info(res.getString());
            }
            else if(lv.size()==2)
            {
                String level = lv.get(0).evalValue(c).getString().toLowerCase(Locale.ROOT);
                res = lv.get(1).evalValue(c);
                switch(level){
                    case "error": CarpetSettings.LOG.error(res.getString()); break;
                    case "warn":  CarpetSettings.LOG.warn(res.getString());  break;
                    case "debug": CarpetSettings.LOG.debug(res.getString()); break;
                    case "fatal": CarpetSettings.LOG.fatal(res.getString()); break;
                    case "info":  CarpetSettings.LOG.info(res.getString());  break;
                    default: throw new InternalExpressionException("Unknown log level for 'logger': "+level);
                }
            }
            else throw new InternalExpressionException("logger takes 1 or 2 arguments");

            return (_c, _t) -> res; // pass through for variables
        });

        expression.addLazyFunction("read_file", 2, (c, t, lv) -> {
            String resource = recognizeResource(lv.get(0).evalValue(c));
            String origtype = lv.get(1).evalValue(c).getString().toLowerCase(Locale.ROOT);
            boolean shared = origtype.startsWith("shared_");
            String type = shared ? origtype.substring(7) : origtype; //len(shared_)
            if (!type.equals("raw") && !type.equals("text") && !type.equals("nbt"))
                throw new InternalExpressionException("Unsupported file type: "+origtype);
            Value retVal;
            if (type.equals("nbt"))
            {
                Tag state = ((CarpetScriptHost)((CarpetContext)c).host).readFileTag(resource, shared);
                if (state == null) return LazyValue.NULL;
                retVal = new NBTSerializableValue(state);
            }
            else
            {
                List<String> content = ((CarpetScriptHost) ((CarpetContext) c).host).readTextResource(resource, shared);
                if (content == null) return LazyValue.NULL;
                retVal = ListValue.wrap(content.stream().map(StringValue::new).collect(Collectors.toList()));
            }
            return (cc, tt) -> retVal;
        });

        expression.addLazyFunction("delete_file", 2, (c, t, lv) -> {
            String resource = recognizeResource(lv.get(0).evalValue(c));
            String origtype = lv.get(1).evalValue(c).getString().toLowerCase(Locale.ROOT);
            boolean shared = origtype.startsWith("shared_");
            String type = shared ? origtype.substring(7) : origtype; //len(shared_)
            if (!type.equals("raw") && !type.equals("text") && !type.equals("nbt"))
                throw new InternalExpressionException("Unsupported file type: "+origtype);
            boolean success = ((CarpetScriptHost)((CarpetContext)c).host).removeResourceFile(resource, shared, type);
            return success?LazyValue.TRUE:LazyValue.FALSE;
        });

        expression.addLazyFunction("write_file", -1, (c, t, lv) -> {
            if (lv.size() < 3) throw new InternalExpressionException("'write_file' requires three or more arguments");
            String resource = recognizeResource(lv.get(0).evalValue(c));
            String origtype = lv.get(1).evalValue(c).getString().toLowerCase(Locale.ROOT);
            boolean shared = origtype.startsWith("shared_");
            String type = shared ? origtype.substring(7) : origtype; //len(shared_)
            if (!type.equals("raw") && !type.equals("text") && !type.equals("nbt"))
                throw new InternalExpressionException("Unsupported file type: "+origtype);
            boolean success;
            if (type.equals("nbt"))
            {
                Value val = lv.get(2).evalValue(c);
                NBTSerializableValue tagValue =  (val instanceof NBTSerializableValue)
                        ? (NBTSerializableValue) val
                        : new NBTSerializableValue(val.getString());
                Tag tag = tagValue.getTag();
                success = ((CarpetScriptHost)((CarpetContext)c).host).writeTagFile(tag, resource, shared);
            }
            else
            {
                List<String> data = new ArrayList<>();
                if (lv.size()==3)
                {
                    Value val = lv.get(2).evalValue(c);
                    if (val instanceof ListValue)
                    {
                        List<Value> lval = ((ListValue) val).getItems();
                        lval.forEach(v -> data.add(v.getString()));
                    }
                    else
                    {
                        data.add(val.getString());
                    }
                }
                else
                {
                    for(int i = 2; i < lv.size(); i++)
                    {
                        data.add(lv.get(i).evalValue(c).getString());
                    }
                }
                success = ((CarpetScriptHost) ((CarpetContext) c).host).appendLogFile(resource, shared, type, data);
            }
            return success?LazyValue.TRUE:LazyValue.FALSE;
        });

        //write_file

        expression.addLazyFunction("load_app_data", -1, (c, t, lv) ->
        {
            String file = null;
            boolean shared = false;
            if (lv.size()>0)
            {
                file = recognizeResource(lv.get(0).evalValue(c));
                if (lv.size() > 1)
                {
                    shared = lv.get(1).evalValue(c).getBoolean();
                }
            }
            Tag state = ((CarpetScriptHost)((CarpetContext)c).host).readFileTag(file, shared);
            if (state == null)
                return (cc, tt) -> Value.NULL;
            Value retVal = new NBTSerializableValue(state);
            return (cc, tt) -> retVal;
        });

        expression.addLazyFunction("store_app_data", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'store_app_data' needs NBT tag and an optional file");
            Value val = lv.get(0).evalValue(c);
            String file = null;
            boolean shared = false;
            if (lv.size()>1)
            {
                file = recognizeResource(lv.get(1).evalValue(c));
                if (lv.size() > 2)
                {
                    shared = lv.get(2).evalValue(c).getBoolean();
                }
            }
            NBTSerializableValue tagValue =  (val instanceof NBTSerializableValue)
                    ? (NBTSerializableValue) val
                    : new NBTSerializableValue(val.getString());
            Tag tag = tagValue.getTag();
            boolean success = ((CarpetScriptHost)((CarpetContext)c).host).writeTagFile(tag, file, shared);
            return success?LazyValue.TRUE:LazyValue.FALSE;
        });

        expression.addLazyFunction("statistic", 3, (c, t, lv) ->
        {
            Value playerValue = lv.get(0).evalValue(c);
            CarpetContext cc = (CarpetContext)c;
            ServerPlayerEntity player = EntityValue.getPlayerByValue(cc.s.getMinecraftServer(), playerValue);
            if (player == null) return LazyValue.NULL;
            Identifier category;
            Identifier statName;
            try
            {
                category = new Identifier(lv.get(1).evalValue(c).getString());
                statName = new Identifier(lv.get(2).evalValue(c).getString());
            }
            catch (InvalidIdentifierException e)
            {
                return LazyValue.NULL;
            }
            StatType<?> type = Registry.STAT_TYPE.get(category);
            if (type == null)
                return LazyValue.NULL;
            Stat<?> stat = getStat(type, statName);
            if (stat == null)
                return LazyValue.NULL;
            return (_c, _t) -> new NumericValue(player.getStatHandler().getStat(stat));
        });

        //handle_event('event', function...)
        expression.addLazyFunction("handle_event", -1, (c, t, lv) ->
        {
            if (lv.size() < 2)
                throw new InternalExpressionException("'handle_event' requires at least two arguments, event name, and a callback");
            String event = lv.get(0).evalValue(c).getString();
            FunctionArgument<LazyValue> callback = FunctionArgument.findIn(c, expression.module, lv, 1, true, false);
            CarpetScriptHost host = ((CarpetScriptHost)c.host);
            Value success;
            if (callback.function == null)
            {
                success = host.getScriptServer().events.removeBuiltInEvent(event, host)?Value.TRUE:Value.FALSE;
            }
            else
            {
                success = host.getScriptServer().events.handleCustomEvent(event, host, callback.function, FunctionValue.resolveArgs(callback.args, c, t) )?Value.TRUE:Value.FALSE;
            }
            return (cc, tt) -> success;
        });
        //signal_event('event', player or null, args.... ) -> number of apps notified
        expression.addLazyFunction("signal_event", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'signal' requires at least one argument");
            CarpetScriptServer server = ((CarpetScriptHost)c.host).getScriptServer();
            String eventName = lv.get(0).evalValue(c).getString();
            // no such event yet
            if (CarpetEventServer.Event.getEvent(eventName, server) == null) return LazyValue.NULL;
            ServerPlayerEntity player = null;
            List<Value> args = Collections.emptyList();
            if (lv.size() > 1)
            {
                player = EntityValue.getPlayerByValue(server.server, lv.get(1).evalValue(c));
                if (lv.size() > 2)
                {
                    args = FunctionValue.resolveArgs(lv.subList(2, lv.size()), c, t);
                }
            }
            int counts = ((CarpetScriptHost)c.host).getScriptServer().events.signalEvent(eventName, (CarpetScriptHost) c.host, player, args);
            if (counts < 0) return LazyValue.NULL;
            Value ret = new NumericValue(counts);
            return (cc, tt) -> ret;
        });
    }

    private static <T> Stat<T> getStat(StatType<T> type, Identifier id)
    {
        T key = type.getRegistry().get(id);
        if (key == null || !((StatTypeInterface)type).hasStatCreated(key))
            return null;
        return type.getOrCreateStat(key);
    }
}
