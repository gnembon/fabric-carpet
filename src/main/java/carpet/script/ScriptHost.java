package carpet.script;

import carpet.CarpetServer;
import carpet.script.bundled.ModuleInterface;
import carpet.settings.CarpetSettings;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.max;

public class ScriptHost
{
    public final Map<String, ScriptHost> userHosts = new HashMap<>();
    public Map<String, UserDefinedFunction> globalFunctions = new HashMap<>();
    public Map<String, LazyValue> globalVariables = new HashMap<>();

    private String name;
    public String getName() {return name;}

    private final ModuleInterface myCode;
    public final boolean perUser;

    ScriptHost(String name, ModuleInterface code, boolean perUser)
    {
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

    public ScriptHost retrieveForExecution(String /*Nullable*/ user)
    {
        if (!perUser)
            return this;
        ScriptHost userHost = userHosts.get(user);
        if (userHost != null)
            return userHost;
        userHost = new ScriptHost(this.name, myCode, false);
        userHost.globalVariables.putAll(this.globalVariables);
        userHost.globalFunctions.putAll(this.globalFunctions);
        userHosts.put(user, userHost);
        return userHost;
    }

    public ScriptHost retrieveForExecution(ServerCommandSource source)
    {
        if (!perUser)
            return this;
        try
        {
            ServerPlayerEntity player = source.getPlayer();
            return retrieveForExecution(player.getName().getString());
        }
        catch (CommandSyntaxException e)
        {
            return retrieveForExecution((String)null);
        }
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
    public List<String> getAvailableFunctions()
    {
        return globalFunctions.keySet().stream().filter((str) -> !str.startsWith("__")).collect(Collectors.toList());
    }

    public String call(ServerCommandSource source, String call, List<Integer> coords, String arg)
    {
        if (CarpetServer.scriptServer.stopAll)
            return "SCRIPTING PAUSED";
        UserDefinedFunction acf = globalFunctions.get(call);
        if (acf == null)
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
        List<String> args = acf.getArguments();
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
                    () -> acf.lazyEval(context, Context.VOID, acf.expression, acf.token, argv),
                    context,
                    Context.VOID
            ).getString();
        }
        catch (ExpressionException e)
        {
            return e.getMessage();
        }
    }

    public void callUDF(BlockPos pos, ServerCommandSource source, UserDefinedFunction acf, List<LazyValue> argv) throws InvalidCallbackException
    {
        if (CarpetServer.scriptServer.stopAll)
            return;

        List<String> args = acf.getArguments();
        if (argv.size() != args.size())
        {
            throw new InvalidCallbackException();
        }
        try
        {
            // TODO: this is just for now - invoke would be able to invoke other hosts scripts
            Context context = new CarpetContext(this, source, pos);
            Expression.evalValue(
                    () -> acf.lazyEval(context, Context.VOID, acf.expression, acf.token, argv),
                    context,
                    Context.VOID);
        }
        catch (ExpressionException e)
        {
            CarpetSettings.LOG.error("Callback failed: "+e.getMessage());
        }
    }


}
