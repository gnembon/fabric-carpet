package carpet.commands;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.CarpetScriptServer;
import carpet.script.utils.AppStoreManager;
import carpet.script.CarpetEventServer;
import carpet.script.CarpetExpression;
import carpet.script.CarpetScriptHost;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.ScriptHost;
import carpet.script.Tokenizer;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import carpet.settings.SettingsManager;
import carpet.utils.CarpetProfiler;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.command.CommandSource.suggestMatching;

public class ScriptCommand
{
    private static final TreeSet<String> scarpetFunctions;
    private static final TreeSet<String> APIFunctions;
    static
    {
        Set<String> allFunctions = (new CarpetExpression(null, "null", null, null)).getExpr().getFunctionNames();
        scarpetFunctions = new TreeSet<>(Expression.none.getFunctionNames());
        APIFunctions = allFunctions.stream().filter(s -> !scarpetFunctions.contains(s)).collect(Collectors.toCollection(TreeSet::new));
    }

    public static List<String> suggestFunctions(ScriptHost host, String previous, String prefix)
    {
        previous = previous.replace("\\'", "");
        int quoteCount = StringUtils.countMatches(previous,'\'');
        if (quoteCount % 2 == 1)
            return Collections.emptyList();
        int maxLen = prefix.length()<3 ? (prefix.length()*2+1) : 1234;
        String eventPrefix = prefix.startsWith("__on_")?prefix.substring(5):null;
        List<String> scarpetMatches = scarpetFunctions.stream().
                filter(s -> s.startsWith(prefix) && s.length() <= maxLen).map(s -> s+"(").collect(Collectors.toList());
        scarpetMatches.addAll(APIFunctions.stream().
                filter(s -> s.startsWith(prefix) && s.length() <= maxLen).map(s -> s+"(").collect(Collectors.toList()));
        // not that useful in commandline, more so in external scripts, so skipping here
        if (eventPrefix != null) scarpetMatches.addAll(CarpetEventServer.Event.publicEvents(null).stream().
                filter(e -> e.name.startsWith(eventPrefix)).map(s -> "__on_"+s.name+"(").collect(Collectors.toList()));
        scarpetMatches.addAll(host.globalFunctionNames(host.main, s -> s.startsWith(prefix)).map(s -> s+"(").collect(Collectors.toList()));
        scarpetMatches.addAll(host.globalVariableNames(host.main, s -> s.startsWith(prefix)).collect(Collectors.toList()));
        return scarpetMatches;
    }

    private static CompletableFuture<Suggestions> suggestCode(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder suggestionsBuilder
    ) throws CommandSyntaxException
    {
        CarpetScriptHost currentHost = getHost(context);
        String previous = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
        int strlen = previous.length();
        StringBuilder lastToken = new StringBuilder();
        for (int idx = strlen-1; idx >=0; idx--)
        {
            char ch = previous.charAt(idx);
            if (Character.isLetterOrDigit(ch) || ch == '_') lastToken.append(ch); else break;
        }
        if (lastToken.length() == 0)
            return suggestionsBuilder.buildFuture();
        String prefix = lastToken.reverse().toString();
        String previousString =  previous.substring(0,previous.length()-prefix.length()) ;
        suggestFunctions(currentHost, previousString, prefix).forEach(text -> suggestionsBuilder.suggest(previousString+text));
        return suggestionsBuilder.buildFuture();
    }

