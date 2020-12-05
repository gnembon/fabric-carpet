package carpet.script;

import carpet.CarpetServer;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.EntityValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EntityEventsGroup
{
    private final Map<Event, Map<Pair<String,String>, CarpetEventServer.Callback>> actions;
    private final Entity entity;
    public EntityEventsGroup(Entity e)
    {
        actions = new HashMap<>();
        entity = e;
    }

    public void onEvent(Event type, Object... args)
    {
        if (actions.isEmpty()) return; // most of the cases, trying to be nice
        Map<Pair<String,String>, CarpetEventServer.Callback> actionSet = actions.get(type);
        if (actionSet == null) return;


        for (Iterator<Map.Entry<Pair<String,String>, CarpetEventServer.Callback>> iterator = actionSet.entrySet().iterator(); iterator.hasNext(); )
        {
            Map.Entry<Pair<String,String>, CarpetEventServer.Callback> action = iterator.next();
            Pair<String,String> key = action.getKey();
            ScriptHost host = CarpetServer.scriptServer.getHostByName(key.getLeft());
            if (host == null)
            {
                iterator.remove();
                continue;
            }
            if (key.getRight() != null)
            {
                if (entity.getServer().getPlayerManager().getPlayer(key.getRight())==null)
                {
                    iterator.remove();
                    continue;
                }
            }
            if (!type.call(action.getValue(), entity, args))
                iterator.remove();
        }
        if (actionSet.isEmpty()) actions.remove(type);
    }

    public void addEvent(Event type, ScriptHost host, FunctionValue fun, List<Value> extraargs)
    {
        Pair<String,String> key = Pair.of(host.getName(), host.user);
        if (fun != null)
        {
            CarpetEventServer.Callback call = type.create(key, fun, extraargs);
            if (call == null)
                throw new InternalExpressionException("wrong number of arguments for callback, required "+type.argcount);
            actions.computeIfAbsent(type, k -> new HashMap<>()).put(key, call);
        }
        else
        {
            actions.computeIfAbsent(type, k -> new HashMap<>()).remove(key);
            if (actions.get(type).isEmpty())
                actions.remove(type);
        }
    }


    public static class Event
    {
        public static final Map<String, Event> byName = new HashMap<>();
        public static final Event ON_DEATH = new Event("on_death", 1)
        {
            @Override
            public List<Value> makeArgs(Entity entity, Object... providedArgs)
            {
                return Arrays.asList(
                        new EntityValue(entity),
                        new StringValue((String)providedArgs[0])
                );
            }
        };
        public static final Event ON_REMOVED = new Event("on_removed", 0);
        public static final Event ON_TICK = new Event("on_tick", 0);
        public static final Event ON_DAMAGE = new Event("on_damaged", 3)
        {
            @Override
            public List<Value> makeArgs(Entity entity, Object... providedArgs)
            {
                float amount = (Float)providedArgs[0];
                DamageSource source = (DamageSource)providedArgs[1];
                return Arrays.asList(
                        new EntityValue(entity),
                        new NumericValue(amount),
                        new StringValue(source.getName()),
                        source.getAttacker()==null?Value.NULL:new EntityValue(source.getAttacker())
                );
            }
        };

        public final int argcount;
        public final String id;
        public Event(String identifier, int args)
        {
            id = identifier;
            argcount = args+1; // entity is not extra
            byName.put(identifier, this);
        }
        public CarpetEventServer.Callback create(Pair<String,String> key, FunctionValue function, List<Value> extraArgs)
        {
            if ((function.getArguments().size()-(extraArgs == null ? 0 : extraArgs.size())) != argcount)
            {
                return null;
            }
            return new CarpetEventServer.Callback(key.getLeft(), key.getRight(), function, extraArgs);
        }
        public boolean call(CarpetEventServer.Callback tickCall, Entity entity, Object ... args)
        {
            assert args.length == argcount-1;
            return tickCall.execute(entity.getCommandSource(), makeArgs(entity, args));
        }
        protected List<Value> makeArgs(Entity entity, Object ... args)
        {
            return Collections.singletonList(new EntityValue(entity));
        }
    }
}
