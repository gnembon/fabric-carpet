package carpet.script;

import carpet.script.exception.InternalExpressionException;
import carpet.script.external.Vanilla;
import carpet.script.value.EntityValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityEventsGroup
{
    private record EventKey(String host, String user)
    {
    }

    private final Map<Event, Map<EventKey, CarpetEventServer.Callback>> actions;
    private final Entity entity;

    public EntityEventsGroup(final Entity e)
    {
        actions = new HashMap<>();
        entity = e;
    }

    public void onEvent(final Event type, final Object... args)
    {
        if (actions.isEmpty())
        {
            return; // most of the cases, trying to be nice
        }
        final Map<EventKey, CarpetEventServer.Callback> actionSet = actions.get(type);
        if (actionSet == null)
        {
            return;
        }
        final CarpetScriptServer scriptServer = Vanilla.MinecraftServer_getScriptServer(entity.getServer());
        if (scriptServer.stopAll)
        {
            return; // executed after world is closin down
        }
        for (final Iterator<Map.Entry<EventKey, CarpetEventServer.Callback>> iterator = actionSet.entrySet().iterator(); iterator.hasNext(); )
        {
            final Map.Entry<EventKey, CarpetEventServer.Callback> action = iterator.next();
            final EventKey key = action.getKey();
            final ScriptHost host = scriptServer.getAppHostByName(key.host());
            if (host == null)
            {
                iterator.remove();
                continue;
            }
            if (key.user() != null && entity.getServer().getPlayerList().getPlayerByName(key.user()) == null)
            {
                iterator.remove();
                continue;
            }
            if (type.call(action.getValue(), entity, args) == CarpetEventServer.CallbackResult.FAIL)
            {
                iterator.remove();
            }
        }
        if (actionSet.isEmpty())
        {
            actions.remove(type);
        }
    }

    public void addEvent(final Event type, final ScriptHost host, final FunctionValue fun, final List<Value> extraargs)
    {
        final EventKey key = new EventKey(host.getName(), host.user);
        if (fun != null)
        {
            final CarpetEventServer.Callback call = type.create(key, fun, extraargs, (CarpetScriptServer) host.scriptServer());
            if (call == null)
            {
                throw new InternalExpressionException("wrong number of arguments for callback, required " + type.argcount);
            }
            actions.computeIfAbsent(type, k -> new HashMap<>()).put(key, call);
        }
        else
        {
            actions.computeIfAbsent(type, k -> new HashMap<>()).remove(key);
            if (actions.get(type).isEmpty())
            {
                actions.remove(type);
            }
        }
    }


    public static class Event
    {
        public static final Map<String, Event> byName = new HashMap<>();
        public static final Event ON_DEATH = new Event("on_death", 1)
        {
            @Override
            public List<Value> makeArgs(final Entity entity, final Object... providedArgs)
            {
                return Arrays.asList(
                        new EntityValue(entity),
                        new StringValue((String) providedArgs[0])
                );
            }
        };
        public static final Event ON_REMOVED = new Event("on_removed", 0);
        public static final Event ON_TICK = new Event("on_tick", 0);
        public static final Event ON_DAMAGE = new Event("on_damaged", 3)
        {
            @Override
            public List<Value> makeArgs(final Entity entity, final Object... providedArgs)
            {
                final float amount = (Float) providedArgs[0];
                final DamageSource source = (DamageSource) providedArgs[1];
                return Arrays.asList(
                        new EntityValue(entity),
                        new NumericValue(amount),
                        new StringValue(source.getMsgId()),
                        source.getEntity() == null ? Value.NULL : new EntityValue(source.getEntity())
                );
            }
        };
        public static final Event ON_MOVE = new Event("on_move", 3)
        {
            @Override
            public List<Value> makeArgs(final Entity entity, final Object... providedArgs)
            {
                return Arrays.asList(
                        new EntityValue(entity),
                        ValueConversions.of((Vec3) providedArgs[0]),
                        ValueConversions.of((Vec3) providedArgs[1]),
                        ValueConversions.of((Vec3) providedArgs[2])
                );
            }
        };

        public final int argcount;
        public final String id;

        public Event(final String identifier, final int args)
        {
            id = identifier;
            argcount = args + 1; // entity is not extra
            byName.put(identifier, this);
        }

        public CarpetEventServer.Callback create(final EventKey key, final FunctionValue function, final List<Value> extraArgs, final CarpetScriptServer scriptServer)
        {
            if ((function.getArguments().size() - (extraArgs == null ? 0 : extraArgs.size())) != argcount)
            {
                return null;
            }
            return new CarpetEventServer.Callback(key.host(), key.user(), function, extraArgs, scriptServer);
        }

        public CarpetEventServer.CallbackResult call(final CarpetEventServer.Callback tickCall, final Entity entity, final Object... args)
        {
            assert args.length == argcount - 1;
            return tickCall.execute(entity.createCommandSourceStack(), makeArgs(entity, args));
        }

        protected List<Value> makeArgs(final Entity entity, final Object... args)
        {
            return Collections.singletonList(new EntityValue(entity));
        }
    }
}