    /**
     * A method to suggest the available scarpet scripts based off of the current player input and {@link AppStoreManager#appStoreRoot}
     * variable.
     */
    private static CompletableFuture<Suggestions> suggestDownloadableApps(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder suggestionsBuilder
    ) throws CommandSyntaxException {
        try {
            String previous = suggestionsBuilder.getRemaining();
            AppStoreManager.suggestionsFromPath(previous).forEach(suggestionsBuilder::suggest);
            return suggestionsBuilder.buildFuture();
        }
        catch (IOException e)
        {
            throw new SimpleCommandExceptionType(Messenger.c("rb "+e.getMessage())).create();
        }
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> b = literal("globals").
                executes(context -> listGlobals(context, false)).
                then(literal("all").executes(context -> listGlobals(context, true)));
        LiteralArgumentBuilder<ServerCommandSource> o = literal("stop").
                executes( (cc) -> { CarpetServer.scriptServer.stopAll = true; return 1;});
        LiteralArgumentBuilder<ServerCommandSource> u = literal("resume").
                executes( (cc) -> { CarpetServer.scriptServer.stopAll = false; return 1;});
        LiteralArgumentBuilder<ServerCommandSource> l = literal("run").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandScriptACE)).
                then(argument("expr", StringArgumentType.greedyString()).suggests(ScriptCommand::suggestCode).
                        executes((cc) -> compute(
                                cc,
                                StringArgumentType.getString(cc, "expr"))));
        LiteralArgumentBuilder<ServerCommandSource> s = literal("invoke").
                then(argument("call", StringArgumentType.word()).suggests( (cc, bb)->suggestMatching(suggestFunctionCalls(cc),bb)).
                        executes( (cc) -> invoke(
                                cc,
                                StringArgumentType.getString(cc, "call"),
                                null,
                                null,
                                ""
                        ))  .
                        then(argument("arguments", StringArgumentType.greedyString()).
                                executes( (cc) -> invoke(
                                        cc,
                                        StringArgumentType.getString(cc, "call"),
                                        null,
                                        null,
                                        StringArgumentType.getString(cc, "arguments")
                                ))));
        LiteralArgumentBuilder<ServerCommandSource> c = literal("invokepoint").
                then(argument("call", StringArgumentType.word()).suggests( (cc, bb)->suggestMatching(suggestFunctionCalls(cc),bb)).
                        then(argument("origin", BlockPosArgumentType.blockPos()).
                                executes( (cc) -> invoke(
                                        cc,
                                        StringArgumentType.getString(cc, "call"),
                                        BlockPosArgumentType.getBlockPos(cc, "origin"),
                                        null,
                                        ""
                                )).
                                then(argument("arguments", StringArgumentType.greedyString()).
                                        executes( (cc) -> invoke(
                                                cc,
                                                StringArgumentType.getString(cc, "call"),
                                                BlockPosArgumentType.getBlockPos(cc, "origin"),
                                                null,
                                                StringArgumentType.getString(cc, "arguments")
                                        )))));
        LiteralArgumentBuilder<ServerCommandSource> h = literal("invokearea").
                then(argument("call", StringArgumentType.word()).suggests( (cc, bb)->suggestMatching(suggestFunctionCalls(cc),bb)).
                        then(argument("from", BlockPosArgumentType.blockPos()).
                                then(argument("to", BlockPosArgumentType.blockPos()).
                                        executes( (cc) -> invoke(
                                                cc,
                                                StringArgumentType.getString(cc, "call"),
                                                BlockPosArgumentType.getBlockPos(cc, "from"),
                                                BlockPosArgumentType.getBlockPos(cc, "to"),
                                                ""
                                        )).
                                        then(argument("arguments", StringArgumentType.greedyString()).
                                                executes( (cc) -> invoke(
                                                        cc,
                                                        StringArgumentType.getString(cc, "call"),
                                                        BlockPosArgumentType.getBlockPos(cc, "from"),
                                                        BlockPosArgumentType.getBlockPos(cc, "to"),
                                                        StringArgumentType.getString(cc, "arguments")
                                                ))))));
        LiteralArgumentBuilder<ServerCommandSource> i = literal("scan").requires((player) -> player.hasPermissionLevel(2)).
                then(argument("origin", BlockPosArgumentType.blockPos()).
                        then(argument("from", BlockPosArgumentType.blockPos()).
                                then(argument("to", BlockPosArgumentType.blockPos()).
                                        then(argument("expr", StringArgumentType.greedyString()).
                                                suggests(ScriptCommand::suggestCode).
                                                executes( (cc) -> scriptScan(
                                                        cc,
                                                        BlockPosArgumentType.getBlockPos(cc, "origin"),
                                                        BlockPosArgumentType.getBlockPos(cc, "from"),
                                                        BlockPosArgumentType.getBlockPos(cc, "to"),
                                                        StringArgumentType.getString(cc, "expr")
                                                ))))));
        LiteralArgumentBuilder<ServerCommandSource> e = literal("fill").requires((player) -> player.hasPermissionLevel(2)).
                then(argument("origin", BlockPosArgumentType.blockPos()).
                        then(argument("from", BlockPosArgumentType.blockPos()).
                                then(argument("to", BlockPosArgumentType.blockPos()).
                                        then(argument("expr", StringArgumentType.string()).
                                                then(argument("block", BlockStateArgumentType.blockState()).
                                                        executes((cc) -> scriptFill(
                                                                cc,
                                                                BlockPosArgumentType.getBlockPos(cc, "origin"),
                                                                BlockPosArgumentType.getBlockPos(cc, "from"),
                                                                BlockPosArgumentType.getBlockPos(cc, "to"),
                                                                StringArgumentType.getString(cc, "expr"),
                                                                BlockStateArgumentType.getBlockState(cc, "block"),
                                                                null, "solid"
                                                        )).
                                                        then(literal("replace").
                                                                then(argument("filter", BlockPredicateArgumentType.blockPredicate())
                                                                        .executes((cc) -> scriptFill(
                                                                                cc,
                                                                                BlockPosArgumentType.getBlockPos(cc, "origin"),
                                                                                BlockPosArgumentType.getBlockPos(cc, "from"),
                                                                                BlockPosArgumentType.getBlockPos(cc, "to"),
                                                                                StringArgumentType.getString(cc, "expr"),
                                                                                BlockStateArgumentType.getBlockState(cc, "block"),
                                                                                BlockPredicateArgumentType.getBlockPredicate(cc, "filter"),
                                                                                "solid"
                                                                        )))))))));
        LiteralArgumentBuilder<ServerCommandSource> t = literal("outline").requires((player) -> player.hasPermissionLevel(2)).
                then(argument("origin", BlockPosArgumentType.blockPos()).
                        then(argument("from", BlockPosArgumentType.blockPos()).
                                then(argument("to", BlockPosArgumentType.blockPos()).
                                        then(argument("expr", StringArgumentType.string()).
                                                then(argument("block", BlockStateArgumentType.blockState()).
                                                        executes((cc) -> scriptFill(
                                                                cc,
                                                                BlockPosArgumentType.getBlockPos(cc, "origin"),
                                                                BlockPosArgumentType.getBlockPos(cc, "from"),
                                                                BlockPosArgumentType.getBlockPos(cc, "to"),
                                                                StringArgumentType.getString(cc, "expr"),
                                                                BlockStateArgumentType.getBlockState(cc, "block"),
                                                                null, "outline"
                                                        )).
                                                        then(literal("replace").
                                                                then(argument("filter", BlockPredicateArgumentType.blockPredicate())
                                                                        .executes((cc) -> scriptFill(
                                                                                cc,
                                                                                BlockPosArgumentType.getBlockPos(cc, "origin"),
                                                                                BlockPosArgumentType.getBlockPos(cc, "from"),
                                                                                BlockPosArgumentType.getBlockPos(cc, "to"),
                                                                                StringArgumentType.getString(cc, "expr"),
                                                                                BlockStateArgumentType.getBlockState(cc, "block"),
                                                                                BlockPredicateArgumentType.getBlockPredicate(cc, "filter"),
                                                                                "outline"
                                                                        )))))))));
        LiteralArgumentBuilder<ServerCommandSource> a = literal("load").requires( (player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandScriptACE) ).
                then(argument("app", StringArgumentType.word()).
                        suggests( (cc, bb) -> suggestMatching(CarpetServer.scriptServer.listAvailableModules(true),bb)).
                        executes((cc) ->
                        {
                            boolean success = CarpetServer.scriptServer.addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, true, false, false, null);
                            return success?1:0;
                        }).
                        then(literal("global").
                                executes((cc) ->
                                {
                                    boolean success = CarpetServer.scriptServer.addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, false, false, false, null);
                                    return success?1:0;
                                }
                                )
                        )
                );
        LiteralArgumentBuilder<ServerCommandSource> f = literal("unload").requires( (player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandScriptACE) ).
                then(argument("app", StringArgumentType.word()).
                        suggests( (cc, bb) -> suggestMatching(CarpetServer.scriptServer.unloadableModules,bb)).
                        executes((cc) ->
                        {
                            boolean success =CarpetServer.scriptServer.removeScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), true, false);
                            return success?1:0;
                        }));

        LiteralArgumentBuilder<ServerCommandSource> q = literal("event").requires( (player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandScriptACE) ).
                executes( (cc) -> listEvents(cc.getSource())).
                then(literal("add_to").
                        then(argument("event", StringArgumentType.word()).
                                suggests( (cc, bb) -> suggestMatching(CarpetEventServer.Event.publicEvents(CarpetServer.scriptServer).stream().map(ev -> ev.name).collect(Collectors.toList()),bb)).
                                then(argument("call", StringArgumentType.word()).
                                        suggests( (cc, bb) -> suggestMatching(suggestFunctionCalls(cc), bb)).
                                        executes( (cc) -> CarpetServer.scriptServer.events.addEventFromCommand(
                                                cc.getSource(),
                                                StringArgumentType.getString(cc, "event"),
                                                null,
                                                StringArgumentType.getString(cc, "call")
                                        )?1:0)).
                                then(literal("from").
                                        then(argument("app", StringArgumentType.word()).
                                                suggests( (cc, bb) -> suggestMatching(CarpetServer.scriptServer.modules.keySet(), bb)).
                                                then(argument("call", StringArgumentType.word()).
                                                        suggests( (cc, bb) -> suggestMatching(suggestFunctionCalls(cc), bb)).
                                                        executes( (cc) -> CarpetServer.scriptServer.events.addEventFromCommand(
                                                                cc.getSource(),
                                                                StringArgumentType.getString(cc, "event"),
                                                                StringArgumentType.getString(cc, "app"),
                                                                StringArgumentType.getString(cc, "call")
                                                        )?1:0)))))).
                then(literal("remove_from").
                        then(argument("event", StringArgumentType.word()).
                                suggests( (cc, bb) -> suggestMatching(CarpetEventServer.Event.publicEvents(CarpetServer.scriptServer).stream().filter(CarpetEventServer.Event::isNeeded).map(ev -> ev.name).collect(Collectors.toList()) ,bb)).
                                then(argument("call", StringArgumentType.greedyString()).
                                        suggests( (cc, bb) -> suggestMatching(CarpetEventServer.Event.getEvent(StringArgumentType.getString(cc, "event"), CarpetServer.scriptServer).handler.callList.stream().map(CarpetEventServer.Callback::toString), bb)).
                                        executes( (cc) -> CarpetServer.scriptServer.events.removeEventFromCommand(
                                                cc.getSource(),
                                                StringArgumentType.getString(cc, "event"),
                                                StringArgumentType.getString(cc, "call")
                                        )?1:0))));

        LiteralArgumentBuilder<ServerCommandSource> d = literal("download").requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandScriptACE)).
                then(argument("path", StringArgumentType.greedyString()).
                        suggests(ScriptCommand::suggestDownloadableApps).
                        executes(cc-> AppStoreManager.downloadScript(cc.getSource(), StringArgumentType.getString(cc,"path"))));
        LiteralArgumentBuilder<ServerCommandSource> r = literal("remove").requires( (player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandScriptACE) ).
                then(argument("app", StringArgumentType.word()).
                        suggests( (cc, bb) -> suggestMatching(CarpetServer.scriptServer.unloadableModules,bb)).
                        executes((cc) ->
                        {
                            boolean success =CarpetServer.scriptServer.uninstallApp(cc.getSource(), StringArgumentType.getString(cc, "app"));
                            return success?1:0;
                        }));

        dispatcher.register(literal("script").
                requires((player) ->  SettingsManager.canUseCommand(player, CarpetSettings.commandScript)).
                then(b).then(u).then(o).then(l).then(s).then(c).then(h).then(i).then(e).then(t).then(a).then(f).then(q).then(d).then(r));
        dispatcher.register(literal("script").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandScript)).
                then(literal("in").
                        then(argument("app", StringArgumentType.word()).
                                suggests( (cc, bb) -> suggestMatching(CarpetServer.scriptServer.modules.keySet(), bb)).
                                then(b).then(u).then(o).then(l).then(s).then(c).then(h).then(i).then(e).then(t))));
    }
    private static CarpetScriptHost getHost(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        CarpetScriptHost host;
        try
        {
            String name = StringArgumentType.getString(context, "app").toLowerCase(Locale.ROOT);
            CarpetScriptHost parentHost = CarpetServer.scriptServer.modules.getOrDefault(name, CarpetServer.scriptServer.globalHost);
            host =  parentHost.retrieveOwnForExecution(context.getSource());
        }
        catch (IllegalArgumentException ignored)
        {
            host =  CarpetServer.scriptServer.globalHost;
        }
        host.setChatErrorSnooper(context.getSource());
        return host;
    }
    private static Collection<String> suggestFunctionCalls(CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        CarpetScriptHost host = getHost(c);
        return host.globalFunctionNames(host.main, s ->  !s.startsWith("_")).sorted().collect(Collectors.toList());
    }
    private static int listEvents(ServerCommandSource source)
    {
        Messenger.m(source, "w Lists ALL event handlers:");
        for (CarpetEventServer.Event event: CarpetEventServer.Event.getAllEvents(CarpetServer.scriptServer, null))
        {
            boolean shownEvent = false;
            for (CarpetEventServer.Callback c: event.handler.callList)
            {
                if (!shownEvent)
                {
                    Messenger.m(source, "w Handlers for "+event.name+": ");
                    shownEvent = true;
                }
                Messenger.m(source, "w  - "+c.function.getString() +(c.host==null?"":" (from "+c.host+")"));
            }
        }
        return 1;
    }
    private static int listGlobals(CommandContext<ServerCommandSource> context, boolean all) throws CommandSyntaxException
    {
        CarpetScriptHost host = getHost(context);
        ServerCommandSource source = context.getSource();

        Messenger.m(source, "lb Stored functions"+((host == CarpetServer.scriptServer.globalHost)?":":" in "+host.getName()+":"));
        host.globalFunctionNames(host.main, (str) -> all || !str.startsWith("__")).sorted().forEach( (s) -> {
            FunctionValue fun = host.getFunction(s);
            if (fun == null)
            {
                Messenger.m(source, "gb "+s, "g  - unused import");
                Messenger.m(source, "gi ----------------");
                return;
            }
            Expression expr = fun.getExpression();
            Tokenizer.Token tok = fun.getToken();
            List<String> snippet = expr.getExpressionSnippet(tok);
            Messenger.m(source, "wb "+fun.fullName(),"t  defined at: line "+(tok.lineno+1)+" pos "+(tok.linepos+1));
            for (String snippetLine: snippet)
            {
                Messenger.m(source, "w "+snippetLine);
            }
            Messenger.m(source, "gi ----------------");
        });
        //Messenger.m(source, "w "+code);
        Messenger.m(source, "w  ");
        Messenger.m(source, "lb Global variables"+((host == CarpetServer.scriptServer.globalHost)?":":" in "+host.getName()+":"));
        host.globalVariableNames(host.main, (s) -> s.startsWith("global_")).sorted().forEach( (s) -> {
            LazyValue variable = host.getGlobalVariable(s);
            if (variable == null)
            {
                Messenger.m(source, "gb "+s, "g  - unused import");
            }
            else
            {
                Messenger.m(source, "wb "+s+": ", "w "+ variable.evalValue(null).getPrettyString());
            }
        });
        return 1;
    }

    public static int handleCall(ServerCommandSource source, CarpetScriptHost host, Supplier<Value> call)
    {
        try
        {
            CarpetProfiler.ProfilerToken currentSection = CarpetProfiler.start_section(null, "Scarpet run", CarpetProfiler.TYPE.GENERAL);
            host.setChatErrorSnooper(source);
            long start = System.nanoTime();
            Value result = call.get();
            long time = ((System.nanoTime()-start)/1000);
            String metric = "\u00B5s";
            if (time > 5000)
            {
                time /= 1000;
                metric = "ms";
            }
            if (time > 10000)
            {
                time /= 1000;
                metric = "s";
            }
            Messenger.m(source, "wi  = ", "wb "+result.getString(), "gi  ("+time+metric+")");
            int intres = (int)result.readInteger();
            CarpetProfiler.end_current_section(currentSection);
            return intres;
        }
        catch (CarpetExpressionException e)
        {
            host.handleErrorWithStack("Error while evaluating expression", e);
        }
        catch (ArithmeticException ae)
        {
            host.handleErrorWithStack("Math doesn't compute", ae);
        }
        return 0;
        //host.resetErrorSnooper();  // lets say no need to reset the snooper in case something happens on the way
    }

    private static int invoke(CommandContext<ServerCommandSource> context, String call, BlockPos pos1, BlockPos pos2,  String args) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        CarpetScriptHost host = getHost(context);
        if (call.startsWith("__"))
        {
            Messenger.m(source, "r Hidden functions are only callable in scripts");
            return 0;
        }
        List<Integer> positions = new ArrayList<>();
        if (pos1 != null)
        {
            positions.add(pos1.getX());
            positions.add(pos1.getY());
            positions.add(pos1.getZ());
        }
        if (pos2 != null)
        {
            positions.add(pos2.getX());
            positions.add(pos2.getY());
            positions.add(pos2.getZ());
        }
        //if (!(args.trim().isEmpty()))
        //    arguments.addAll(Arrays.asList(args.trim().split("\\s+")));
        return handleCall(source, host, () ->  host.callLegacy(source, call, positions, args));
    }


    private static int compute(CommandContext<ServerCommandSource> context, String expr) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        CarpetScriptHost host = getHost(context);
        return handleCall(source, host, () -> {
            CarpetExpression ex = new CarpetExpression(host.main, expr, source, new BlockPos(0, 0, 0));
            return ex.scriptRunCommand(host, new BlockPos(source.getPosition()));
        });
    }

    private static int scriptScan(CommandContext<ServerCommandSource> context, BlockPos origin, BlockPos a, BlockPos b, String expr) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        CarpetScriptHost host = getHost(context);
        BlockBox area = new BlockBox(a, b);
        CarpetExpression cexpr = new CarpetExpression(host.main, expr, source, origin);
        int int_1 = area.getBlockCountX() * area.getBlockCountY() * area.getBlockCountZ();
        if (int_1 > CarpetSettings.fillLimit)
        {
            Messenger.m(source, "r too many blocks to evaluate: " + int_1);
            return 1;
        }
        int successCount = 0;
        CarpetSettings.impendingFillSkipUpdates.set(!CarpetSettings.fillUpdates);
        try
        {
            for (int x = area.minX; x <= area.maxX; x++)
            {
                for (int y = area.minY; y <= area.maxY; y++)
                {
                    for (int z = area.minZ; z <= area.maxZ; z++)
                    {
                        try
                        {
                            if (cexpr.fillAndScanCommand(host, x, y, z)) successCount++;
                        }
                        catch (ArithmeticException ignored)
                        {
                        }
                    }
                }
            }
        }
        catch (CarpetExpressionException exc)
        {
            host.handleErrorWithStack("Error while processing command", exc);
            return 0;
        }
        finally
        {
            CarpetSettings.impendingFillSkipUpdates.set(false);
        }
        Messenger.m(source, "w Expression successful in " + successCount + " out of " + int_1 + " blocks");
        return successCount;

    }


    private static int scriptFill(CommandContext<ServerCommandSource> context, BlockPos origin, BlockPos a, BlockPos b, String expr,
                                BlockStateArgument block, Predicate<CachedBlockPosition> replacement, String mode) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        CarpetScriptHost host = getHost(context);
        BlockBox area = new BlockBox(a, b);
        CarpetExpression cexpr = new CarpetExpression(host.main, expr, source, origin);
        int int_1 = area.getBlockCountX() * area.getBlockCountY() * area.getBlockCountZ();
        if (int_1 > CarpetSettings.fillLimit)
        {
            Messenger.m(source, "r too many blocks to evaluate: "+ int_1);
            return 1;
        }

        boolean[][][] volume = new boolean[area.getBlockCountX()][area.getBlockCountY()][area.getBlockCountZ()];

        BlockPos.Mutable mbpos = origin.mutableCopy();
        ServerWorld world = source.getWorld();

        for (int x = area.minX; x <= area.maxX; x++)
        {
            for (int y = area.minY; y <= area.maxY; y++)
            {
                for (int z = area.minZ; z <= area.maxZ; z++)
                {
                    try
                    {
                        if (cexpr.fillAndScanCommand(host, x, y, z))
                        {
                            volume[x-area.minX][y-area.minY][z-area.minZ]=true;
                        }
                    }
                    catch (CarpetExpressionException e)
                    {
                        host.handleErrorWithStack("Exception while filling the area", e);
                        return 0;
                    }
                    catch (ArithmeticException e)
                    {
                    }
                }
            }
        }
        final int maxx = area.getBlockCountX()-1;
        final int maxy = area.getBlockCountY()-1;
        final int maxz = area.getBlockCountZ()-1;
        if ("outline".equalsIgnoreCase(mode))
        {
            boolean[][][] newVolume = new boolean[area.getBlockCountX()][area.getBlockCountY()][area.getBlockCountZ()];
            for (int x = 0; x <= maxx; x++)
            {
                for (int y = 0; y <= maxy; y++)
                {
                    for (int z = 0; z <= maxz; z++)
                    {
                        if (volume[x][y][z])
                        {
                            if ( (  (x != 0    && !volume[x-1][y  ][z  ]) ||
                                    (x != maxx && !volume[x+1][y  ][z  ]) ||
                                    (y != 0    && !volume[x  ][y-1][z  ]) ||
                                    (y != maxy && !volume[x  ][y+1][z  ]) ||
                                    (z != 0    && !volume[x  ][y  ][z-1]) ||
                                    (z != maxz && !volume[x  ][y  ][z+1])
                            ))
                            {
                                newVolume[x][y][z] = true;
                            }
                        }
                    }
                }
            }
            volume = newVolume;
        }
        int affected = 0;

        CarpetSettings.impendingFillSkipUpdates.set(!CarpetSettings.fillUpdates);
        for (int x = 0; x <= maxx; x++)
        {
            for (int y = 0; y <= maxy; y++)
            {
                for (int z = 0; z <= maxz; z++)
                {
                    if (volume[x][y][z])
                    {
                        mbpos.set(x+area.minX, y+area.minY, z+area.minZ);
                        if (replacement == null || replacement.test(
                                new CachedBlockPosition( world, mbpos, true)))
                        {
                            BlockEntity tileentity = world.getBlockEntity(mbpos);
                            Clearable.clear(tileentity);
                            
                            if (block.setBlockState(world, mbpos,2))
                            {
                                ++affected;
                            }
                        }
                    }
                }
            }
        }
        CarpetSettings.impendingFillSkipUpdates.set(false);

        if (CarpetSettings.fillUpdates && block != null)
        {
            for (int x = 0; x <= maxx; x++)
            {
                for (int y = 0; y <= maxy; y++)
                {
                    for (int z = 0; z <= maxz; z++)
                    {
                        if (volume[x][y][z])
                        {
                            mbpos.set(x+area.minX, y+area.minY, z+area.minZ);
                            Block blokc = world.getBlockState(mbpos).getBlock();
                            world.updateNeighbors(mbpos, blokc);
                        }
                    }
                }
            }
        }
        Messenger.m(source, "gi Affected "+affected+" blocks in "+area.getBlockCountX() * area.getBlockCountY() * area.getBlockCountZ()+" block volume");
        return 1;
    }
}

