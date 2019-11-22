package carpet.script;

import carpet.script.bundled.ModuleInterface;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScriptHost
{
    private final Map<String, ScriptHost> userHosts = new Object2ObjectOpenHashMap<>();
    public Map<String, FunctionValue> globalFunctions = new Object2ObjectOpenHashMap<>();
    public Map<String, LazyValue> globalVariables = new Object2ObjectOpenHashMap<>();

    protected ScriptHost parent;
    protected boolean perUser;

    private String name;
    public String getName() {return name;}

    protected final ModuleInterface myCode;

    ScriptHost(String name, ModuleInterface code, boolean perUser, ScriptHost parent)
    {
        this.parent = parent;
        this.name = name;
        this.myCode = code;
        this.perUser = perUser;
        globalVariables.put("euler", (c, t) -> Expression.euler);
        globalVariables.put("pi", (c, t) -> Expression.PI);
        globalVariables.put("null", (c, t) -> Value.NULL);
        globalVariables.put("true", (c, t) -> Value.TRUE);
        globalVariables.put("false", (c, t) -> Value.FALSE);

        //special variables for second order functions so we don't need to check them all the time
        globalVariables.put("_", (c, t) -> Value.ZERO);
        globalVariables.put("_i", (c, t) -> Value.ZERO);
        globalVariables.put("_a", (c, t) -> Value.ZERO);
    }

    public void addUserDefinedFunction(String funName, FunctionValue function)
    {
        globalFunctions.put(funName, function);
    }

    public void delFunction(String funName)
    {
        globalFunctions.remove(funName);
    }

    public void delGlobalVariable(String varName)
    {
        globalFunctions.remove(varName);
    }

    public ScriptHost retrieveForExecution(String /*Nullable*/ user)
    {
        if (!perUser)
            return this;
        ScriptHost userHost = userHosts.get(user);
        if (userHost != null)
            return userHost;
        userHost = this.duplicate();
        userHost.globalVariables.putAll(this.globalVariables);
        userHost.globalFunctions.putAll(this.globalFunctions);
        userHosts.put(user, userHost);
        return userHost;
    }

    protected ScriptHost duplicate()
    {
        return new ScriptHost(this.name, myCode, false, this);
    }

    public Expression getExpressionForFunction(String name)
    {
        return globalFunctions.get(name).getExpression();
    }
    public Tokenizer.Token getTokenForFunction(String name)
    {
        return globalFunctions.get(name).getToken();
    }

    public List<String> getPublicFunctions()
    {
        return globalFunctions.keySet().stream().filter((str) -> !str.startsWith("_")).collect(Collectors.toList());
    }
    public List<String> getAvailableFunctions(boolean all)
    {
        return globalFunctions.keySet().stream().filter((str) -> all || !str.startsWith("__")).collect(Collectors.toList());
    }

    public void onClose() { }

    public void setPerPlayer(boolean isPerUser)
    {
        perUser = isPerUser;
    }
}
