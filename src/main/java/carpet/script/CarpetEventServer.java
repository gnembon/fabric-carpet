package carpet.script;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.utils.Messenger;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CarpetEventServer
{
    public final List<ScheduledCall> scheduledCalls = new LinkedList<>();
    public final MinecraftServer server;

    public static class Callback
    {
        public final String host;
        public final FunctionValue function;
        public final List<Value> parametrizedArgs;

        public Callback(String host, FunctionValue function, List<Value> parametrizedArgs)
        {
            this.host = host;
            this.function = function;
            this.parametrizedArgs = parametrizedArgs;
        }

        /**
         * Used in entity events
         * @param asSource - entity command source
         * @param runtimeArgs = options
         */
        public boolean execute(ServerCommandSource asSource, List<Value> runtimeArgs)
        {
            //!CarpetServer.scriptServer.runas(source, call.host, call.function, argv)
            //CarpetServer.scriptServer.runas(source, call.host, call.function, argv)
            if (this.parametrizedArgs == null || this.parametrizedArgs.isEmpty())
                return CarpetServer.scriptServer.runas(
                        asSource.withLevel(CarpetSettings.runPermissionLevel),
                        host, function, runtimeArgs);
            else
            {
                List<Value> combinedArgs = new ArrayList<>(runtimeArgs);
                combinedArgs.addAll(this.parametrizedArgs);
                return CarpetServer.scriptServer.runas(
                        asSource.withLevel(CarpetSettings.runPermissionLevel),
                        host, function, combinedArgs);
            }
        }

        @Override
        public String toString()
        {
            return function.getString()+((host==null)?"":"(from "+host+")");
        }
    }

    public static class ScheduledCall extends Callback
    {

        private final CarpetContext ctx;
        public long dueTime;

        public ScheduledCall(CarpetContext context, FunctionValue function, List<Value> args, long dueTime)
        {
            super(context.host.getName(), function, args);
            this.ctx = context;
            this.dueTime = dueTime;
        }

        /**
         * used in scheduled calls
         */
        public void execute()
        {
            CarpetServer.scriptServer.runas(ctx.origin, ctx.s, host, function, parametrizedArgs);
        }


    }

    public static class CallbackList
    {

        public final List<Callback> callList;
        public final int reqArgs;

        public CallbackList(int reqArgs)
        {
            this.callList = new ArrayList<>();
            this.reqArgs = reqArgs;
        }

        public void call(Supplier<List<Value>> argumentSupplier, Supplier<ServerCommandSource> cmdSourceSupplier)
        {
            if (callList.size() > 0)
            {
                List<Value> argv = argumentSupplier.get(); // empty for onTickDone
                ServerCommandSource source = cmdSourceSupplier.get();
                assert argv.size() == reqArgs;
                List<Callback> fails = new ArrayList<>();
                for (Callback call: callList)
                {
                    if (!call.execute(source, argv)) fails.add(call);
                }
                for (Callback call : fails) callList.remove(call);
            }
        }
        public boolean addEventCall(ServerCommandSource source,  String hostName, String funName, Function<ScriptHost, Boolean> verifier)
        {
            ScriptHost host = CarpetServer.scriptServer.getHostByName(hostName);
            if (host == null)
            {
                // impossible call to add
                Messenger.m(source, "r Unknown app "+hostName);
                return false;
            }
            if (!verifier.apply(host))
            {
                Messenger.m(source, "r Global event can only be added to apps with global scope");
                return false;
            }
            FunctionValue udf = host.getFunction(funName);
            if (udf == null || udf.getArguments().size() != reqArgs)
            {
                // call won't match arguments
                Messenger.m(source, "r Callback doesn't expect required number of arguments: "+reqArgs);
                return false;
            }
            //all clear
            //remove duplicates

            removeEventCall(hostName, udf.getString());
            callList.add(new Callback(hostName, udf, null));
            return true;
        }
        public boolean addEventCallInternal(ScriptHost host, FunctionValue function, List<Value> args)
        {
            if (function == null || (function.getArguments().size() - (args==null?0:args.size())) != reqArgs)
            {
                return false;
            }
            //all clear
            //remove duplicates
            removeEventCall(host.getName(), function.getString());
            callList.add(new Callback(host.getName(), function, args));
            return true;
        }

        public void removeEventCall(String hostName, String funName)
        {
            callList.removeIf((c)->  c.function.getString().equals(funName) && ( hostName == c.host || (c.host != null && c.host.equalsIgnoreCase(hostName) )) );
        }

        public void removeAllCalls(String hostName)
        {
            callList.removeIf((c)-> c.host != null && c.host.equals(hostName));
        }
        public void clearEverything()
        {
            callList.clear();
        }
    }

    public static class Event
    {
        public static final Map<String, Event> byName = new HashMap<>();
        public static List<String> publicEvents()
        {
            return byName.entrySet().stream().filter(e -> e.getValue().isPublic).map(Map.Entry::getKey).collect(Collectors.toList());
        }

        public static final Event TICK = new Event("tick", 0, true)
        {
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.getCommandSource().
                                withWorld(CarpetServer.minecraft_server.getWorld(World.OVERWORLD))
                );
            }
        };
        public static final Event NETHER_TICK = new Event("tick_nether", 0, true)
        {
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.getCommandSource().
                                withWorld(CarpetServer.minecraft_server.getWorld(World.NETHER))
                );
            }
        };
        public static final Event ENDER_TICK = new Event("tick_ender", 0, true)
        {
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.getCommandSource().
                                withWorld(CarpetServer.minecraft_server.getWorld(World.END))
                );
            }
        };
        public static final Event CHUNK_GENERATED = new Event("chunk_generated", 2, true)
        {
            @Override
            public void onChunkGenerated(ServerWorld world, Chunk chunk)
            {
                handler.call( () ->
                        {
                            ChunkPos pos = chunk.getPos();
                            return Arrays.asList(
                                    (new NumericValue(pos.x << 4)),
                                    (new NumericValue(pos.z << 4))
                            );
                        }, () -> CarpetServer.minecraft_server.getCommandSource().withWorld(world)
                );
            }
        };

        public static final Event PLAYER_JUMPS = new Event("player_jumps", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_DEPLOYS_ELYTRA = new Event("player_deploys_elytra", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_WAKES_UP = new Event("player_wakes_up", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_RIDES = new Event("player_rides", 5, false)
        {
            @Override
            public void onMountControls(ServerPlayerEntity player, float strafeSpeed, float forwardSpeed, boolean jumping, boolean sneaking)
            {
                handler.call( () -> Arrays.asList(
                        (new EntityValue(player)),
                        (new NumericValue(forwardSpeed)),
                        (new NumericValue(strafeSpeed)),
                        (new NumericValue(jumping)),
                        (new NumericValue(sneaking))
                ), player::getCommandSource);
            }
        };
        public static final Event PLAYER_USES_ITEM = new Event("player_uses_item", 3, false)
        {
            @Override
            public void onItemAction(ServerPlayerEntity player, Hand enumhand, ItemStack itemstack)
            {
                handler.call( () ->
                {
                    //ItemStack itemstack = player.getStackInHand(enumhand);
                    return Arrays.asList(
                            (new EntityValue(player)),
                            (ListValue.fromItemStack(itemstack)),
                            (new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand"))
                    );
                }, player::getCommandSource);
            }
        };
        public static final Event PLAYER_CLICKS_BLOCK = new Event("player_clicks_block", 3, false)
        {
            @Override
            public void onBlockAction(ServerPlayerEntity player, BlockPos blockpos, Direction facing)
            {
                handler.call( () ->
                {
                    return Arrays.asList(
                            (new EntityValue(player)),
                            (new BlockValue(null, player.getServerWorld(), blockpos)),
                            (new StringValue(facing.getName()))
                    );
                }, player::getCommandSource);
            }
        };
        public static final Event PLAYER_RIGHT_CLICKS_BLOCK = new Event("player_right_clicks_block", 6, false)
        {
            @Override
            public void onBlockHit(ServerPlayerEntity player, Hand enumhand, BlockHitResult hitRes)//ItemStack itemstack, Hand enumhand, BlockPos blockpos, Direction enumfacing, Vec3d vec3d)
            {
                handler.call( () ->
                {
                    ItemStack itemstack = player.getStackInHand(enumhand);
                    BlockPos blockpos = hitRes.getBlockPos();
                    Direction enumfacing = hitRes.getSide();
                    Vec3d vec3d = hitRes.getPos().subtract(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                    return Arrays.asList(
                            (new EntityValue(player)),
                            (ListValue.fromItemStack(itemstack)),
                            (new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand")),
                            (new BlockValue(null, player.getServerWorld(), blockpos)),
                            (new StringValue(enumfacing.getName())),
                            (ListValue.of(
                                    new NumericValue(vec3d.x),
                                    new NumericValue(vec3d.y),
                                    new NumericValue(vec3d.z)
                            ))
                    );
                }, player::getCommandSource);
            }
        };
        public static final Event PLAYER_INTERACTS_WITH_BLOCK = new Event("player_interacts_with_block", 5, false)
        {
            @Override
            public void onBlockHit(ServerPlayerEntity player, Hand enumhand, BlockHitResult hitRes)
            {
                handler.call( () ->
                {
                    BlockPos blockpos = hitRes.getBlockPos();
                    Direction enumfacing = hitRes.getSide();
                    Vec3d vec3d = hitRes.getPos().subtract(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                    return Arrays.asList(
                            (new EntityValue(player)),
                            (new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand")),
                            (new BlockValue(null, player.getServerWorld(), blockpos)),
                            (new StringValue(enumfacing.getName())),
                            (ListValue.of(
                                    new NumericValue(vec3d.x),
                                    new NumericValue(vec3d.y),
                                    new NumericValue(vec3d.z)
                            ))
                    );
                }, player::getCommandSource);
            }
        };
        public static final Event PLAYER_PLACES_BLOCK = new Event("player_places_block", 4, false)
        {
            @Override
            public void onBlockPlaced(ServerPlayerEntity player, BlockPos pos, Hand enumhand, ItemStack itemstack)
            {
                handler.call( () -> Arrays.asList(
                        (new EntityValue(player)),
                        (ListValue.fromItemStack(itemstack)),
                        (new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand")),
                        (new BlockValue(null, player.getServerWorld(), pos))
                ), player::getCommandSource);
            }
        };
        public static final Event PLAYER_BREAK_BLOCK = new Event("player_breaks_block", 2, false)
        {
            @Override
            public void onBlockBroken(ServerPlayerEntity player, BlockPos pos, BlockState previousBS)
            {
                handler.call( () -> Arrays.asList(
                        (new EntityValue(player)),
                        (new BlockValue(previousBS, player.getServerWorld(), pos))
                ), player::getCommandSource);
            }
        };
        public static final Event PLAYER_INTERACTS_WITH_ENTITY = new Event("player_interacts_with_entity", 3, false)
        {
            @Override
            public void onEntityHandAction(ServerPlayerEntity player, Entity entity, Hand enumhand)
            {
                handler.call( () -> Arrays.asList(
                        (new EntityValue(player)),
                        (new EntityValue(entity)),
                        (new StringValue(enumhand==Hand.MAIN_HAND?"mainhand":"offhand"))
                ), player::getCommandSource);
            }
        };
        public static final Event PLAYER_PICKS_UP_ITEM = new Event("player_picks_up_item", 2, false)
        {
            @Override
            public void onItemAction(ServerPlayerEntity player, Hand enumhand, ItemStack itemstack) {
                handler.call( () ->
                {
                    //ItemStack itemstack = player.getStackInHand(enumhand);
                    return Arrays.asList(
                            (new EntityValue(player)),
                            (ListValue.fromItemStack(itemstack))
                    );
                }, player::getCommandSource);
            }
        };

        public static final Event PLAYER_ATTACKS_ENTITY = new Event("player_attacks_entity", 2, false)
        {
            @Override
            public void onEntityHandAction(ServerPlayerEntity player, Entity entity, Hand enumhand)
            {
                handler.call( () -> Arrays.asList(
                        (new EntityValue(player)),
                        (new EntityValue(entity))
                ), player::getCommandSource);
            }
        };
        public static final Event PLAYER_STARTS_SNEAKING = new Event("player_starts_sneaking", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_STOPS_SNEAKING = new Event("player_stops_sneaking", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_STARTS_SPRINTING = new Event("player_starts_sprinting", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_STOPS_SPRINTING = new Event("player_stops_sprinting", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };

        public static final Event PLAYER_RELEASED_ITEM = new Event("player_releases_item", 3, false)
        {
            @Override
            public void onItemAction(ServerPlayerEntity player, Hand enumhand, ItemStack itemstack)
            {
                // this.getStackInHand(this.getActiveHand()), this.activeItemStack)
                handler.call( () ->
                {
                    return Arrays.asList(
                            (new EntityValue(player)),
                            (ListValue.fromItemStack(itemstack)),
                            (new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand"))
                    );
                }, player::getCommandSource);
            }
        };
        public static final Event PLAYER_FINISHED_USING_ITEM = new Event("player_finishes_using_item", 3, false)
        {
            @Override
            public void onItemAction(ServerPlayerEntity player, Hand enumhand, ItemStack itemstack)
            {
                // this.getStackInHand(this.getActiveHand()), this.activeItemStack)
                handler.call( () ->
                {
                    return Arrays.asList(
                            (new EntityValue(player)),
                            (ListValue.fromItemStack(itemstack)),
                            (new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand"))
                    );
                }, player::getCommandSource);
            }
        };
        public static final Event PLAYER_DROPS_ITEM = new Event("player_drops_item", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_DROPS_STACK = new Event("player_drops_stack", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_CHOOSES_RECIPE = new Event("player_chooses_recipe", 3, false)
        {
            @Override
            public void onRecipeSelected(ServerPlayerEntity player, Identifier recipe, boolean fullStack)
            {
                handler.call( () ->
                {
                    return Arrays.asList(
                            (new EntityValue(player)),
                            (new StringValue(NBTSerializableValue.nameFromRegistryId(recipe))),
                            (new NumericValue(fullStack))
                    );
                }, player::getCommandSource);
            }
        };
        public static final Event PLAYER_SWITCHES_SLOT = new Event("player_switches_slot", 3, false)
        {
            @Override
            public void onSlotSwitch(ServerPlayerEntity player, int from, int to)
            {
                if (from == to) return; // initial slot update
                handler.call( () ->
                {
                    return Arrays.asList(
                            (new EntityValue(player)),
                            (new NumericValue(from)),
                            (new NumericValue(to))
                    );
                }, player::getCommandSource);
            }
        };
        public static final Event PLAYER_SWAPS_HANDS = new Event("player_swaps_hands", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_TAKES_DAMAGE = new Event("player_takes_damage", 4, false)
        {
            @Override
            public void onDamage(Entity target, float amount, DamageSource source)
            {
                handler.call( () ->
                {
                    return Arrays.asList(
                            ( new EntityValue(target)),
                            ( new NumericValue(amount)),
                            (new StringValue(source.getName())),
                            ( source.getAttacker()==null?Value.NULL:new EntityValue(source.getAttacker()))
                    );
                }, target::getCommandSource);
            }
        };
        public static final Event PLAYER_DEALS_DAMAGE = new Event("player_deals_damage", 3, false)
        {
            @Override
            public void onDamage(Entity target, float amount, DamageSource source)
            {
                handler.call( () ->
                {
                    return Arrays.asList(
                            (new EntityValue(source.getAttacker())),
                            (new NumericValue(amount)),
                            (new EntityValue(target))
                    );
                }, () -> source.getAttacker().getCommandSource());
            }
        };
        public static final Event PLAYER_COLLIDES_WITH_ENTITY = new Event("player_collides_with_entity", 2, false)
        {
            @Override
            public void onEntityHandAction(ServerPlayerEntity player, Entity entity, Hand enumhand) {
                handler.call( () -> Arrays.asList(
                        (new EntityValue(player)),
                        (new EntityValue(entity))
                ), player::getCommandSource);
            }
        };

        public static final Event PLAYER_DIES = new Event("player_dies", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_RESPAWNS = new Event("player_respawns", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_CHANGES_DIMENSION = new Event("player_changes_dimension", 5, false)
        {
            @Override
            public void onDimensionChange(ServerPlayerEntity player, Vec3d from, Vec3d to, RegistryKey<World> fromDim, RegistryKey<World> dimTo)
            {
                // eligibility already checked in mixin
                Value fromValue = ListValue.fromTriple(from.x, from.y, from.z);
                Value toValue = (to == null)?Value.NULL:ListValue.fromTriple(to.x, to.y, to.z);
                Value fromDimStr = new StringValue(NBTSerializableValue.nameFromRegistryId(fromDim.getValue()));
                Value toDimStr = new StringValue(NBTSerializableValue.nameFromRegistryId(dimTo.getValue()));

                handler.call( () -> Arrays.asList(
                        (new EntityValue(player)),
                        (fromValue),
                        (fromDimStr),
                        (toValue),
                        (toDimStr)
                ), player::getCommandSource);
            }
        };
        public static final Event PLAYER_CONNECTS = new Event("player_connects", 1, false) {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList((new EntityValue(player))), player::getCommandSource);
            }
        };
        public static final Event PLAYER_DISCONNECTS = new Event("player_disconnects", 2, false) {
            @Override
            public void onPlayerMessage(ServerPlayerEntity player, String message)
            {
                handler.call( () -> Arrays.asList(
                        (new EntityValue(player)),
                        (new StringValue(message))
                ), player::getCommandSource);
            }
        };
        public static final Event STATISTICS = new Event("statistic", 4, false)
        {
            private <T> Identifier getStatId(Stat<T> stat)
            {
                return stat.getType().getRegistry().getId(stat.getValue());
            }
            private final Set<Identifier> skippedStats = new HashSet<Identifier>(){{
                add(Stats.TIME_SINCE_DEATH);
                add(Stats.TIME_SINCE_REST);
                add(Stats.PLAY_ONE_MINUTE);
            }};
            @Override
            public void onPlayerStatistic(ServerPlayerEntity player, Stat<?> stat, int amount)
            {
                Identifier id = getStatId(stat);
                if (skippedStats.contains(id)) return;
                handler.call( () -> Arrays.asList(
                        (new EntityValue(player)),
                        (new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.STAT_TYPE.getId(stat.getType())))),
                        (new StringValue(NBTSerializableValue.nameFromRegistryId(id))),
                        (new NumericValue(amount))
                ), player::getCommandSource);
            }
        };
        public static final Event LIGHTNING = new Event("lightning", 2, true)
        {
            @Override
            public void onWorldEventFlag(ServerWorld world, BlockPos pos, int flag)
            {
                handler.call(
                        () -> Arrays.asList(
                                (new BlockValue(null, world, pos)),
                                (flag>0?Value.TRUE:Value.FALSE)
                        ), () -> CarpetServer.minecraft_server.getCommandSource().withWorld(world)
                );
            }
        };

        public static String getLoadEvent(EntityType<? extends Entity> et)
        {
            return "entity_loaded_" + Registry.ENTITY_TYPE.getId(et);
        }

        public static final Map<EntityType<? extends Entity>, Event> ENTITY_LOAD= new HashMap<EntityType<? extends Entity>, Event>() {{
            EntityType.get("zombie");
            Registry.ENTITY_TYPE.forEach(et -> {
                put(et, new Event(getLoadEvent(et), 1, true, false)
                {
                    @Override
                    public void onEntityAction(Entity entity)
                    {
                        handler.call(
                                () -> Collections.singletonList((new EntityValue(entity))),
                                () -> CarpetServer.minecraft_server.getCommandSource().withWorld((ServerWorld) entity.world).withLevel(CarpetSettings.runPermissionLevel)
                        );
                    }
                });
            });
        }};

        // on projectile thrown (arrow from bows, crossbows, tridents, snoballs, e-pearls

        public final String name;

        public final CallbackList handler;
        public final boolean globalOnly;
        public final boolean isPublic; // public events can be targetted with __on_<event> defs
        public Event(String name, int reqArgs, boolean isGlobalOnly)
        {
            this(name, reqArgs, isGlobalOnly, true);
        }
        public Event(String name, int reqArgs, boolean isGlobalOnly, boolean isPublic)
        {
            this.name = name;
            this.handler = new CallbackList(reqArgs);
            this.globalOnly = isGlobalOnly;
            this.isPublic = isPublic;
            byName.put(name, this);
        }
        public boolean isNeeded()
        {
            return handler.callList.size() > 0;
        }
        //stubs for calls just to ease calls in vanilla code so they don't need to deal with scarpet value types
        public void onTick() { }
        public void onChunkGenerated(ServerWorld world, Chunk chunk) { }
        public void onPlayerEvent(ServerPlayerEntity player) { }
        public void onPlayerMessage(ServerPlayerEntity player, String message) { }
        public void onPlayerStatistic(ServerPlayerEntity player, Stat<?> stat, int amount) { }
        public void onMountControls(ServerPlayerEntity player, float strafeSpeed, float forwardSpeed, boolean jumping, boolean sneaking) { }
        public void onItemAction(ServerPlayerEntity player, Hand enumhand, ItemStack itemstack) { }
        public void onBlockAction(ServerPlayerEntity player, BlockPos blockpos, Direction facing) { }
        public void onBlockHit(ServerPlayerEntity player, Hand enumhand, BlockHitResult hitRes) { }
        public void onBlockBroken(ServerPlayerEntity player, BlockPos pos, BlockState previousBS) { }
        public void onBlockPlaced(ServerPlayerEntity player, BlockPos pos, Hand enumhand, ItemStack itemstack) { }
        public void onEntityHandAction(ServerPlayerEntity player, Entity entity, Hand enumhand) { }
        public void onEntityAction(Entity entity) { }
        public void onDimensionChange(ServerPlayerEntity player, Vec3d from, Vec3d to, RegistryKey<World> fromDim, RegistryKey<World> dimTo) {}
        public void onDamage(Entity target, float amount, DamageSource source) { }
        public void onRecipeSelected(ServerPlayerEntity player, Identifier recipe, boolean fullStack) {}
        public void onSlotSwitch(ServerPlayerEntity player, int from, int to) {}


        public void onWorldEvent(ServerWorld world, BlockPos pos) { }
        public void onWorldEventFlag(ServerWorld world, BlockPos pos, int flag) { }
    }


    public CarpetEventServer(MinecraftServer server)
    {
        this.server = server;
        for (Event e: Event.byName.values())
            e.handler.callList.clear();
    }

    public void tick()
    {
        Iterator<ScheduledCall> eventIterator = scheduledCalls.iterator();
        List<ScheduledCall> currentCalls = new ArrayList<>();
        while(eventIterator.hasNext())
        {
            ScheduledCall call = eventIterator.next();
            call.dueTime--;
            if (call.dueTime <= 0)
            {
                currentCalls.add(call);
                eventIterator.remove();
            }
        }
        for (ScheduledCall call: currentCalls)
        {
            call.execute();
        }

    }
    public void scheduleCall(CarpetContext context, FunctionValue function, List<Value> args, long due)
    {
        scheduledCalls.add(new ScheduledCall(context, function, args, due));
    }

    public boolean addEvent(ServerCommandSource source, String event, String host, String funName)
    {
        if (!Event.byName.containsKey(event))
        {
            return false;
        }
        Event ev = Event.byName.get(event);
        boolean added = ev.handler.addEventCall(source, host, funName, h -> canAddEvent(ev, h));
        if (added) Messenger.m(source, "gi Added " + funName + " to " + event);
        return added;
    }

    public void addEventDirectly(String event, ScriptHost host, FunctionValue function, List<Value> args)
    {
        Event ev = Event.byName.get(event);
        if (!canAddEvent(ev, host))
            throw new InternalExpressionException("Global event "+event+" can only be added to apps with global scope");
        boolean success =  ev.handler.addEventCallInternal(host, function, args);
        if (!success) throw new InternalExpressionException("Global event "+event+" requires "+ev.handler.reqArgs+", not "+(function.getNumParams()-((args==null)?0:args.size())));
    }

    private boolean canAddEvent(Event event, ScriptHost host)
    {
        return !(event.globalOnly && (host.perUser || host.parent != null));
    }


    public boolean removeEvent(ServerCommandSource source, String event, String funName)
    {

        if (!Event.byName.containsKey(event))
        {
            Messenger.m(source, "r Unknown event: " + event);
            return false;
        }
        Pair<String,String> call = decodeCallback(funName);
        Event.byName.get(event).handler.removeEventCall(call.getLeft(), call.getRight());
        // could verified if actually removed
        Messenger.m(source, "gi Removed event: " + funName + " from "+event);
        return true;
    }
    public boolean removeEventDirectly(String event, ScriptHost host)
    {

        if (!Event.byName.containsKey(event))
        {
            return false;
        }
        Event.byName.get(event).handler.removeAllCalls(host.getName());
        return true;
    }

    public void removeEventDirectly(String event, ScriptHost host, String funName)
    {
        Event.byName.get(event).handler.removeEventCall(host.getName(), funName);
    }

    public void removeAllHostEvents(String hostName)
    {
        Event.byName.values().forEach((e) -> e.handler.removeAllCalls(hostName));
        scheduledCalls.removeIf(sc -> sc.host != null && sc.host.equals(hostName));
    }

    public void clearAll()
    {
        Event.byName.values().forEach(e -> e.handler.clearEverything());
    }

    private Pair<String,String> decodeCallback(String funName)
    {
        Pattern find = Pattern.compile("(\\w+)\\(from (\\w+)\\)");
        Matcher matcher = find.matcher(funName);
        if(matcher.matches())
        {
            return Pair.of(matcher.group(2), matcher.group(1));
        }
        return Pair.of(null, funName);
    }
}
