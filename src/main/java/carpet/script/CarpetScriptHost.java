package carpet.script;

import carpet.CarpetServer;
import carpet.script.bundled.Module;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.value.FunctionValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.Tag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.max;

public class CarpetScriptHost extends ScriptHost
{
    private final CarpetScriptServer scriptServer;
    ServerCommandSource responsibleSource;

    private Tag globalState;
    private int saveTimeout;

    private CarpetScriptHost(CarpetScriptServer server, Module code, boolean perUser, ScriptHost parent)
    {
        super(code, perUser, parent);
        this.saveTimeout = 0;
        this.scriptServer = server;
        if (parent == null && code != null)
            globalState = loadState();
    }

    public static CarpetScriptHost create(CarpetScriptServer scriptServer, Module module, boolean perPlayer, ServerCommandSource source)
    {
        CarpetScriptHost host = new CarpetScriptHost(scriptServer, module, perPlayer, null );
        // parse code and convert to expression
        if (module != null)
        {
            try
            {
                String code = module.getCode();
                if (code == null)
                {
                    Messenger.m(source, "r Unable to load "+module.getName()+" app - code not found");
                    return null;
                }
                host.setChatErrorSnooper(source);
                CarpetExpression ex = new CarpetExpression(host.main, code, source, new BlockPos(0, 0, 0));
                ex.getExpr().asATextSource();
                ex.scriptRunCommand(host, new BlockPos(source.getPosition()));
            }
            catch (CarpetExpressionException e)
            {
                host.handleErrorWithStack("Error while evaluating expression", e);
                host.resetErrorSnooper();
                return null;
            }
        }
        return host;
    }

    @Override
    protected ScriptHost duplicate()
    {
        return new CarpetScriptHost(scriptServer, main, false, this);
    }

    @Override
    public void addUserDefinedFunction(Module module, String funName, FunctionValue function)
    {
        super.addUserDefinedFunction(module, funName, function);
        // mcarpet
        if (funName.startsWith("__on_")) // here we can make a determination if we want to only accept events from main module.
        {
            // this is nasty, we have the host and function, yet we add it via names, but hey - works for now
            String event = funName.replaceFirst("__on_","");
            if (CarpetEventServer.Event.byName.containsKey(event))
                scriptServer.events.addEventDirectly(event, this, function);
        }
    }

    @Override
    protected Module getModuleOrLibraryByName(String name)
    {
        Module module = scriptServer.getModule(name, true);
        if (module == null || module.getCode() == null)
            throw new InternalExpressionException("Unable to locate package: "+name);
        return module;
    }

    @Override
    protected void runModuleCode(Context c, Module module)
    {
        CarpetContext cc = (CarpetContext)c;
        CarpetExpression ex = new CarpetExpression(module, module.getCode(), cc.s, cc.origin);
        ex.getExpr().asATextSource();
        ex.scriptRunCommand(this, cc.origin);
    }

    @Override
    public void delFunction(Module module, String funName)
    {
        super.delFunction(module, funName);
        // mcarpet
        if (funName.startsWith("__on_"))
        {
            // this is nasty, we have the host and function, yet we add it via names, but hey - works for now
            String event = funName.replaceFirst("__on_","");
            if (CarpetEventServer.Event.byName.containsKey(event))
                scriptServer.events.removeEventDirectly(event, this, funName);
        }
    }

    public CarpetScriptHost retrieveForExecution(ServerCommandSource source)
    {
        CarpetScriptHost host = this;
        if (perUser)
        {
            try
            {
                ServerPlayerEntity player = source.getPlayer();
                host = (CarpetScriptHost) retrieveForExecution(player.getName().getString());
            }
            catch (CommandSyntaxException e)
            {
                host = (CarpetScriptHost)  retrieveForExecution((String) null);
            }
        }
        if (host.errorSnooper == null) host.setChatErrorSnooper(source);
        return host;
    }

    public String handleCommand(ServerCommandSource source, String call, List<Integer> coords, String arg)
    {
        try
        {
            return call(source, call, coords, arg);
        }
        catch (CarpetExpressionException exc)
        {
            handleErrorWithStack("Error while running custom command", exc);
            return "";
        }
    }

