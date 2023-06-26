package carpet.script;

import carpet.script.external.Carpet;
import carpet.script.external.Vanilla;
import carpet.script.utils.AppStoreManager;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class ScriptCommand
{
    private static final List<String> scarpetFunctions;
    private static final List<String> APIFunctions;

    static
    {
        Set<String> allFunctions = new CarpetExpression(null, "null", null, null).getExpr().getFunctionNames();
        Set<String> scNames = Expression.none.getFunctionNames();
        scarpetFunctions = scNames.stream().sorted().toList();
        APIFunctions = allFunctions.stream().filter(s -> !scNames.contains(s)).sorted().toList();
    }

    public static List<String> suggestFunctions(ScriptHost host, String previous, String prefix)
    {
        previous = previous.replace("\\'", "");
        int quoteCount = StringUtils.countMatches(previous, '\'');
        if (quoteCount % 2 == 1)
        {
            return List.of();
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
        LiteralArgumentBuilder<CommandSourceStack> command = literal("script")
                .requires(Vanilla::ServerPlayer_canScriptGeneral)
                .then(literal("resume")
                        .executes(ctx -> {
                            ss(ctx).stopAll = false;
                            return 1;
                        }))
                .then(literal("stop")
                        .executes(ctx -> {
                            ss(ctx).stopAll = true;
                            return 1;
                        }))
                .then(literal("load")
                        .then(argument("app", StringArgumentType.word())
                                .suggests((ctx, bb) -> suggest(ss(ctx).listAvailableModules(true), bb))
                                .executes(ctx ->
                                {
                                    boolean success = ss(ctx).addScriptHost(ctx.getSource(), StringArgumentType.getString(ctx, "app"), null, true, false, false, null);
                                    return success ? 1 : 0;
                                })
                                .then(literal("global")
                                        .executes(ctx ->
                                                {
                                                    boolean success = ss(ctx).addScriptHost(ctx.getSource(), StringArgumentType.getString(ctx, "app"), null, false, false, false, null);
                                                    return success ? 1 : 0;
                                                }
                                        )
                                )
                        ))
                .then(literal("unload")
                        .then(argument("app", StringArgumentType.word())
                                .suggests((ctx, bb) -> suggest(ss(ctx).unloadableModules, bb))
                                .executes(ctx ->
                                {
                                    boolean success = ss(ctx).removeScriptHost(ctx.getSource(), StringArgumentType.getString(ctx, "app"), true, false);
                                    return success ? 1 : 0;
                                })))
                .then(literal("event")
                        .executes(ScriptCommand::listEvents)
                        .then(literal("add_to")
                                .then(argument("event", StringArgumentType.word())
                                        .suggests((ctx, bb) -> suggest(CarpetEventServer.Event.publicEvents(ss(ctx)).stream().map(ev -> ev.name), bb))
                                        .then(argument("call", StringArgumentType.word())
                                                .suggests((ctx, bb) -> suggest(suggestFunctionCalls(ctx), bb))
                                                .executes(ctx -> ss(ctx).events.addEventFromCommand(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "event"),
                                                        null,
                                                        StringArgumentType.getString(ctx, "call")
                                                ) ? 1 : 0))
                                        .then(literal("from")
                                                .then(argument("app", StringArgumentType.word())
                                                        .suggests((ctx, bb) -> suggest(ss(ctx).modules.keySet(), bb))
                                                        .then(argument("call", StringArgumentType.word())
                                                                .suggests((ctx, bb) -> suggest(suggestFunctionCalls(ctx), bb))
                                                                .executes(ctx -> ss(ctx).events.addEventFromCommand(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "event"),
                                                                        StringArgumentType.getString(ctx, "app"),
                                                                        StringArgumentType.getString(ctx, "call")
                                                                ) ? 1 : 0))))))
                        .then(literal("remove_from")
                                .then(argument("event", StringArgumentType.word())
                                        .suggests((ctx, bb) -> suggest(CarpetEventServer.Event.publicEvents(ss(ctx)).stream().filter(CarpetEventServer.Event::isNeeded).map(ev -> ev.name), bb))
                                        .then(argument("call", StringArgumentType.greedyString())
                                                .suggests((ctx, bb) -> suggest(CarpetEventServer.Event.getEvent(StringArgumentType.getString(ctx, "event"), ss(ctx)).handler.inspectCurrentCalls().stream().map(CarpetEventServer.Callback::toString), bb))
                                                .executes(ctx -> ss(ctx).events.removeEventFromCommand(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "event"),
                                                        StringArgumentType.getString(ctx, "call")
                                                ) ? 1 : 0)))))
                .then(literal("download")
                        .requires(ctx -> Vanilla.ServerPlayer_canScriptACE(ctx) && AppStoreManager.enabled())
                        .then(argument("path", StringArgumentType.greedyString())
                                .suggests(ScriptCommand::suggestDownloadableApps)
                                .executes(ctx -> AppStoreManager.downloadScript(ctx.getSource(), StringArgumentType.getString(ctx, "path")))))
                .then(literal("remove")
                        .then(argument("app", StringArgumentType.word())
                                .suggests((ctx, bb) -> suggest(ss(ctx).unloadableModules, bb))
                                .executes(ctx ->
                                {
                                    boolean success = ss(ctx).uninstallApp(ctx.getSource(), StringArgumentType.getString(ctx, "app"));
                                    return success ? 1 : 0;
                                })));

        // Add host commands for the global host
        appendHostCommands(command, commandBuildContext);

        RequiredArgumentBuilder<CommandSourceStack, String> appSpecific = argument("app", StringArgumentType.word())
                .suggests((ctx, bb) -> suggest(ss(ctx).modules.keySet(), bb));

        // Add host commands for specific apps, under "/script in <app>"
        appendHostCommands(appSpecific, commandBuildContext);

        command.then(literal("in")
                .then(appSpecific));
        
        dispatcher.register(command);
    }

    private static void appendHostCommands(ArgumentBuilder<CommandSourceStack, ?> node, CommandBuildContext buildCtx)
    {
        node
        .then(literal("globals")
                .executes(context -> listGlobals(context, false))
                .then(literal("all")
                        .executes(context -> listGlobals(context, true))))
        .then(literal("run")
                .then(argument("expr", StringArgumentType.greedyString())
                        .suggests(ScriptCommand::suggestCode)
                        .executes(ctx -> compute(ctx, StringArgumentType.getString(ctx, "expr")))))
        .then(literal("invoke")
                .then(argument("call", StringArgumentType.word())
                        .suggests((ctx, bb) -> suggest(suggestFunctionCalls(ctx), bb))
                        .executes(ctx -> invoke(
                                ctx,
                                StringArgumentType.getString(ctx, "call"),
                                null,
                                null,
                                ""
                                ))
                        .then(argument("arguments", StringArgumentType.greedyString())
                                .executes(ctx -> invoke(
                                        ctx,
                                        StringArgumentType.getString(ctx, "call"),
                                        null,
                                        null,
                                        StringArgumentType.getString(ctx, "arguments")
                                        )))))
        .then(literal("invokepoint").
                then(argument("call", StringArgumentType.word())
                        .suggests((ctx, bb) -> suggest(suggestFunctionCalls(ctx), bb))
                        .then(argument("origin", BlockPosArgument.blockPos())
                                .executes(ctx -> invoke(
                                        ctx,
                                        StringArgumentType.getString(ctx, "call"),
                                        BlockPosArgument.getSpawnablePos(ctx, "origin"),
                                        null,
                                        ""
                                        ))
                                .then(argument("arguments", StringArgumentType.greedyString())
                                        .executes(ctx -> invoke(
                                                ctx,
                                                StringArgumentType.getString(ctx, "call"),
                                                BlockPosArgument.getSpawnablePos(ctx, "origin"),
                                                null,
                                                StringArgumentType.getString(ctx, "arguments")
                                                ))))))
        .then(literal("invokearea")
                .then(argument("call", StringArgumentType.word())
                        .suggests((ctx, bb) -> suggest(suggestFunctionCalls(ctx), bb))
                        .then(argument("from", BlockPosArgument.blockPos())
                                .then(argument("to", BlockPosArgument.blockPos())
                                        .executes(ctx -> invoke(
                                                ctx,
                                                StringArgumentType.getString(ctx, "call"),
                                                BlockPosArgument.getSpawnablePos(ctx, "from"),
                                                BlockPosArgument.getSpawnablePos(ctx, "to"),
                                                ""
                                                ))
                                        .then(argument("arguments", StringArgumentType.greedyString())
                                                .executes(ctx -> invoke(
                                                        ctx,
                                                        StringArgumentType.getString(ctx, "call"),
                                                        BlockPosArgument.getSpawnablePos(ctx, "from"),
                                                        BlockPosArgument.getSpawnablePos(ctx, "to"),
                                                        StringArgumentType.getString(ctx, "arguments")
                                                        )))))))
        .then(literal("scan")
                .then(argument("origin", BlockPosArgument.blockPos())
                        .then(argument("from", BlockPosArgument.blockPos())
                                .then(argument("to", BlockPosArgument.blockPos())
                                        .then(argument("expr", StringArgumentType.greedyString())
                                                .suggests(ScriptCommand::suggestCode)
                                                .executes(ctx -> scriptScan(
                                                        ctx,
                                                        BlockPosArgument.getSpawnablePos(ctx, "origin"),
                                                        BlockPosArgument.getSpawnablePos(ctx, "from"),
                                                        BlockPosArgument.getSpawnablePos(ctx, "to"),
                                                        StringArgumentType.getString(ctx, "expr")
                                                        )))))))
        .then(literal("fill")
                .then(argument("origin", BlockPosArgument.blockPos())
                        .then(argument("from", BlockPosArgument.blockPos())
                                .then(argument("to", BlockPosArgument.blockPos())
                                        .then(argument("expr", StringArgumentType.string())
                                                        .then(argument("block", BlockStateArgument.block(buildCtx))
                                                        .executes(ctx -> scriptFill(
                                                                ctx,
                                                                BlockPosArgument.getSpawnablePos(ctx, "origin"),
                                                                BlockPosArgument.getSpawnablePos(ctx, "from"),
                                                                BlockPosArgument.getSpawnablePos(ctx, "to"),
                                                                StringArgumentType.getString(ctx, "expr"),
                                                                BlockStateArgument.getBlock(ctx, "block"),
                                                                null, "solid"
                                                                ))
                                                        .then(literal("replace")
                                                                        .then(argument("filter", BlockPredicateArgument.blockPredicate(buildCtx))
                                                                        .executes(ctx -> scriptFill(
                                                                                ctx,
                                                                                BlockPosArgument.getSpawnablePos(ctx, "origin"),
                                                                                BlockPosArgument.getSpawnablePos(ctx, "from"),
                                                                                BlockPosArgument.getSpawnablePos(ctx, "to"),
                                                                                StringArgumentType.getString(ctx, "expr"),
                                                                                BlockStateArgument.getBlock(ctx, "block"),
                                                                                BlockPredicateArgument.getBlockPredicate(ctx, "filter"),
                                                                                "solid"
                                                                                ))))))))))
        .then(literal("outline")
                .then(argument("origin", BlockPosArgument.blockPos())
                        .then(argument("from", BlockPosArgument.blockPos())
                                .then(argument("to", BlockPosArgument.blockPos())
                                        .then(argument("expr", StringArgumentType.string())
                                                        .then(argument("block", BlockStateArgument.block(buildCtx))
                                                        .executes(ctx -> scriptFill(
                                                                ctx,
                                                                BlockPosArgument.getSpawnablePos(ctx, "origin"),
                                                                BlockPosArgument.getSpawnablePos(ctx, "from"),
                                                                BlockPosArgument.getSpawnablePos(ctx, "to"),
                                                                StringArgumentType.getString(ctx, "expr"),
                                                                BlockStateArgument.getBlock(ctx, "block"),
                                                                null, "outline"
                                                                ))
                                                        .then(literal("replace")
                                                                        .then(argument("filter", BlockPredicateArgument.blockPredicate(buildCtx))
                                                                        .executes(ctx -> scriptFill(
                                                                                ctx,
                                                                                BlockPosArgument.getSpawnablePos(ctx, "origin"),
                                                                                BlockPosArgument.getSpawnablePos(ctx, "from"),
                                                                                BlockPosArgument.getSpawnablePos(ctx, "to"),
                                                                                StringArgumentType.getString(ctx, "expr"),
                                                                                BlockStateArgument.getBlock(ctx, "block"),
                                                                                BlockPredicateArgument.getBlockPredicate(ctx, "filter"),
                                                                                "outline"
                                                                                ))))))))));
    }

    private static CarpetScriptHost getHost(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        CarpetScriptHost host;
        CarpetScriptServer scriptServer = ss(context);
        try
        {
            String name = StringArgumentType.getString(context, "app").toLowerCase(Locale.ROOT);
            CarpetScriptHost parentHost = scriptServer.modules.get(name);
            if (parentHost == null)
            {
                throw new SimpleCommandExceptionType(Component.literal("No app with name '" + name + "' is loaded")).create();
            }
            host = parentHost.retrieveOwnForExecution(context.getSource());
        }
        catch (IllegalArgumentException notPresent)
        {
            host = scriptServer.globalHost;
        }
        host.setChatErrorSnooper(context.getSource());
        return host;
    }

    private static Stream<String> suggestFunctionCalls(CommandContext<CommandSourceStack> c) throws CommandSyntaxException
    {
        CarpetScriptHost host = getHost(c);
        return host.globalFunctionNames(host.main, s -> !s.startsWith("_")).sorted();
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

        Carpet.Messenger_message(source, "lb Stored functions" + ((host == scriptServer.globalHost) ? ":" : " in " + host.getName() + ":"));
        host.globalFunctionNames(host.main, (str) -> all || !str.startsWith("__")).sorted().forEach((s) -> {
            FunctionValue fun = host.getFunction(s);
            if (fun == null)
            {
                Carpet.Messenger_message(source, "gb " + s, "g  - unused import");
                Carpet.Messenger_message(source, "gi ----------------");
                return;
            }
            Expression expr = fun.getExpression();
            Tokenizer.Token tok = fun.getToken();
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
        Carpet.Messenger_message(source, "lb Global variables" + ((host == scriptServer.globalHost) ? ":" : " in " + host.getName() + ":"));
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
            return ex.scriptRunCommand(host, BlockPos.containing(source.getPosition()));
        });
    }

    private static int scriptScan(CommandContext<CommandSourceStack> context, BlockPos origin, BlockPos a, BlockPos b, String expr) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        CarpetScriptHost host = getHost(context);
        BoundingBox area = BoundingBox.fromCorners(a, b);
        CarpetExpression cexpr = new CarpetExpression(host.main, expr, source, origin);
        int int_1 = area.getXSpan() * area.getYSpan() * area.getZSpan(); // X Y Z
        if (int_1 > Vanilla.MinecraftServer_getFillLimit(source.getServer()) )
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
        if (int_1 > Vanilla.MinecraftServer_getFillLimit(source.getServer()))
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
                            BlockEntity tileentity = world.getBlockEntity(mbpos);
                            Clearable.tryClear(tileentity);

                            if (block.place(world, mbpos, 2))
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
                            world.blockUpdated(mbpos, blokc);
                        }
                    }
                }
            }
        }
        Carpet.Messenger_message(source, "gi Affected " + affected + " blocks in " + area.getXSpan() * area.getYSpan() * area.getZSpan() + " block volume");
        return 1;
    }
}

