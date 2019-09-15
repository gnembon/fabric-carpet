package carpet.script;

import carpet.CarpetServer;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.settings.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

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
import java.util.stream.Collectors;

public class CarpetEventServer
{
    public static class Callback
    {
        public String host;
        public String udf;

        public Callback(String host, String udf)
        {
            this.host = host;
            this.udf = udf;
        }

        @Override
        public String toString()
        {
            return udf+((host==null)?"":"(from "+host+")");
        }
    }

    public static class ScheduledCall extends Callback
    {
        public List<LazyValue> args;
        public ServerCommandSource context_source;
        public BlockPos context_origin;
        public long dueTime;

        public ScheduledCall(CarpetContext context, String udf, List<LazyValue> args, long dueTime)
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
            UserDefinedFunction udf = host.globalFunctions.get(funName);
            if (udf == null || udf.getArguments().size() != reqArgs)
            {
                // call won't match arguments
                return false;
            }
            //all clear
            //remove duplicates
            removeEventCall(hostName, funName);
            callList.add(new Callback(hostName, funName));
            return true;
        }
        public void removeEventCall(String hostName, String callName)
        {
            callList.removeIf((c)->  c.udf.equalsIgnoreCase(callName) && ( hostName == null || c.host.equalsIgnoreCase(hostName) ) );
        }