    public String call(ServerCommandSource source, String call, List<Integer> coords, String arg)
    {
        if (CarpetServer.scriptServer.stopAll)
            return "SCARPET PAUSED";
        FunctionValue function = getFunction(call);
        if (function == null)
            return "UNDEFINED";
        List<LazyValue> argv = new ArrayList<>();
        if (coords != null)
            for (Integer i: coords)
                argv.add( (c, t) -> new NumericValue(i));
        String sign = "";
        for (Tokenizer.Token tok : Tokenizer.simplepass(arg))
        {
            switch (tok.type)
            {
                case VARIABLE:
                    LazyValue var = getGlobalVariable(tok.surface.toLowerCase(Locale.ROOT));
                    if (var != null)
                    {
                        argv.add(var);
                        break;
                    }
                case STRINGPARAM:
                    argv.add((c, t) -> new StringValue(tok.surface));
                    sign = "";
                    break;

                case LITERAL:
                    try
                    {
                        String finalSign = sign;
                        argv.add((c, t) ->new NumericValue(finalSign+tok.surface));
                        sign = "";
                    }
                    catch (NumberFormatException exception)
                    {
                        return "Fail: "+sign+tok.surface+" seems like a number but it is not a number. Use quotes to ensure its a string";
                    }
                    break;
                case HEX_LITERAL:
                    try
                    {
                        String finalSign = sign;
                        argv.add((c, t) -> new NumericValue(new BigInteger(finalSign+tok.surface.substring(2), 16).doubleValue()));
                        sign = "";
                    }
                    catch (NumberFormatException exception)
                    {
                        return "Fail: "+sign+tok.surface+" seems like a number but it is not a number. Use quotes to ensure its a string";
                    }
                    break;
                case OPERATOR:
                case UNARY_OPERATOR:
                    if ((tok.surface.equals("-") || tok.surface.equals("-u")) && sign.isEmpty())
                    {
                        sign = "-";
                    }
                    else
                    {
                        return "Fail: operators, like " + tok.surface + " are not allowed in invoke";
                    }
                    break;
                case FUNCTION:
                    return "Fail: passing functions like "+tok.surface+"() to invoke is not allowed";
                case OPEN_PAREN:
                case COMMA:
                case CLOSE_PAREN:
                case MARKER:
                    return "Fail: "+tok.surface+" is not allowed in invoke";
            }
        }
        List<String> args = function.getArguments();
        if (argv.size() != args.size())
        {
            String error = "Fail: stored function "+call+" takes "+args.size()+" arguments, not "+argv.size()+ ":\n";
            for (int i = 0; i < max(argv.size(), args.size()); i++)
            {
                error += (i<args.size()?args.get(i):"??")+" => "+(i<argv.size()?argv.get(i).evalValue(null).getString():"??")+"\n";
            }
            return error;
        }
        try
        {
            // TODO: this is just for now - invoke would be able to invoke other hosts scripts
            Context context = new CarpetContext(this, source, BlockPos.ORIGIN);
            return function.getExpression().evalValue(
                    () -> function.lazyEval(context, Context.VOID, function.getExpression(), function.getToken(), argv),
                    context,
                    Context.VOID
            ).getString();
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage(), e.stack);
        }
    }

    public Value callUDF(BlockPos pos, ServerCommandSource source, FunctionValue fun, List<LazyValue> argv) throws InvalidCallbackException
    {
        if (CarpetServer.scriptServer.stopAll)
            return Value.NULL;
        if (argv.size() != fun.getArguments().size())
        {
            throw new InvalidCallbackException();
        }
        try
        {
            // TODO: this is just for now - invoke would be able to invoke other hosts scripts
            Context context = new CarpetContext(this, source, pos);
            return fun.getExpression().evalValue(
                    () -> fun.lazyEval(context, Context.VOID, fun.getExpression(), fun.getToken(), argv),
                    context,
                    Context.VOID);
        }
        catch (ExpressionException e)
        {
            handleExpressionException("Callback failed", e);
        }
        return Value.NULL;
    }

    @Override
    public void onClose()
    {
        FunctionValue closing = getFunction("__on_close");
        if (closing != null)
        {
            try
            {
                callUDF(BlockPos.ORIGIN, CarpetServer.minecraft_server.getCommandSource(), closing, Collections.emptyList());
            }
            catch (InvalidCallbackException ignored)
            {
            }
        }
        userHosts.forEach((key, value) ->
        {
            FunctionValue userClosing = value.getFunction("__on_close");
            if (userClosing != null)
            {
                ServerPlayerEntity player = CarpetServer.minecraft_server.getPlayerManager().getPlayer(key);
                ServerCommandSource source = (player != null)?player.getCommandSource():CarpetServer.minecraft_server.getCommandSource();
                try
                {
                    ((CarpetScriptHost) value).callUDF(BlockPos.ORIGIN, source, userClosing, Collections.emptyList());
                }
                catch (InvalidCallbackException ignored)
                {
                }
            }
        });

        String markerName = ExpressionInspector.MARKER_STRING+"_"+((getName()==null)?"":getName());
        for (ServerWorld world : CarpetServer.minecraft_server.getWorlds())
        {
            for (Entity e : world.getEntities(EntityType.ARMOR_STAND, (as) -> as.getScoreboardTags().contains(markerName)))
            {
                e.remove();
            }
        }
        if (this.saveTimeout > 0)
            dumpState();
        super.onClose();
    }

    private void dumpState()
    {
        main.saveData(null, globalState);
    }

    private Tag loadState()
    {
        return main.getData(null);
    }

    public Tag getGlobalState(String file)
    {
        if (getName() == null ) return null;
        if (file != null)
            return main.getData(file);
        if (parent == null)
            return globalState;
        return ((CarpetScriptHost)parent).globalState;
    }

    public void setGlobalState(Tag tag, String file)
    {
        if (getName() == null ) return;

        if (file!= null)
        {
            main.saveData(file, tag);
            return;
        }

        CarpetScriptHost responsibleHost = (parent != null)?(CarpetScriptHost) parent:this;
        responsibleHost.globalState = tag;
        if (responsibleHost.saveTimeout == 0)
        {
            responsibleHost.dumpState();
            responsibleHost.saveTimeout = 200;
        }
    }

    public void tick()
    {
        if (this.saveTimeout > 0)
        {
            this.saveTimeout --;
            if (this.saveTimeout == 0)
            {
                dumpState();
            }
        }
    }

    public void setChatErrorSnooper(ServerCommandSource source)
    {
        responsibleSource = source;
        errorSnooper = (expr, /*Nullable*/ token, message) ->
        {
            try
            {
                source.getPlayer();
            }
            catch (CommandSyntaxException e)
            {
                return null;
            }

            String shebang = message;
            if (expr.module != null)
            {
                shebang += " in " + expr.module.getName() + "";
            }
            else
            {
                shebang += " in system chat";
            }
            if (token != null)
            {
                String[] lines = expr.getCodeString().split("\n");

                if (lines.length > 1)
                {
                    shebang += " at line " + (token.lineno + 1) + ", pos " + (token.linepos + 1);
                }
                else
                {
                    shebang += " at pos " + (token.pos + 1);
                }
                Messenger.m(source, "r " + shebang);
                if (lines.length > 1 && token.lineno > 0)
                {
                    Messenger.m(source, "l " + lines[token.lineno - 1]);
                }
                Messenger.m(source, "l " + lines[token.lineno].substring(0, token.linepos), "r  HERE>> ", "l " +
                        lines[token.lineno].substring(token.linepos));
                if (lines.length > 1 && token.lineno < lines.length - 1)
                {
                    Messenger.m(source, "l " + lines[token.lineno + 1]);
                }
            }
            else
            {
                Messenger.m(source, "r " + shebang);
            }
            return new ArrayList<>();
        };
    }

    @Override
    public void resetErrorSnooper()
    {
        responsibleSource = null;
        super.resetErrorSnooper();
    }

    public void handleErrorWithStack(String intro, CarpetExpressionException exception)
    {
        if (responsibleSource != null)
        {
            exception.printStack(responsibleSource);
            String message = exception.getMessage();
            Messenger.m(responsibleSource, "r "+intro+(message.isEmpty()?"":": "+message));
        }
        else
        {
            CarpetSettings.LOG.error(intro+": "+exception.getMessage());
        }
    }

    @Override
    public void handleExpressionException(String message, ExpressionException exc)
    {
        handleErrorWithStack(message, new CarpetExpressionException(exc.getMessage(), exc.stack));
    }
}
