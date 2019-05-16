package carpet.script;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Context
{
    static final int NONE = 0;
    static final int VOID = 1;
    static final int BOOLEAN = 2;
    static final int NUMBER = 3;
    static final int STRING = 4;
    static final int LIST = 5;
    static final int ITERATOR = 6;
    static final int SIGNATURE = 7;
    static final int LOCALIZATION = 8;

    private Map<String, LazyValue> variables = new HashMap<>();

    protected ScriptHost host;

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

    void setVariable(String name, LazyValue lv)
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
        if (variable.startsWith("global_"))
        {
            host.globalVariables.remove(variable);
            return;
        }
        variables.remove(variable);
    }
    void clearAll(String variable)
    {
        if (variable.startsWith("global_"))
        {
            host.globalVariables.remove(variable);
            return;
        }
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
