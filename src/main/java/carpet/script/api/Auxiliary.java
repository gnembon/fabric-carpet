package carpet.script.api;

import carpet.CarpetServer;
import carpet.fakes.MinecraftServerInterface;
import carpet.fakes.ServerWorldInterface;
import carpet.fakes.StatTypeInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.helpers.FeatureGenerator;
import carpet.logging.HUDController;
import carpet.script.argument.FileArgument;
import carpet.script.CarpetContext;
import carpet.script.CarpetEventServer;
import carpet.script.CarpetScriptHost;
import carpet.script.CarpetScriptServer;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.FunctionArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.utils.SnoopyCommandSource;
import carpet.script.utils.InputValidator;
import carpet.script.utils.ScarpetJsonDeserializer;
import carpet.script.utils.ShapeDispatcher;
import carpet.script.utils.WorldTools;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.utils.Messenger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.command.DataCommandStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
//import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
//import net.minecraft.network.packet.s2c.play.TitleS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static carpet.script.value.NBTSerializableValue.nameFromRegistryId;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class Auxiliary {
    public static final String MARKER_STRING = "__scarpet_marker";
    private static final Map<String, SoundCategory> mixerMap = Arrays.stream(SoundCategory.values()).collect(Collectors.toMap(SoundCategory::getName, k -> k));
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(Value.class, new ScarpetJsonDeserializer()).create();

    @Deprecated
    public static String recognizeResource(Value value, boolean isFloder)
    {
        String origfile = value.getString();
        String file = origfile.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9\\-+_/]", "");
        file = Arrays.stream(file.split("/+")).filter(s -> !s.isEmpty()).collect(Collectors.joining("/"));
        if (file.isEmpty() && !isFloder)
        {
            throw new InternalExpressionException("Cannot use "+origfile+" as resource name - must have some letters and numbers");
        }
        return file;
    }

    public static void apply(Expression expression)
    {
        expression.addContextFunction("sound", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() == 0)
            {
                return ListValue.wrap(Registry.SOUND_EVENT.getIds().stream().map(ValueConversions::of));
            }
            String rawString = lv.get(0).getString();
            Identifier soundName = new Identifier(rawString);
            Vector3Argument locator = Vector3Argument.findIn(lv, 1);
            if (Registry.SOUND_EVENT.get(soundName) == null)
                throw new ThrowStatement(rawString, Throwables.UNKNOWN_SOUND);
            float volume = 1.0F;
            float pitch = 1.0F;
            SoundCategory mixer = SoundCategory.MASTER;
            if (lv.size() > 0+locator.offset)
            {
                volume = (float) NumericValue.asNumber(lv.get(0+locator.offset)).getDouble();
                if (lv.size() > 1+locator.offset)
                {
                    pitch = (float) NumericValue.asNumber(lv.get(1+locator.offset)).getDouble();
                    if (lv.size() > 2+locator.offset)
                    {
                        String mixerName = lv.get(2+locator.offset).getString();
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
            return new NumericValue(count);
        });

        expression.addContextFunction("particle", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() == 0) return ListValue.wrap(Registry.PARTICLE_TYPE.getIds().stream().map(ValueConversions::of));
            MinecraftServer ms = cc.s.getMinecraftServer();
            ServerWorld world = cc.s.getWorld();
            Vector3Argument locator = Vector3Argument.findIn(lv, 1);
            String particleName = lv.get(0).getString();
            int count = 10;
            double speed = 0;
            float spread = 0.5f;
            ServerPlayerEntity player = null;
            if (lv.size() > locator.offset)
            {
                count = (int) NumericValue.asNumber(lv.get(locator.offset)).getLong();
                if (lv.size() > 1+locator.offset)
                {
                    spread = (float) NumericValue.asNumber(lv.get(1+locator.offset)).getDouble();
                    if (lv.size() > 2+locator.offset)
                    {
                        speed = NumericValue.asNumber(lv.get(2 + locator.offset)).getDouble();
                        if (lv.size() > 3 + locator.offset) // should accept entity as well as long as it is player
                        {
                            player = ms.getPlayerManager().getPlayer(lv.get(3 + locator.offset).getString());
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

            return Value.TRUE;
        });

        expression.addContextFunction("particle_line", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            String particleName = lv.get(0).getString();
            ParticleEffect particle = ShapeDispatcher.getParticleData(particleName);
            Vector3Argument pos1 = Vector3Argument.findIn(lv, 1);
            Vector3Argument pos2 = Vector3Argument.findIn(lv, pos1.offset);
            double density = 1.0;
            ServerPlayerEntity player = null;
            if (lv.size() > pos2.offset+0 )
            {
                density = NumericValue.asNumber(lv.get(pos2.offset+0)).getDouble();
                if (density <= 0)
                {
                    throw new InternalExpressionException("Particle density should be positive");
                }
                if (lv.size() > pos2.offset+1)
                {
                    Value playerValue = lv.get(pos2.offset+1);
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

            return new NumericValue(ShapeDispatcher.drawParticleLine(
                    (player == null)?world.getPlayers(): Collections.singletonList(player),
                    particle, pos1.vec, pos2.vec, density
            ));
        });

        expression.addContextFunction("particle_box", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            String particleName = lv.get(0).getString();
            ParticleEffect particle = ShapeDispatcher.getParticleData(particleName);
            Vector3Argument pos1 = Vector3Argument.findIn(lv, 1);
            Vector3Argument pos2 = Vector3Argument.findIn(lv, pos1.offset);

            double density = 1.0;
            ServerPlayerEntity player = null;
            if (lv.size() > pos2.offset+0 )
            {
                density = NumericValue.asNumber(lv.get(pos2.offset+0)).getDouble();
                if (density <= 0)
                {
                    throw new InternalExpressionException("Particle density should be positive");
                }
                if (lv.size() > pos2.offset+1)
                {
                    Value playerValue = lv.get(pos2.offset+1);
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
            return new NumericValue(particleCount);
        });
        // deprecated
        expression.alias("particle_rect", "particle_box");


        expression.addContextFunction("draw_shape", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            MinecraftServer server = world.getServer();
            Set<ServerPlayerEntity> playerTargets = new HashSet<>();
            List<Pair<ShapeDispatcher.ExpiringShape, Map<String,Value>>> shapes = new ArrayList<>();
            if (lv.size() == 1) // bulk
            {
                Value specLoad = lv.get(0);
                if (!(specLoad instanceof ListValue)) throw new InternalExpressionException("In bulk mode - shapes need to be provided as a list of shape specs");
                for (Value list : ((ListValue) specLoad).getItems())
                {
                    if (!(list instanceof ListValue))  throw new InternalExpressionException("In bulk mode - shapes need to be provided as a list of shape specs");
                    shapes.add( ShapeDispatcher.fromFunctionArgs(server, world, ((ListValue) list).getItems(), playerTargets));
                }
            }
            else
            {
                shapes.add(ShapeDispatcher.fromFunctionArgs(server, world, lv, playerTargets));
            }

            ShapeDispatcher.sendShape(
                    (playerTargets.isEmpty())?cc.s.getWorld().getPlayers():playerTargets,
                    shapes
            );
            return Value.TRUE;
        });

        expression.addContextFunction("create_marker", -1, (c, t, lv) ->{
            CarpetContext cc = (CarpetContext)c;
            BlockState targetBlock = null;
            Vector3Argument pointLocator;
            boolean interactable = true;
            String name;
            try
            {
                Value nameValue = lv.get(0);
                name = nameValue instanceof NullValue ? "" : nameValue.getString();
                pointLocator = Vector3Argument.findIn(lv, 1, true, false);
                if (lv.size()>pointLocator.offset)
                {
                    BlockArgument blockLocator = BlockArgument.findIn(cc, lv, pointLocator.offset, true, true, false);
                    if (blockLocator.block != null) targetBlock = blockLocator.block.getBlockState();
                    if (lv.size() > blockLocator.offset)
                    {
                        interactable = lv.get(blockLocator.offset).getBoolean();
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
            return new EntityValue(armorstand);
        });

        expression.addContextFunction("remove_all_markers", 0, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            int total = 0;
            String markerName = MARKER_STRING+"_"+((cc.host.getName()==null)?"":cc.host.getName());
            for (Entity e : cc.s.getWorld().getEntitiesByType(EntityType.ARMOR_STAND, (as) -> as.getScoreboardTags().contains(markerName)))
            {
                total ++;
                e.discard(); // discard // remove();
            }
            return new NumericValue(total);
        });

        expression.addUnaryFunction("nbt", NBTSerializableValue::fromValue);

        expression.addUnaryFunction("escape_nbt", v -> new StringValue(NbtString.escape(v.getString())));

        expression.addUnaryFunction("parse_nbt", v -> {
            if (v instanceof NBTSerializableValue) return ((NBTSerializableValue) v).toValue();
            NBTSerializableValue ret = NBTSerializableValue.parseString(v.getString(), false);
            if (ret == null) return Value.NULL;
            return ret.toValue();
        });

        expression.addFunction("tag_matches", (lv) -> {
            int numParam = lv.size();
            if (numParam != 2 && numParam != 3) throw new InternalExpressionException("'tag_matches' requires 2 or 3 arguments");
            if (lv.get(1).isNull()) return Value.TRUE;
            if (lv.get(0).isNull()) return Value.FALSE;
            NbtElement source = ((NBTSerializableValue)(NBTSerializableValue.fromValue(lv.get(0)))).getTag();
            NbtElement match = ((NBTSerializableValue)(NBTSerializableValue.fromValue(lv.get(1)))).getTag();
            return BooleanValue.of(NbtHelper.matches(match, source, numParam == 2 || lv.get(2).getBoolean()));
        });

        expression.addFunction("encode_nbt", lv -> {
            int argSize = lv.size();
            if (argSize==0 || argSize > 2) throw new InternalExpressionException("'encode_nbt' requires 1 or 2 parameters");
            Value v = lv.get(0);
            boolean force = (argSize > 1) && lv.get(1).getBoolean();
            NbtElement tag;
            try
            {
                tag = v.toTag(force);
            }
            catch (NBTSerializableValue.IncompatibleTypeException ignored)
            {
                throw new InternalExpressionException("cannot reliably encode to a tag the value of '"+ignored.val.getPrettyString()+"'");
            }
            return new NBTSerializableValue(tag);
        });

        //"overridden" native call that prints to stderr
        expression.addContextFunction("print", -1, (c, t, lv) ->
        {
            if (lv.size() == 0 || lv.size() > 2) throw new InternalExpressionException("'print' takes one or two arguments");
            ServerCommandSource s = ((CarpetContext)c).s;
            MinecraftServer server = s.getMinecraftServer();
            Value res = lv.get(0);
            List<ServerPlayerEntity> targets = null;
            if (lv.size() == 2)
            {
                List<Value> playerValues = (res instanceof ListValue)?((ListValue) res).getItems():Collections.singletonList(res);
                List<ServerPlayerEntity> playerTargets = new ArrayList<>();
                playerValues.forEach(pv -> {
                    ServerPlayerEntity player = EntityValue.getPlayerByValue(server, pv);
                    if (player == null) throw new InternalExpressionException("Cannot target player "+pv.getString()+" in print");
                    playerTargets.add(player);
                });
                targets = playerTargets;
                res = lv.get(1);
            }
            Text message = FormattedTextValue.getTextByValue(res);
            if (targets == null)
            {
                s.sendFeedback(message, false);
            }
            else
            {
                targets.forEach(p -> p.getCommandSource().sendFeedback(message, false));
            }
            return res; // pass through for variables
        });

        expression.addContextFunction("display_title", -1, (c, t, lv) -> {
            if (lv.size() < 2) throw new InternalExpressionException("'display_title' needs at least a target, type and message, and optionally times");
            Value pVal = lv.get(0);
            if (!(pVal instanceof ListValue)) pVal = ListValue.of(pVal);
            MinecraftServer server = ((CarpetContext)c).s.getMinecraftServer();
            Stream<ServerPlayerEntity> targets = ((ListValue) pVal).getItems().stream().map(v ->
            {
                ServerPlayerEntity player = EntityValue.getPlayerByValue(server, v);
                if (player == null) throw new InternalExpressionException("'display_title' requires a valid online player or a list of players as first argument. "+v.getString()+" is not a player.");
                return player;
            });
            Function<Text, Packet<?>> packetGetter = null;
            //TitleS2CPacket.Action action;
            String actionString = lv.get(1).getString().toLowerCase(Locale.ROOT);
            switch (actionString)
            {
                case "title":
                    packetGetter = TitleS2CPacket::new;
                    //action = Action.TITLE;
                    if (lv.size() < 3)
                        throw new InternalExpressionException("Third argument of 'display_title' must be present except for 'clear' type");

                    break;
                case "subtitle":
                    packetGetter = SubtitleS2CPacket::new;
                    if (lv.size() < 3)
                        throw new InternalExpressionException("Third argument of 'display_title' must be present except for 'clear' type");

                    //action = Action.SUBTITLE;
                    break;
                case "actionbar":
                    packetGetter = OverlayMessageS2CPacket::new;
                    if (lv.size() < 3)
                        throw new InternalExpressionException("Third argument of 'display_title' must be present except for 'clear' type");

                    //action = Action.ACTIONBAR;
                    break;
                case "clear":
                    packetGetter = (x) -> new ClearTitleS2CPacket(true); // resetting default fade
                    //action = Action.CLEAR;
                    break;
                case "player_list_header":
                case "player_list_footer":
                    break;
                default:
                    throw new InternalExpressionException("'display_title' requires 'title', 'subtitle', 'actionbar', 'player_list_header', 'player_list_footer' or 'clear' as second argument");
            }
            //if (action != Action.CLEAR && lv.size() < 3)
            //    throw new InternalExpressionException("Third argument of 'display_title' must be present except for 'clear' type");
            Text title;
            boolean soundsTrue = false;
            if (lv.size() > 2)
            {
                pVal = lv.get(2);
                title = FormattedTextValue.getTextByValue(pVal);
                soundsTrue = pVal.getBoolean();
            }
            else title = null; // Will never happen, just to make lambda happy
            if (packetGetter == null)
            {
                Map<String, BaseText> map;
                if (actionString.equals("player_list_header"))
                    map = HUDController.scarpet_headers;
                else
                    map = HUDController.scarpet_footers;

                AtomicInteger total = new AtomicInteger(0);
                List<ServerPlayerEntity> targetList = targets.collect(Collectors.toList());
                if (!soundsTrue) // null or empty string
                    targetList.forEach(target -> {
                        map.remove(target.getEntityName());
                        total.getAndIncrement();
                    });
                else
                    targetList.forEach(target -> {
                        map.put(target.getEntityName(), (BaseText) title);
                        total.getAndIncrement();
                    });
                HUDController.update_hud(((CarpetContext)c).s.getMinecraftServer(), targetList);
                return NumericValue.of(total.get());
            }
            TitleFadeS2CPacket timesPacket; // TimesPacket
            if (lv.size() > 3)
            {
                if (lv.size() != 6) throw new InternalExpressionException("'display_title' needs all fade-in, stay and fade-out times");
                int in = NumericValue.asNumber(lv.get(3),"fade in for display_title" ).getInt();
                int stay = NumericValue.asNumber(lv.get(4),"stay for display_title" ).getInt();
                int out = NumericValue.asNumber(lv.get(5),"fade out for display_title" ).getInt();
                timesPacket = new TitleFadeS2CPacket(in, stay, out);
                //timesPacket = new TitleS2CPacket(Action.TIMES, null, in, stay, out);
            }
            else timesPacket = null;

            Packet<?> packet = packetGetter.apply(title);
            //TitleS2CPacket packet = new TitleS2CPacket(action, title);
            AtomicInteger total = new AtomicInteger(0);
            targets.forEach(p -> {
                if (timesPacket != null) p.networkHandler.sendPacket(timesPacket);
                p.networkHandler.sendPacket(packet);
                total.getAndIncrement();
            });
            return NumericValue.of(total.get());
        });

        expression.addFunction("format", values -> {
            if (values.size() == 0 ) throw new InternalExpressionException("'format' requires at least one component");
            if (values.get(0) instanceof ListValue && values.size()==1)
                values = ((ListValue) values.get(0)).getItems();
            return new FormattedTextValue(Messenger.c(values.stream().map(Value::getString).toArray()));
        });

        expression.addContextFunction("run", 1, (c, t, lv) ->
        {
            ServerCommandSource s = ((CarpetContext)c).s;
            try
            {
                Text[] error = {null};
                List<Text> output = new ArrayList<>();
                Value retval = new NumericValue(s.getMinecraftServer().getCommandManager().execute(
                        new SnoopyCommandSource(s, error, output),
                        lv.get(0).getString())
                );
                return ListValue.of(
                        retval,
                        ListValue.wrap(output.stream().map(FormattedTextValue::new).collect(Collectors.toList())),
                        FormattedTextValue.of(error[0])
                );
            }
            catch (Exception exc)
            {
                return ListValue.of(Value.NULL, ListValue.of(), new FormattedTextValue(new LiteralText(exc.getMessage())));
            }
        });

        expression.addContextFunction("save", 0, (c, t, lv) ->
        {
            ServerCommandSource s = ((CarpetContext)c).s;
            s.getMinecraftServer().getPlayerManager().saveAllPlayerData();
            s.getMinecraftServer().save(true,true,true);
            s.getWorld().getChunkManager().tick(() -> true);
            CarpetScriptServer.LOG.warn("Saved chunks");
            return Value.TRUE;
        });

        expression.addContextFunction("tick_time", 0, (c, t, lv) ->
                new NumericValue(((CarpetContext) c).s.getMinecraftServer().getTicks()));

        expression.addContextFunction("world_time", 0, (c, t, lv) ->
                new NumericValue(((CarpetContext) c).s.getWorld().getTime()));

        expression.addContextFunction("day_time", -1, (c, t, lv) ->
        {
            Value time = new NumericValue(((CarpetContext) c).s.getWorld().getTimeOfDay());
            if (lv.size() > 0)
            {
                long newTime = NumericValue.asNumber(lv.get(0)).getLong();
                if (newTime < 0) newTime = 0;
                ((CarpetContext) c).s.getWorld().setTimeOfDay(newTime);// setTimeOfDay(newTime);
            }
            return time;
        });

        expression.addContextFunction("last_tick_times", -1, (c, t, lv) ->
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
            return ListValue.wrap(ticks);
        });


        expression.addContextFunction("game_tick", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            ServerCommandSource s = cc.s;
            if (!s.getMinecraftServer().isOnThread()) throw new InternalExpressionException("Unable to run ticks from threads");
            if (CarpetServer.scriptServer.tickDepth > 16) throw new InternalExpressionException("'game_tick' function caused other 'game_tick' functions to run. You should not allow that.");
            try
            {
                CarpetServer.scriptServer.tickDepth ++;
                ((MinecraftServerInterface) s.getMinecraftServer()).forceTick(() -> System.nanoTime() - CarpetServer.scriptServer.tickStart < 50000000L);
                if (lv.size() > 0)
                {
                    long ms_total = NumericValue.asNumber(lv.get(0)).getLong();
                    long end_expected = CarpetServer.scriptServer.tickStart + ms_total * 1000000L;
                    long wait = end_expected - System.nanoTime();
                    if (wait > 0L)
                    {
                        try
                        {
                            Thread.sleep(wait / 1000000L);
                        }
                        catch (InterruptedException ignored)
                        {
                        }
                    }
                }
                CarpetServer.scriptServer.tickStart = System.nanoTime(); // for the next tick
                Thread.yield();
            }
            finally
            {
                CarpetServer.scriptServer.tickDepth --;
            }
            if(CarpetServer.scriptServer.stopAll)
                throw new ExitStatement(Value.NULL);
            return Value.TRUE;
        });

        expression.addContextFunction("seed", -1, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            c.host.issueDeprecation("seed()");
            return new NumericValue(s.getWorld().getSeed());
        });

        expression.addContextFunction("relight", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            ServerWorld world = cc.s.getWorld();
            ((ThreadedAnvilChunkStorageInterface) world.getChunkManager().threadedAnvilChunkStorage).relightChunk(new ChunkPos(pos));
            WorldTools.forceChunkUpdate(pos, world);
            return Value.TRUE;
        });

        expression.addContextFunction("current_dimension", 0, (c, t, lv) ->
                ValueConversions.of( ((CarpetContext)c).s.getWorld()));

        expression.addContextFunction("view_distance", 0, (c, t, lv) ->
                new NumericValue(((CarpetContext)c).s.getMinecraftServer().getPlayerManager().getViewDistance()));

        // lazy due to passthrough and context changing ability
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

        expression.addContextFunction("plop", -1, (c, t, lv) ->{
            if (lv.size() == 0)
            {
                Map<Value, Value> plopData = new HashMap<>();
                CarpetContext cc = (CarpetContext)c;
                DynamicRegistryManager registryManager = cc.s.getWorld().getRegistryManager();
                plopData.put(StringValue.of("scarpet_custom"),
                        ListValue.wrap(FeatureGenerator.featureMap.keySet().stream().sorted().map(StringValue::of).collect(Collectors.toList()))
                );
                plopData.put(StringValue.of("features"),
                        ListValue.wrap(Registry.FEATURE.getIds().stream().sorted().map(ValueConversions::of).collect(Collectors.toList()))
                );
                plopData.put(StringValue.of("configured_features"),
                        ListValue.wrap(registryManager.get(Registry.CONFIGURED_FEATURE_KEY).getIds().stream().sorted().map(ValueConversions::of).collect(Collectors.toList()))
                );
                plopData.put(StringValue.of("structures"),
                        ListValue.wrap(Registry.STRUCTURE_FEATURE.getIds().stream().sorted().map(ValueConversions::of).collect(Collectors.toList()))
                );
                plopData.put(StringValue.of("configured_structures"),
                        ListValue.wrap(registryManager.get(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY).getIds().stream().sorted().map(ValueConversions::of).collect(Collectors.toList()))
                );
                return MapValue.wrap(plopData);
            }
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 0);
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("'plop' needs extra argument indicating what to plop");
            String what = lv.get(locator.offset).getString();
            Value [] result = new Value[]{Value.NULL};
            ((CarpetContext)c).s.getMinecraftServer().submitAndJoin( () ->
            {
                Boolean res = FeatureGenerator.plop(what, ((CarpetContext) c).s.getWorld(), locator.block.getPos());

                if (res == null)
                    return;
                if (what.equalsIgnoreCase("boulder"))  // there might be more of those
                    WorldTools.forceChunkUpdate(locator.block.getPos(), ((CarpetContext) c).s.getWorld());
                result[0] = BooleanValue.of(res);
            });
            return result[0];
        });

        expression.addContextFunction("schedule", -1, (c, t, lv) -> {
            if (lv.size()<2)
                throw new InternalExpressionException("'schedule' should have at least 2 arguments, delay and call name");
            long delay = NumericValue.asNumber(lv.get(0)).getLong();

            FunctionArgument functionArgument = FunctionArgument.findIn(c, expression.module, lv, 1, false, false);
            CarpetServer.scriptServer.events.scheduleCall(
                    (CarpetContext) c,
                    functionArgument.function,
                    functionArgument.checkedArgs(),
                    delay
            );
            return Value.TRUE;
        });

        expression.addImpureFunction("logger", lv ->
        {
            Value res;

            if(lv.size()==1)
            {
                res = lv.get(0);
                CarpetScriptServer.LOG.info(res.getString());
            }
            else if(lv.size()==2)
            {
                String level = lv.get(0).getString().toLowerCase(Locale.ROOT);
                res = lv.get(1);
                switch(level){
                    case "error": CarpetScriptServer.LOG.error(res.getString()); break;
                    case "warn":  CarpetScriptServer.LOG.warn(res.getString());  break;
                    case "debug": CarpetScriptServer.LOG.debug(res.getString()); break;
                    case "fatal": CarpetScriptServer.LOG.fatal(res.getString()); break;
                    case "info":  CarpetScriptServer.LOG.info(res.getString());  break;
                    default: throw new InternalExpressionException("Unknown log level for 'logger': "+level);
                }
            }
            else throw new InternalExpressionException("logger takes 1 or 2 arguments");

            return res; // pass through for variables
        });

        expression.addContextFunction("list_files", 2, (c, t, lv) ->
        {
            FileArgument fdesc = FileArgument.from(lv,true, FileArgument.Reason.READ);
            Stream<String> files = ((CarpetScriptHost) c.host).listFolder(fdesc);
            if (files == null) return Value.NULL;
            return ListValue.wrap(files.map(StringValue::of).collect(Collectors.toList()));
        });

        expression.addContextFunction("read_file", 2, (c, t, lv) ->
        {
            FileArgument fdesc = FileArgument.from(lv,false, FileArgument.Reason.READ);
            Value retVal;
            if (fdesc.type == FileArgument.Type.NBT)
            {
                NbtElement state = ((CarpetScriptHost) c.host).readFileTag(fdesc);
                if (state == null) return Value.NULL;
                retVal = new NBTSerializableValue(state);
            }
            else if (fdesc.type == FileArgument.Type.JSON)
            {
                JsonElement json;
                json = ((CarpetScriptHost) c.host).readJsonFile(fdesc);
                Value parsedJson = GSON.fromJson(json, Value.class);
                if (parsedJson == null)
                    retVal = Value.NULL;
                else
                    retVal = parsedJson;
            }
            else
            {
                List<String> content = ((CarpetScriptHost) c.host).readTextResource(fdesc);
                if (content == null) return Value.NULL;
                retVal = ListValue.wrap(content.stream().map(StringValue::new).collect(Collectors.toList()));
            }
            return retVal;
        });

        expression.addContextFunction("delete_file", 2, (c, t, lv) ->
                BooleanValue.of(((CarpetScriptHost) c.host).removeResourceFile(FileArgument.from(lv,false, FileArgument.Reason.DELETE))));

        expression.addContextFunction("write_file", -1, (c, t, lv) -> {
            if (lv.size() < 3) throw new InternalExpressionException("'write_file' requires three or more arguments");
            FileArgument fdesc = FileArgument.from(lv, false, FileArgument.Reason.CREATE);

            boolean success;
            if (fdesc.type == FileArgument.Type.NBT)
            {
                Value val = lv.get(2);
                NBTSerializableValue tagValue =  (val instanceof NBTSerializableValue)
                        ? (NBTSerializableValue) val
                        : new NBTSerializableValue(val.getString());
                NbtElement tag = tagValue.getTag();
                success = ((CarpetScriptHost) c.host).writeTagFile(tag, fdesc);
            }
            else if (fdesc.type == FileArgument.Type.JSON)
            {
                List<String> data = Collections.singletonList(GSON.toJson(lv.get(2).toJson()));
                ((CarpetScriptHost) c.host).removeResourceFile(fdesc);
                success = ((CarpetScriptHost) c.host).appendLogFile(fdesc, data);
            }
            else
            {
                List<String> data = new ArrayList<>();
                if (lv.size()==3)
                {
                    Value val = lv.get(2);
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
                        data.add(lv.get(i).getString());
                    }
                }
                success = ((CarpetScriptHost) c.host).appendLogFile(fdesc, data);
            }
            return BooleanValue.of(success);
        });

        expression.addContextFunction("load_app_data", -1, (c, t, lv) ->
        {
            FileArgument fdesc = new FileArgument(null, FileArgument.Type.NBT, null, false, false, FileArgument.Reason.READ);
            if (lv.size()>0)
            {
                c.host.issueDeprecation("load_app_data(...) with arguments");
                String resource = recognizeResource(lv.get(0), false);
                boolean shared = lv.size() > 1 && lv.get(1).getBoolean();
                fdesc = new FileArgument(resource, FileArgument.Type.NBT, null, false, shared, FileArgument.Reason.READ);
            }
            return NBTSerializableValue.of(((CarpetScriptHost) c.host).readFileTag(fdesc));
        });

        expression.addContextFunction("store_app_data", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'store_app_data' needs NBT tag and an optional file");
            Value val = lv.get(0);
            FileArgument fdesc = new FileArgument(null, FileArgument.Type.NBT, null, false, false, FileArgument.Reason.CREATE);
            if (lv.size()>1)
            {
                c.host.issueDeprecation("store_app_data(...) with more than one argument");
                String resource = recognizeResource(lv.get(1), false);
                boolean shared = lv.size() > 2 && lv.get(2).getBoolean();
                fdesc = new FileArgument(resource, FileArgument.Type.NBT, null, false, shared, FileArgument.Reason.CREATE);
            }
            NBTSerializableValue tagValue =  (val instanceof NBTSerializableValue)
                    ? (NBTSerializableValue) val
                    : new NBTSerializableValue(val.getString());
            return BooleanValue.of(((CarpetScriptHost) c.host).writeTagFile(tagValue.getTag(), fdesc));
        });

        expression.addContextFunction("statistic", 3, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerPlayerEntity player = EntityValue.getPlayerByValue(cc.s.getMinecraftServer(), lv.get(0));
            if (player == null) return Value.NULL;
            Identifier category;
            Identifier statName;
            try
            {
                category = new Identifier(lv.get(1).getString());
                statName = new Identifier(lv.get(2).getString());
            }
            catch (InvalidIdentifierException e)
            {
                return Value.NULL;
            }
            StatType<?> type = Registry.STAT_TYPE.get(category);
            if (type == null) return Value.NULL;
            Stat<?> stat = getStat(type, statName);
            if (stat == null) return Value.NULL;
            return new NumericValue(player.getStatHandler().getStat(stat));
        });

        //handle_event('event', function...)
        expression.addContextFunction("handle_event", -1, (c, t, lv) ->
        {
            if (lv.size() < 2)
                throw new InternalExpressionException("'handle_event' requires at least two arguments, event name, and a callback");
            String event = lv.get(0).getString();
            FunctionArgument callback = FunctionArgument.findIn(c, expression.module, lv, 1, true, false);
            CarpetScriptHost host = ((CarpetScriptHost)c.host);
            if (callback.function == null)
                return BooleanValue.of(host.getScriptServer().events.removeBuiltInEvent(event, host));
            // args don't need to be checked will be checked at the event
            return BooleanValue.of( host.getScriptServer().events.handleCustomEvent(event, host, callback.function, callback.args ));
        });
        //signal_event('event', player or null, args.... ) -> number of apps notified
        expression.addContextFunction("signal_event", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'signal' requires at least one argument");
            CarpetContext cc = (CarpetContext)c;
            CarpetScriptServer server = ((CarpetScriptHost)c.host).getScriptServer();
            String eventName = lv.get(0).getString();
            // no such event yet
            if (CarpetEventServer.Event.getEvent(eventName, server) == null) return Value.NULL;
            ServerPlayerEntity player = null;
            List<Value> args = Collections.emptyList();
            if (lv.size() > 1)
            {
                player = EntityValue.getPlayerByValue(server.server, lv.get(1));
                if (lv.size() > 2) args = lv.subList(2, lv.size());
            }
            int counts = ((CarpetScriptHost)c.host).getScriptServer().events.signalEvent(eventName, cc, player, args);
            if (counts < 0) return Value.NULL;
            return new NumericValue(counts);
        });

        // nbt_storage()
        // nbt_storage(key)
        // nbt_storage(key, nbt)
        expression.addContextFunction("nbt_storage", -1, (c, t, lv) -> {
            if (lv.size() > 2) throw new InternalExpressionException("'nbt_storage' requires 0, 1 or 2 arguments.");
            CarpetContext cc = (CarpetContext) c;
            DataCommandStorage storage = cc.s.getMinecraftServer().getDataCommandStorage();
            if (lv.size() == 0)
                return ListValue.wrap(storage.getIds().map(i -> new StringValue(nameFromRegistryId(i))).collect(Collectors.toList()));
            String key = lv.get(0).getString();
            NbtCompound old_nbt = storage.get(new Identifier(key));
            if (lv.size() == 2) {
                Value nbt = lv.get(1);
                NBTSerializableValue new_nbt = (nbt instanceof NBTSerializableValue) ? (NBTSerializableValue) nbt
                        : NBTSerializableValue.parseString(nbt.getString(), true);
                storage.set(new Identifier(key), new_nbt.getCompoundTag());
            }
            return NBTSerializableValue.of(old_nbt);
        });

        // script run create_datapack('foo', {'foo' -> {'bar.json' -> {'c' -> true,'d' -> false,'e' -> {'foo' -> [1,2,3]},'a' -> 'foobar','b' -> 5}}})
        expression.addContextFunction("create_datapack", 2, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            String origName = lv.get(0).getString();
            String name = InputValidator.validateSimpleString(origName, true);
            MinecraftServer server = cc.s.getMinecraftServer();
            for (String dpName : server.getDataPackManager().getNames())
            {
                if (dpName.equalsIgnoreCase("file/"+name+".zip") ||
                        dpName.equalsIgnoreCase("file/"+name))
                    return Value.NULL;

            }
            Value dpdata = lv.get(1);
            if (!(dpdata instanceof MapValue))
                throw new InternalExpressionException("datapack data needs to be a valid map type");
            ResourcePackManager packManager = server.getDataPackManager();
            Path dbFloder = server.getSavePath(WorldSavePath.DATAPACKS);
            Path packFloder = dbFloder.resolve(name+".zip");
            if (Files.exists(packFloder) || Files.exists(dbFloder.resolve(name))) return Value.NULL;
            Boolean [] successful = new Boolean[]{true};
            server.submitAndJoin( () ->
            {
                try {
                    //Files.createDirectory(packFloder);
                    try (FileSystem zipfs = FileSystems.newFileSystem(URI.create("jar:" + packFloder.toUri().toString()), ImmutableMap.of("create", "true"))) {
                        Path zipRoot = zipfs.getPath("/");
                        zipValueToJson(zipRoot.resolve("pack.mcmeta"), MapValue.wrap(
                                ImmutableMap.of(StringValue.of("pack"), MapValue.wrap(ImmutableMap.of(
                                        StringValue.of("pack_format"), new NumericValue(SharedConstants.getGameVersion().getPackVersion()),
                                        StringValue.of("description"), StringValue.of(name),
                                        StringValue.of("source"), StringValue.of("scarpet")
                                )))
                        ));
                        walkTheDPMap((MapValue) dpdata, zipRoot);
                    }
                    packManager.scanPacks();
                    ResourcePackProfile resourcePackProfile = packManager.getProfile("file/" + name + ".zip");
                    if (resourcePackProfile == null || packManager.getEnabledProfiles().contains(resourcePackProfile)) {
                        throw new IOException();
                    }
                    List<ResourcePackProfile> list = Lists.newArrayList(packManager.getEnabledProfiles());
                    resourcePackProfile.getInitialPosition().insert(list, resourcePackProfile, p -> p, false);


                    server.reloadResources(list.stream().map(ResourcePackProfile::getName).collect(Collectors.toList())).
                            exceptionally(exc -> {
                                successful[0] = false;
                                return null;
                            }).join();
                    if (!successful[0]) {
                        throw new IOException();
                    }
                } catch (IOException e)
                {
                    successful[0] = false;
                    try {
                        FileUtils.forceDelete(packFloder.toFile());
                    } catch (IOException ignored) {
                        throw new InternalExpressionException("Failed to install a datapack and failed to clean up after it");
                    }

                }
            });
            return BooleanValue.of(successful[0]);
        });

        expression.addContextFunction("enable_hidden_dimensions", 0, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            // from minecraft.server.Main.main
            MinecraftServer server = cc.s.getMinecraftServer();
            LevelStorage.Session session = ((MinecraftServerInterface)server).getCMSession();
            DataPackSettings dataPackSettings = session.getDataPackSettings();
            ResourcePackManager resourcePackManager = server.getDataPackManager();
            DataPackSettings dataPackSettings2 = MinecraftServer.loadDataPacks(resourcePackManager, dataPackSettings == null ? DataPackSettings.SAFE_MODE : dataPackSettings, false);
            ServerResourceManager serverRM = ((MinecraftServerInterface)server).getResourceManager();
            ReloadableResourceManagerImpl resourceManager = (ReloadableResourceManagerImpl) serverRM.getResourceManager();

            //believe the other one will fillup based on the datapacks only.
            resourceManager.close();
            resourcePackManager.createResourcePacks().forEach(resourceManager::addPack);

            //not sure its needed, but doesn't seem to have a negative effect and might be used in some custom shtuff
            serverRM.loadRegistryTags();

            RegistryOps<NbtElement> registryOps = RegistryOps.of(NbtOps.INSTANCE, serverRM.getResourceManager(), (DynamicRegistryManager.Impl) server.getRegistryManager());
            SaveProperties saveProperties = session.readLevelProperties(registryOps, dataPackSettings2);
            if (saveProperties == null) return Value.NULL;
            //session.backupLevelDataFile(server.getRegistryManager(), saveProperties); // no need

            // MinecraftServer.createWorlds
            // save properties should now contain dimension settings
            GeneratorOptions generatorOptions = saveProperties.getGeneratorOptions();
            boolean bl = generatorOptions.isDebugWorld();
            long l = generatorOptions.getSeed();
            long m = BiomeAccess.hashSeed(l);
            Map<RegistryKey<World>, ServerWorld> existing_worlds = ((MinecraftServerInterface)server).getCMWorlds();
            List<Value> addeds = new ArrayList<>();
            for (Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions> entry : generatorOptions.getDimensions().getEntries()) {
                RegistryKey<DimensionOptions> registryKey = entry.getKey();
                if (!existing_worlds.containsKey(registryKey))
                {
                    addeds.add(ValueConversions.of(registryKey.getValue()));
                    RegistryKey<World> registryKey2 = RegistryKey.of(Registry.WORLD_KEY, registryKey.getValue());
                    DimensionType dimensionType3 = entry.getValue().getDimensionType();
                    ChunkGenerator chunkGenerator3 = entry.getValue().getChunkGenerator();
                    UnmodifiableLevelProperties unmodifiableLevelProperties = new UnmodifiableLevelProperties(saveProperties, ((ServerWorldInterface) server.getOverworld()).getWorldPropertiesCM());
                    ServerWorld serverWorld2 = new ServerWorld(server, Util.getMainWorkerExecutor(), session, unmodifiableLevelProperties, registryKey2, dimensionType3, WorldTools.NOOP_LISTENER, chunkGenerator3, bl, m, ImmutableList.of(), false);
                    server.getOverworld().getWorldBorder().addListener(new WorldBorderListener.WorldBorderSyncer(serverWorld2.getWorldBorder()));
                    existing_worlds.put(registryKey2, serverWorld2);
                }
            }
            return ListValue.wrap(addeds);
        });
    }

    private static void zipValueToJson(Path path, Value output) throws IOException
    {
        JsonElement element = output.toJson();
        if (element == null)
            throw new InternalExpressionException("Cannot interpret "+output.getPrettyString()+" as a json object");
        String string = GSON.toJson(element);
        Files.createDirectories(path.getParent());
        BufferedWriter bufferedWriter = Files.newBufferedWriter(path);
        Throwable incident = null;
        try
        {
            bufferedWriter.write(string);
        }
        catch (Throwable shitHappened)
        {
            incident = shitHappened;
            throw shitHappened;
        }
        finally
        {
            if (incident != null) {
                try {
                    bufferedWriter.close();
                } catch (Throwable otherShitHappened) {
                    incident.addSuppressed(otherShitHappened);
                }
            } else {
                bufferedWriter.close();
            }
        }
    }

    private static void walkTheDPMap(MapValue node, Path path) throws IOException
    {
        Map<Value,Value> items = node.getMap();
        for (Map.Entry<Value, Value> entry : items.entrySet())
        {
            Value val = entry.getValue();
            String strkey = entry.getKey().getString();
            Path child = path.resolve(strkey);
            if (strkey.endsWith(".json"))
            {
                zipValueToJson(child, val);
            }
            else
            {
                if (!(val instanceof MapValue)) throw new InternalExpressionException("Value of "+strkey+" should be a map");
                Files.createDirectory(child);
                walkTheDPMap((MapValue) val, child);
            }
        }
    }

    private static <T> Stat<T> getStat(StatType<T> type, Identifier id)
    {
        T key = type.getRegistry().get(id);
        if (key == null || !((StatTypeInterface)type).hasStatCreated(key))
            return null;
        return type.getOrCreateStat(key);
    }
}
