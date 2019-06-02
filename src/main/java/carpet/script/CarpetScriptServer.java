package carpet.script;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.bundled.CameraPathModule;
import carpet.script.bundled.FileModule;
import carpet.script.bundled.ModuleInterface;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.value.Value;
import carpet.utils.Messenger;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CarpetScriptServer
{
    //make static for now, but will change that later:
    public ScriptHost globalHost;
    public Map<String, ScriptHost> modules;
    long tickStart;
    public boolean stopAll;
    Set<String> holyMoly;
    public CarpetEventServer events;

    public static List<ModuleInterface> bundledModuleData = new ArrayList<ModuleInterface>(){{
        add(new CameraPathModule());
    }};

    public CarpetScriptServer()
    {
        globalHost = createMinecraftScriptHost(null);
        events = new CarpetEventServer();
        modules = new HashMap<>();
        tickStart = 0L;
        stopAll = false;
        resetErrorSnooper();
        holyMoly = CarpetServer.minecraft_server.getCommandManager().getDispatcher().getRoot().getChildren().stream().map(CommandNode::getName).collect(Collectors.toSet());

    }

    ModuleInterface getModule(String name)
    {
        for (ModuleInterface moduleData : bundledModuleData)
        {
            if (moduleData.getName().equalsIgnoreCase(name))
            {
                return moduleData;
            }
        }

        File folder = CarpetServer.minecraft_server.getLevelStorage().resolveFile(
                CarpetServer.minecraft_server.getLevelName(), "scripts");
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return null;
        for (File script : listOfFiles)
        {
            if (script.getName().equalsIgnoreCase(name+".sc"))
            {
                return new FileModule(script);
            }
        }
        return null;
    }

    public List<String> listAvailableModules()
    {
        List<String> moduleNames = new ArrayList<>();
        for (ModuleInterface mi: bundledModuleData)
        {
            if (!modules.containsKey(mi.getName()))
                moduleNames.add(mi.getName());
        }
        File folder = CarpetServer.minecraft_server.getLevelStorage().resolveFile(
                CarpetServer.minecraft_server.getLevelName(), "scripts");
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return moduleNames;
        for (File script : listOfFiles)
        {
            if (script.getName().endsWith(".sc"))
            {
                String name = script.getName().replaceFirst("\\.sc","").toLowerCase(Locale.ROOT);
                if (!modules.containsKey(name))
                    moduleNames.add(name);
            }
        }
        return moduleNames;
    }

    public ScriptHost getHostByName(String name)
    {
        if (name == null)
            return globalHost;
        return modules.get(name);
    }


    private static ScriptHost createMinecraftScriptHost(String name)
    {
        ScriptHost host = new ScriptHost(name);
        host.globalVariables.put("_x", (c, t) -> Value.ZERO);
        host.globalVariables.put("_y", (c, t) -> Value.ZERO);
        host.globalVariables.put("_z", (c, t) -> Value.ZERO);
        return host;
    }

    public boolean addScriptHost(ServerCommandSource source, String name)
    {
        name = name.toLowerCase(Locale.ROOT);
        ScriptHost newHost = createMinecraftScriptHost(name);
        ModuleInterface module = getModule(name);
        if (module == null)
        {
            Messenger.m(source, "r Unable to locate the package, but created empty host "+name+" instead");
            modules.put(name, newHost);
            return true;
        }
        String code = module.getCode();
        if (code == null)
        {
            Messenger.m(source, "r Unable to load the package - not found");
            return false;
        }
        // parse code and convert to expression
        // fill functions etc.
        // possibly add a command
        try
        {
            setChatErrorSnooper(source);
            CarpetExpression ex = new CarpetExpression(module.getCode(), source, new BlockPos(0, 0, 0));
            ex.scriptRunCommand(newHost, new BlockPos(source.getPosition()));
        }
        catch (CarpetExpressionException e)
        {
            Messenger.m(source, "r Exception white evaluating expression at "+new BlockPos(source.getPosition())+": "+e.getMessage());
            resetErrorSnooper();
            return false;
        }
        modules.put(name, newHost);

        addEvents(source, name);

        addCommand(source, name);
        return true;
    }


    public void addEvents(ServerCommandSource source, String hostName)
    {
        ScriptHost host = modules.get(hostName);
        if (host == null)
        {
            return;
        }
        for (String fun : host.globalFunctions.keySet())
        {
            if (!fun.startsWith("__on_"))
                continue;
            String event = fun.replaceFirst("__on_","");
            if (!events.eventHandlers.containsKey(event))
                continue;
            events.addEvent(event, hostName, fun);
        }
    }


    public void addCommand(ServerCommandSource source, String hostName)
    {
        ScriptHost host = modules.get(hostName);
        if (host == null)
        {
            return;
        }
        if (!host.globalFunctions.containsKey("__command"))
        {
            Messenger.m(source, "gi Package "+hostName+" loaded.");
            return;
        }
        if (holyMoly.contains(hostName))
        {
            Messenger.m(source, "gi Package "+hostName+" loaded with no command.");
            Messenger.m(source, "gi Tried to mask vanilla command.");
            return;
        }

        LiteralArgumentBuilder<ServerCommandSource> command = literal(hostName).
                requires((player) -> modules.containsKey(hostName)).
                executes( (c) ->
                {
                    Messenger.m(c.getSource(), "gi "+modules.get(hostName).call(c.getSource(),"__command", null, ""));
                    return 1;
                });

        for (String function : host.getPublicFunctions())
        {
            command = command.
                    then(literal(function).
                            requires((player) -> modules.containsKey(hostName) && modules.get(hostName).getPublicFunctions().contains(function)).
                            executes( (c) -> {
                                Messenger.m(
                                        c.getSource(),
                                        "gi "+modules.get(hostName).call(
                                                c.getSource(),
                                                function,
                                                null,
                                                ""
                                        )
                                );
                                return 1;
                            }).
                            then(argument("args...", StringArgumentType.greedyString()).
                                    executes( (c) -> {
                                        Messenger.m(
                                                c.getSource(),
                                                "gi "+modules.get(hostName).call(
                                                        c.getSource(),
                                                        function,
                                                        null,
                                                        StringArgumentType.getString(c, "args...")
                                                )
                                        );
                                        return 1;
                                    })));
        }
        Messenger.m(source, "gi Package "+hostName+" loaded with /"+hostName+" command");
        CarpetServer.minecraft_server.getCommandManager().getDispatcher().register(command);
        CarpetSettings.notifyPlayersCommandsChanged();
    }

    public void setChatErrorSnooper(ServerCommandSource source)
    {
        ExpressionException.errorSnooper = (expr, token, message) ->
        {
            try
            {
                source.getPlayer();
            }
            catch (CommandSyntaxException e)
            {
                return null;
            }
            String[] lines = expr.getCodeString().split("\n");

            String shebang = message;

            if (lines.length > 1)
            {
                shebang += " at line "+(token.lineno+1)+", pos "+(token.linepos+1);
            }
            else
            {
                shebang += " at pos "+(token.pos+1);
            }
            if (expr.getName() != null)
            {
                shebang += " in "+expr.getName()+"";
            }
            Messenger.m(source, "r "+shebang);

            if (lines.length > 1 && token.lineno > 0)
            {
                Messenger.m(source, "l "+lines[token.lineno-1]);
            }
            Messenger.m(source, "l "+lines[token.lineno].substring(0, token.linepos), "r  HERE>> ", "l "+
                    lines[token.lineno].substring(token.linepos));

            if (lines.length > 1 && token.lineno < lines.length-1)
            {
                Messenger.m(source, "l "+lines[token.lineno+1]);
            }
            return new ArrayList<>();
        };
    }
    public void resetErrorSnooper()
    {
        ExpressionException.errorSnooper=null;
    }

    public boolean removeScriptHost(ServerCommandSource source, String name)
    {
        name = name.toLowerCase(Locale.ROOT);
        if (!modules.containsKey(name))
        {
            Messenger.m(source, "r No such host found: ", "wb  " + name);
            return false;
        }
        // stop all events associated with name
        modules.remove(name);
        CarpetSettings.notifyPlayersCommandsChanged();
        Messenger.m(source, "w Removed host "+name);
        return true;
    }

    public boolean runas(ServerCommandSource source, String hostname, String udf_name, List<LazyValue> argv)
    {
        return runas(BlockPos.ORIGIN, source, hostname, udf_name, argv);
    }

    public boolean runas(BlockPos origin, ServerCommandSource source, String hostname, String udf_name, List<LazyValue> argv)
    {
        ScriptHost host = globalHost;
        if (hostname != null)
            host = modules.get(hostname);
        try
        {
            host.callUDF(origin, source, host.globalFunctions.get(udf_name), argv);
        }
        catch (NullPointerException | InvalidCallbackException npe)
        {
            return false;
        }
        return true;
    }
}
