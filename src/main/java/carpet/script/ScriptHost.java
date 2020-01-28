package carpet.script;

import carpet.script.bundled.Module;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class ScriptHost
{
    public static Map<Value, Value> systemGlobals = new ConcurrentHashMap<>();
    private static Map<Long, Random> randomizers = new Long2ObjectOpenHashMap<>();

    public Random getRandom(long aLong)
    {
        if (randomizers.size() > 1024)
            randomizers.clear();
        return randomizers.computeIfAbsent(aLong, Random::new);
    }


    public static class ModuleData
    {
        Module parent;
        public Map<String, FunctionValue> globalFunctions = new Object2ObjectOpenHashMap<>();
        public Map<String, LazyValue> globalVariables = new Object2ObjectOpenHashMap<>();
        public Map<String, ModuleData> functionImports = new Object2ObjectOpenHashMap<>(); // imported functions string to module
        public Map<String, ModuleData> globalsImports = new Object2ObjectOpenHashMap<>(); // imported global variables string to module
        public Map<String, ModuleData> futureImports = new Object2ObjectOpenHashMap<>(); // imports not known before used

        public ModuleData(Module parent, ModuleData other)
        {
            // imports are just pointers, but they still point to the wrong modules (point to the parent)
            this.parent = parent;
            globalFunctions.putAll(other.globalFunctions);
            other.globalVariables.forEach((key, value) ->
            {
                Value var = value.evalValue(null);
                Value copy = var.deepcopy();
                copy.boundVariable = var.boundVariable;
                globalVariables.put(key, (c, t) -> copy);
            });
        }

        public void setImportsBasedOn(ScriptHost host, ModuleData other)
        {
            // fixing imports
            other.functionImports.forEach((name, targetData) -> {
                functionImports.put(name, host.moduleData.get(targetData.parent));
            });
            other.globalsImports.forEach((name, targetData) -> {
                globalsImports.put(name, host.moduleData.get(targetData.parent));
            });
            other.futureImports.forEach((name, targetData) -> {
                futureImports.put(name, host.moduleData.get(targetData.parent));
            });

        }

        public ModuleData(Module parent)
        {
            this.parent = parent;
        }
    }
    protected final Map<String, ScriptHost> userHosts = new Object2ObjectOpenHashMap<>();
    private Map<Module,ModuleData> moduleData = new HashMap<>(); // marking imports
    private Map<String,Module> modules = new HashMap<>();

    protected ScriptHost parent;
    protected boolean perUser;

    public String getName() {return main ==null?null: main.getName();}

    public final Module main;

    public Fluff.TriFunction<Expression, Tokenizer.Token, String, List<String>> errorSnooper = null;

    protected ScriptHost(Module code, boolean perUser, ScriptHost parent)
    {
        this.parent = parent;
        this.main = code;
        this.perUser = perUser;
        ModuleData moduleData = new ModuleData(code);
        initializeModuleGlobals(moduleData);
        this.moduleData.put(code, moduleData);
        this.modules.put(code==null?null:code.getName(), code);
    }

    void initializeModuleGlobals(ModuleData md)
    {
        md.globalVariables.put("euler", (c, t) -> Expression.euler);
        md.globalVariables.put("pi", (c, t) -> Expression.PI);
        md.globalVariables.put("null", (c, t) -> Value.NULL);
        md.globalVariables.put("true", (c, t) -> Value.TRUE);
        md.globalVariables.put("false", (c, t) -> Value.FALSE);
    }

    public void importModule(Context c, String moduleName)
    {
        if (modules.containsKey(moduleName)) return;  // aready imported
        Module module = getModuleOrLibraryByName(moduleName);
        if (modules.containsKey(module.getName())) return;  // aready imported, once again, in case some discrepancies in names?
        modules.put(module.getName(), module);
        ModuleData data = new ModuleData(module);
        initializeModuleGlobals(data);
        moduleData.put(module, data);
        runModuleCode(c, module);
        //moduleData.remove(module); // we are pooped already, but doesn't hurt to clean that up.
        //modules.remove(module.getName());
        //throw new InternalExpressionException("Failed to import a module "+moduleName);
    }
    public void importNames(Context c, Module targetModule, String sourceModuleName, List<String> identifiers )
    {
        if (!moduleData.containsKey(targetModule))
        {
            throw new InternalExpressionException("Cannot import to module that doesn't exist");
        }
        Module source = modules.get(sourceModuleName);
        ModuleData sourceData = moduleData.get(source);
        ModuleData targetData = moduleData.get(targetModule);
        if (sourceData == null || targetData == null)
        {
            throw new InternalExpressionException("Cannot import from module that is not imported");
        }
        for (String identifier: identifiers)
        {
            if (sourceData.globalFunctions.containsKey(identifier))
            {
                targetData.functionImports.put(identifier, sourceData);
            }
            else if (sourceData.globalVariables.containsKey(identifier))
            {
                targetData.globalsImports.put(identifier, sourceData);
            }
            else
            {
                targetData.futureImports.put(identifier, sourceData);
            }
        }
    }

    public Stream<String> availableImports(String moduleName)
    {
        Module source = modules.get(moduleName);
        ModuleData sourceData = moduleData.get(source);
        if (sourceData == null)
        {
            throw new InternalExpressionException("Cannot import from module that is not imported");
        }
        return Stream.concat(
                globaVariableNames(source, s -> s.startsWith("global_")),
                globaFunctionNames(source, s -> true)
        ).distinct().sorted();
    }

    protected abstract Module getModuleOrLibraryByName(String name); // this should be shell out in the executor

    protected abstract void runModuleCode(Context c, Module module); // this should be shell out in the executor

    public FunctionValue getFunction(String name) { return getFunction(main, name); }
    public FunctionValue getAssertFunction(Module module, String name)
    {
        FunctionValue ret = getFunction(module, name);
        if (ret == null) throw new InternalExpressionException("Function "+name+" is not defined yet");
        return ret;
    }
    private FunctionValue getFunction(Module module, String name)
    {
        ModuleData local = moduleData.get(module);
        FunctionValue ret = local.globalFunctions.get(name); // most uses would be from local scope anyways
        if (ret != null) return ret;
        ModuleData target = local.functionImports.get(name);
        if (target != null)
        {
            ret = target.globalFunctions.get(name);
            if (ret != null) return ret;
        }
        // not in local scope - will need to travel over import links
        target = local.futureImports.get(name);
        if (target == null) return null;
        target = findModuleDataFromFunctionImports(name, target, 0);
        if (target == null) return null;
        local.futureImports.remove(name);
        local.functionImports.put(name, target);
        return target.globalFunctions.get(name);
    }

    private ModuleData findModuleDataFromFunctionImports(String name, ModuleData source, int ttl)
    {
        if (ttl > 64) throw new InternalExpressionException("Cannot import "+name+", either your imports are too deep or too loopy");
        if (source.globalFunctions.containsKey(name))
            return source;
        if (source.functionImports.containsKey(name))
            return findModuleDataFromFunctionImports(name, source.functionImports.get(name), ttl+1);
        if (source.futureImports.containsKey(name))
            return findModuleDataFromFunctionImports(name, source.futureImports.get(name), ttl+1);
        return null;
    }

    public LazyValue getGlobalVariable(String name) { return getGlobalVariable(main, name); }
    public LazyValue getGlobalVariable(Module module, String name)
    {
        ModuleData local = moduleData.get(module);
        LazyValue ret = local.globalVariables.get(name); // most uses would be from local scope anyways
        if (ret != null) return ret;
        ModuleData target = local.globalsImports.get(name);
        if (target != null)
        {
            ret = target.globalVariables.get(name);
            if (ret != null) return ret;
        }
        // not in local scope - will need to travel over import links
        target = local.futureImports.get(name);
        if (target == null) return null;
        target = findModuleDataFromGlobalImports(name, target, 0);
        if (target == null) return null;
        local.futureImports.remove(name);
        local.globalsImports.put(name, target);
        return target.globalVariables.get(name);
    }

    private ModuleData findModuleDataFromGlobalImports(String name, ModuleData source, int ttl)
    {
        if (ttl > 64) throw new InternalExpressionException("Cannot import "+name+", either your imports are too deep or too loopy");
        if (source.globalVariables.containsKey(name))
            return source;
        if (source.globalsImports.containsKey(name))
            return findModuleDataFromGlobalImports(name, source.globalsImports.get(name), ttl+1);
        if (source.futureImports.containsKey(name))
            return findModuleDataFromGlobalImports(name, source.futureImports.get(name), ttl+1);
        return null;
    }

    public void delFunctionWithPrefix(Module module, String prefix)
    {
        ModuleData data = moduleData.get(module);
        data.globalFunctions.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        data.functionImports.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    }
    public void delFunction(Module module, String funName)
    {
        ModuleData data = moduleData.get(module);
        data.globalFunctions.remove(funName);
        data.functionImports.remove(funName);
    }

    public void delGlobalVariableWithPrefix(Module module, String prefix)
    {
        ModuleData data = moduleData.get(module);
        data.globalVariables.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        data.globalsImports.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    }

    public void delGlobalVariable(Module module, String varName)
    {
        ModuleData data = moduleData.get(module);
        data.globalFunctions.remove(varName);
        data.functionImports.remove(varName);
    }

    public void addUserDefinedFunction(Module module, String name, FunctionValue fun)
    {
        moduleData.get(module).globalFunctions.put(name, fun);
    }

    public void setGlobalVariable(Module module, String name, LazyValue lv)
    {
        moduleData.get(module).globalVariables.put(name, lv);
    }

    public Stream<String> globaVariableNames(Module module, Predicate<String> predicate)
    {
        return Stream.concat(Stream.concat(
                moduleData.get(module).globalVariables.keySet().stream(),
                moduleData.get(module).globalsImports.keySet().stream()
        ), moduleData.get(module).futureImports.keySet().stream().filter(s -> s.startsWith("global_"))).filter(predicate);
    }

    public Stream<String> globaFunctionNames(Module module, Predicate<String> predicate)
    {
        return Stream.concat(Stream.concat(
                moduleData.get(module).globalFunctions.keySet().stream(),
                moduleData.get(module).functionImports.keySet().stream()
        ),moduleData.get(module).futureImports.keySet().stream().filter(s -> !s.startsWith("global_"))).filter(predicate);
    }

    public ScriptHost retrieveForExecution(String /*Nullable*/ user)
    {
        if (!perUser) return this;
        ScriptHost oldUserHost = userHosts.get(user);
        if (oldUserHost != null) return oldUserHost;
        ScriptHost userHost = this.duplicate();
        userHost.modules.putAll(this.modules);
        for (Map.Entry<Module, ScriptHost.ModuleData> e : this.moduleData.entrySet())
        {
            userHost.moduleData.put(e.getKey(), new ModuleData(e.getKey(), e.getValue()));
        }
        // fixing imports
        userHost.moduleData.forEach((module, data) ->
        {
            data.setImportsBasedOn(userHost, this.moduleData.get(data.parent));
        });
        userHosts.put(user, userHost);
        return userHost;
    }

    public void handleExpressionException(String msg, ExpressionException exc)
    {
        System.out.println(msg+": "+exc);
    }


    protected abstract ScriptHost duplicate();

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
