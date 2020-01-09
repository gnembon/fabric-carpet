package carpet.script;

import carpet.script.value.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Context
{
    public static final int NONE = 0;
    public static final int VOID = 1;
    public static final int BOOLEAN = 2;
    public static final int NUMBER = 3;
    public static final int STRING = 4;
    public static final int LIST = 5;
    public static final int ITERATOR = 6;
    public static final int SIGNATURE = 7;
    public static final int LOCALIZATION = 8;
    public static final int LVALUE = 9;

    public final Map<String, LazyValue> variables = new HashMap<>();

    public final ScriptHost host;

    Context(ScriptHost host)
    {
        this.host = host;
    }

    LazyValue getVariable(String name)
    {
        return variables.get(name);
    }

    public void setVariable(String name, LazyValue lv)
    {
        variables.put(name, lv);
    }

    void delVariable(String variable)
    {
        variables.remove(variable);
    }

    public void removeVariablesMatching(String varname)
    {
        variables.entrySet().removeIf(e -> e.getKey().startsWith(varname));
    }

    public Context with(String variable, LazyValue lv)
    {
        variables.put(variable, lv);
        return this;
    }

    public Set<String> getAllVariableNames()
    {
        return variables.keySet();
    }

    public Context recreate()
    {
        Context ctx = duplicate();
        ctx.initialize();
        return ctx;
    }

    protected void initialize()
    {
        //special variables for second order functions so we don't need to check them all the time
        variables.put("_", (c, t) -> Value.ZERO);
        variables.put("_i", (c, t) -> Value.ZERO);
        variables.put("_a", (c, t) -> Value.ZERO);
    }

    public Context duplicate()
    {
        return new Context(this.host);
    }
}
