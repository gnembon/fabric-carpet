package carpet.script;

import carpet.CarpetServer;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarpetEventServer
{
    public List<ScheduledCall> scheduledCalls = new LinkedList<>();

    public static class Callback
    {
        public String host;
        public FunctionValue udf;

        public Callback(String host, FunctionValue udf)
        {
            this.host = host;
            this.udf = udf;
        }

        @Override
        public String toString()
        {
            return udf.getString()+((host==null)?"":"(from "+host+")");
        }
    }

    public static class ScheduledCall extends Callback
    {
        public List<LazyValue> args;
        public ServerCommandSource context_source;
        public BlockPos context_origin;
        public long dueTime;

        public ScheduledCall(CarpetContext context, FunctionValue udf, List<LazyValue> args, long dueTime)
        {
            super(context.host.getName(), udf);
            this.args = args;
            this.context_source = context.s;
            this.context_origin = context.origin;
            this.dueTime = dueTime;
        }

        public void execute()
        {
            CarpetServer.scriptServer.runas(context_origin, context_source, host, udf, args);
        }

        public void execute(List<LazyValue> args)
        {
            if (this.args == null)
                CarpetServer.scriptServer.runas(context_origin, context_source, host, udf, args);
            else
            {
                List<LazyValue> combinedArgs = new ArrayList<>();
                combinedArgs.addAll(args);
                combinedArgs.addAll(this.args);
                CarpetServer.scriptServer.runas(context_origin, context_source, host, udf, combinedArgs);
            }
        }
    }

    public static class CallbackList
    {

        public List<Callback> callList;
        public int reqArgs;

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
                callList.removeIf(call -> !CarpetServer.scriptServer.runas(source, call.host, call.udf, argv)); // this actually does the calls
            }
        }
        public boolean addEventCall(String hostName, String funName)
        {
            ScriptHost host = CarpetServer.scriptServer.getHostByName(hostName);
            if (host == null)
            {
                // impossible call to add
                return false;
            }
            FunctionValue udf = host.globalFunctions.get(funName);
            if (udf == null || udf.getArguments().size() != reqArgs)
            {
                // call won't match arguments
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
            callList.removeIf((c)->  c.udf.getString().equals(funName) && ( hostName == null || c.host.equalsIgnoreCase(hostName) ) );
        }

        public void removeAllCalls(String hostName)
        {
            callList.removeIf((c)-> c.host.equals(hostName));
        }
    }

    public enum Event
    {
        TICK("tick", new CallbackList(0))
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
        NETHER_TICK("tick_nether",new CallbackList(0))
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
        ENDER_TICK("tick_ender",new CallbackList(0))
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
        PLAYER_JUMPS("player_jumps", new CallbackList(1))
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_DEPLOYS_ELYTRA("player_deploys_elytra",new CallbackList(1))
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_WAKES_UP("player_wakes_up",new CallbackList(1))
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_RIDES("player_rides",new CallbackList(5))
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
        PLAYER_USES_ITEM("player_uses_item",new CallbackList(3))
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
        PLAYER_CLICKS_BLOCK("player_clicks_block",new CallbackList(3))
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
        PLAYER_RIGHT_CLICKS_BLOCK("player_right_clicks_block",new CallbackList(6))
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
        PLAYER_BREAK_BLOCK("player_breaks_block",new CallbackList(2))
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
        PLAYER_INERACTSW_WITH_ENTITY("player_interacts_with_entity",new CallbackList(3))
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
        PLAYER_ATTACKS_ENTITY("player_attacks_entity",new CallbackList(2))
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
        PLAYER_STARTS_SNEAKING("player_starts_sneaking",new CallbackList(1))
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_STOPS_SNEAKING("player_stops_sneaking",new CallbackList(1))
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_STARTS_SPRINTING("player_starts_sprinting",new CallbackList(1))
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_STOPS_SPRINTING("player_stops_sprinting",new CallbackList(1))
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },

        PLAYER_RELEASED_ITEM("player_releases_item",new CallbackList(3))
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
        PLAYER_FINISHED_USING_ITEM("player_finishes_using_item",new CallbackList(3))
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
        PLAYER_DROPS_ITEM("player_drops_item", new CallbackList(1))
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        },
        PLAYER_DROPS_STACK("player_drops_stack", new CallbackList(1))
        {
            @Override
            public void onPlayerEvent(ServerPlayerEntity player)
            {
                handler.call( () -> Collections.singletonList(((c, t) -> new EntityValue(player))), player::getCommandSource);
            }
        };

        // on projectile thrown (arrow from bows, crossbows, tridents, snoballs, e-pearls

        public String name;
        public static Map<String, Event> byName = new HashMap<String, Event>(){{
            for (Event e: Event.values())
            {
                put(e.name, e);
            }
        }};
        public CallbackList handler;
        Event(String name, CallbackList eventHandler)
        {
            this.name = name;
            this.handler = eventHandler;
        }
        public boolean isNeeded()
        {
            return handler.callList.size() > 0;
        }
        //stubs for calls just to ease calls in vanilla code so they don't need to deal with scarpet value types
        public void onTick() { }
        public void onPlayerEvent(ServerPlayerEntity player) { }
        public void onMountControls(ServerPlayerEntity player, float strafeSpeed, float forwardSpeed, boolean jumping, boolean sneaking) { }
        public void onItemAction(ServerPlayerEntity player, Hand enumhand, ItemStack itemstack) { }
        public void onBlockAction(ServerPlayerEntity player, BlockPos blockpos, Direction facing) { }
        public void onBlockHit(ServerPlayerEntity player, Hand enumhand, BlockHitResult hitRes) { }
        public void onBlockBroken(ServerPlayerEntity player, BlockPos pos, BlockState previousBS) { }
        public void onEntityAction(ServerPlayerEntity player, Entity entity, Hand enumhand) { }
    }


    public CarpetEventServer()
    {
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
    public void scheduleCall(CarpetContext context, FunctionValue function, List<LazyValue> args, long due)
    {
        scheduledCalls.add(new ScheduledCall(context, function, args, due));
    }

    public boolean addEvent(String event, String host, String funName)
    {
        if (!Event.byName.containsKey(event))
        {
            return false;
        }
        return Event.byName.get(event).handler.addEventCall(host, funName);
    }

    public boolean addEventDirectly(String event, ScriptHost host, FunctionValue function)
    {
        return Event.byName.get(event).handler.addEventCallDirect(host, function);
    }


    public boolean removeEvent(String event, String funName)
    {

        if (!Event.byName.containsKey(event))
            return false;
        Pair<String,String> call = decodeCallback(funName);
        Event.byName.get(event).handler.removeEventCall(call.getLeft(), call.getRight());
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

    private ScheduledCall makeEventCall(CarpetContext cc, FunctionValue function, List<Value> extraArgs, int argCount)
    {
        if (function == null || (function.getArguments().size()-(extraArgs == null ? 0 : extraArgs.size())) != argCount)
        {
            // call won't match arguments
            return null;
        }
        List<LazyValue> lazyArgs = null;
        if (extraArgs != null)
        {
            lazyArgs = new ArrayList<>();
            for (Value v : extraArgs)
                lazyArgs.add( (c, t) -> v);
        }
        return new ScheduledCall(cc, function, lazyArgs, 0);
    }

    public ScheduledCall makeDeathCall(CarpetContext cc, FunctionValue function, List<Value> extraArgs)
    {
        return makeEventCall(cc, function, extraArgs, 2);
    }
    public ScheduledCall makeRemovedCall(CarpetContext cc, FunctionValue function, List<Value> extraArgs)
    {
        return makeEventCall(cc, function, extraArgs, 1);
    }
    public ScheduledCall makeTickCall(CarpetContext cc, FunctionValue function, List<Value> extraArgs)
    {
        return makeEventCall(cc, function, extraArgs, 1);
    }
    public ScheduledCall makeDamageCall(CarpetContext cc, FunctionValue function, List<Value> extraArgs)
    {
        return makeEventCall(cc, function, extraArgs, 4);
    }

    public void onEntityDeath(ScheduledCall call, Entity e, String reason)
    {
        call.execute( Arrays.asList( (c, t)-> new EntityValue(e), (c, t)-> new StringValue(reason)));
    }
    public void onEntityRemoved(ScheduledCall removeCall, Entity entity)
    {
        removeCall.execute(Collections.singletonList((c, t) -> new EntityValue(entity)));
    }
    public void onEntityTick(ScheduledCall tickCall, Entity entity)
    {
        tickCall.execute(Collections.singletonList((c, t) -> new EntityValue(entity)));
    }
    public void onEntityDamage(ScheduledCall call, Entity e, float amount, DamageSource source)
    {
        call.execute(Arrays.asList(
                (c, t) -> new EntityValue(e),
                (c, t) -> new NumericValue(amount),
                (c, t) -> new StringValue(source.getName()),
                (c, t) -> source.getAttacker()==null?Value.NULL:new EntityValue(source.getAttacker())
        ));
    }
}
