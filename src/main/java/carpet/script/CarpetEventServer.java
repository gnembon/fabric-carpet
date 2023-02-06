package carpet.script;

import carpet.script.exception.IntegrityException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.external.Carpet;
import carpet.script.external.Vanilla;
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
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
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
        public final CarpetScriptServer scriptServer;

        public Callback(final String host, final String target, final FunctionValue function, final List<Value> parametrizedArgs, final CarpetScriptServer scriptServer)
        {
            this.host = host;
            this.function = function;
            this.optionalTarget = target;
            this.parametrizedArgs = parametrizedArgs == null ? NOARGS : parametrizedArgs;
            this.scriptServer = scriptServer;
        }

        /**
         * Used also in entity events
         *
         * @param sender      - entity command source
         * @param runtimeArgs = options
         */
        public CallbackResult execute(final CommandSourceStack sender, List<Value> runtimeArgs)
        {
            if (!this.parametrizedArgs.isEmpty())
            {
                runtimeArgs = new ArrayList<>(runtimeArgs);
                runtimeArgs.addAll(this.parametrizedArgs);
            }
            if (scriptServer.stopAll)
            {
                return CallbackResult.FAIL; // already stopped
            }
            return scriptServer.events.runEventCall(
                    sender.withPermission(Vanilla.MinecraftServer_getRunPermissionLevel(sender.getServer())),
                    host, optionalTarget, function, runtimeArgs);
        }

        /**
         * Used also in entity events
         *
         * @param sender            - sender of the signal
         * @param optionalRecipient - optional target player argument
         * @param runtimeArgs       = options
         */
        public CallbackResult signal(final CommandSourceStack sender, final ServerPlayer optionalRecipient, final List<Value> runtimeArgs)
        {
            // recipent of the call doesn't match the handlingHost
            return optionalRecipient != null && !optionalRecipient.getScoreboardName().equals(optionalTarget) ? CallbackResult.FAIL : execute(sender, runtimeArgs);
        }


        @Override
        public String toString()
        {
            return function.getString() + ((host == null) ? "" : "(from " + host + (optionalTarget == null ? "" : "/" + optionalTarget) + ")");
        }

        public record Signature(String function, String host, String target)
        {
        }

        public static Signature fromString(final String str)
        {
            final Pattern find = Pattern.compile("(\\w+)(?:\\(from (\\w+)(?:/(\\w+))?\\))?");
            final Matcher matcher = find.matcher(str);
            if (matcher.matches())
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

        public ScheduledCall(final CarpetContext context, final FunctionValue function, final List<Value> args, final long dueTime)
        {
            // ignoring target as we will be always calling self
            super(context.host.getName(), null, function, args, (CarpetScriptServer) context.scriptServer());
            this.ctx = context.duplicate();
            this.dueTime = dueTime;
        }

        /**
         * used in scheduled calls
         */
        public void execute()
        {
            scriptServer.events.runScheduledCall(ctx.origin(), ctx.source(), host, (CarpetScriptHost) ctx.host, function, parametrizedArgs);
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

        public CallbackList(final int reqArgs, final boolean isSystem, final boolean isGlobalOnly)
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

        private void removeCallsIf(final Predicate<Callback> when)
        {
            if (!inCall && !inSignal)
            {
                callList.removeIf(when);
                return;
            }
            // we are ok with list growing in the meantime and parallel access, we are only scanning.
            for (int i = 0; i < callList.size(); i++)
            {
                final Callback call = callList.get(i);
                if (when.test(call))
                {
                    removedCalls.add(call);
                }
            }
        }

        /**
         * Handles only built-in events from the events system
         *
         * @param argumentSupplier
         * @param cmdSourceSupplier
         * @return Whether this event call has been cancelled
         */
        public boolean call(final Supplier<List<Value>> argumentSupplier, final Supplier<CommandSourceStack> cmdSourceSupplier)
        {
            if (callList.isEmpty())
            {
                return false;
            }
            final CommandSourceStack source;
            try
            {
                source = cmdSourceSupplier.get();
            }
            catch (final NullPointerException noReference) // todo figure out what happens when closing.
            {
                return false;
            }
            final CarpetScriptServer scriptServer = Vanilla.MinecraftServer_getScriptServer(source.getServer());
            if (scriptServer.stopAll)
            {
                return false;
            }
            final Boolean isCancelled = scriptServer.events.handleEvents.runIfEnabled(() -> {
                final Runnable profilerToken = Carpet.startProfilerSection("Scarpet events");
                final List<Value> argv = argumentSupplier.get(); // empty for onTickDone
                final String nameCheck = perPlayerDistribution ? source.getTextName() : null;
                assert argv.size() == reqArgs;
                boolean cancelled = false;
                try
                {
                    // we are ok with list growing in the meantime
                    // which might happen during inCall or inSignal
                    inCall = true;
                    for (int i = 0; i < callList.size(); i++)
                    {
                        final Callback call = callList.get(i);
                        // supressing calls where target player hosts simply don't match
                        // handling global hosts with player targets is left to when the host is resolved (few calls deeper).
                        if (nameCheck != null && call.optionalTarget != null && !nameCheck.equals(call.optionalTarget))
                        {
                            continue;
                        }
                        final CallbackResult result = call.execute(source, argv);
                        if (result == CallbackResult.CANCEL)
                        {
                            cancelled = true;
                            break;
                        }
                        if (result == CallbackResult.FAIL)
                        {
                            removedCalls.add(call);
                        }
                    }
                }
                finally
                {
                    inCall = false;
                }
                for (final Callback call : removedCalls)
                {
                    callList.remove(call);
                }
                removedCalls.clear();
                profilerToken.run();
                return cancelled;
            });
            return isCancelled != null && isCancelled;
        }

        public int signal(final CommandSourceStack sender, final ServerPlayer optinoalReceipient, final List<Value> callArg)
        {
            if (callList.isEmpty())
            {
                return 0;
            }
            int successes = 0;
            try
            {
                inSignal = true;
                for (int i = 0; i < callList.size(); i++)
                {
                    // skipping tracking of fails, its explicit call
                    if (callList.get(i).signal(sender, optinoalReceipient, callArg) == CallbackResult.SUCCESS)
                    {
                        successes++;
                    }
                }
            }
            finally
            {
                inSignal = false;
            }
            return successes;
        }

        public boolean addFromExternal(final CommandSourceStack source, final String hostName, final String funName, final Consumer<ScriptHost> hostOnEventHandler, final CarpetScriptServer scriptServer)
        {
            final ScriptHost host = scriptServer.getAppHostByName(hostName);
            if (host == null)
            {
                // impossible call to add
                Carpet.Messenger_message(source, "r Unknown app " + hostName);
                return false;
            }
            hostOnEventHandler.accept(host);
            final FunctionValue udf = host.getFunction(funName);
            if (udf == null || udf.getArguments().size() != reqArgs)
            {
                // call won't match arguments
                Carpet.Messenger_message(source, "r Callback doesn't expect required number of arguments: " + reqArgs);
                return false;
            }
            String target = null;
            if (host.isPerUser())
            {
                try
                {
                    target = source.getPlayerOrException().getScoreboardName();
                }
                catch (final CommandSyntaxException e)
                {
                    Carpet.Messenger_message(source, "r Cannot add event to a player scoped app from a command without a player context");
                    return false;
                }
            }
            //all clear
            //remove duplicates

            removeEventCall(hostName, target, udf.getString());
            callList.add(new Callback(hostName, target, udf, null, scriptServer));
            return true;
        }

        public boolean addEventCallInternal(final ScriptHost host, final FunctionValue function, final List<Value> args)
        {
            if (function == null || (function.getArguments().size() - args.size()) != reqArgs)
            {
                return false;
            }
            //removing duplicates
            removeEventCall(host.getName(), host.user, function.getString());
            callList.add(new Callback(host.getName(), host.user, function, args, (CarpetScriptServer) host.scriptServer()));
            return true;
        }

        public void removeEventCall(final String hostName, final String target, final String funName)
        {
            removeCallsIf((c) -> c.function.getString().equals(funName)
                    && (Objects.equals(c.host, hostName))
                    && (Objects.equals(c.optionalTarget, target))
            );
        }

        public void removeAllCalls(final CarpetScriptHost host)
        {
            removeCallsIf((c) -> (Objects.equals(c.host, host.getName()))
                    && (Objects.equals(c.optionalTarget, host.user)));
        }

        public void createChildEvents(final CarpetScriptHost host)
        {
            final List<Callback> copyCalls = new ArrayList<>();
            callList.forEach((c) ->
            {
                if ((Objects.equals(c.host, host.getName())) // TODO fix me
                        && c.optionalTarget == null)
                {
                    copyCalls.add(new Callback(c.host, host.user, c.function, c.parametrizedArgs, host.scriptServer()));
                }
            });
            callList.addAll(copyCalls);
        }

        public void clearEverything()
        {
            // when some moron puts /reload in an event call.
            if (inSignal || inCall)
            {
                callList = new ArrayList<>();
            }
            callList.clear();
        }

        public void sortByPriority(final CarpetScriptServer scriptServer)
        {
            callList.sort(Comparator.comparingDouble(c -> -scriptServer.getAppHostByName(c.host).eventPriority));
        }
    }

    public static class Event
    {
        public static final Map<String, Event> byName = new HashMap<>();

        public static List<Event> publicEvents(final CarpetScriptServer server)
        {
            final List<Event> events = byName.values().stream().filter(e -> e.isPublic).collect(Collectors.toList());
            if (server != null)
            {
                events.addAll(server.events.customEvents.values());
            }
            return events;
        }

        public static final Event START = new Event("server_starts", 0, true)
        {
            @Override
            public void onTick(final MinecraftServer server)
            {
                handler.call(Collections::emptyList, () ->
                        server.createCommandSourceStack().withLevel(server.getLevel(Level.OVERWORLD))
                );
            }
        };

        public static final Event SHUTDOWN = new Event("server_shuts_down", 0, true)
        {
            @Override
            public void onTick(final MinecraftServer server)
            {
                handler.call(Collections::emptyList, () ->
                        server.createCommandSourceStack().withLevel(server.getLevel(Level.OVERWORLD))
                );
            }
        };

        public static final Event TICK = new Event("tick", 0, true)
        {
            @Override
            public void onTick(final MinecraftServer server)
            {
                handler.call(Collections::emptyList, () ->
                        server.createCommandSourceStack().withLevel(server.getLevel(Level.OVERWORLD))
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
            public void onTick(final MinecraftServer server)
            {
                handler.call(Collections::emptyList, () ->
                        server.createCommandSourceStack().
                                withLevel(server.getLevel(Level.NETHER))
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
            public void onTick(final MinecraftServer server)
            {
                handler.call(Collections::emptyList, () ->
                        server.createCommandSourceStack().
                                withLevel(server.getLevel(Level.END))
                );
            }
        };
        public static final Event CHUNK_GENERATED = new Event("chunk_generated", 2, true)
        {
            @Override
            public void onChunkEvent(final ServerLevel world, final ChunkPos chPos, final boolean generated)
            {
                handler.call(
                        () -> Arrays.asList(new NumericValue(chPos.x << 4), new NumericValue(chPos.z << 4)),
                        () -> world.getServer().createCommandSourceStack().withLevel(world)
                );
            }
        };
        public static final Event CHUNK_LOADED = new Event("chunk_loaded", 2, true)
        {
            @Override
            public void onChunkEvent(final ServerLevel world, final ChunkPos chPos, final boolean generated)
            {
                handler.call(
                        () -> Arrays.asList(new NumericValue(chPos.x << 4), new NumericValue(chPos.z << 4)),
                        () -> world.getServer().createCommandSourceStack().withLevel(world)
                );
            }
        };

        public static final Event CHUNK_UNLOADED = new Event("chunk_unloaded", 2, true)
        {
            @Override
            public void onChunkEvent(final ServerLevel world, final ChunkPos chPos, final boolean generated)
            {
                handler.call(
                        () -> Arrays.asList(new NumericValue(chPos.x << 4), new NumericValue(chPos.z << 4)),
                        () -> world.getServer().createCommandSourceStack().withLevel(world)
                );
            }
        };

        public static final Event PLAYER_JUMPS = new Event("player_jumps", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_DEPLOYS_ELYTRA = new Event("player_deploys_elytra", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_WAKES_UP = new Event("player_wakes_up", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_ESCAPES_SLEEP = new Event("player_escapes_sleep", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_RIDES = new Event("player_rides", 5, false)
        {
            @Override
            public void onMountControls(final ServerPlayer player, final float strafeSpeed, final float forwardSpeed, final boolean jumping, final boolean sneaking)
            {
                handler.call(() -> Arrays.asList(new EntityValue(player),
                        new NumericValue(forwardSpeed), new NumericValue(strafeSpeed), BooleanValue.of(jumping), BooleanValue.of(sneaking)
                ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_USES_ITEM = new Event("player_uses_item", 3, false)
        {
            @Override
            public boolean onItemAction(final ServerPlayer player, final InteractionHand enumhand, final ItemStack itemstack)
            {
                return handler.call(() ->
                        Arrays.asList(
                                new EntityValue(player),
                                ValueConversions.of(itemstack, player.getLevel().registryAccess()),
                                StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand")
                        ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_CLICKS_BLOCK = new Event("player_clicks_block", 3, false)
        {
            @Override
            public boolean onBlockAction(final ServerPlayer player, final BlockPos blockpos, final Direction facing)
            {
                return handler.call(() ->
                        Arrays.asList(
                                new EntityValue(player),
                                new BlockValue(null, player.getLevel(), blockpos),
                                StringValue.of(facing.getName())
                        ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_RIGHT_CLICKS_BLOCK = new Event("player_right_clicks_block", 6, false)
        {
            @Override
            public boolean onBlockHit(final ServerPlayer player, final InteractionHand enumhand, final BlockHitResult hitRes)
            {
                return handler.call(() ->
                {
                    final ItemStack itemstack = player.getItemInHand(enumhand);
                    final BlockPos blockpos = hitRes.getBlockPos();
                    final Direction enumfacing = hitRes.getDirection();
                    final Vec3 vec3d = hitRes.getLocation().subtract(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                    return Arrays.asList(
                            new EntityValue(player),
                            ValueConversions.of(itemstack, player.getLevel().registryAccess()),
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
            public boolean onBlockHit(final ServerPlayer player, final InteractionHand enumhand, final BlockHitResult hitRes)
            {
                handler.call(() ->
                {
                    final BlockPos blockpos = hitRes.getBlockPos();
                    final Direction enumfacing = hitRes.getDirection();
                    final Vec3 vec3d = hitRes.getLocation().subtract(blockpos.getX(), blockpos.getY(), blockpos.getZ());
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
            public boolean onBlockPlaced(final ServerPlayer player, final BlockPos pos, final InteractionHand enumhand, final ItemStack itemstack)
            {
                return handler.call(() -> Arrays.asList(
                        new EntityValue(player),
                        ValueConversions.of(itemstack, player.getLevel().registryAccess()),
                        StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand"),
                        new BlockValue(null, player.getLevel(), pos)
                ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_PLACES_BLOCK = new Event("player_places_block", 4, false)
        {
            @Override
            public boolean onBlockPlaced(final ServerPlayer player, final BlockPos pos, final InteractionHand enumhand, final ItemStack itemstack)
            {
                handler.call(() -> Arrays.asList(
                        new EntityValue(player),
                        ValueConversions.of(itemstack, player.getLevel().registryAccess()),
                        StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand"),
                        new BlockValue(null, player.getLevel(), pos)
                ), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_BREAK_BLOCK = new Event("player_breaks_block", 2, false)
        {
            @Override
            public boolean onBlockBroken(final ServerPlayer player, final BlockPos pos, final BlockState previousBS)
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
            public boolean onEntityHandAction(final ServerPlayer player, final Entity entity, final InteractionHand enumhand)
            {
                return handler.call(() -> Arrays.asList(
                        new EntityValue(player), new EntityValue(entity), StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand")
                ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_TRADES = new Event("player_trades", 5, false)
        {
            @Override
            public void onTrade(final ServerPlayer player, final Merchant merchant, final MerchantOffer tradeOffer)
            {
                final RegistryAccess regs = player.getLevel().registryAccess();
                handler.call(() -> Arrays.asList(
                        new EntityValue(player),
                        merchant instanceof final AbstractVillager villager ? new EntityValue(villager) : Value.NULL,
                        ValueConversions.of(tradeOffer.getBaseCostA(), regs),
                        ValueConversions.of(tradeOffer.getCostB(), regs),
                        ValueConversions.of(tradeOffer.getResult(), regs)
                ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_PICKS_UP_ITEM = new Event("player_picks_up_item", 2, false)
        {
            @Override
            public boolean onItemAction(final ServerPlayer player, final InteractionHand enumhand, final ItemStack itemstack)
            {
                handler.call(() -> Arrays.asList(new EntityValue(player), ValueConversions.of(itemstack, player.getLevel().registryAccess())), player::createCommandSourceStack);
                return false;
            }
        };

        public static final Event PLAYER_ATTACKS_ENTITY = new Event("player_attacks_entity", 2, false)
        {
            @Override
            public boolean onEntityHandAction(final ServerPlayer player, final Entity entity, final InteractionHand enumhand)
            {
                return handler.call(() -> Arrays.asList(new EntityValue(player), new EntityValue(entity)), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_STARTS_SNEAKING = new Event("player_starts_sneaking", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_STOPS_SNEAKING = new Event("player_stops_sneaking", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_STARTS_SPRINTING = new Event("player_starts_sprinting", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_STOPS_SPRINTING = new Event("player_stops_sprinting", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };

        public static final Event PLAYER_RELEASED_ITEM = new Event("player_releases_item", 3, false)
        {
            @Override
            public boolean onItemAction(final ServerPlayer player, final InteractionHand enumhand, final ItemStack itemstack)
            {
                // this.getStackInHand(this.getActiveHand()), this.activeItemStack)
                handler.call(() ->
                        Arrays.asList(
                                new EntityValue(player),
                                ValueConversions.of(itemstack, player.getLevel().registryAccess()),
                                StringValue.of(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand")
                        ), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_FINISHED_USING_ITEM = new Event("player_finishes_using_item", 3, false)
        {
            @Override
            public boolean onItemAction(final ServerPlayer player, final InteractionHand enumhand, final ItemStack itemstack)
            {
                // this.getStackInHand(this.getActiveHand()), this.activeItemStack)
                return handler.call(() ->
                        Arrays.asList(
                                new EntityValue(player),
                                ValueConversions.of(itemstack, player.getLevel().registryAccess()),
                                new StringValue(enumhand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand")
                        ), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_DROPS_ITEM = new Event("player_drops_item", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                return handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_DROPS_STACK = new Event("player_drops_stack", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                return handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_CHOOSES_RECIPE = new Event("player_chooses_recipe", 3, false)
        {
            @Override
            public boolean onRecipeSelected(final ServerPlayer player, final ResourceLocation recipe, final boolean fullStack)
            {
                return handler.call(() ->
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
            public void onSlotSwitch(final ServerPlayer player, final int from, final int to)
            {
                if (from == to)
                {
                    return; // initial slot update
                }
                handler.call(() ->
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
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                return handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_SWINGS_HAND = new Event("player_swings_hand", 2, false)
        {
            @Override
            public void onHandAction(final ServerPlayer player, final InteractionHand hand)
            {
                handler.call(() -> Arrays.asList(
                                new EntityValue(player),
                                StringValue.of(hand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand")
                        )
                        , player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_TAKES_DAMAGE = new Event("player_takes_damage", 4, false)
        {
            @Override
            public boolean onDamage(final Entity target, final float amount, final DamageSource source)
            {
                return handler.call(() ->
                        Arrays.asList(
                                new EntityValue(target),
                                new NumericValue(amount),
                                StringValue.of(source.getMsgId()),
                                source.getEntity() == null ? Value.NULL : new EntityValue(source.getEntity())
                        ), target::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_DEALS_DAMAGE = new Event("player_deals_damage", 3, false)
        {
            @Override
            public boolean onDamage(final Entity target, final float amount, final DamageSource source)
            {
                return handler.call(() ->
                                Arrays.asList(new EntityValue(source.getEntity()), new NumericValue(amount), new EntityValue(target)),
                        () -> source.getEntity().createCommandSourceStack()
                );
            }
        };
        public static final Event PLAYER_COLLIDES_WITH_ENTITY = new Event("player_collides_with_entity", 2, false)
        {
            @Override
            public boolean onEntityHandAction(final ServerPlayer player, final Entity entity, final InteractionHand enumhand)
            {
                handler.call(() -> Arrays.asList(new EntityValue(player), new EntityValue(entity)), player::createCommandSourceStack);
                return false;
            }
        };

        public static final Event PLAYER_DIES = new Event("player_dies", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_RESPAWNS = new Event("player_respawns", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_CHANGES_DIMENSION = new Event("player_changes_dimension", 5, false)
        {
            @Override
            public void onDimensionChange(final ServerPlayer player, final Vec3 from, final Vec3 to, final ResourceKey<Level> fromDim, final ResourceKey<Level> dimTo)
            {
                // eligibility already checked in mixin
                final Value fromValue = ListValue.fromTriple(from.x, from.y, from.z);
                final Value toValue = (to == null) ? Value.NULL : ListValue.fromTriple(to.x, to.y, to.z);
                final Value fromDimStr = new StringValue(NBTSerializableValue.nameFromRegistryId(fromDim.location()));
                final Value toDimStr = new StringValue(NBTSerializableValue.nameFromRegistryId(dimTo.location()));

                handler.call(() -> Arrays.asList(new EntityValue(player), fromValue, fromDimStr, toValue, toDimStr), player::createCommandSourceStack);
            }
        };
        public static final Event PLAYER_CONNECTS = new Event("player_connects", 1, false)
        {
            @Override
            public boolean onPlayerEvent(final ServerPlayer player)
            {
                handler.call(() -> Collections.singletonList(new EntityValue(player)), player::createCommandSourceStack);
                return false;
            }
        };
        public static final Event PLAYER_DISCONNECTS = new Event("player_disconnects", 2, false)
        {
            @Override
            public boolean onPlayerMessage(final ServerPlayer player, final String message)
            {
                handler.call(() -> Arrays.asList(new EntityValue(player), new StringValue(message)), player::createCommandSourceStack);
                return false;
            }
        };

        public static final Event PLAYER_MESSAGE = new Event("player_message", 2, false)
        {
            @Override
            public boolean onPlayerMessage(final ServerPlayer player, final String message)
            {
                return handler.call(() -> Arrays.asList(new EntityValue(player), new StringValue(message)), player::createCommandSourceStack);
            }
        };

        public static final Event PLAYER_COMMAND = new Event("player_command", 2, false)
        {
            @Override
            public boolean onPlayerMessage(final ServerPlayer player, final String message)
            {
                return handler.call(() -> Arrays.asList(new EntityValue(player), new StringValue(message)), player::createCommandSourceStack);
            }
        };

        public static final Event STATISTICS = new Event("statistic", 4, false)
        {
            private <T> ResourceLocation getStatId(final Stat<T> stat)
            {
                return stat.getType().getRegistry().getKey(stat.getValue());
            }

            private final Set<ResourceLocation> skippedStats = Set.of(
                    Stats.TIME_SINCE_DEATH,
                    Stats.TIME_SINCE_REST,
                    Stats.PLAY_TIME,
                    Stats.TOTAL_WORLD_TIME
            );

            @Override
            public void onPlayerStatistic(final ServerPlayer player, final Stat<?> stat, final int amount)
            {
                final ResourceLocation id = getStatId(stat);
                if (skippedStats.contains(id))
                {
                    return;
                }
                final Registry<StatType<?>> registry = player.getLevel().registryAccess().registryOrThrow(Registries.STAT_TYPE);
                handler.call(() -> Arrays.asList(
                        new EntityValue(player),
                        StringValue.of(NBTSerializableValue.nameFromRegistryId(registry.getKey(stat.getType()))),
                        StringValue.of(NBTSerializableValue.nameFromRegistryId(id)),
                        new NumericValue(amount)
                ), player::createCommandSourceStack);
            }
        };
        public static final Event LIGHTNING = new Event("lightning", 2, true)
        {
            @Override
            public void onWorldEventFlag(final ServerLevel world, final BlockPos pos, final int flag)
            {
                handler.call(
                        () -> Arrays.asList(
                                new BlockValue(null, world, pos),
                                flag > 0 ? Value.TRUE : Value.FALSE
                        ), () -> world.getServer().createCommandSourceStack().withLevel(world)
                );
            }
        };

        //copy of Explosion.getCausingEntity() #TRACK#
        private static LivingEntity getExplosionCausingEntity(final Entity entity)
        {
            if (entity == null)
            {
                return null;
            }
            else if (entity instanceof final PrimedTnt tnt)
            {
                return tnt.getOwner();
            }
            else if (entity instanceof final LivingEntity le)
            {
                return le;
            }
            else if (entity instanceof final Projectile p)
            {
                final Entity owner = p.getOwner();
                if (owner instanceof final LivingEntity le)
                {
                    return le;
                }
            }
            return null;
        }

        public static final Event EXPLOSION_OUTCOME = new Event("explosion_outcome", 8, true)
        {
            @Override
            public void onExplosion(final ServerLevel world, final Entity e, final Supplier<LivingEntity> attacker, final double x, final double y, final double z, final float power, final boolean createFire, final List<BlockPos> affectedBlocks, final List<Entity> affectedEntities, final Explosion.BlockInteraction type)
            {
                handler.call(
                        () -> Arrays.asList(
                                ListValue.fromTriple(x, y, z),
                                NumericValue.of(power),
                                EntityValue.of(e),
                                EntityValue.of(attacker != null ? attacker.get() : Event.getExplosionCausingEntity(e)),
                                StringValue.of(type.name().toLowerCase(Locale.ROOT)),
                                BooleanValue.of(createFire),
                                ListValue.wrap(affectedBlocks.stream().filter(b -> !world.isEmptyBlock(b)).map( // da heck they send air blocks
                                        b -> new BlockValue(world.getBlockState(b), world, b)
                                )),
                                ListValue.wrap(affectedEntities.stream().map(EntityValue::of))
                        ), () -> world.getServer().createCommandSourceStack().withLevel(world)
                );
            }
        };


        public static final Event EXPLOSION = new Event("explosion", 6, true)
        {
            @Override
            public void onExplosion(final ServerLevel world, final Entity e, final Supplier<LivingEntity> attacker, final double x, final double y, final double z, final float power, final boolean createFire, final List<BlockPos> affectedBlocks, final List<Entity> affectedEntities, final Explosion.BlockInteraction type)
            {
                handler.call(
                        () -> Arrays.asList(
                                ListValue.fromTriple(x, y, z),
                                NumericValue.of(power),
                                EntityValue.of(e),
                                EntityValue.of(attacker != null ? attacker.get() : Event.getExplosionCausingEntity(e)),
                                StringValue.of(type.name().toLowerCase(Locale.ROOT)),
                                BooleanValue.of(createFire)
                        ), () -> world.getServer().createCommandSourceStack().withLevel(world)
                );
            }
        };

        @Deprecated
        public static String getEntityLoadEventName(final EntityType<? extends Entity> et)
        {
            return "entity_loaded_" + ValueConversions.of(BuiltInRegistries.ENTITY_TYPE.getKey(et)).getString();
        }

        @Deprecated
        public static final Map<EntityType<? extends Entity>, Event> ENTITY_LOAD = BuiltInRegistries.ENTITY_TYPE
                .stream()
                .map(et -> Map.entry(et, new Event(getEntityLoadEventName(et), 1, true, false)
                {
                    @Override
                    public void onEntityAction(final Entity entity, final boolean created)
                    {
                        handler.call(
                                () -> Collections.singletonList(new EntityValue(entity)),
                                () -> entity.getServer().createCommandSourceStack().withLevel((ServerLevel) entity.level).withPermission(Vanilla.MinecraftServer_getRunPermissionLevel(entity.getServer()))
                        );
                    }
                })).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        public static String getEntityHandlerEventName(final EntityType<? extends Entity> et)
        {
            return "entity_handler_" + ValueConversions.of(BuiltInRegistries.ENTITY_TYPE.getKey(et)).getString();
        }

        public static final Map<EntityType<? extends Entity>, Event> ENTITY_HANDLER = BuiltInRegistries.ENTITY_TYPE
                .stream()
                .map(et -> Map.entry(et, new Event(getEntityHandlerEventName(et), 2, true, false)
                {
                    @Override
                    public void onEntityAction(final Entity entity, final boolean created)
                    {
                        handler.call(
                                () -> Arrays.asList(new EntityValue(entity), BooleanValue.of(created)),
                                () -> entity.getServer().createCommandSourceStack().withLevel((ServerLevel) entity.level).withPermission(Vanilla.MinecraftServer_getRunPermissionLevel(entity.getServer()))
                        );
                    }
                }))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        // on projectile thrown (arrow from bows, crossbows, tridents, snoballs, e-pearls

        public final String name;

        public final CallbackList handler;
        public final boolean isPublic; // public events can be targetted with __on_<event> defs

        public Event(final String name, final int reqArgs, final boolean isGlobalOnly)
        {
            this(name, reqArgs, isGlobalOnly, true);
        }

        public Event(final String name, final int reqArgs, final boolean isGlobalOnly, final boolean isPublic)
        {
            this.name = name;
            this.handler = new CallbackList(reqArgs, true, isGlobalOnly);
            this.isPublic = isPublic;
            byName.put(name, this);
        }

        public static List<Event> getAllEvents(final CarpetScriptServer server, final Predicate<Event> predicate)
        {
            final List<CarpetEventServer.Event> eventList = new ArrayList<>(CarpetEventServer.Event.byName.values());
            eventList.addAll(server.events.customEvents.values());
            if (predicate == null)
            {
                return eventList;
            }
            return eventList.stream().filter(predicate).toList();
        }

        public static Event getEvent(final String name, final CarpetScriptServer server)
        {
            if (byName.containsKey(name))
            {
                return byName.get(name);
            }
            return server.events.customEvents.get(name);
        }

        public static Event getOrCreateCustom(final String name, final CarpetScriptServer server)
        {
            final Event event = getEvent(name, server);
            if (event != null)
            {
                return event;
            }
            return new Event(name, server);
        }

        public static void removeAllHostEvents(final CarpetScriptHost host)
        {
            byName.values().forEach((e) -> e.handler.removeAllCalls(host));
            host.scriptServer().events.customEvents.values().forEach((e) -> e.handler.removeAllCalls(host));
        }

        public static void transferAllHostEventsToChild(final CarpetScriptHost host)
        {
            byName.values().forEach((e) -> e.handler.createChildEvents(host));
            host.scriptServer().events.customEvents.values().forEach((e) -> e.handler.createChildEvents(host));
        }

        public static void clearAllBuiltinEvents()
        {
            byName.values().forEach(e -> e.handler.clearEverything());
        }

        // custom event constructor
        private Event(final String name, final CarpetScriptServer server)
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

        public boolean deprecated()
        {
            return false;
        }

        //stubs for calls just to ease calls in vanilla code so they don't need to deal with scarpet value types
        public void onTick(final MinecraftServer server)
        {
        }

        public void onChunkEvent(final ServerLevel world, final ChunkPos chPos, final boolean generated)
        {
        }

        public boolean onPlayerEvent(final ServerPlayer player)
        {
            return false;
        }

        public boolean onPlayerMessage(final ServerPlayer player, final String message)
        {
            return false;
        }

        public void onPlayerStatistic(final ServerPlayer player, final Stat<?> stat, final int amount)
        {
        }

        public void onMountControls(final ServerPlayer player, final float strafeSpeed, final float forwardSpeed, final boolean jumping, final boolean sneaking)
        {
        }

        public boolean onItemAction(final ServerPlayer player, final InteractionHand enumhand, final ItemStack itemstack)
        {
            return false;
        }

        public boolean onBlockAction(final ServerPlayer player, final BlockPos blockpos, final Direction facing)
        {
            return false;
        }

        public boolean onBlockHit(final ServerPlayer player, final InteractionHand enumhand, final BlockHitResult hitRes)
        {
            return false;
        }

        public boolean onBlockBroken(final ServerPlayer player, final BlockPos pos, final BlockState previousBS)
        {
            return false;
        }

        public boolean onBlockPlaced(final ServerPlayer player, final BlockPos pos, final InteractionHand enumhand, final ItemStack itemstack)
        {
            return false;
        }

        public boolean onEntityHandAction(final ServerPlayer player, final Entity entity, final InteractionHand enumhand)
        {
            return false;
        }

        public void onHandAction(final ServerPlayer player, final InteractionHand enumhand)
        {
        }

        public void onEntityAction(final Entity entity, final boolean created)
        {
        }

        public void onDimensionChange(final ServerPlayer player, final Vec3 from, final Vec3 to, final ResourceKey<Level> fromDim, final ResourceKey<Level> dimTo)
        {
        }

        public boolean onDamage(final Entity target, final float amount, final DamageSource source)
        {
            return false;
        }

        public boolean onRecipeSelected(final ServerPlayer player, final ResourceLocation recipe, final boolean fullStack)
        {
            return false;
        }

        public void onSlotSwitch(final ServerPlayer player, final int from, final int to)
        {
        }

        public void onTrade(final ServerPlayer player, final Merchant merchant, final MerchantOffer tradeOffer)
        {
        }

        public void onExplosion(final ServerLevel world, final Entity e, final Supplier<LivingEntity> attacker, final double x, final double y, final double z, final float power, final boolean createFire, final List<BlockPos> affectedBlocks, final List<Entity> affectedEntities, final Explosion.BlockInteraction type)
        {
        }

        public void onWorldEvent(final ServerLevel world, final BlockPos pos)
        {
        }

        public void onWorldEventFlag(final ServerLevel world, final BlockPos pos, final int flag)
        {
        }

        public void handleAny(final Object... args)
        {
        }

        public void onCustomPlayerEvent(final ServerPlayer player, final Object... args)
        {
            if (handler.reqArgs != (args.length + 1))
            {
                throw new InternalExpressionException("Expected " + handler.reqArgs + " arguments for " + name + ", got " + (args.length + 1));
            }
            handler.call(
                    () -> {
                        final List<Value> valArgs = new ArrayList<>();
                        valArgs.add(EntityValue.of(player));
                        for (final Object o : args)
                        {
                            valArgs.add(ValueConversions.guess(player.getLevel(), o));
                        }
                        return valArgs;
                    }, player::createCommandSourceStack
            );
        }

        public void onCustomWorldEvent(final ServerLevel world, final Object... args)
        {
            if (handler.reqArgs != args.length)
            {
                throw new InternalExpressionException("Expected " + handler.reqArgs + " arguments for " + name + ", got " + args.length);
            }
            handler.call(
                    () -> {
                        final List<Value> valArgs = new ArrayList<>();
                        for (final Object o : args)
                        {
                            valArgs.add(ValueConversions.guess(world, o));
                        }
                        return valArgs;
                    }, () -> world.getServer().createCommandSourceStack().withLevel(world)
            );
        }
    }


    public CarpetEventServer(final CarpetScriptServer scriptServer)
    {
        this.scriptServer = scriptServer;
        Event.clearAllBuiltinEvents();
    }

    public void tick()
    {
        if (Carpet.isTickProcessingPaused())
        {
            return;
        }
        final Iterator<ScheduledCall> eventIterator = scheduledCalls.iterator();
        final List<ScheduledCall> currentCalls = new ArrayList<>();
        while (eventIterator.hasNext())
        {
            final ScheduledCall call = eventIterator.next();
            call.dueTime--;
            if (call.dueTime <= 0)
            {
                currentCalls.add(call);
                eventIterator.remove();
            }
        }
        for (final ScheduledCall call : currentCalls)
        {
            call.execute();
        }

    }

    public void scheduleCall(final CarpetContext context, final FunctionValue function, final List<Value> args, final long due)
    {
        scheduledCalls.add(new ScheduledCall(context, function, args, due));
    }

    public void runScheduledCall(final BlockPos origin, final CommandSourceStack source, final String hostname, final CarpetScriptHost host, final FunctionValue udf, final List<Value> argv)
    {
        if (hostname != null && !scriptServer.modules.containsKey(hostname)) // well - scheduled call app got unloaded
        {
            return;
        }
        try
        {
            host.callUDF(origin, source, udf, argv);
        }
        catch (final NullPointerException | InvalidCallbackException | IntegrityException ignored)
        {
        }
    }

    public CallbackResult runEventCall(final CommandSourceStack sender, final String hostname, final String optionalTarget, final FunctionValue udf, final List<Value> argv)
    {
        final CarpetScriptHost appHost = scriptServer.getAppHostByName(hostname);
        // no such app
        if (appHost == null)
        {
            return CallbackResult.FAIL;
        }
        // dummy call for player apps that reside on the global copy - do not run them, but report as passes.
        if (appHost.isPerUser() && optionalTarget == null)
        {
            return CallbackResult.PASS;
        }
        ServerPlayer target = null;
        if (optionalTarget != null)
        {
            target = sender.getServer().getPlayerList().getPlayerByName(optionalTarget);
            if (target == null)
            {
                return CallbackResult.FAIL;
            }
        }
        final CommandSourceStack source = sender.withPermission(Vanilla.MinecraftServer_getRunPermissionLevel(sender.getServer()));
        final CarpetScriptHost executingHost = appHost.retrieveForExecution(sender, target);
        if (executingHost == null)
        {
            return CallbackResult.FAIL;
        }
        try
        {
            final Value returnValue = executingHost.callUDF(source, udf, argv);
            return returnValue instanceof StringValue && returnValue.getString().equals("cancel") ? CallbackResult.CANCEL : CallbackResult.SUCCESS;
        }
        catch (final NullPointerException | InvalidCallbackException | IntegrityException error)
        {
            CarpetScriptServer.LOG.error("Got exception when running event call ", error);
            return CallbackResult.FAIL;
        }
    }

    public boolean addEventFromCommand(final CommandSourceStack source, final String event, final String host, final String funName)
    {
        final Event ev = Event.getEvent(event, scriptServer);
        if (ev == null)
        {
            return false;
        }
        final boolean added = ev.handler.addFromExternal(source, host, funName, h -> onEventAddedToHost(ev, h), scriptServer);
        if (added)
        {
            Carpet.Messenger_message(source, "gi Added " + funName + " to " + event);
        }
        return added;
    }

    public void addBuiltInEvent(final String event, final ScriptHost host, final FunctionValue function, final List<Value> args)
    {
        // this is globals only
        final Event ev = Event.byName.get(event);
        onEventAddedToHost(ev, host);
        final boolean success = ev.handler.addEventCallInternal(host, function, args == null ? NOARGS : args);
        if (!success)
        {
            throw new InternalExpressionException("Global event " + event + " requires " + ev.handler.reqArgs + ", not " + (function.getNumParams() - ((args == null) ? 0 : args.size())));
        }
    }

    public boolean handleCustomEvent(final String event, final CarpetScriptHost host, final FunctionValue function, final List<Value> args)
    {
        final Event ev = Event.getOrCreateCustom(event, scriptServer);
        onEventAddedToHost(ev, host);
        return ev.handler.addEventCallInternal(host, function, args == null ? NOARGS : args);
    }

    public int signalEvent(final String event, final CarpetContext cc, final ServerPlayer optionalTarget, final List<Value> callArgs)
    {
        final Event ev = Event.getEvent(event, ((CarpetScriptHost) cc.host).scriptServer());
        return ev == null ? -1 : ev.handler.signal(cc.source(), optionalTarget, callArgs);
    }

    private void onEventAddedToHost(final Event event, final ScriptHost host)
    {
        if (event.deprecated())
        {
            host.issueDeprecation(event.name + " event");
        }
        event.handler.sortByPriority(this.scriptServer);
    }

    public boolean removeEventFromCommand(final CommandSourceStack source, final String event, final String funName)
    {
        final Event ev = Event.getEvent(event, scriptServer);
        if (ev == null)
        {
            Carpet.Messenger_message(source, "r Unknown event: " + event);
            return false;
        }
        final Callback.Signature call = Callback.fromString(funName);
        ev.handler.removeEventCall(call.host, call.target, call.function);
        // could verified if actually removed
        Carpet.Messenger_message(source, "gi Removed event: " + funName + " from " + event);
        return true;
    }

    public boolean removeBuiltInEvent(final String event, final CarpetScriptHost host)
    {
        final Event ev = Event.getEvent(event, host.scriptServer());
        if (ev == null)
        {
            return false;
        }
        ev.handler.removeAllCalls(host);
        return true;
    }

    public void removeBuiltInEvent(final String event, final CarpetScriptHost host, final String funName)
    {
        final Event ev = Event.getEvent(event, host.scriptServer());
        if (ev != null)
        {
            ev.handler.removeEventCall(host.getName(), host.user, funName);
        }
    }

    public void removeAllHostEvents(final CarpetScriptHost host)
    {
        // remove event handlers
        Event.removeAllHostEvents(host);
        if (host.isPerUser())
        {
            for (final ScriptHost child : host.userHosts.values())
            {
                Event.removeAllHostEvents((CarpetScriptHost) child);
            }
        }
        // remove scheduled calls
        scheduledCalls.removeIf(sc -> sc.host != null && sc.host.equals(host.getName()));
    }
}