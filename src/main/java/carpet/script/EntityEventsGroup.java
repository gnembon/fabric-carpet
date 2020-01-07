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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityEventsGroup
{
    private final Map<EntityEventType, Map<String, CarpetEventServer.ScheduledCall>> actions;
    public EntityEventsGroup()
    {
        actions = new EnumMap<>(EntityEventType.class);
    }

    public void onEvent(EntityEventType type, Entity entity, Object ... args)
    {
        if (actions.isEmpty()) return; // most of the cases, trying to be nice
        Map<String, CarpetEventServer.ScheduledCall> actionSet = actions.get(type);
        if (actionSet == null) return;

        for (Iterator<Map.Entry<String, CarpetEventServer.ScheduledCall>> iterator = actionSet.entrySet().iterator(); iterator.hasNext(); )
        {
            Map.Entry<String, CarpetEventServer.ScheduledCall> action = iterator.next();
            ScriptHost host = CarpetServer.scriptServer.getHostByName(action.getKey());
            if (host == null)
            {
                iterator.remove();
                continue;
            }
            type.call(action.getValue(), entity, args); // optionally remove when call failing
        }
        if (actionSet.isEmpty()) actions.remove(type);
    }

    public void addEvent(EntityEventType type, CarpetContext cc, FunctionValue fun, List<Value> extraargs)
    {
        if (fun != null)
        {
            CarpetEventServer.ScheduledCall call = type.create(cc, fun, extraargs);
            if (call == null)
                throw new InternalExpressionException("wrong number of arguments for callback, required "+type.argcount);
            actions.computeIfAbsent(type, k -> new HashMap<>()).put(cc.host.getName(), call);
        }
        else
        {
            actions.computeIfAbsent(type, k -> new HashMap<>()).remove(cc.host.getName());
            if (actions.get(type).isEmpty())
                actions.remove(type);
        }
    }


    public enum EntityEventType
    {
        ON_DEATH("on_death", 1)
        {
            @Override
            public List<Value> makeArgs(Entity entity, Object... providedArgs)
            {
                return Arrays.asList(
                        new EntityValue(entity),
                        new StringValue((String)providedArgs[0])
                );
            }
        },
        ON_REMOVED("on_removed", 0),
        ON_TICK("on_tick", 0),
        ON_DAMAGE("on_damaged", 2)
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

        public static final Map<String, EntityEventType> byName = Arrays.stream(EntityEventType.values()).collect(Collectors.toMap(ev -> ev.id, ev -> ev));

        public final int argcount;
        public final String id;
        EntityEventType(String identifier, int args)
        {
            id = identifier;
            argcount = args+1; // entity is not extra
        }
        public CarpetEventServer.ScheduledCall create(CarpetContext cc, FunctionValue function, List<Value> extraArgs)
        {
            if ((function.getArguments().size()-(extraArgs == null ? 0 : extraArgs.size())) != argcount)
            {
                return null;
            }
            return new CarpetEventServer.ScheduledCall(cc, function, extraArgs, 0);
        }
        public void call(CarpetEventServer.ScheduledCall tickCall, Entity entity, Object ... args)
        {
            assert args.length == argcount-1;
            tickCall.execute(makeArgs(entity, args));
        }
        protected List<Value> makeArgs(Entity entity, Object ... args)
        {
            return Collections.singletonList(new EntityValue(entity));
        }
    }
}
