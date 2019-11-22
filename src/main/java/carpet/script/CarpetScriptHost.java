package carpet.script;

import carpet.CarpetServer;
import carpet.script.bundled.ModuleInterface;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.value.FunctionValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.settings.CarpetSettings;
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
import java.util.List;
import java.util.Locale;

import static java.lang.Math.max;

public class CarpetScriptHost extends ScriptHost
{
    private CarpetScriptServer scriptServer;

    private Tag globalState;
    private int saveTimeout;

    CarpetScriptHost(CarpetScriptServer server, String name, ModuleInterface code, boolean perUser, ScriptHost parent)
    {
        super(name, code, perUser, parent);
        this.saveTimeout = 0;
        this.scriptServer = server;
        if (parent == null && name != null)
            globalState = loadState();
    }

    @Override
    protected ScriptHost duplicate()
    {
        return new CarpetScriptHost(scriptServer, this.getName(), myCode, false, this);
    }

    @Override
    public void addUserDefinedFunction(String funName, FunctionValue function)
    {
        super.addUserDefinedFunction(funName, function);
        // mcarpet
        if (funName.startsWith("__on_"))
        {
            // this is nasty, we have the host and function, yet we add it via names, but hey - works for now
            String event = funName.replaceFirst("__on_","");
            if (CarpetEventServer.Event.byName.containsKey(event))
                scriptServer.events.addEventDirectly(event, this, function);
        }
    }

    @Override
    public void delFunction(String funName)
    {
        super.delFunction(funName);
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
        if (!perUser)
            return this;
        try
        {
            ServerPlayerEntity player = source.getPlayer();
            return (CarpetScriptHost) retrieveForExecution(player.getName().getString());
        }
        catch (CommandSyntaxException e)
        {
            return (CarpetScriptHost) retrieveForExecution((String)null);
        }
    }

    public String call(ServerCommandSource source, String call, List<Integer> coords, String arg)
    {
        if (CarpetServer.scriptServer.stopAll)
            return "SCARPET PAUSED";
        FunctionValue function = globalFunctions.get(call);
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
                    if (globalVariables.containsKey(tok.surface.toLowerCase(Locale.ROOT)))
                    {
                        argv.add(globalVariables.get(tok.surface.toLowerCase(Locale.ROOT)));
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
            return Expression.evalValue(
                    () -> function.lazyEval(context, Context.VOID, function.getExpression(), function.getToken(), argv),
                    context,
                    Context.VOID
            ).getString();
        }
        catch (ExpressionException e)
        {
            return e.getMessage();
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
            return Expression.evalValue(
                    () -> fun.lazyEval(context, Context.VOID, fun.getExpression(), fun.getToken(), argv),
                    context,
                    Context.VOID);
        }
        catch (ExpressionException e)
        {
            CarpetSettings.LOG.error("Callback failed: "+e.getMessage());
        }
        return Value.NULL;
    }

    @Override
    public void onClose()
    {
        super.onClose();
        if (this.saveTimeout > 0)
            dumpState();
        String markerName = ExpressionInspector.MARKER_STRING+"_"+((getName()==null)?"":getName());
        for (ServerWorld world : CarpetServer.minecraft_server.getWorlds())
        {
            for (Entity e : world.getEntities(EntityType.ARMOR_STAND, (as) -> as.getScoreboardTags().contains(markerName)))
            {
                e.remove();
            }
        }
    }

    private void dumpState()
    {
        myCode.saveData(null, globalState);
    }

    private Tag loadState()
    {
        return myCode.getData(null);
    }

    public Tag getGlobalState(String file)
    {
        if (getName() == null || myCode.isInternal()) return null;
        if (file != null)
            myCode.getData(file);
        if (parent == null)
            return globalState;
        return ((CarpetScriptHost)parent).globalState;
    }

    public void setGlobalState(Tag tag, String file)
    {
        if (getName() == null || myCode.isInternal()) return;

        if (file!= null)
        {
            myCode.saveData(file, tag);
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

}