        public void removeAllCalls(String hostName)
        {
            callList.removeIf((c)-> c.host.equals(hostName));
        }
    }

    public Map<String, CallbackList> eventHandlers = new HashMap<>();

    public List<ScheduledCall> scheduledCalls = new LinkedList<>();

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
    public void scheduleCall(CarpetContext context, String function, List<LazyValue> args, long due)
    {
        scheduledCalls.add(new ScheduledCall(context, function, args, due));
    }


    public CarpetEventServer()
    {
        eventHandlers.put("tick",new CallbackList(0));
        eventHandlers.put("tick_nether",new CallbackList(0));
        eventHandlers.put("tick_ender",new CallbackList(0));
        eventHandlers.put("player_jumps",new CallbackList(1));
        eventHandlers.put("player_deploys_elytra",new CallbackList(1));
        eventHandlers.put("player_wakes_up",new CallbackList(1));
        eventHandlers.put("player_rides",new CallbackList(5));
        eventHandlers.put("player_uses_item",new CallbackList(3));
        eventHandlers.put("player_clicks_block",new CallbackList(3));
        eventHandlers.put("player_right_clicks_block",new CallbackList(6));
        eventHandlers.put("player_breaks_block",new CallbackList(2));
        eventHandlers.put("player_interacts_with_entity",new CallbackList(3));
        eventHandlers.put("player_attacks_entity",new CallbackList(2));
        eventHandlers.put("player_starts_sneaking",new CallbackList(1));
        eventHandlers.put("player_stops_sneaking",new CallbackList(1));
        eventHandlers.put("player_starts_sprinting",new CallbackList(1));
        eventHandlers.put("player_stops_sprinting",new CallbackList(1));
    }

    public boolean addEvent(String event, String host, String funName)
    {
        if (!eventHandlers.containsKey(event))
        {
            return false;
        }
        return eventHandlers.get(event).addEventCall(host, funName);
    }

    public boolean removeEvent(String event, String funName)
    {

        if (!eventHandlers.containsKey(event))
            return false;
        Callback callback= decodeCallback(funName);
        eventHandlers.get(event).removeEventCall(callback.host, callback.udf);
        return true;
    }

    public void removeAllHostEvents(String hostName)
    {
        eventHandlers.forEach((e, a) -> a.removeAllCalls(hostName));
    }

    private Callback decodeCallback(String funName)
    {
        Pattern find = Pattern.compile("(\\w+)\\(from (\\w+)\\)");
        Matcher matcher = find.matcher(funName);
        if(matcher.matches())
        {
            return new Callback(matcher.group(2), matcher.group(1));
        }
        return new Callback(null, funName);
    }

    private ScheduledCall makeEventCall(CarpetContext cc, String function, List<Value> extraArgs, int argCount)
    {
        UserDefinedFunction udf = cc.host.globalFunctions.get(function);
        if (udf == null || (udf.getArguments().size()-extraArgs.size()) != argCount)
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

    public void onTick()
    {
        eventHandlers.get("tick").call(Collections::emptyList, CarpetServer.minecraft_server::getCommandSource);
        eventHandlers.get("tick_nether").call(Collections::emptyList, () ->
                CarpetServer.minecraft_server.getCommandSource().withWorld(CarpetServer.minecraft_server.getWorld(DimensionType.THE_NETHER)));
        eventHandlers.get("tick_ender").call(Collections::emptyList, () ->
                CarpetServer.minecraft_server.getCommandSource().withWorld(CarpetServer.minecraft_server.getWorld(DimensionType.THE_END)));
    }

    public void onPlayerJumped(PlayerEntity player)
    {
        eventHandlers.get("player_jumps").call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
    }

    public void onElytraDeploy(ServerPlayerEntity player)
    {
        eventHandlers.get("player_deploys_elytra").call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
    }

    public void onOutOfBed(ServerPlayerEntity player)
    {
        eventHandlers.get("player_wakes_up").call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
    }

    public void onMountControls(PlayerEntity player, float strafeSpeed, float forwardSpeed, boolean jumping, boolean sneaking)
    {
        eventHandlers.get("player_rides").call( () -> Arrays.asList(
                ((c, t) -> new EntityValue(player)),
                ((c, t) -> new NumericValue(forwardSpeed)),
                ((c, t) -> new NumericValue(strafeSpeed)),
                ((c, t) -> new NumericValue(jumping)),
                ((c, t) -> new NumericValue(sneaking))
        ), player::getCommandSource);
    }

    public void onRightClick(ServerPlayerEntity player, Hand enumhand)
    {
        eventHandlers.get("player_uses_item").call( () ->
        {
            ItemStack itemstack = player.getStackInHand(enumhand);
            return Arrays.asList(
                    ((c, t) -> new EntityValue(player)),
                    ((c, t) -> ListValue.fromItemStack(itemstack)),
                    ((c, t) -> new StringValue(enumhand == Hand.MAIN_HAND ? "mainhand" : "offhand"))
            );
        }, player::getCommandSource);
    }

    public void onBlockClicked(ServerPlayerEntity player, BlockPos blockpos, Direction facing)
    {
        eventHandlers.get("player_clicks_block").call( () -> Arrays.asList(
                ((c, t) -> new EntityValue(player)),
                ((c, t) -> new BlockValue(null, player.getServerWorld(), blockpos)),
                ((c, t) -> new StringValue(facing.getName()))
        ), player::getCommandSource);
    }

    public void onRightClickBlock(ServerPlayerEntity player, Hand enumhand, BlockHitResult hitRes)//ItemStack itemstack, Hand enumhand, BlockPos blockpos, Direction enumfacing, Vec3d vec3d)
    {
        eventHandlers.get("player_right_clicks_block").call( () ->
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

    public void onBlockBroken(ServerPlayerEntity player, BlockPos pos, BlockState previousBS)
    {
        eventHandlers.get("player_breaks_block").call( () -> Arrays.asList(
                ((c, t) -> new EntityValue(player)),
                ((c, t) -> new BlockValue(previousBS, player.getServerWorld(), pos))
        ), player::getCommandSource);
    }

    public void onEntityInteracted(ServerPlayerEntity player, Entity entity, Hand enumhand)
    {
        eventHandlers.get("player_interacts_with_entity").call( () -> Arrays.asList(
                ((c, t) -> new EntityValue(player)),
                ((c, t) -> new EntityValue(entity)),
                ((c, t) -> new StringValue(enumhand==Hand.MAIN_HAND?"mainhand":"offhand"))
        ), player::getCommandSource);
    }

    public void onEntityAttacked(ServerPlayerEntity player, Entity entity)
    {
        eventHandlers.get("player_attacks_entity").call( () -> Arrays.asList(
                ((c, t) -> new EntityValue(player)),
                ((c, t) -> new EntityValue(entity))
        ), player::getCommandSource);
    }

    public void onStartSneaking(ServerPlayerEntity player)
    {
        eventHandlers.get("player_starts_sneaking").call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
    }

    public void onStopSneaking(ServerPlayerEntity player)
    {
        eventHandlers.get("player_stops_sneaking").call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
    }

    public void onStartSprinting(ServerPlayerEntity player)
    {
        eventHandlers.get("player_starts_sprinting").call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
    }

    public void onStopSprinting(ServerPlayerEntity player)
    {
        eventHandlers.get("player_stops_sprinting").call( () -> Arrays.asList(((c, t) -> new EntityValue(player))), player::getCommandSource);
    }

    public ScheduledCall makeDeathCall(CarpetContext cc, String function, List<Value> extraArgs)
    {
        return makeEventCall(cc, function, extraArgs, 2);
    }
    public ScheduledCall makeRemovedCall(CarpetContext cc, String function, List<Value> extraArgs)
    {
        return makeEventCall(cc, function, extraArgs, 1);
    }
    public ScheduledCall makeTickCall(CarpetContext cc, String function, List<Value> extraArgs)
    {
        return makeEventCall(cc, function, extraArgs, 1);
    }
    public ScheduledCall makeDamageCall(CarpetContext cc, String function, List<Value> extraArgs)
    {
        return makeEventCall(cc, function, extraArgs, 3);
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
