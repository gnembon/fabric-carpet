package carpet.script;

import carpet.script.exception.ExpressionException;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class ScriptHost
{
    private static final Map<Long, Random> randomizers = new Long2ObjectOpenHashMap<>();

    public static Thread mainThread = null;
    private final Map<Value, ThreadPoolExecutor> executorServices = new HashMap<>();
    private final Map<Value, Object> locks = new ConcurrentHashMap<>();
    private final ScriptServer scriptServer;
    protected boolean inTermination = false;
    public boolean strict;

    private final Set<String> deprecations = new HashSet<>();

    public Random getRandom(long aLong)
    {
        if (randomizers.size() > 65536)
        {
            randomizers.clear();
        }
        return randomizers.computeIfAbsent(aLong, Random::new);
    }

    public boolean resetRandom(long aLong)
    {
        return randomizers.remove(aLong) != null;
    }

    public Path resolveScriptFile(String suffix)
    {
        return scriptServer.resolveResource(suffix);
    }

    public boolean canSynchronouslyExecute()
    {
        return true;
    }

    public static class ModuleData
    {
        Module parent;
        public final Map<String, FunctionValue> globalFunctions = new Object2ObjectOpenHashMap<>();
        public final Map<String, LazyValue> globalVariables = new Object2ObjectOpenHashMap<>();
        public final Map<String, ModuleData> functionImports = new Object2ObjectOpenHashMap<>(); // imported functions string to module
        public final Map<String, ModuleData> globalsImports = new Object2ObjectOpenHashMap<>(); // imported global variables string to module
        public final Map<String, ModuleData> futureImports = new Object2ObjectOpenHashMap<>(); // imports not known before used

        public ModuleData(Module parent, ModuleData other)
        {
            super();
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
    private final Map<Module, ModuleData> moduleData = new HashMap<>(); // marking imports
    private final Map<String, Module> modules = new HashMap<>();

    protected ScriptHost parent;
    protected boolean perUser;
    public String user;

    public String getName()
    {
        return main == null ? null : main.name();
    }

    public String getVisualName()
    {
        return main == null ? "built-in default app" : main.name();
    }

    public boolean isDefaultApp()
    {
        return main == null;
    }

    @Nullable
    public final Module main;

    @FunctionalInterface
    public interface ErrorSnooper
    {
        List<String> apply(Expression expression, Tokenizer.Token token, Context context, String message);
    }

    public ErrorSnooper errorSnooper = null;

    protected ScriptHost(@Nullable Module code, ScriptServer scriptServer, boolean perUser, ScriptHost parent)
    {
        this.parent = parent;
        this.main = code;
        this.perUser = perUser;
        this.user = null;
        this.strict = false;
        this.scriptServer = scriptServer;
        ModuleData moduleData = new ModuleData(code);
        initializeModuleGlobals(moduleData);
        this.moduleData.put(code, moduleData);
        this.modules.put(code == null ? null : code.name(), code);
        mainThread = Thread.currentThread();
    }

    void initializeModuleGlobals(ModuleData md)
    {
    }

    public void importModule(Context c, String moduleName)
    {
        if (modules.containsKey(moduleName.toLowerCase(Locale.ROOT)))
        {
            return;  // aready imported
        }
        Module module = getModuleOrLibraryByName(moduleName);
        if (modules.containsKey(module.name()))
        {
            return;  // aready imported, once again, in case some discrepancies in names?
        }
        modules.put(module.name(), module);
        ModuleData data = new ModuleData(module);
        initializeModuleGlobals(data);
        moduleData.put(module, data);
        runModuleCode(c, module);
        //moduleData.remove(module); // we are pooped already, but doesn't hurt to clean that up.
        //modules.remove(module.getName());
        //throw new InternalExpressionException("Failed to import a module "+moduleName);
    }

    public void importNames(Context c, Module targetModule, String sourceModuleName, List<String> identifiers)
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
        for (String identifier : identifiers)
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
                globalVariableNames(source, s -> s.startsWith("global_")),
                globalFunctionNames(source, s -> true)
        ).distinct().sorted();
    }

    protected abstract Module getModuleOrLibraryByName(String name); // this should be shell out in the executor

    protected abstract void runModuleCode(Context c, Module module); // this should be shell out in the executor

    public FunctionValue getFunction(String name)
    {
        return getFunction(main, name);
    }

    public FunctionValue getAssertFunction(Module module, String name)
    {
        FunctionValue ret = getFunction(module, name);
        if (ret == null)
        {
            if (module == main)
            {
                throw new InternalExpressionException("Function '" + name + "' is not defined yet");
            }
            else
            {
                throw new InternalExpressionException("Function '" + name + "' is not defined nor visible by its name in the imported module '" + module.name() + "'");
            }
        }
        return ret;
    }

    private FunctionValue getFunction(Module module, String name)
    {
        ModuleData local = getModuleData(module);
        FunctionValue ret = local.globalFunctions.get(name); // most uses would be from local scope anyways
        if (ret != null)
        {
            return ret;
        }
        ModuleData target = local.functionImports.get(name);
        if (target != null)
        {
            ret = target.globalFunctions.get(name);
            if (ret != null)
            {
                return ret;
            }
        }
        // not in local scope - will need to travel over import links
        target = local.futureImports.get(name);
        if (target == null)
        {
            return null;
        }
        target = findModuleDataFromFunctionImports(name, target, 0);
        if (target == null)
        {
            return null;
        }
        local.futureImports.remove(name);
        local.functionImports.put(name, target);
        return target.globalFunctions.get(name);
    }

    private ModuleData findModuleDataFromFunctionImports(String name, ModuleData source, int ttl)
    {
        if (ttl > 64)
        {
            throw new InternalExpressionException("Cannot import " + name + ", either your imports are too deep or too loopy");
        }
        if (source.globalFunctions.containsKey(name))
        {
            return source;
        }
        if (source.functionImports.containsKey(name))
        {
            return findModuleDataFromFunctionImports(name, source.functionImports.get(name), ttl + 1);
        }
        if (source.futureImports.containsKey(name))
        {
            return findModuleDataFromFunctionImports(name, source.futureImports.get(name), ttl + 1);
        }
        return null;
    }

    public LazyValue getGlobalVariable(String name)
    {
        return getGlobalVariable(main, name);
    }

    public LazyValue getGlobalVariable(Module module, String name)
    {
        ModuleData local = getModuleData(module);
        LazyValue ret = local.globalVariables.get(name); // most uses would be from local scope anyways
        if (ret != null)
        {
            return ret;
        }
        ModuleData target = local.globalsImports.get(name);
        if (target != null)
        {
            ret = target.globalVariables.get(name);
            if (ret != null)
            {
                return ret;
            }
        }
        // not in local scope - will need to travel over import links
        target = local.futureImports.get(name);
        if (target == null)
        {
            return null;
        }
        target = findModuleDataFromGlobalImports(name, target, 0);
        if (target == null)
        {
            return null;
        }
        local.futureImports.remove(name);
        local.globalsImports.put(name, target);
        return target.globalVariables.get(name);
    }

    private ModuleData findModuleDataFromGlobalImports(String name, ModuleData source, int ttl)
    {
        if (ttl > 64)
        {
            throw new InternalExpressionException("Cannot import " + name + ", either your imports are too deep or too loopy");
        }
        if (source.globalVariables.containsKey(name))
        {
            return source;
        }
        if (source.globalsImports.containsKey(name))
        {
            return findModuleDataFromGlobalImports(name, source.globalsImports.get(name), ttl + 1);
        }
        if (source.futureImports.containsKey(name))
        {
            return findModuleDataFromGlobalImports(name, source.futureImports.get(name), ttl + 1);
        }
        return null;
    }

    public void delFunctionWithPrefix(Module module, String prefix)
    {
        ModuleData data = getModuleData(module);
        data.globalFunctions.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        data.functionImports.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    }

    public void delFunction(Module module, String funName)
    {
        ModuleData data = getModuleData(module);
        data.globalFunctions.remove(funName);
        data.functionImports.remove(funName);
    }

    public void delGlobalVariableWithPrefix(Module module, String prefix)
    {
        ModuleData data = getModuleData(module);
        data.globalVariables.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        data.globalsImports.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    }

    public void delGlobalVariable(Module module, String varName)
    {
        ModuleData data = getModuleData(module);
        data.globalFunctions.remove(varName);
        data.functionImports.remove(varName);
    }

    private ModuleData getModuleData(Module module)
    {
        ModuleData data = moduleData.get(module);
        if (data == null)
        {
            throw new IntegrityException("Module structure changed for the app. Did you reload the app with tasks running?");
        }
        return data;
    }

    protected void assertAppIntegrity(Module module)
    {
        getModuleData(module);
    }

    public void addUserDefinedFunction(Context ctx, Module module, String name, FunctionValue fun)
    {
        getModuleData(module).globalFunctions.put(name, fun);
    }

    public void setGlobalVariable(Module module, String name, LazyValue lv)
    {
        getModuleData(module).globalVariables.put(name, lv);
    }

    public Stream<String> globalVariableNames(Module module, Predicate<String> predicate)
    {
        return Stream.concat(Stream.concat(
                getModuleData(module).globalVariables.keySet().stream(),
                getModuleData(module).globalsImports.keySet().stream()
        ), getModuleData(module).futureImports.keySet().stream().filter(s -> s.startsWith("global_"))).filter(predicate);
    }

    public Stream<String> globalFunctionNames(Module module, Predicate<String> predicate)
    {
        return Stream.concat(Stream.concat(
                getModuleData(module).globalFunctions.keySet().stream(),
                getModuleData(module).functionImports.keySet().stream()
        ), getModuleData(module).futureImports.keySet().stream().filter(s -> !s.startsWith("global_"))).filter(predicate);
    }

    public ScriptHost retrieveForExecution(String /*Nullable*/ user)
    {
        if (!perUser)
        {
            return this;
        }
        ScriptHost oldUserHost = userHosts.get(user);
        if (oldUserHost != null)
        {
            return oldUserHost;
        }
        ScriptHost userHost = this.duplicate();
        userHost.user = user;
        this.setupUserHost(userHost);
        userHosts.put(user, userHost);
        return userHost;
    }

    protected void setupUserHost(ScriptHost host)
    {
        // adding imports
        host.modules.putAll(this.modules);
        this.moduleData.forEach((key, value) -> host.moduleData.put(key, new ModuleData(key, value)));
        // fixing imports
        host.moduleData.forEach((module, data) -> data.setImportsBasedOn(host, this.moduleData.get(data.parent)));
    }

    public synchronized void handleExpressionException(String msg, ExpressionException exc)
    {
        System.out.println(msg + ": " + exc);
    }

    protected abstract ScriptHost duplicate();

    public Object getLock(Value name)
    {
        return locks.computeIfAbsent(name, n -> new Object());
    }

    public ThreadPoolExecutor getExecutor(Value pool)
    {
        if (inTermination)
        {
            return null;
        }
        return executorServices.computeIfAbsent(pool, v -> (ThreadPoolExecutor) Executors.newCachedThreadPool());
    }

    public int taskCount()
    {
        return executorServices.values().stream().map(ThreadPoolExecutor::getActiveCount).reduce(0, Integer::sum);
    }

    public int taskCount(Value pool)
    {
        return executorServices.containsKey(pool) ? executorServices.get(pool).getActiveCount() : 0;
    }

    public void onClose()
    {
        inTermination = true;
        executorServices.values().forEach(ThreadPoolExecutor::shutdown);
        for (ScriptHost uh : userHosts.values())
        {
            uh.onClose();
        }
        if (taskCount() > 0)
        {
            executorServices.values().forEach(e -> {
                ExecutorService stopper = Executors.newSingleThreadExecutor();
                stopper.submit(() -> {
                    try
                    {
                        // Wait a while for existing tasks to terminate
                        if (!e.awaitTermination(1500, TimeUnit.MILLISECONDS))
                        {
                            e.shutdownNow(); // Cancel currently executing tasks
                            // Wait a while for tasks to respond to being cancelled
                            if (!e.awaitTermination(1500, TimeUnit.MILLISECONDS))
                            {
                                CarpetScriptServer.LOG.error("Failed to stop app's thread");
                            }
                        }
                    }
                    catch (InterruptedException ie)
                    {
                        // (Re-)Cancel if current thread also interrupted
                        e.shutdownNow();
                        // Preserve interrupt status
                        Thread.currentThread().interrupt();
                    }
                    stopper.shutdown();
                    stopper.shutdownNow();
                });
            });
        }
    }

    public void setPerPlayer(boolean isPerUser)
    {
        perUser = isPerUser;
    }

    public boolean isPerUser()
    {
        return perUser;
    }

    public Set<String> getUserList()
    {
        return userHosts.keySet();
    }


    public void resetErrorSnooper()
    {
        errorSnooper = null;
    }

    public static final Logger DEPRECATION_LOG = LoggerFactory.getLogger("Scarpet Deprecation Warnings");

    public boolean issueDeprecation(String feature)
    {
        if (deprecations.contains(feature))
        {
            return false;
        }
        deprecations.add(feature);
        DEPRECATION_LOG.warn("App '" + getVisualName() + "' uses '" + feature + "', which is deprecated for removal. Check the docs for a replacement");
        return true;
    }

    public ScriptServer scriptServer()
    {
        return scriptServer;
    }
}
