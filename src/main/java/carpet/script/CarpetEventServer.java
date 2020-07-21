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
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
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

        public Callback(String host, FunctionValue function)
        {
            this.host = host;
            this.function = function;
        }

        @Override
        public String toString()
        {
            return function.getString()+((host==null)?"":"(from "+host+")");
        }
    }

    public static class ScheduledCall extends Callback
    {
        public final List<Value> args;
        private final CarpetContext ctx;
        public long dueTime;

        public ScheduledCall(CarpetContext context, FunctionValue function, List<Value> args, long dueTime)
        {
            super(context.host.getName(), function);
            this.args = args;
            this.ctx = context;
            this.dueTime = dueTime;
        }

        /**
         * used in scheduled calls
         */
        public void execute()
        {
            CarpetServer.scriptServer.runas(ctx.origin, ctx.s, host, function, lazify(args));
        }

        /**
         * Used in entity events
         * @param asSource - entity command source
         * @param args = options
         */
        public void execute(ServerCommandSource asSource, List<Value> args)
        {
            if (this.args == null || this.args.isEmpty())
                CarpetServer.scriptServer.runas(
                        ctx.origin, asSource.withLevel(CarpetSettings.runPermissionLevel),
                        host, function, lazify(args));
            else
            {
                List<Value> combinedArgs = new ArrayList<>();
                combinedArgs.addAll(args);
                combinedArgs.addAll(this.args);
                CarpetServer.scriptServer.runas(
                        ctx.origin, asSource.withLevel(CarpetSettings.runPermissionLevel),
                        host, function, lazify(combinedArgs));
            }
        }
        private List<LazyValue> lazify(List<Value> args)
        {
            return args.stream().map(v -> (LazyValue) (c, t) -> v ).collect(Collectors.toList());
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

        public void call(Supplier<List<LazyValue>> argumentSupplier, Supplier<ServerCommandSource> cmdSourceSupplier)
        {
            if (callList.size() > 0)
            {
                List<LazyValue> argv = argumentSupplier.get(); // empty for onTickDone
                ServerCommandSource source = cmdSourceSupplier.get();
                assert argv.size() == reqArgs;
                List<Callback> fails = new ArrayList<>();
                for (Callback call: callList)
                {
                    if (!CarpetServer.scriptServer.runas(source, call.host, call.function, argv))
                        fails.add(call);
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
            callList.add(new Callback(hostName, udf));
            return true;
        }
        public boolean addEventCallDirect(ScriptHost host, FunctionValue function)
        {
            if (function == null || function.getArguments().size() != reqArgs)
            {
                return false;
            }
            //all clear
            //remove duplicates
            removeEventCall(host.getName(), function.getString());
            callList.add(new Callback(host.getName(), function));
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
    }

    public enum Event
    {
        TICK("tick", 0, true)
        {
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.getCommandSource().
                                withWorld(CarpetServer.minecraft_server.getWorld(DimensionType.OVERWORLD))
                );
            }
        },
        NETHER_TICK("tick_nether", 0, true)
        {
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.getCommandSource().
                                withWorld(CarpetServer.minecraft_server.getWorld(DimensionType.THE_NETHER))
                );
            }
        },
        ENDER_TICK("tick_ender", 0, true)
        {
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.getCommandSource().
                                withWorld(CarpetServer.minecraft_server.getWorld(DimensionType.THE_END))
                );
            }
        },
        CHUNK_GENERATED("chunk_generated", 2, true)
        {
            @Override
            public void onChunkGenerated(ServerWorld world, Chunk chunk)
            {
                handler.call( () ->
                        {
                            ChunkPos pos = chunk.getPos();
                            return Arrays.asList(
                                    ((c, t) -> new NumericValue(pos.x << 4)),
                                    ((c, t) -> new NumericValue(pos.z << 4))
                            );
                        }, () -> CarpetServer.minecraft_server.getCommandSource().withWorld(world)
                );
            }
        },

        PLAYER_JUMPS("player_jumps", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_DEPLOYS_ELYTRA("player_deploys_elytra", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_WAKES_UP("player_wakes_up", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_RIDES("player_rides", 5, false)
        {
            @Override
            public void onMountControls(ServerPlayerEntity player, float strafeSpeed, float forwardSpeed, boolean jumping, boolean sneaking)
            {
                handler.call( () -> Arrays.asList(
                        ((c, t) -> new EntityValue(player)),
                        ((c, t) -> new NumericValue(forwardSpeed)),
                        ((c, t) -> new NumericValue(strafeSpeed)),
                        ((c, t) -> new NumericValue(jumping)),
                        ((c, t) -> new NumericValue(sneaking))
                ), player::getCommandSource);
            }
        },
        PLAYER_USES_ITEM("player_uses_item", 3, false)
        {
            @Override
            public void onItemAction(ServerPlayerEntity player, Hand enumhand, ItemStack itemstack)
            {
                handler.call( () ->
                {
                    //ItemStack itemstack = player.getStackInHand(enumhand);
                    return Arrays.asList(
                            ((c, t) -> new EntityValue(player)),
                            ((c, t) -> ListValue.fromItemStack(itemstack)),
                            ((c, t) -> new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand"))
                    );
                }, player::getCommandSource);
            }
        },
        PLAYER_CLICKS_BLOCK("player_clicks_block", 3, false)
        {
            @Override
            public void onBlockAction(ServerPlayerEntity player, BlockPos blockpos, Direction facing)
            {
                handler.call( () ->
                {
                    return Arrays.asList(
                            ((c, t) -> new EntityValue(player)),
                            ((c, t) -> new BlockValue(null, player.getServerWorld(), blockpos)),
                            ((c, t) -> new StringValue(facing.getName()))
                    );
                }, player::getCommandSource);
            }
        },
        PLAYER_RIGHT_CLICKS_BLOCK("player_right_clicks_block", 6, false)
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
                            ((c, t) -> new EntityValue(player)),
                            ((c, t) -> ListValue.fromItemStack(itemstack)),
                            ((c, t) -> new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand")),
                            ((c, t) -> new BlockValue(null, player.getServerWorld(), blockpos)),
                            ((c, t) -> new StringValue(enumfacing.getName())),
                            ((c, t) -> ListValue.of(
                                    new NumericValue(vec3d.x),
                                    new NumericValue(vec3d.y),
                                    new NumericValue(vec3d.z)
                            ))
                    );
                }, player::getCommandSource);
            }
        },
        PLAYER_INTERACTS_WITH_BLOCK("player_interacts_with_block", 5, false)
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
                            ((c, t) -> new EntityValue(player)),
                            ((c, t) -> new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand")),
                            ((c, t) -> new BlockValue(null, player.getServerWorld(), blockpos)),
                            ((c, t) -> new StringValue(enumfacing.getName())),
                            ((c, t) -> ListValue.of(
                                    new NumericValue(vec3d.x),
                                    new NumericValue(vec3d.y),
                                    new NumericValue(vec3d.z)
                            ))
                    );
                }, player::getCommandSource);
            }
        },
        PLAYER_PLACES_BLOCK("player_places_block", 4, false)
        {
            @Override
            public void onBlockPlaced(ServerPlayerEntity player, BlockPos pos, Hand enumhand, ItemStack itemstack)
            {
                handler.call( () -> Arrays.asList(
                        ((c, t) -> new EntityValue(player)),
                        ((c, t) -> ListValue.fromItemStack(itemstack)),
                        ((c, t) -> new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand")),
                        ((c, t) -> new BlockValue(null, player.getServerWorld(), pos))
                ), player::getCommandSource);
            }
        },
        PLAYER_BREAK_BLOCK("player_breaks_block", 2, false)
        {
            @Override
            public void onBlockBroken(ServerPlayerEntity player, BlockPos pos, BlockState previousBS)
            {
                handler.call( () -> Arrays.asList(
                        ((c, t) -> new EntityValue(player)),
                        ((c, t) -> new BlockValue(previousBS, player.getServerWorld(), pos))
                ), player::getCommandSource);
            }
        },
        PLAYER_INTERACTS_WITH_ENTITY("player_interacts_with_entity", 3, false)
        {
            @Override
            public void onEntityAction(ServerPlayerEntity player, Entity entity, Hand enumhand)
            {
                handler.call( () -> Arrays.asList(
                        ((c, t) -> new EntityValue(player)),
                        ((c, t) -> new EntityValue(entity)),
                        ((c, t) -> new StringValue(enumhand==Hand.MAIN_HAND?"mainhand":"offhand"))
                ), player::getCommandSource);
            }
        },
        PLAYER_ATTACKS_ENTITY("player_attacks_entity", 2, false)
        {
            @Override
            public void onEntityAction(ServerPlayerEntity player, Entity entity, Hand enumhand)
            {
                handler.call( () -> Arrays.asList(
                        ((c, t) -> new EntityValue(player)),
                        ((c, t) -> new EntityValue(entity))
                ), player::getCommandSource);
            }
        },
        PLAYER_STARTS_SNEAKING("player_starts_sneaking", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_STOPS_SNEAKING("player_stops_sneaking", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_STARTS_SPRINTING("player_starts_sprinting", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_STOPS_SPRINTING("player_stops_sprinting", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },

        PLAYER_RELEASED_ITEM("player_releases_item", 3, false)
        {
            @Override
            public void onItemAction(ServerPlayerEntity player, Hand enumhand, ItemStack itemstack)
            {
                // this.getStackInHand(this.getActiveHand()), this.activeItemStack)
                handler.call( () ->
                {
                    return Arrays.asList(
                            ((c, t) -> new EntityValue(player)),
                            ((c, t) -> ListValue.fromItemStack(itemstack)),
                            ((c, t) -> new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand"))
                    );
                }, player::getCommandSource);
            }
        },
        PLAYER_FINISHED_USING_ITEM("player_finishes_using_item", 3, false)
        {
            @Override
            public void onItemAction(ServerPlayerEntity player, Hand enumhand, ItemStack itemstack)
            {
                // this.getStackInHand(this.getActiveHand()), this.activeItemStack)
                handler.call( () ->
                {
                    return Arrays.asList(
                            ((c, t) -> new EntityValue(player)),
                            ((c, t) -> ListValue.fromItemStack(itemstack)),
                            ((c, t) -> new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand"))
                    );
                }, player::getCommandSource);
            }
        },
        PLAYER_DROPS_ITEM("player_drops_item", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_DROPS_STACK("player_drops_stack", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_CHOOSES_RECIPE("player_chooses_recipe", 3, false)
        {
            @Override
            public void onRecipeSelected(ServerPlayerEntity player, Identifier recipe, boolean fullStack)
            {
                handler.call( () ->
                {
                    return Arrays.asList(
                            ((c, t) -> new EntityValue(player)),
                            ((c, t) -> new StringValue(NBTSerializableValue.nameFromRegistryId(recipe))),
                            ((c, t) -> new NumericValue(fullStack))
                    );
                }, player::getCommandSource);
            }
        },
        PLAYER_SWITCHES_SLOT("player_switches_slot", 3, false)
        {
            @Override
            public void onSlotSwitch(ServerPlayerEntity player, int from, int to)
            {
                if (from == to) return; // initial slot update
                handler.call( () ->
                {
                    return Arrays.asList(
                            ((c, t) -> new EntityValue(player)),
                            ((c, t) -> new NumericValue(from)),
                            ((c, t) -> new NumericValue(to))
                    );
                }, player::getCommandSource);
            }
        },
        PLAYER_SWAPS_HANDS("player_swaps_hands", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_TAKES_DAMAGE("player_takes_damage", 4, false)
        {
            @Override
            public void onDamage(Entity target, float amount, DamageSource source)
            {
                handler.call( () ->
                {
                    return Arrays.asList(
                            ((c, t) -> new EntityValue(target)),
                            ((c, t) -> new NumericValue(amount)),
                            ((c, t) ->new StringValue(source.getName())),
                            ((c, t) -> source.getAttacker()==null?Value.NULL:new EntityValue(source.getAttacker()))
                    );
                }, target::getCommandSource);
            }
        },
        PLAYER_DEALS_DAMAGE("player_deals_damage", 3, false)
        {
            @Override
            public void onDamage(Entity target, float amount, DamageSource source)
            {
                handler.call( () ->
                {
                    return Arrays.asList(
                            ((c, t) -> new EntityValue(source.getAttacker())),
                            ((c, t) -> new NumericValue(amount)),
                            ((c, t) -> new EntityValue(target))
                    );
                }, () -> source.getAttacker().getCommandSource());
            }
        },
        PLAYER_DIES("player_dies", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_RESPAWNS("player_respawns", 1, false)
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_CHANGES_DIMENSION("player_changes_dimension", 5, false)
        {
            @Override
            public void onDimensionChange(ServerPlayerEntity player, Vec3d from, Vec3d to, DimensionType fromDim, DimensionType dimTo)
            {
                // eligibility already checked in mixin
                Value fromValue = ListValue.fromTriple(from.x, from.y, from.z);
                Value toValue = (to == null)?Value.NULL:ListValue.fromTriple(to.x, to.y, to.z);
                Value fromDimStr = new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.DIMENSION_TYPE.getId(fromDim)));
                Value toDimStr = new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.DIMENSION_TYPE.getId(dimTo)));

                handler.call( () -> Arrays.asList(
                        ((c, t) -> new EntityValue(player)),
                        ((c, t) -> fromValue),
                        ((c, t) -> fromDimStr),
                        ((c, t) -> toValue),
                        ((c, t) -> toDimStr)
                ), player::getCommandSource);
            }
        },
        PLAYER_CONNECTS("player_connects", 1, false) {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_DISCONNECTS("player_disconnects", 2, false) {
            @Override
            public void onPlayerMessage(ServerPlayerEntity player, String message)
            {
                handler.call( () -> Arrays.asList(
                        ((c, t) -> new EntityValue(player)),
                        ((c, t) -> new StringValue(message))
                ), player::getCommandSource);
            }
        },
        STATISTICS("statistic", 4, false)
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
                        ((c, t) -> new EntityValue(player)),
                        ((c, t) -> new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.STAT_TYPE.getId(stat.getType())))),
                        ((c, t) -> new StringValue(NBTSerializableValue.nameFromRegistryId(id))),
                        ((c, t) -> new NumericValue(amount))
                ), player::getCommandSource);
            }
        },
        LIGHTNING("lightning", 2, true)
        {
            @Override
            public void onWorldEventFlag(ServerWorld world, BlockPos pos, int flag)
            {
                handler.call(
                        () -> Arrays.asList(
                                ((c, t) -> new BlockValue(null, world, pos)),
                                ((c, t) -> flag>0?Value.TRUE:Value.FALSE)
                        ), () -> CarpetServer.minecraft_server.getCommandSource().withWorld(world)
                );
            }
        },
        ;

        // on projectile thrown (arrow from bows, crossbows, tridents, snoballs, e-pearls

        public final String name;
        public static final Map<String, Event> byName = new HashMap<String, Event>(){{
            for (Event e: Event.values())
            {
                put(e.name, e);
            }
        }};
        public final CallbackList handler;
        public final boolean globalOnly;
        Event(String name, int reqArgs, boolean isGlobalOnly)
        {
            this.name = name;
            this.handler = new CallbackList(reqArgs);
            this.globalOnly = isGlobalOnly;
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
        public void onEntityAction(ServerPlayerEntity player, Entity entity, Hand enumhand) { }
        public void onDimensionChange(ServerPlayerEntity player, Vec3d from, Vec3d to, DimensionType fromDim, DimensionType dimTo) {}
        public void onDamage(Entity target, float amount, DamageSource source) { }
        public void onRecipeSelected(ServerPlayerEntity player, Identifier recipe, boolean fullStack) {}
        public void onSlotSwitch(ServerPlayerEntity player, int from, int to) {}


        public void onWorldEvent(ServerWorld world, BlockPos pos) { }
        public void onWorldEventFlag(ServerWorld world, BlockPos pos, int flag) { }
    }


    public CarpetEventServer(MinecraftServer server)
    {
        this.server = server;
        for (Event e: Event.values())
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

    public boolean addEventDirectly(String event, ScriptHost host, FunctionValue function)
    {
        Event ev = Event.byName.get(event);
        if (!canAddEvent(ev, host))
            throw new InternalExpressionException("Global event "+event+" can only be added to apps with global scope");
        return ev.handler.addEventCallDirect(host, function);
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

    public void removeEventDirectly(String event, ScriptHost host, String funName)
    {
        Event.byName.get(event).handler.removeEventCall(host.getName(), funName);
    }

    public void removeAllHostEvents(String hostName)
    {
        Event.byName.values().forEach((e) -> e.handler.removeAllCalls(hostName));
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
