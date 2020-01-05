package carpet.script;

import carpet.script.bundled.ModuleInterface;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ScriptHost
{
    protected final Map<String, ScriptHost> userHosts = new Object2ObjectOpenHashMap<>();
    public Map<String, FunctionValue> globalFunctions = new Object2ObjectOpenHashMap<>();
    public Map<String, LazyValue> globalVariables = new Object2ObjectOpenHashMap<>();
    private Set<String> imports = new HashSet<>();

    protected ScriptHost parent;
    protected boolean perUser;

    //private String name;
    public String getName() {return myCode==null?null:myCode.getName();}

    protected final ModuleInterface myCode;

    public Fluff.TriFunction<Expression, Tokenizer.Token, String, List<String>> errorSnooper = null;

    ScriptHost(ModuleInterface code, boolean perUser, ScriptHost parent)
    {
        this.parent = parent;
        //this.name = name;
        this.myCode = code;
        this.perUser = perUser;
        if (code != null) imports.add(code.getName());
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

    public boolean importModule(Context c, String moduleName)
    {
        if (imports.contains(moduleName)) return false;  // aready imported
        ModuleInterface module = getModuleByName(moduleName);
        if (imports.contains(module.getName())) return false;  // aready imported, once again, in case some discrepancies
        imports.add(module.getName());
        if (runModuleCode(c, module))
        {
            return true;
        }
        imports.remove(module.getName());
        return false;
    }

    protected abstract ModuleInterface getModuleByName(String name); // this should be shell out in the executuor

    protected abstract boolean runModuleCode(Context c, ModuleInterface module); // this should be shell out in the executuor

    public void delFunction(String funName)
    {
        globalFunctions.remove(funName);
    }

    public void delGlobalVariable(String varName)
    {
        globalVariables.remove(varName);
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
        userHost.imports.addAll(this.imports);
        userHosts.put(user, userHost);
        return userHost;
    }

    protected abstract ScriptHost duplicate();

    public List<String> getPublicFunctions()
    {
        return globalFunctions.keySet().stream().filter((str) -> !str.startsWith("_")).sorted().collect(Collectors.toList());
    }
    public List<String> getAvailableFunctions(boolean all)
    {
        return globalFunctions.keySet().stream().filter((str) -> all || !str.startsWith("__")).sorted().collect(Collectors.toList());
    }

    public void onClose() { }

    public void setPerPlayer(boolean isPerUser)
    {
        perUser = isPerUser;
    }

    public void resetErrorSnooper()
    {
        errorSnooper=null;
    }
}
