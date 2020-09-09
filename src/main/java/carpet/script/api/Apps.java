package carpet.script.api;

import carpet.script.CarpetScriptHost;
import carpet.script.Expression;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

import java.util.stream.Collectors;

public class Apps {
    public static void apply(Expression expression)
    {
        //todo doc and test all
        expression.addLazyFunction("loaded_apps", 0, (c, t, lv) ->
        {
            // return a set of all loaded apps
            Value ret = new MapValue(((CarpetScriptHost) c.host).getScriptServer().modules.keySet().stream().map(StringValue::new).collect(Collectors.toSet()));
            return (cc, tt) -> ret;
        });

        expression.addLazyFunction("is_app_loaded", 1, (c, t, lv) ->
        {
            Value ret = new NumericValue(((CarpetScriptHost) c.host).getScriptServer().modules.containsKey(lv.get(0).evalValue(c).getString()));
            return (cc, tt) -> ret;
        });

    }
}
