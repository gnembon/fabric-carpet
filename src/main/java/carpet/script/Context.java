package carpet.script;

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
        if (variables.containsKey(name))
        {
            return variables.get(name);
        }
        return host.globalVariables.get(name);
    }

    public void setVariable(String name, LazyValue lv)
    {
        if (name.startsWith("global_"))
        {
            host.globalVariables.put(name, lv);
            return;
        }
        variables.put(name, lv);
    }


    boolean isAVariable(String name)
    {
        return variables.containsKey(name) || host.globalVariables.containsKey(name);
    }


    void delVariable(String variable)
    {
        variables.remove(variable);
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
        return new Context(this.host);
    }
}
