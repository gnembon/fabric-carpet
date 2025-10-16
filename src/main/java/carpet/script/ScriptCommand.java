package carpet.script;

import carpet.script.external.Carpet;
import carpet.script.external.Vanilla;
import carpet.script.utils.AppStoreManager;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameRules;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

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
        int quoteCount = StringUtils.countMatches(previous, '\'');
        if (quoteCount % 2 == 1)
        {
            return Collections.emptyList();
        }
        int maxLen = prefix.length() < 3 ? (prefix.length() * 2 + 1) : 1234;
        String eventPrefix = prefix.startsWith("__on_") ? prefix.substring(5) : null;
        List<String> scarpetMatches = scarpetFunctions.stream().
                filter(s -> s.startsWith(prefix) && s.length() <= maxLen).map(s -> s + "(").collect(Collectors.toList());
        scarpetMatches.addAll(APIFunctions.stream().
                filter(s -> s.startsWith(prefix) && s.length() <= maxLen).map(s -> s + "(").toList());
        // not that useful in commandline, more so in external scripts, so skipping here
        if (eventPrefix != null)
        {
            scarpetMatches.addAll(CarpetEventServer.Event.publicEvents(null).stream().
                    filter(e -> e.name.startsWith(eventPrefix)).map(s -> "__on_" + s.name + "(").toList());
        }
        scarpetMatches.addAll(host.globalFunctionNames(host.main, s -> s.startsWith(prefix)).map(s -> s + "(").toList());
        scarpetMatches.addAll(host.globalVariableNames(host.main, s -> s.startsWith(prefix)).toList());
        return scarpetMatches;
    }

    private static CompletableFuture<Suggestions> suggestCode(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder suggestionsBuilder
    ) throws CommandSyntaxException
    {
        CarpetScriptHost currentHost = getHost(context);
        String previous = suggestionsBuilder.getRemaining();
        int strlen = previous.length();
        StringBuilder lastToken = new StringBuilder();
        for (int idx = strlen - 1; idx >= 0; idx--)
        {
            char ch = previous.charAt(idx);
            if (Character.isLetterOrDigit(ch) || ch == '_')
            {
                lastToken.append(ch);
            }
            else
            {
                break;
            }
        }
        if (lastToken.length() == 0)
        {
            return suggestionsBuilder.buildFuture();
        }
        String prefix = lastToken.reverse().toString();
        String previousString = previous.substring(0, previous.length() - prefix.length());
        suggestFunctions(currentHost, previousString, prefix).forEach(text -> suggestionsBuilder.suggest(previousString + text));
        return suggestionsBuilder.buildFuture();
    }

    /**
     * A method to suggest the available scarpet scripts based off of the current player input and {@link AppStoreManager#APP_STORE_ROOT}
     * variable.
     */
    private static CompletableFuture<Suggestions> suggestDownloadableApps(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder suggestionsBuilder
    ) throws CommandSyntaxException
    {

        return CompletableFuture.supplyAsync(() -> {
            String previous = suggestionsBuilder.getRemaining();
            try
            {
                AppStoreManager.suggestionsFromPath(previous, context.getSource()).forEach(suggestionsBuilder::suggest);
            }
            catch (IOException e)
            {
                CarpetScriptServer.LOG.warn("Exception when fetching app store structure", e);
            }
            return suggestionsBuilder.build();
        });
    }

    private static CarpetScriptServer ss(CommandContext<CommandSourceStack> context)
    {
        return Vanilla.MinecraftServer_getScriptServer(context.getSource().getServer());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> b = literal("globals").
                executes(context -> listGlobals(context, false)).
                then(literal("all").executes(context -> listGlobals(context, true)));
        LiteralArgumentBuilder<CommandSourceStack> o = literal("stop").
                executes((cc) -> {
                    ss(cc).stopAll = true;
                    return 1;
                });
        LiteralArgumentBuilder<CommandSourceStack> u = literal("resume").
                executes((cc) -> {
                    ss(cc).stopAll = false;
                    return 1;
                });
        LiteralArgumentBuilder<CommandSourceStack> l = literal("run").
                requires(Vanilla::ServerPlayer_canScriptACE).
                then(argument("expr", StringArgumentType.greedyString()).suggests(ScriptCommand::suggestCode).
                        executes((cc) -> compute(
                                cc,
                                StringArgumentType.getString(cc, "expr"))));
        LiteralArgumentBuilder<CommandSourceStack> s = literal("invoke").
                then(argument("call", StringArgumentType.word()).suggests((cc, bb) -> suggest(suggestFunctionCalls(cc), bb)).
                        executes((cc) -> invoke(
                                cc,
                                StringArgumentType.getString(cc, "call"),
                                null,
                                null,
                                ""
                        )).
                        then(argument("arguments", StringArgumentType.greedyString()).
                                executes((cc) -> invoke(
                                        cc,
                                        StringArgumentType.getString(cc, "call"),
                                        null,
                                        null,
                                        StringArgumentType.getString(cc, "arguments")
                                ))));
        LiteralArgumentBuilder<CommandSourceStack> c = literal("invokepoint").
                then(argument("call", StringArgumentType.word()).suggests((cc, bb) -> suggest(suggestFunctionCalls(cc), bb)).
                        then(argument("origin", BlockPosArgument.blockPos()).
                                executes((cc) -> invoke(
                                        cc,
                                        StringArgumentType.getString(cc, "call"),
                                        BlockPosArgument.getSpawnablePos(cc, "origin"),
                                        null,
                                        ""
                                )).
                                then(argument("arguments", StringArgumentType.greedyString()).
                                        executes((cc) -> invoke(
                                                cc,
                                                StringArgumentType.getString(cc, "call"),
                                                BlockPosArgument.getSpawnablePos(cc, "origin"),
                                                null,
                                                StringArgumentType.getString(cc, "arguments")
                                        )))));
        LiteralArgumentBuilder<CommandSourceStack> h = literal("invokearea").
                then(argument("call", StringArgumentType.word()).suggests((cc, bb) -> suggest(suggestFunctionCalls(cc), bb)).
                        then(argument("from", BlockPosArgument.blockPos()).
                                then(argument("to", BlockPosArgument.blockPos()).
                                        executes((cc) -> invoke(
                                                cc,
                                                StringArgumentType.getString(cc, "call"),
                                                BlockPosArgument.getSpawnablePos(cc, "from"),
                                                BlockPosArgument.getSpawnablePos(cc, "to"),
                                                ""
                                        )).
                                        then(argument("arguments", StringArgumentType.greedyString()).
                                                executes((cc) -> invoke(
                                                        cc,
                                                        StringArgumentType.getString(cc, "call"),
                                                        BlockPosArgument.getSpawnablePos(cc, "from"),
                                                        BlockPosArgument.getSpawnablePos(cc, "to"),
                                                        StringArgumentType.getString(cc, "arguments")
                                                ))))));
        LiteralArgumentBuilder<CommandSourceStack> i = literal("scan").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).
                then(argument("origin", BlockPosArgument.blockPos()).
                        then(argument("from", BlockPosArgument.blockPos()).
                                then(argument("to", BlockPosArgument.blockPos()).
                                        then(argument("expr", StringArgumentType.greedyString()).
                                                suggests(ScriptCommand::suggestCode).
                                                executes((cc) -> scriptScan(
                                                        cc,
                                                        BlockPosArgument.getSpawnablePos(cc, "origin"),
                                                        BlockPosArgument.getSpawnablePos(cc, "from"),
                                                        BlockPosArgument.getSpawnablePos(cc, "to"),
                                                        StringArgumentType.getString(cc, "expr")
                                                ))))));
        LiteralArgumentBuilder<CommandSourceStack> e = literal("fill").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).
                then(argument("origin", BlockPosArgument.blockPos()).
                        then(argument("from", BlockPosArgument.blockPos()).
                                then(argument("to", BlockPosArgument.blockPos()).
                                        then(argument("expr", StringArgumentType.string()).
                                                then(argument("block", BlockStateArgument.block(commandBuildContext)).
                                                        executes((cc) -> scriptFill(
                                                                cc,
                                                                BlockPosArgument.getSpawnablePos(cc, "origin"),
                                                                BlockPosArgument.getSpawnablePos(cc, "from"),
                                                                BlockPosArgument.getSpawnablePos(cc, "to"),
                                                                StringArgumentType.getString(cc, "expr"),
                                                                BlockStateArgument.getBlock(cc, "block"),
                                                                null, "solid"
                                                        )).
                                                        then(literal("replace").
                                                                then(argument("filter", BlockPredicateArgument.blockPredicate(commandBuildContext))
                                                                        .executes((cc) -> scriptFill(
                                                                                cc,
                                                                                BlockPosArgument.getSpawnablePos(cc, "origin"),
                                                                                BlockPosArgument.getSpawnablePos(cc, "from"),
                                                                                BlockPosArgument.getSpawnablePos(cc, "to"),
                                                                                StringArgumentType.getString(cc, "expr"),
                                                                                BlockStateArgument.getBlock(cc, "block"),
                                                                                BlockPredicateArgument.getBlockPredicate(cc, "filter"),
                                                                                "solid"
                                                                        )))))))));
        LiteralArgumentBuilder<CommandSourceStack> t = literal("outline").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).
                then(argument("origin", BlockPosArgument.blockPos()).
                        then(argument("from", BlockPosArgument.blockPos()).
                                then(argument("to", BlockPosArgument.blockPos()).
                                        then(argument("expr", StringArgumentType.string()).
                                                then(argument("block", BlockStateArgument.block(commandBuildContext)).
                                                        executes((cc) -> scriptFill(
                                                                cc,
                                                                BlockPosArgument.getSpawnablePos(cc, "origin"),
                                                                BlockPosArgument.getSpawnablePos(cc, "from"),
                                                                BlockPosArgument.getSpawnablePos(cc, "to"),
                                                                StringArgumentType.getString(cc, "expr"),
                                                                BlockStateArgument.getBlock(cc, "block"),
                                                                null, "outline"
                                                        )).
                                                        then(literal("replace").
                                                                then(argument("filter", BlockPredicateArgument.blockPredicate(commandBuildContext))
                                                                        .executes((cc) -> scriptFill(
                                                                                cc,
                                                                                BlockPosArgument.getSpawnablePos(cc, "origin"),
                                                                                BlockPosArgument.getSpawnablePos(cc, "from"),
                                                                                BlockPosArgument.getSpawnablePos(cc, "to"),
                                                                                StringArgumentType.getString(cc, "expr"),
                                                                                BlockStateArgument.getBlock(cc, "block"),
                                                                                BlockPredicateArgument.getBlockPredicate(cc, "filter"),
                                                                                "outline"
                                                                        )))))))));
        LiteralArgumentBuilder<CommandSourceStack> a = literal("load").requires(Vanilla::ServerPlayer_canScriptACE).
                then(argument("app", StringArgumentType.word()).
                        suggests((cc, bb) -> suggest(ss(cc).listAvailableModules(true), bb)).
                        executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, true, false, false, null, Expression.LoadOverride.DEFAULT)).
                        //then(literal("canonical").executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, true, false, false, null, Expression.LoadOverride.CANONICAL))).
                        //then(literal("optimized").executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, true, false, false, null, Expression.LoadOverride.OPTIMIZED))).
                        //then(literal("functional").executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, true, false, false, null, Expression.LoadOverride.FUNCTIONAL))).
                        //then(literal("functional_optimized").executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, true, false, false, null, Expression.LoadOverride.FUNCTIONAL_OPTIMIZED))).
                        then(literal("global").
                                executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, false, false, false, null, Expression.LoadOverride.DEFAULT))
                                        //.
                                //then(literal("canonical").executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, false, false, false, null, Expression.LoadOverride.CANONICAL))).
                                //then(literal("optimized").executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, false, false, false, null, Expression.LoadOverride.OPTIMIZED))).
                                //then(literal("functional").executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, false, false, false, null, Expression.LoadOverride.FUNCTIONAL))).
                                //then(literal("functional_optimized").executes((cc) -> ss(cc).addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), null, false, false, false, null, Expression.LoadOverride.FUNCTIONAL_OPTIMIZED)))
                        )
                );
        LiteralArgumentBuilder<CommandSourceStack> f = literal("unload").requires(Vanilla::ServerPlayer_canScriptACE).
                then(argument("app", StringArgumentType.word()).
                        suggests((cc, bb) -> suggest(ss(cc).unloadableModules, bb)).
                        executes((cc) ->
                        {
                            boolean success = ss(cc).removeScriptHost(cc.getSource(), StringArgumentType.getString(cc, "app"), true, false);
                            return success ? 1 : 0;
                        }));

        LiteralArgumentBuilder<CommandSourceStack> q = literal("event").requires(Vanilla::ServerPlayer_canScriptACE).
                executes(ScriptCommand::listEvents).
                then(literal("add_to").
                        then(argument("event", StringArgumentType.word()).
                                suggests((cc, bb) -> suggest(CarpetEventServer.Event.publicEvents(ss(cc)).stream().map(ev -> ev.name).collect(Collectors.toList()), bb)).
                                then(argument("call", StringArgumentType.word()).
                                        suggests((cc, bb) -> suggest(suggestFunctionCalls(cc), bb)).
                                        executes((cc) -> ss(cc).events.addEventFromCommand(
                                                cc.getSource(),
                                                StringArgumentType.getString(cc, "event"),
                                                null,
                                                StringArgumentType.getString(cc, "call")
                                        ) ? 1 : 0)).
                                then(literal("from").
                                        then(argument("app", StringArgumentType.word()).
                                                suggests((cc, bb) -> suggest(ss(cc).modules.keySet(), bb)).
                                                then(argument("call", StringArgumentType.word()).
                                                        suggests((cc, bb) -> suggest(suggestFunctionCalls(cc), bb)).
                                                        executes((cc) -> ss(cc).events.addEventFromCommand(
                                                                cc.getSource(),
                                                                StringArgumentType.getString(cc, "event"),
                                                                StringArgumentType.getString(cc, "app"),
                                                                StringArgumentType.getString(cc, "call")
                                                        ) ? 1 : 0)))))).
                then(literal("remove_from").
                        then(argument("event", StringArgumentType.word()).
                                suggests((cc, bb) -> suggest(CarpetEventServer.Event.publicEvents(ss(cc)).stream().filter(CarpetEventServer.Event::isNeeded).map(ev -> ev.name).collect(Collectors.toList()), bb)).
                                then(argument("call", StringArgumentType.greedyString()).
                                        suggests((cc, bb) -> suggest(CarpetEventServer.Event.getEvent(StringArgumentType.getString(cc, "event"), ss(cc)).handler.inspectCurrentCalls().stream().map(CarpetEventServer.Callback::toString), bb)).
                                        executes((cc) -> ss(cc).events.removeEventFromCommand(
                                                cc.getSource(),
                                                StringArgumentType.getString(cc, "event"),
                                                StringArgumentType.getString(cc, "call")
                                        ) ? 1 : 0))));

        LiteralArgumentBuilder<CommandSourceStack> d = literal("download").requires(Vanilla::ServerPlayer_canScriptACE).
                then(argument("path", StringArgumentType.greedyString()).
                        suggests(ScriptCommand::suggestDownloadableApps).
                        executes(cc -> AppStoreManager.downloadScript(cc.getSource(), StringArgumentType.getString(cc, "path"))));
        LiteralArgumentBuilder<CommandSourceStack> r = literal("remove").requires(Vanilla::ServerPlayer_canScriptACE).
                then(argument("app", StringArgumentType.word()).
                        suggests((cc, bb) -> suggest(ss(cc).unloadableModules, bb)).
                        executes((cc) ->
                        {
                            boolean success = ss(cc).uninstallApp(cc.getSource(), StringArgumentType.getString(cc, "app"));
                            return success ? 1 : 0;
                        }));

        LiteralArgumentBuilder<CommandSourceStack> x = literal("explain").requires(Vanilla::ServerPlayer_canScriptACE).
                        executes((cc) -> explain(cc, null, null, null)).
                        then(literal("expression").
                                then(argument("style", StringArgumentType.word()).suggests((cc, bb) -> suggest(List.of("raw", "clean", "functional", "canonical", "optimized", "functional_optimized"), bb)).
                                        then(argument("expr", StringArgumentType.greedyString()).suggests(ScriptCommand::suggestCode).
                                                executes((cc) -> explain(cc, StringArgumentType.getString(cc, "expr"), null, StringArgumentType.getString(cc, "style")))
                                        )
                                )
                        ).
                        then(literal("invoke").
                                then(argument("call", StringArgumentType.word()).suggests((cc, bb) -> suggest(suggestFunctionCalls(cc), bb)).
                                        executes((cc) -> explain(cc, null, StringArgumentType.getString(cc, "call"), null))
                                )
                        ).
                        then(literal("source").
                                then(argument("style", StringArgumentType.word()).suggests((cc, bb) -> suggest(List.of("raw", "clean", "functional", "canonical", "optimized", "functional_optimized"), bb)).
                                        executes((cc) -> explain(cc, null, null, StringArgumentType.getString(cc, "style")))
                                )
                        );

        dispatcher.register(literal("script").
                requires(Vanilla::ServerPlayer_canScriptGeneral).
                then(b).then(u).then(o).then(l).then(s).then(c).then(h).then(i).then(e).then(t).then(a).then(f).then(q).then(d).then(r));
        dispatcher.register(literal("script").
                requires(Vanilla::ServerPlayer_canScriptGeneral).
                then(literal("in").
                        then(argument("app", StringArgumentType.word()).
                                suggests((cc, bb) -> suggest(ss(cc).modules.keySet(), bb)).
                                then(b).then(u).then(o).then(l).then(s).then(c).then(h).then(i).then(e).then(t))));
    }

    private static CarpetScriptHost getHost(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        CarpetScriptHost host;
        CarpetScriptServer scriptServer = ss(context);
        try
        {
            String name = StringArgumentType.getString(context, "app").toLowerCase(Locale.ROOT);
            CarpetScriptHost parentHost = scriptServer.modules.getOrDefault(name, scriptServer.globalHost);
            host = parentHost.retrieveOwnForExecution(context.getSource());
        }
        catch (IllegalArgumentException ignored)
        {
            host = scriptServer.globalHost;
        }
        host.setChatErrorSnooper(context.getSource());
        return host;
    }

    private static Collection<String> suggestFunctionCalls(CommandContext<CommandSourceStack> c) throws CommandSyntaxException
    {
        CarpetScriptHost host = getHost(c);
        return host.globalFunctionNames(host.main, s -> !s.startsWith("_")).sorted().collect(Collectors.toList());
    }

    private static int listEvents(CommandContext<CommandSourceStack> context)
    {
        CarpetScriptServer scriptServer = ss(context);
        CommandSourceStack source = context.getSource();
        Carpet.Messenger_message(source, "w Lists ALL event handlers:");
        for (CarpetEventServer.Event event : CarpetEventServer.Event.getAllEvents(scriptServer, null))
        {
            boolean shownEvent = false;
            for (CarpetEventServer.Callback c : event.handler.inspectCurrentCalls())
            {
                if (!shownEvent)
                {
                    Carpet.Messenger_message(source, "w Handlers for " + event.name + ": ");
                    shownEvent = true;
                }
                Carpet.Messenger_message(source, "w  - " + c.function.getString() + (c.host == null ? "" : " (from " + c.host + ")"));
            }
        }
        return 1;
    }

    private static int listGlobals(CommandContext<CommandSourceStack> context, boolean all) throws CommandSyntaxException
    {
        CarpetScriptHost host = getHost(context);
        CommandSourceStack source = context.getSource();
        CarpetScriptServer scriptServer = ss(context);

        Carpet.Messenger_message(source, "lb Stored functions" + ((host == scriptServer.globalHost) ? ":" : " in " + host.getVisualName() + ":"));
        host.globalFunctionNames(host.main, (str) -> all || !str.startsWith("__")).sorted().forEach((s) -> {
            FunctionValue fun = host.getFunction(s);
            if (fun == null)
            {
                Carpet.Messenger_message(source, "gb " + s, "g  - unused import");
                Carpet.Messenger_message(source, "gi ----------------");
                return;
            }
            Expression expr = fun.getExpression();
            Token tok = fun.getToken();
            List<String> snippet = expr.getExpressionSnippet(tok);
            Carpet.Messenger_message(source, "wb " + fun.fullName(), "t  defined at: line " + (tok.lineno + 1) + " pos " + (tok.linepos + 1));
            for (String snippetLine : snippet)
            {
                Carpet.Messenger_message(source, "w " + snippetLine);
            }
            Carpet.Messenger_message(source, "gi ----------------");
        });
        //Messenger.m(source, "w "+code);
        Carpet.Messenger_message(source, "w  ");
        Carpet.Messenger_message(source, "lb Global variables" + ((host == scriptServer.globalHost) ? ":" : " in " + host.getVisualName() + ":"));
        host.globalVariableNames(host.main, (s) -> s.startsWith("global_")).sorted().forEach((s) -> {
            LazyValue variable = host.getGlobalVariable(s);
            if (variable == null)
            {
                Carpet.Messenger_message(source, "gb " + s, "g  - unused import");
            }
            else
            {
                Carpet.Messenger_message(source, "wb " + s + ": ", "w " + variable.evalValue(null).getPrettyString());
            }
        });
        return 1;
    }

    public static int handleCall(CommandSourceStack source, CarpetScriptHost host, Supplier<Value> call)
    {
        try
        {
            Runnable token = Carpet.startProfilerSection("Scarpet run");
            host.setChatErrorSnooper(source);
            long start = System.nanoTime();
            Value result = call.get();
            long time = ((System.nanoTime() - start) / 1000);
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
            Carpet.Messenger_message(source, "wi  = ", "wb " + result.getString(), "gi  (" + time + metric + ")");
            int intres = (int) result.readInteger();
            token.run();
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
        catch (StackOverflowError soe)
        {
            host.handleErrorWithStack("Your thoughts are too deep", soe);
        }
        return 0;
        //host.resetErrorSnooper();  // lets say no need to reset the snooper in case something happens on the way
    }

    private static int invoke(CommandContext<CommandSourceStack> context, String call, BlockPos pos1, BlockPos pos2, String args) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        CarpetScriptHost host = getHost(context);
        if (call.startsWith("__"))
        {
            Carpet.Messenger_message(source, "r Hidden functions are only callable in scripts");
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
        return handleCall(source, host, () -> host.callLegacy(source, call, positions, args));
    }


    private static int compute(CommandContext<CommandSourceStack> context, String expr) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        CarpetScriptHost host = getHost(context);
        return handleCall(source, host, () -> {
            CarpetExpression ex = new CarpetExpression(host.main, expr, source, new BlockPos(0, 0, 0));
            return ex.scriptRunCommand(host, BlockPos.containing(source.getPosition())).getLeft();
        });
    }

    private static int explain(CommandContext<CommandSourceStack> context, @Nullable String expr, @Nullable String method, @Nullable String style) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        CarpetScriptHost host = getHost(context);
        return handleCall(source, host, () -> {
            // expression is irrelevant as we pass approprirate strings for parsing directly
            // we only really need access to operators and functions?
            CarpetExpression ex = new CarpetExpression(host.main, "", source, BlockPos.ZERO);
            List<Token> results = ex.explain(host, expr != null ? expr : (style != null? (host.main != null ? host.main.code() : "") :null), method, style, BlockPos.containing(source.getPosition()));
            prettyPrintTokens(source, results);
            return NumericValue.of(results.size());
        });
    }

    private static void prettyPrintTokens(CommandSourceStack source, List<Token> tokens)
    {
        Map<Integer, Integer> indents = new HashMap<>();
        for (Token token : tokens)
        {
            indents.put(token.lineno, Math.min(indents.getOrDefault(token.lineno, token.linepos), token.linepos));
        }
        List<Integer> lines = new ArrayList<>(indents.keySet());
        Collections.sort(lines);
        int lineMax = String.valueOf(lines.getLast()).length();
        Deque<Integer> indentsStack = new ArrayDeque<>(lines);

        List<Component> elements = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++)
        {
            Token token = tokens.get(i);
            if (!indentsStack.isEmpty() && token.lineno == indentsStack.peekFirst())
            {
                flushElements(source, elements);
                indentsStack.pollFirst();
                // remove all elements of intendStack that don't have any more tokens
                outer: while (!indentsStack.isEmpty())
                {
                    int nextLineNo = indentsStack.peekFirst();
                    for (int j = i + 1; j < tokens.size(); j++)
                    {
                        if (tokens.get(j).lineno == nextLineNo)
                        {
                            break outer;
                        }
                    }
                    indentsStack.pollFirst();
                }

                int lastPos = indents.getOrDefault(token.lineno, 0);
                elements.add(Carpet.Messenger_compose("fi " + " ".repeat(lineMax - String.valueOf(token.lineno).length()) + token.lineno));
                elements.add(Carpet.Messenger_compose("g | " + " ".repeat(lastPos)));
            }
            elements.add(tokenToComponent(token));
        }
        flushElements(source, elements);
    }

    private static Component tokenToComponent(Token token)
    {
        String surface = token.surface;
        if (!token.display.isEmpty())
        {
            surface = token.display;
        }
        if (token.comment.isEmpty())
        {
            return Carpet.Messenger_compose(styleForToken(token) + " " + surface);//, "^gi " + String.format("%s: line %s, pos %s", token.surface, token.lineno, token.linepos));
        }
        return Carpet.Messenger_compose(styleForToken(token)+"u " + surface, "^gi " + token.comment);
    }

    private static String styleForToken(Token token)
    {
        return switch (token.type) {
            case Token.TokenType.LITERAL -> "l";
            case Token.TokenType.HEX_LITERAL -> "m";
            case Token.TokenType.CONSTANT -> "q";
            case Token.TokenType.OPEN_PAREN, Token.TokenType.CLOSE_PAREN, Token.TokenType.COMMA,
                 Token.TokenType.OPERATOR, Token.TokenType.UNARY_OPERATOR -> "w";
            case Token.TokenType.FUNCTION -> "d";
            case Token.TokenType.VARIABLE -> "y";
            case Token.TokenType.STRINGPARAM -> "c";
            default -> // marker
                    "r";
        };
    }

    private static void flushElements(CommandSourceStack source, List<Component> elements)
    {
        if (!elements.isEmpty())
        {
            Carpet.Messenger_message(source, (Object []) elements.toArray(new Component[0]));
            elements.clear();
        }
    }

    private static int scriptScan(CommandContext<CommandSourceStack> context, BlockPos origin, BlockPos a, BlockPos b, String expr) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        CarpetScriptHost host = getHost(context);
        BoundingBox area = BoundingBox.fromCorners(a, b);
        CarpetExpression cexpr = new CarpetExpression(host.main, expr, source, origin);
        int int_1 = area.getXSpan() * area.getYSpan() * area.getZSpan(); // X Y Z
        if (int_1 > source.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT))
        {
            Carpet.Messenger_message(source, "r too many blocks to evaluate: " + int_1);
            return 1;
        }
        int successCount = 0;
        Carpet.getImpendingFillSkipUpdates().set(!Carpet.getFillUpdates());
        try
        {
            for (int x = area.minX(); x <= area.maxX(); x++)
            {
                for (int y = area.minY(); y <= area.maxY(); y++)
                {
                    for (int z = area.minZ(); z <= area.maxZ(); z++)
                    {
                        try
                        {
                            if (cexpr.fillAndScanCommand(host, x, y, z))
                            {
                                successCount++;
                            }
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
            Carpet.getImpendingFillSkipUpdates().set(false);
        }
        Carpet.Messenger_message(source, "w Expression successful in " + successCount + " out of " + int_1 + " blocks");
        return successCount;

    }


    private static int scriptFill(CommandContext<CommandSourceStack> context, BlockPos origin, BlockPos a, BlockPos b, String expr,
                                  BlockInput block, Predicate<BlockInWorld> replacement, String mode) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        CarpetScriptHost host = getHost(context);
        BoundingBox area = BoundingBox.fromCorners(a, b);
        CarpetExpression cexpr = new CarpetExpression(host.main, expr, source, origin);
        int int_1 = area.getXSpan() * area.getYSpan() * area.getZSpan();
        if (int_1 > source.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT))
        {
            Carpet.Messenger_message(source, "r too many blocks to evaluate: " + int_1);
            return 1;
        }

        boolean[][][] volume = new boolean[area.getXSpan()][area.getYSpan()][area.getZSpan()]; //X then Y then Z got messedup

        BlockPos.MutableBlockPos mbpos = origin.mutable();
        ServerLevel world = source.getLevel();

        for (int x = area.minX(); x <= area.maxX(); x++)
        {
            for (int y = area.minY(); y <= area.maxY(); y++)
            {
                for (int z = area.minZ(); z <= area.maxZ(); z++)
                {
                    try
                    {
                        if (cexpr.fillAndScanCommand(host, x, y, z))
                        {
                            volume[x - area.minX()][y - area.minY()][z - area.minZ()] = true;
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
        int maxx = area.getXSpan() - 1;
        int maxy = area.getYSpan() - 1;
        int maxz = area.getZSpan() - 1;
        if ("outline".equalsIgnoreCase(mode))
        {
            boolean[][][] newVolume = new boolean[area.getXSpan()][area.getYSpan()][area.getZSpan()];
            for (int x = 0; x <= maxx; x++)
            {
                for (int y = 0; y <= maxy; y++)
                {
                    for (int z = 0; z <= maxz; z++)
                    {
                        if (volume[x][y][z])
                        {
                            if (((x != 0 && !volume[x - 1][y][z]) ||
                                    (x != maxx && !volume[x + 1][y][z]) ||
                                    (y != 0 && !volume[x][y - 1][z]) ||
                                    (y != maxy && !volume[x][y + 1][z]) ||
                                    (z != 0 && !volume[x][y][z - 1]) ||
                                    (z != maxz && !volume[x][y][z + 1])
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

        Carpet.getImpendingFillSkipUpdates().set(!Carpet.getFillUpdates());
        for (int x = 0; x <= maxx; x++)
        {
            for (int y = 0; y <= maxy; y++)
            {
                for (int z = 0; z <= maxz; z++)
                {
                    if (volume[x][y][z])
                    {
                        mbpos.set(x + area.minX(), y + area.minY(), z + area.minZ());
                        if (replacement == null || replacement.test(
                                new BlockInWorld(world, mbpos, true)))
                        {
                            if (block.place(world, mbpos, 2 & Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS))
                            {
                                ++affected;
                            }
                        }
                    }
                }
            }
        }
        Carpet.getImpendingFillSkipUpdates().set(false);

        if (Carpet.getFillUpdates() && block != null)
        {
            for (int x = 0; x <= maxx; x++)
            {
                for (int y = 0; y <= maxy; y++)
                {
                    for (int z = 0; z <= maxz; z++)
                    {
                        if (volume[x][y][z])
                        {
                            mbpos.set(x + area.minX(), y + area.minY(), z + area.minZ());
                            Block blokc = world.getBlockState(mbpos).getBlock();
                            world.updateNeighborsAt(mbpos, blokc);
                        }
                    }
                }
            }
        }
        Carpet.Messenger_message(source, "gi Affected " + affected + " blocks in " + area.getXSpan() * area.getYSpan() * area.getZSpan() + " block volume");
        return 1;
    }
}

