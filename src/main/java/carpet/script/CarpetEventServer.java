package carpet.script;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.helpers.TickSpeed;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.utils.GlocalFlag;
import carpet.script.value.BlockValue;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.utils.CarpetProfiler;
import carpet.utils.Messenger;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CarpetEventServer
{
    public final List<ScheduledCall> scheduledCalls = new LinkedList<>();
    public final CarpetScriptServer scriptServer;
    private static final List<Value> NOARGS = Collections.emptyList();
    public final Map<String, Event> customEvents = new HashMap<>();
    public GlocalFlag handleEvents = new GlocalFlag(true);

    public enum CallbackResult
    {
        SUCCESS, PASS, FAIL, CANCEL
    }

    public static class Callback
    {
        public final String host;
        public final String optionalTarget;
        public final FunctionValue function;
        public final List<Value> parametrizedArgs;

        public Callback(String host, String target, FunctionValue function, List<Value> parametrizedArgs)
        {
            this.host = host;
            this.function = function;
            this.optionalTarget = target;
            this.parametrizedArgs = parametrizedArgs==null?NOARGS:parametrizedArgs;
        }

        /**
         * Used also in entity events
         * @param sender - entity command source
         * @param runtimeArgs = options
         */
        public CallbackResult execute(CommandSourceStack sender, List<Value> runtimeArgs)
        {
            if (!this.parametrizedArgs.isEmpty())
            {
                runtimeArgs = new ArrayList<>(runtimeArgs);
                runtimeArgs.addAll(this.parametrizedArgs);
            }
            if (CarpetServer.scriptServer == null) return CallbackResult.FAIL; // already stopped
            return CarpetServer.scriptServer.events.runEventCall(
                    sender.withPermission(CarpetSettings.runPermissionLevel),
                    host, optionalTarget, function, runtimeArgs);
        }

        /**
         * Used also in entity events
         * @param sender - sender of the signal
         * @param optionalRecipient - optional target player argument
         * @param runtimeArgs = options
         */
        public CallbackResult signal(CommandSourceStack sender, ServerPlayer optionalRecipient, List<Value> runtimeArgs)
        {
            // recipent of the call doesn't match the handlingHost
            if (optionalRecipient != null && !optionalRecipient.getScoreboardName().equals(optionalTarget))
                return CallbackResult.FAIL;
            return execute(sender, runtimeArgs);
        }


        @Override
        public String toString()
        {
            return function.getString()+((host==null)?"":"(from "+host+(optionalTarget == null?"":"/"+optionalTarget)+")");
        }
        public static record Signature(String function, String host, String target) {}
        
        public static Signature fromString(String str)
        {
            Pattern find = Pattern.compile("(\\w+)(?:\\(from (\\w+)(?:/(\\w+))?\\))?");
            Matcher matcher = find.matcher(str);
            if(matcher.matches())
            {
                return new Signature(matcher.group(1), matcher.group(2), matcher.group(3));
            }
            return new Signature(str, null, null);
        }
    }

    public static class ScheduledCall extends Callback
    {

        private final CarpetContext ctx;
        public long dueTime;

        public ScheduledCall(CarpetContext context, FunctionValue function, List<Value> args, long dueTime)
        {
            // ignoring target as we will be always calling self
            super(context.host.getName(), null, function, args);
            this.ctx = context.duplicate();
            this.dueTime = dueTime;
        }

        /**
         * used in scheduled calls
         */
        public void execute()
        {
            CarpetServer.scriptServer.events.runScheduledCall(ctx.origin, ctx.s, host, (CarpetScriptHost) ctx.host, function, parametrizedArgs);
        }
    }

    public static class CallbackList
    {

        private List<Callback> callList;
        private final List<Callback> removedCalls;
        private boolean inCall;
        private boolean inSignal;
        public final int reqArgs;
        final boolean isSystem;
        final boolean perPlayerDistribution;

        public CallbackList(int reqArgs, boolean isSystem, boolean isGlobalOnly)
        {
            this.callList = new ArrayList<>();
            this.removedCalls = new ArrayList<>();
            this.inCall = false;
            this.inSignal = false;
            this.reqArgs = reqArgs;
            this.isSystem = isSystem;
            perPlayerDistribution = isSystem && !isGlobalOnly;
        }

        public List<Callback> inspectCurrentCalls()
        {
            return new ArrayList<>(callList);
        }

        private void removeCallsIf(Predicate<Callback> when)
        {
            if (!inCall && !inSignal)
            {
                callList.removeIf(when);
                return;
            }
            // we are ok with list growing in the meantime and parallel access, we are only scanning.
            for (int i = 0; i < callList.size(); i++)
            {
                Callback call = callList.get(i);
                if (when.test(call)) removedCalls.add(call);
            }
        }

        /**
         * Handles only built-in events from the events system
         * @param argumentSupplier
         * @param cmdSourceSupplier
         * @return Whether this event call has been cancelled
         */
        public boolean call(Supplier<List<Value>> argumentSupplier, Supplier<CommandSourceStack> cmdSourceSupplier)
        {
            if (callList.size() > 0 && CarpetServer.scriptServer != null)
            {
                Boolean isCancelled = CarpetServer.scriptServer.events.handleEvents.runIfEnabled( () -> {
                    CarpetProfiler.ProfilerToken currentSection = CarpetProfiler.start_section(null, "Scarpet events", CarpetProfiler.TYPE.GENERAL);
                    List<Value> argv = argumentSupplier.get(); // empty for onTickDone
                    CommandSourceStack source;
                    try
                    {
                        source = cmdSourceSupplier.get();
                    }
                    catch (NullPointerException noReference) // todo figure out what happens when closing.
                    {
                        return false;
                    }
                    String nameCheck = perPlayerDistribution ? source.getTextName() : null;
                    assert argv.size() == reqArgs;
                    boolean cancelled = false;
                    try
                    {
                        // we are ok with list growing in the meantime
                        // which might happen during inCall or inSignal
                        inCall = true;
                        for (int i = 0; i < callList.size(); i++)
                        {
                            Callback call = callList.get(i);
                            // supressing calls where target player hosts simply don't match
                            // handling global hosts with player targets is left to when the host is resolved (few calls deeper).
                            if (nameCheck != null && call.optionalTarget != null && !nameCheck.equals(call.optionalTarget)) continue;
                            CallbackResult result = call.execute(source, argv);
                            if(result == CallbackResult.CANCEL) {
                                cancelled = true;
                                break;
                            }
                            if (result == CallbackResult.FAIL) removedCalls.add(call);
                        }
                    }
                    finally
                    {
                        inCall = false;
                    }
                    for (Callback call : removedCalls) callList.remove(call);
                    removedCalls.clear();
                    CarpetProfiler.end_current_section(currentSection);
                    return cancelled;
                });
                return isCancelled != null && isCancelled;
            }
            return false;
        }

        public int signal(CommandSourceStack sender, ServerPlayer optinoalReceipient, List<Value> callArg)
        {
            if (callList.isEmpty()) return 0;
            int successes = 0;
            try
            {
                inSignal = true;
                for (int i = 0; i < callList.size(); i++)
                {
                    // skipping tracking of fails, its explicit call
                    if (callList.get(i).signal(sender, optinoalReceipient, callArg) == CallbackResult.SUCCESS) successes++;
                }
            }
            finally
            {
                inSignal = false;
            }
            return successes;
        }

        public boolean addFromExternal(CommandSourceStack source, String hostName, String funName, Consumer<ScriptHost> hostOnEventHandler)
        {
            ScriptHost host = CarpetServer.scriptServer.getAppHostByName(hostName);
            if (host == null)
            {
                // impossible call to add
                Messenger.m(source, "r Unknown app "+hostName);
                return false;
            }
            hostOnEventHandler.accept(host);
            FunctionValue udf = host.getFunction(funName);
            if (udf == null || udf.getArguments().size() != reqArgs)
            {
                // call won't match arguments
                Messenger.m(source, "r Callback doesn't expect required number of arguments: "+reqArgs);
                return false;
            }
            String target = null;
            if (host.isPerUser())
            {
                try
                {
                    target = source.getPlayerOrException().getScoreboardName();
                }
                catch (CommandSyntaxException e)
                {
                    Messenger.m(source, "r Cannot add event to a player scoped app from a command without a player context");
                    return false;
                }
            }
            //all clear
            //remove duplicates

            removeEventCall(hostName, target, udf.getString());
            callList.add(new Callback(hostName, target, udf, null));
            return true;
        }
        public boolean addEventCallInternal(ScriptHost host, FunctionValue function, List<Value> args)
        {
            if (function == null || (function.getArguments().size() - args.size()) != reqArgs)
            {
                return false;
            }
            //removing duplicates
            removeEventCall(host.getName(), host.user, function.getString());
            callList.add(new Callback(host.getName(), host.user, function, args));
            return true;
        }

        public void removeEventCall(String hostName, String target, String funName)
        {
            removeCallsIf((c)->  c.function.getString().equals(funName)
                    && (Objects.equals(c.host, hostName))
                    && (Objects.equals(c.optionalTarget, target))
            );
        }

        public void removeAllCalls(CarpetScriptHost host)
        {
            removeCallsIf((c)-> (Objects.equals(c.host, host.getName()))
                    && (Objects.equals(c.optionalTarget, host.user)));
        }

        public void createChildEvents(CarpetScriptHost host)
        {
            List<Callback> copyCalls = new ArrayList<>();
            callList.forEach((c)->
            {
                if ((Objects.equals(c.host, host.getName()))
                    && c.optionalTarget == null)
                {
                    copyCalls.add(new Callback(c.host, host.user, c.function, c.parametrizedArgs));
                }
            });
            callList.addAll(copyCalls);
        }

        public void clearEverything()
        {
            // when some moron puts /reload in an event call.
            if (inSignal || inCall) callList = new ArrayList<>();
            callList.clear();
        }

        public void sortByPriority(CarpetScriptServer scriptServer) {
            callList.sort(Comparator.comparingDouble(c -> -scriptServer.getAppHostByName(c.host).eventPriority));
        }
    }

    public static class Event
    {
        public static final Map<String, Event> byName = new HashMap<>();
        public static List<Event> publicEvents(CarpetScriptServer server)
        {
            List<Event> events = byName.values().stream().filter(e -> e.isPublic).collect(Collectors.toList());
            if (server != null) events.addAll(server.events.customEvents.values());
            return events;
        }

        public static final Event START = new Event("server_starts", 0, true)
        {
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.createCommandSourceStack().
                                withLevel(CarpetServer.minecraft_server.getLevel(Level.OVERWORLD))
                );
            }
        };

        public static final Event SHUTDOWN = new Event("server_shuts_down", 0, true)
        {
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.createCommandSourceStack().
                                withLevel(CarpetServer.minecraft_server.getLevel(Level.OVERWORLD))
                );
            }
        };

        public static final Event TICK = new Event("tick", 0, true)
        {
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.createCommandSourceStack().
                                withLevel(CarpetServer.minecraft_server.getLevel(Level.OVERWORLD))
                );
            }
        };
        public static final Event NETHER_TICK = new Event("tick_nether", 0, true)
        {
            @Override
            public boolean deprecated()
            {
                return true;
            }

            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.createCommandSourceStack().
                                withLevel(CarpetServer.minecraft_server.getLevel(Level.NETHER))
                );
            }
        };
        public static final Event ENDER_TICK = new Event("tick_ender", 0, true)
        {
            @Override
            public boolean deprecated()
            {
                return true;
            }
            @Override
            public void onTick()
            {
                handler.call(Collections::emptyList, () ->
                        CarpetServer.minecraft_server.createCommandSourceStack().
                                withLevel(CarpetServer.minecraft_server.getLevel(Level.END))
                );
            }
        };
        public static final Event CHUNK_GENERATED = new Event("chunk_generated", 2, true)
        {
            @Override
            public void onChunkEvent(ServerLevel world, ChunkPos chPos, boolean generated)
            {
                handler.call( () ->
                        {
                            return Arrays.asList(new NumericValue(chPos.x << 4), new NumericValue(chPos.z << 4));
                        }, () -> CarpetServer.minecraft_server.createCommandSourceStack().withLevel(world)
                );
            }
        };
        public static final Event CHUNK_LOADED = new Event("chunk_loaded", 2, true)
        {
            @Override
            public void onChunkEvent(ServerLevel world, ChunkPos chPos, boolean generated)
            {
                handler.call( () ->
                        {
                            return Arrays.asList(new NumericValue(chPos.x << 4), new NumericValue(chPos.z << 4));
                        }, () -> CarpetServer.minecraft_server.createCommandSourceStack().withLevel(world)
                );
            }
        };

        public static final Event CHUNK_UNLOADED = new Event("chunk_unloaded", 2, true)
        {
            @Override
            public void onChunkEvent(ServerLevel world, ChunkPos chPos, boolean generated)
            {
                handler.call(
                        () -> Arrays.asList(new NumericValue(chPos.x << 4), new NumericValue(chPos.z << 4)),
                        () -> CarpetServer.minecraft_server.createCommandSourceStack().withLevel(world)
                );
            }
        };

        public static final Event PLAYER_JUMPS = new Event("player_jumps", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_DEPLOYS_ELYTRA = new Event("player_deploys_elytra", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_WAKES_UP = new Event("player_wakes_up", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_ESCAPES_SLEEP = new Event("player_escapes_sleep", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_RIDES = new Event("player_rides", 5, false)
        {
            @Override
            public void onMountControls(ServerPlayer player, float strafeSpeed, float forwardSpeed, boolean jumping, boolean sneaking)
            {
                handler.call( () -> Arrays.asList(new EntityValue(player),
                        new NumericValue(forwardSpeed), new NumericValue(strafeSpeed), BooleanValue.of(jumping), BooleanValue.of(sneaking)
                ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_USES_ITEM = new Event("player_uses_item", 3, false)
        {
            @Override
            public boolean onItemAction(ServerPlayer player, InteractionHand enumhand, ItemStack itemstack)
            {
                return handler.call( () ->
                {
                    //ItemStack itemstack = player.getStackInHand(enumhand);
                    return Arrays.asList(
                            new EntityValue(player),
                            ValueConversions.of(itemstack),
                            StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand")
                    );
                }, player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_CLICKS_BLOCK = new Event("player_clicks_block", 3, false)
        {
            @Override
            public boolean onBlockAction(ServerPlayer player, BlockPos blockpos, Direction facing)
            {
                return handler.call( () ->
                {
                    return Arrays.asList(
                            new EntityValue(player),
                            new BlockValue(null, player.getLevel(), blockpos),
                            StringValue.of(facing.getName())
                    );
                }, player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_RIGHT_CLICKS_BLOCK = new Event("player_right_clicks_block", 6, false)
        {
            @Override
            public boolean onBlockHit(ServerPlayer player, InteractionHand enumhand, BlockHitResult hitRes)//ItemStack itemstack, Hand enumhand, BlockPos blockpos, Direction enumfacing, Vec3d vec3d)
            {
                return handler.call( () ->
                {
                    ItemStack itemstack = player.getItemInHand(enumhand);
                    BlockPos blockpos = hitRes.getBlockPos();
                    Direction enumfacing = hitRes.getDirection();
                    Vec3 vec3d = hitRes.getLocation().subtract(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                    return Arrays.asList(
                            new EntityValue(player),
                            ValueConversions.of(itemstack),
                            StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand"),
                            new BlockValue(null, player.getLevel(), blockpos),
                            StringValue.of(enumfacing.getName()),
                            ListValue.of(
                                    new NumericValue(vec3d.x),
                                    new NumericValue(vec3d.y),
                                    new NumericValue(vec3d.z)
                            )
                    );
                }, player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_INTERACTS_WITH_BLOCK = new Event("player_interacts_with_block", 5, false)
        {
            @Override
            public boolean onBlockHit(ServerPlayer player, InteractionHand enumhand, BlockHitResult hitRes)
            {
                handler.call( () ->
                {
                    BlockPos blockpos = hitRes.getBlockPos();
                    Direction enumfacing = hitRes.getDirection();
                    Vec3 vec3d = hitRes.getLocation().subtract(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                    return Arrays.asList(
                            new EntityValue(player),
                            StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand"),
                            new BlockValue(null, player.getLevel(), blockpos),
                            StringValue.of(enumfacing.getName()),
                            ListValue.of(
                                    new NumericValue(vec3d.x),
                                    new NumericValue(vec3d.y),
                                    new NumericValue(vec3d.z)
                            )
                    );
                }, player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_PLACING_BLOCK = new Event("player_placing_block", 4, false)
        {
            @Override
            public boolean onBlockPlaced(ServerPlayer player, BlockPos pos, InteractionHand enumhand, ItemStack itemstack)
            {
                return handler.call( () -> Arrays.asList(
                        new EntityValue(player),
                        ValueConversions.of(itemstack),
                        StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand"),
                        new BlockValue(null, player.getLevel(), pos)
                ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_PLACES_BLOCK = new Event("player_places_block", 4, false)
        {
            @Override
            public boolean onBlockPlaced(ServerPlayer player, BlockPos pos, InteractionHand enumhand, ItemStack itemstack)
            {
                handler.call( () -> Arrays.asList(
                        new EntityValue(player),
                        ValueConversions.of(itemstack),
                        StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand"),
                        new BlockValue(null, player.getLevel(), pos)
                ), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_BREAK_BLOCK = new Event("player_breaks_block", 2, false)
        {
            @Override
            public boolean onBlockBroken(ServerPlayer player, BlockPos pos, BlockState previousBS)
            {
                return handler.call(
                        () -> Arrays.asList(new EntityValue(player), new BlockValue(previousBS, player.getLevel(), pos)),
                        player::createCommandSourceStack
                );
            }
        };
        public static final Event PLAYER_INTERACTS_WITH_ENTITY = new Event("player_interacts_with_entity", 3, false)
        {
            @Override
            public boolean onEntityHandAction(ServerPlayer player, Entity entity, InteractionHand enumhand)
            {
                return handler.call( () -> Arrays.asList(
                        new EntityValue(player), new EntityValue(entity), StringValue.of(enumhand==InteractionHand.MAIN_HAND?"mainhand":"offhand")
                ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_TRADES = new Event("player_trades", 5, false)
        {
            @Override
            public void onTrade(ServerPlayer player, Merchant merchant, MerchantOffer tradeOffer)
            {
                handler.call( () -> Arrays.asList(
                        new EntityValue(player),
                        merchant instanceof AbstractVillager ? new EntityValue((AbstractVillager) merchant) : Value.NULL,
                        ValueConversions.of(tradeOffer.getBaseCostA()),
                        ValueConversions.of(tradeOffer.getCostB()),
                        ValueConversions.of(tradeOffer.getResult())
                ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_PICKS_UP_ITEM = new Event("player_picks_up_item", 2, false)
        {
            @Override
            public boolean onItemAction(ServerPlayer player, InteractionHand enumhand, ItemStack itemstack) {
                handler.call( () -> Arrays.asList(new EntityValue(player), ValueConversions.of(itemstack)), player::createCommandSourceStack);
                return false;
            }
        };

        public static final Event PLAYER_ATTACKS_ENTITY = new Event("player_attacks_entity", 2, false)
        {
            @Override
            public boolean onEntityHandAction(ServerPlayer player, Entity entity, InteractionHand enumhand)
            {
                return handler.call( () -> Arrays.asList(new EntityValue(player), new EntityValue(entity)), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_STARTS_SNEAKING = new Event("player_starts_sneaking", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_STOPS_SNEAKING = new Event("player_stops_sneaking", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_STARTS_SPRINTING = new Event("player_starts_sprinting", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_STOPS_SPRINTING = new Event("player_stops_sprinting", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };

        public static final Event PLAYER_RELEASED_ITEM = new Event("player_releases_item", 3, false)
        {
            @Override
            public boolean onItemAction(ServerPlayer player, InteractionHand enumhand, ItemStack itemstack)
            {
                // this.getStackInHand(this.getActiveHand()), this.activeItemStack)
                handler.call( () ->
                        Arrays.asList(
                                new EntityValue(player),
                                ValueConversions.of(itemstack),
                                StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand")
                        ), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_FINISHED_USING_ITEM = new Event("player_finishes_using_item", 3, false)
        {
            @Override
            public boolean onItemAction(ServerPlayer player, InteractionHand enumhand, ItemStack itemstack)
            {
                // this.getStackInHand(this.getActiveHand()), this.activeItemStack)
                return handler.call( () ->
                        Arrays.asList(
                                new EntityValue(player),
                                ValueConversions.of(itemstack),
                                new StringValue(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand")
                        ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_DROPS_ITEM = new Event("player_drops_item", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                return handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_DROPS_STACK = new Event("player_drops_stack", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                return handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_CHOOSES_RECIPE = new Event("player_chooses_recipe", 3, false)
        {
            @Override
            public boolean onRecipeSelected(ServerPlayer player, ResourceLocation recipe, boolean fullStack)
            {
                return handler.call( () ->
                        Arrays.asList(
                                new EntityValue(player),
                                StringValue.of(NBTSerializableValue.nameFromRegistryId(recipe)),
                                BooleanValue.of(fullStack)
                        ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_SWITCHES_SLOT = new Event("player_switches_slot", 3, false)
        {
            @Override
            public void onSlotSwitch(ServerPlayer player, int from, int to)
            {
                if (from == to) return; // initial slot update
                handler.call( () ->
                        Arrays.asList(
                                new EntityValue(player),
                                new NumericValue(from),
                                new NumericValue(to)
                        ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_SWAPS_HANDS = new Event("player_swaps_hands", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                return handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_SWINGS_HAND = new Event("player_swings_hand", 2, false)
        {
            @Override
            public void onHandAction(ServerPlayer player, InteractionHand hand)
            {
                handler.call( () -> Arrays.asList(
                            new EntityValue(player),
                            StringValue.of(hand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand")
                        )
                        , player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_TAKES_DAMAGE = new Event("player_takes_damage", 4, false)
        {
            @Override
            public boolean onDamage(Entity target, float amount, DamageSource source)
            {
                return handler.call( () ->
                        Arrays.asList(
                                 new EntityValue(target),
                                 new NumericValue(amount),
                                 StringValue.of(source.getMsgId()),
                                 source.getEntity()==null?Value.NULL:new EntityValue(source.getEntity())
                        ), target::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_DEALS_DAMAGE = new Event("player_deals_damage", 3, false)
        {
            @Override
            public boolean onDamage(Entity target, float amount, DamageSource source)
            {
                return handler.call( () ->
                        Arrays.asList(new EntityValue(source.getEntity()), new NumericValue(amount), new EntityValue(target)),
                        () -> source.getEntity().createCommandSourceStack()
                );
            }
        };
        public static final Event PLAYER_COLLIDES_WITH_ENTITY = new Event("player_collides_with_entity", 2, false)
        {
            @Override
            public boolean onEntityHandAction(ServerPlayer player, Entity entity, InteractionHand enumhand) {
                handler.call( () -> Arrays.asList(new EntityValue(player), new EntityValue(entity)), player::createCommandSourceStack);
                return false;
            }
        };

        public static final Event PLAYER_DIES = new Event("player_dies", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_RESPAWNS = new Event("player_respawns", 1, false)
        {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_CHANGES_DIMENSION = new Event("player_changes_dimension", 5, false)
        {
            @Override
            public void onDimensionChange(ServerPlayer player, Vec3 from, Vec3 to, ResourceKey<Level> fromDim, ResourceKey<Level> dimTo)
            {
                // eligibility already checked in mixin
                Value fromValue = ListValue.fromTriple(from.x, from.y, from.z);
                Value toValue = (to == null)?Value.NULL:ListValue.fromTriple(to.x, to.y, to.z);
                Value fromDimStr = new StringValue(NBTSerializableValue.nameFromRegistryId(fromDim.location()));
                Value toDimStr = new StringValue(NBTSerializableValue.nameFromRegistryId(dimTo.location()));

                handler.call( () -> Arrays.asList(new EntityValue(player), fromValue, fromDimStr, toValue, toDimStr), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_CONNECTS = new Event("player_connects", 1, false) {
            @Override
            public boolean onPlayerEvent(ServerPlayer player)
            {
                handler.call( () -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_DISCONNECTS = new Event("player_disconnects", 2, false) {
            @Override
            public boolean onPlayerMessage(ServerPlayer player, String message)
            {
                handler.call( () -> Arrays.asList(new EntityValue(player), new StringValue(message)), player::createCommandSourceStack);
                return false;
            }
        };

        public static final Event PLAYER_MESSAGE = new Event("player_message", 2, false) {
            @Override
            public boolean onPlayerMessage(ServerPlayer player, String message) {
                return handler.call( () -> Arrays.asList(new EntityValue(player), new StringValue(message)), player::createCommandSourceStack);
            }
        };

        public static final Event PLAYER_COMMAND = new Event("player_command", 2, false) {
            @Override
            public boolean onPlayerMessage(ServerPlayer player, String message) {
                return handler.call( () -> Arrays.asList(new EntityValue(player), new StringValue(message)), player::createCommandSourceStack);
            }
        };

        public static final Event STATISTICS = new Event("statistic", 4, false)
        {
            private <T> ResourceLocation getStatId(Stat<T> stat)
            {
                return stat.getType().getRegistry().getKey(stat.getValue());
            }
            private final Set<ResourceLocation> skippedStats = Set.of(
                Stats.TIME_SINCE_DEATH,
                Stats.TIME_SINCE_REST,
                //Stats.PLAY_ONE_MINUTE,
                Stats.PLAY_TIME,
                Stats.TOTAL_WORLD_TIME
            );
            @Override
            public void onPlayerStatistic(ServerPlayer player, Stat<?> stat, int amount)
            {
                ResourceLocation id = getStatId(stat);
                if (skippedStats.contains(id)) return;
                handler.call( () -> Arrays.asList(
                        new EntityValue(player),
                        StringValue.of(NBTSerializableValue.nameFromRegistryId(BuiltInRegistries.STAT_TYPE.getKey(stat.getType()))),
                        StringValue.of(NBTSerializableValue.nameFromRegistryId(id)),
                        new NumericValue(amount)
                ), player::createCommandSourceStack);
            }
        };
        public static final Event LIGHTNING = new Event("lightning", 2, true)
        {
            @Override
            public void onWorldEventFlag(ServerLevel world, BlockPos pos, int flag)
            {
                handler.call(
                        () -> Arrays.asList(
                                new BlockValue(null, world, pos),
                                flag>0?Value.TRUE:Value.FALSE
                        ), () -> CarpetServer.minecraft_server.createCommandSourceStack().withLevel(world)
                );
            }
        };
        public static final Event CARPET_RULE_CHANGES = new Event("carpet_rule_changes", 2, true)
        {
            @Override
            public void onCarpetRuleChanges(CarpetRule<?> rule, CommandSourceStack source)
            {
                String identifier = rule.settingsManager().identifier();
                final String namespace;
                if (!identifier.equals("carpet")) 
                {
                    namespace = identifier+":";
                } else { namespace = "";}
                handler.call(
                        () -> Arrays.asList(
                                new StringValue(namespace+rule.name()),
                                new StringValue(RuleHelper.toRuleString(rule.value()))
                        ), () -> source
                );
            }
        };
        //copy of Explosion.getCausingEntity() #TRACK#
        private static LivingEntity getExplosionCausingEntity(Entity entity)
        {
            if (entity == null)  return null;
            else if (entity instanceof PrimedTnt) return ((PrimedTnt)entity).getOwner();
            else if (entity instanceof LivingEntity) return (LivingEntity)entity;
            else if (entity instanceof Projectile) {
                Entity owner = ((Projectile)entity).getOwner();
                if (owner instanceof LivingEntity) return (LivingEntity)owner;
            }
            return null;
        }

        public static final Event EXPLOSION_OUTCOME = new Event("explosion_outcome", 8, true)
        {
            @Override
            public void onExplosion(ServerLevel world, Entity e,  Supplier<LivingEntity> attacker, double x, double y, double z, float power, boolean createFire, List<BlockPos> affectedBlocks, List<Entity> affectedEntities, Explosion.BlockInteraction type)
            {
                handler.call(
                        () -> Arrays.asList(
                                ListValue.fromTriple(x, y, z),
                                NumericValue.of(power),
                                EntityValue.of(e),
                                EntityValue.of(attacker!= null?attacker.get():Event.getExplosionCausingEntity(e)),
                                StringValue.of(type.name().toLowerCase(Locale.ROOT)),
                                BooleanValue.of(createFire),
                                new ListValue(affectedBlocks.stream().filter(b -> !world.isEmptyBlock(b)). map( // da heck they send air blocks
                                        b -> new BlockValue(world.getBlockState(b),world,b)
                                ).collect(Collectors.toList())),
                                new ListValue(affectedEntities.stream().map(EntityValue::of).collect(Collectors.toList()))
                        ), () -> CarpetServer.minecraft_server.createCommandSourceStack().withLevel(world)
                );
            }
        };


        public static final Event EXPLOSION = new Event("explosion", 6, true)
        {
            @Override
            public void onExplosion(ServerLevel world, Entity e, Supplier<LivingEntity> attacker, double x, double y, double z, float power, boolean createFire, List<BlockPos> affectedBlocks, List<Entity> affectedEntities, Explosion.BlockInteraction type)
            {
                handler.call(
                        () -> Arrays.asList(
                                ListValue.fromTriple(x, y, z),
                                NumericValue.of(power),
                                EntityValue.of(e),
                                EntityValue.of(attacker!= null?attacker.get():Event.getExplosionCausingEntity(e)),
                                StringValue.of(type.name().toLowerCase(Locale.ROOT)),
                                BooleanValue.of(createFire)
                        ), () -> CarpetServer.minecraft_server.createCommandSourceStack().withLevel(world)
                );
            }
        };

        @Deprecated
        public static String getEntityLoadEventName(EntityType<? extends Entity> et)
        {
            return "entity_loaded_" + ValueConversions.of(BuiltInRegistries.ENTITY_TYPE.getKey(et)).getString();
        }

        @Deprecated
        public static final Map<EntityType<? extends Entity>, Event> ENTITY_LOAD = BuiltInRegistries.ENTITY_TYPE
                .stream()
                .map(et -> Map.entry(et, new Event(getEntityLoadEventName(et), 1, true, false)
                {
                    @Override
                    public void onEntityAction(Entity entity, boolean created)
                    {
                        handler.call(
                                () -> Collections.singletonList(new EntityValue(entity)),
                                () -> CarpetServer.minecraft_server.createCommandSourceStack().withLevel((ServerLevel) entity.level).withPermission(CarpetSettings.runPermissionLevel)
                        );
                    }
                })).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        public static String getEntityHandlerEventName(EntityType<? extends Entity> et)
        {
            return "entity_handler_" + ValueConversions.of(BuiltInRegistries.ENTITY_TYPE.getKey(et)).getString();
        }

        public static final Map<EntityType<? extends Entity>, Event> ENTITY_HANDLER = BuiltInRegistries.ENTITY_TYPE
                .stream()
                .map(et -> Map.entry(et, new Event(getEntityHandlerEventName(et), 2, true, false) {
                    @Override
                    public void onEntityAction(Entity entity, boolean created) {
                        handler.call(
                                () -> Arrays.asList(new EntityValue(entity), BooleanValue.of(created)),
                                () -> CarpetServer.minecraft_server.createCommandSourceStack().withLevel((ServerLevel) entity.level).withPermission(CarpetSettings.runPermissionLevel)
                        );
                    }
                }))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        // on projectile thrown (arrow from bows, crossbows, tridents, snoballs, e-pearls

        public final String name;

        public final CallbackList handler;
        public final boolean isPublic; // public events can be targetted with __on_<event> defs
        public Event(String name, int reqArgs, boolean isGlobalOnly)
        {
            this(name, reqArgs, isGlobalOnly, true);
        }
        public Event(String name, int reqArgs, boolean isGlobalOnly, boolean isPublic)
        {
            this.name = name;
            this.handler = new CallbackList(reqArgs, true, isGlobalOnly);
            this.isPublic = isPublic;
            byName.put(name, this);
        }

        public static List<Event> getAllEvents(CarpetScriptServer server, Predicate<Event> predicate)
        {
            List<CarpetEventServer.Event> eventList = new ArrayList<>(CarpetEventServer.Event.byName.values());
            eventList.addAll(server.events.customEvents.values());
            if (predicate == null) return eventList;
            return eventList.stream().filter(predicate).collect(Collectors.toList());
        }

        public static Event getEvent(String name, CarpetScriptServer server)
        {
            if (byName.containsKey(name)) return byName.get(name);
            return server.events.customEvents.get(name);
        }

        public static Event getOrCreateCustom(String name, CarpetScriptServer server)
        {
            Event event = getEvent(name, server);
            if (event != null) return event;
            return new Event(name, server);
        }

        public static void removeAllHostEvents(CarpetScriptHost host)
        {
            byName.values().forEach((e) -> e.handler.removeAllCalls(host));
            host.scriptServer().events.customEvents.values().forEach((e) -> e.handler.removeAllCalls(host));
        }

        public static void transferAllHostEventsToChild(CarpetScriptHost host)
        {
            byName.values().forEach((e) -> e.handler.createChildEvents(host));
            host.scriptServer().events.customEvents.values().forEach((e) -> e.handler.createChildEvents(host));
        }

        public static void clearAllBuiltinEvents()
        {
            byName.values().forEach(e -> e.handler.clearEverything());
        }

        // custom event constructor
        private Event(String name, CarpetScriptServer server)
        {
            this.name = name;
            this.handler = new CallbackList(1, false, false);
            this.isPublic = true;
            server.events.customEvents.put(name, this);
        }

        //handle_event('event', function...)
        //signal_event('event', player or null, args.... ) -> number of apps notified

        public boolean isNeeded()
        {
            return handler.callList.size() > 0;
        }
        public boolean deprecated() {return false;}
        //stubs for calls just to ease calls in vanilla code so they don't need to deal with scarpet value types
        public void onTick() { }
        public void onChunkEvent(ServerLevel world, ChunkPos chPos, boolean generated) { }
        public boolean onPlayerEvent(ServerPlayer player) {return false;}
        public boolean onPlayerMessage(ServerPlayer player, String message) {return false;}
        public void onPlayerStatistic(ServerPlayer player, Stat<?> stat, int amount) { }
        public void onMountControls(ServerPlayer player, float strafeSpeed, float forwardSpeed, boolean jumping, boolean sneaking) { }
        public boolean onItemAction(ServerPlayer player, InteractionHand enumhand, ItemStack itemstack) {return false;}
        public boolean onBlockAction(ServerPlayer player, BlockPos blockpos, Direction facing) {return false;}
        public boolean onBlockHit(ServerPlayer player, InteractionHand enumhand, BlockHitResult hitRes) {return false;}
        public boolean onBlockBroken(ServerPlayer player, BlockPos pos, BlockState previousBS) {return false;}
        public boolean onBlockPlaced(ServerPlayer player, BlockPos pos, InteractionHand enumhand, ItemStack itemstack) {return false;}
        public boolean onEntityHandAction(ServerPlayer player, Entity entity, InteractionHand enumhand) {return false;}
        public void onHandAction(ServerPlayer player, InteractionHand enumhand) { }
        public void onEntityAction(Entity entity, boolean created) { }
        public void onDimensionChange(ServerPlayer player, Vec3 from, Vec3 to, ResourceKey<Level> fromDim, ResourceKey<Level> dimTo) {}
        public boolean onDamage(Entity target, float amount, DamageSource source) {return false;}
        public boolean onRecipeSelected(ServerPlayer player, ResourceLocation recipe, boolean fullStack) {return false;}
        public void onSlotSwitch(ServerPlayer player, int from, int to) {}
        public void onTrade(ServerPlayer player, Merchant merchant, MerchantOffer tradeOffer) {}

        public void onExplosion(ServerLevel world, Entity e,  Supplier<LivingEntity> attacker, double x, double y, double z, float power, boolean createFire, List<BlockPos> affectedBlocks, List<Entity> affectedEntities, Explosion.BlockInteraction type) { }
        public void onWorldEvent(ServerLevel world, BlockPos pos) { }
        public void onWorldEventFlag(ServerLevel world, BlockPos pos, int flag) { }
        public void onCarpetRuleChanges(CarpetRule<?> rule, CommandSourceStack source) { }
        public void onCustomPlayerEvent(ServerPlayer player, Object ... args)
        {
            if (handler.reqArgs != (args.length+1))
                throw new InternalExpressionException("Expected "+handler.reqArgs+" arguments for "+name+", got "+(args.length+1));
            handler.call(
                    () -> {
                        List<Value> valArgs = new ArrayList<>();
                        valArgs.add(EntityValue.of(player));
                        for (Object o: args)
                        {
                            valArgs.add(ValueConversions.guess(player.getLevel(), o));
                        }
                        return valArgs;
                    }, player::createCommandSourceStack
            );
        }
        public void onCustomWorldEvent(ServerLevel world, Object ... args)
        {
            if (handler.reqArgs != args.length)
                throw new InternalExpressionException("Expected "+handler.reqArgs+" arguments for "+name+", got "+args.length);
            handler.call(
                    () -> {
                        List<Value> valArgs = new ArrayList<>();
                        for (Object o: args)
                        {
                            valArgs.add(ValueConversions.guess(world, o));
                        }
                        return valArgs;
                    }, () -> CarpetServer.minecraft_server.createCommandSourceStack().withLevel(world)
            );
        }
    }


    public CarpetEventServer(CarpetScriptServer scriptServer)
    {
        this.scriptServer = scriptServer;
        Event.clearAllBuiltinEvents();
    }

    public void tick()
    {
        if (!TickSpeed.process_entities)
            return;
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

    public void runScheduledCall(BlockPos origin, CommandSourceStack source, String hostname, CarpetScriptHost host, FunctionValue udf, List<Value> argv)
    {
        if (hostname != null && !scriptServer.modules.containsKey(hostname)) // well - scheduled call app got unloaded
            return;
        try
        {
            host.callUDF(origin, source, udf, argv);
        }
        catch (NullPointerException | InvalidCallbackException | IntegrityException ignored) { }
    }

    public CallbackResult runEventCall(CommandSourceStack sender, String hostname, String optionalTarget, FunctionValue udf, List<Value> argv)
    {
        CarpetScriptHost appHost = scriptServer.getAppHostByName(hostname);
        // no such app
        if (appHost == null) return CallbackResult.FAIL;
        // dummy call for player apps that reside on the global copy - do not run them, but report as passes.
        if (appHost.isPerUser() && optionalTarget==null) return CallbackResult.PASS;
        ServerPlayer target = null;
        if (optionalTarget != null)
        {
            target = sender.getServer().getPlayerList().getPlayerByName(optionalTarget);
            if (target == null) return CallbackResult.FAIL;
        }
        CommandSourceStack source = sender.withPermission(CarpetSettings.runPermissionLevel);
        CarpetScriptHost executingHost = appHost.retrieveForExecution(sender, target);
        if (executingHost == null) return CallbackResult.FAIL;
        try
        {
            Value returnValue = executingHost.callUDF(source.withPermission(CarpetSettings.runPermissionLevel), udf, argv);
            return returnValue instanceof StringValue && returnValue.getString().equals("cancel") ? CallbackResult.CANCEL : CallbackResult.SUCCESS;
        }
        catch (NullPointerException | InvalidCallbackException | IntegrityException error)
        {
            CarpetScriptServer.LOG.error("Got exception when running event call ", error);
            return CallbackResult.FAIL;
        }
    }

    public boolean addEventFromCommand(CommandSourceStack source, String event, String host, String funName)
    {
        Event ev = Event.getEvent(event, CarpetServer.scriptServer);
        if (ev == null)
        {
            return false;
        }
        boolean added = ev.handler.addFromExternal(source, host, funName, h -> onEventAddedToHost(ev, h));
        if (added) Messenger.m(source, "gi Added " + funName + " to " + event);
        return added;
    }

    public void addBuiltInEvent(String event, ScriptHost host, FunctionValue function, List<Value> args)
    {
        // this is globals only
        Event ev = Event.byName.get(event);
        onEventAddedToHost(ev, host);
        boolean success =  ev.handler.addEventCallInternal(host, function, args==null?NOARGS:args);
        if (!success) throw new InternalExpressionException("Global event "+event+" requires "+ev.handler.reqArgs+", not "+(function.getNumParams()-((args==null)?0:args.size())));
    }

    public boolean handleCustomEvent(String event, CarpetScriptHost host, FunctionValue function, List<Value> args)
    {
        Event ev = Event.getOrCreateCustom(event, scriptServer);
        onEventAddedToHost(ev, host);
        return ev.handler.addEventCallInternal(host, function, args==null?NOARGS:args);
    }

    public int signalEvent(String event, CarpetContext cc, ServerPlayer optionalTarget, List<Value> callArgs)
    {
        Event ev = Event.getEvent(event, ((CarpetScriptHost)cc.host).scriptServer());
        if (ev == null) return -1;
        return ev.handler.signal(cc.s, optionalTarget, callArgs);
    }

    private void onEventAddedToHost(Event event, ScriptHost host)
    {
        if (event.deprecated()) host.issueDeprecation(event.name+" event");
        event.handler.sortByPriority(this.scriptServer);
        //return !(event.globalOnly && (host.perUser || host.parent != null));
    }

    public boolean removeEventFromCommand(CommandSourceStack source, String event, String funName)
    {
        Event ev = Event.getEvent(event, CarpetServer.scriptServer);
        if (ev == null)
        {
            Messenger.m(source, "r Unknown event: " + event);
            return false;
        }
        Callback.Signature call = Callback.fromString(funName);
        ev.handler.removeEventCall(call.host, call.target, call.function);
        // could verified if actually removed
        Messenger.m(source, "gi Removed event: " + funName + " from "+event);
        return true;
    }
    public boolean removeBuiltInEvent(String event, CarpetScriptHost host)
    {
        Event ev = Event.getEvent(event, host.scriptServer());
        if (ev == null) return false;
        ev.handler.removeAllCalls(host);
        return true;
    }

    public void removeBuiltInEvent(String event, CarpetScriptHost host, String funName)
    {
        Event ev = Event.getEvent(event, host.scriptServer());
        if (ev != null) ev.handler.removeEventCall(host.getName(), host.user, funName);
    }

    public void removeAllHostEvents(CarpetScriptHost host)
    {
        // remove event handlers
        Event.removeAllHostEvents(host);
        if (host.isPerUser())
            for (ScriptHost child: host.userHosts.values()) Event.removeAllHostEvents((CarpetScriptHost) child);
        // remove scheduled calls
        scheduledCalls.removeIf(sc -> sc.host != null && sc.host.equals(host.getName()));
    }
}
