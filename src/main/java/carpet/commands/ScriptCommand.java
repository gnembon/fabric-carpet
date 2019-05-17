package carpet.commands;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.CarpetExpression;
import carpet.script.Expression;
import carpet.script.ExpressionInspector;
import carpet.script.ScriptHost;
import carpet.script.Tokenizer;
import carpet.script.exception.CarpetExpressionException;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Block;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.BlockPredicateArgumentType;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.command.arguments.BlockStateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MutableIntBoundingBox;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandSource.suggestMatching;

public class ScriptCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> b = literal("globals").
                executes(ScriptCommand::listGlobals);
        LiteralArgumentBuilder<ServerCommandSource> o = literal("stop").
                executes( (cc) -> { CarpetServer.scriptServer.stopAll = true; return 1;});
        LiteralArgumentBuilder<ServerCommandSource> u = literal("resume").
                executes( (cc) -> { CarpetServer.scriptServer.stopAll = false; return 1;});
        LiteralArgumentBuilder<ServerCommandSource> l = literal("run").
                requires((player) -> player.hasPermissionLevel(2)).
                then(argument("expr", StringArgumentType.greedyString()).
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
                        )).
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
                        then(argument("origin", BlockPosArgumentType.create()).
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
                        then(argument("from", BlockPosArgumentType.create()).
                                then(argument("to", BlockPosArgumentType.create()).
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
                then(argument("origin", BlockPosArgumentType.create()).
                        then(argument("from", BlockPosArgumentType.create()).
                                then(argument("to", BlockPosArgumentType.create()).
                                        then(argument("expr", StringArgumentType.greedyString()).
                                                executes( (cc) -> scriptScan(
                                                        cc,
                                                        BlockPosArgumentType.getBlockPos(cc, "origin"),
                                                        BlockPosArgumentType.getBlockPos(cc, "from"),
                                                        BlockPosArgumentType.getBlockPos(cc, "to"),
                                                        StringArgumentType.getString(cc, "expr")
                                                ))))));
        LiteralArgumentBuilder<ServerCommandSource> e = literal("fill").requires((player) -> player.hasPermissionLevel(2)).
                then(argument("origin", BlockPosArgumentType.create()).
                        then(argument("from", BlockPosArgumentType.create()).
                                then(argument("to", BlockPosArgumentType.create()).
                                        then(argument("expr", StringArgumentType.string()).
                                                then(argument("block", BlockStateArgumentType.create()).
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
                                                                then(argument("filter", BlockPredicateArgumentType.create())
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
                then(argument("origin", BlockPosArgumentType.create()).
                        then(argument("from", BlockPosArgumentType.create()).
                                then(argument("to", BlockPosArgumentType.create()).
                                        then(argument("expr", StringArgumentType.string()).
                                                then(argument("block", BlockStateArgumentType.create()).
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
                                                                then(argument("filter", BlockPredicateArgumentType.create())
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
        LiteralArgumentBuilder<ServerCommandSource> a = literal("load").requires( (player) -> player.hasPermissionLevel(2) ).
                then(argument("package", StringArgumentType.word()).
                        suggests( (cc, bb) -> suggestMatching(CarpetServer.scriptServer.listAvailableModules(),bb)).
                        executes((cc) ->
                        {
                            boolean success = CarpetServer.scriptServer.addScriptHost(cc.getSource(), StringArgumentType.getString(cc, "package"));
                            Messenger.m(cc.getSource(), success?"w Successfully added a module":"r Failed to add a module");
                            return success?1:0;
                        })
                );
        LiteralArgumentBuilder<ServerCommandSource> f = literal("unload").requires( (player) -> player.hasPermissionLevel(2) ).
                then(argument("package", StringArgumentType.word()).
                        suggests( (cc, bb) -> suggestMatching(CarpetServer.scriptServer.modules.keySet(),bb)).
                        executes((cc) ->
                        {
                            boolean success =CarpetServer.scriptServer.removeScriptHost(StringArgumentType.getString(cc, "package"));
                            Messenger.m(cc.getSource(), success?"w Successfully removed a module":"r Failed to remove a module");
                            return success?1:0;
                        }));

        dispatcher.register(literal("script").
                requires((player) -> CarpetSettings.getBool("commandScript")).
                then(b).then(u).then(o).then(l).then(s).then(c).then(h).then(i).then(e).then(t).then(a).then(f));
        dispatcher.register(literal("script").
                requires((player) -> CarpetSettings.getBool("commandScript")).
                then(literal("in").
                        then(argument("package", StringArgumentType.word()).
                                suggests( (cc, bb) -> suggestMatching(CarpetServer.scriptServer.modules.keySet(), bb)).
                                then(b).then(u).then(o).then(l).then(s).then(c).then(h).then(i).then(e).then(t))));
    }
    private static ScriptHost getHost(CommandContext<ServerCommandSource> context)
    {
        try
        {
            String name = StringArgumentType.getString(context, "package").toLowerCase(Locale.ROOT);
            return CarpetServer.scriptServer.modules.getOrDefault(name, CarpetServer.scriptServer.globalHost);

        }
        catch (IllegalArgumentException ignored)
        {
            return CarpetServer.scriptServer.globalHost;
        }
    }
    private static Collection<String> suggestFunctionCalls(CommandContext<ServerCommandSource> c)
    {
        ServerCommandSource s = c.getSource();
        ScriptHost host = getHost(c);
        return host.getPublicFunctions();
    }
    private static int listGlobals(CommandContext<ServerCommandSource> context)
    {
        ScriptHost host = getHost(context);
        ServerCommandSource source = context.getSource();

        Messenger.m(source, "w Global functions"+((host == CarpetServer.scriptServer.globalHost)?":":" in "+host.getName()+":"));
        for (String fname : host.getAvailableFunctions())
        {
            Expression expr = host.getExpressionForFunction(fname);
            Tokenizer.Token tok = host.getTokenForFunction(fname);
            List<String> snippet = ExpressionInspector.Expression_getExpressionSnippet(tok, expr);
            Messenger.m(source, "w Function "+fname+" defined at: line "+(tok.lineno+1)+" pos "+(tok.linepos+1));
            for (String snippetLine: snippet)
            {
                Messenger.m(source, "li "+snippetLine);
            }
            Messenger.m(source, "gi ----------------");
        }
        //Messenger.m(source, "w "+code);
        Messenger.m(source, "w Global Variables:");

        for (String vname : host.globalVariables.keySet())
        {
            Messenger.m(source, "w Variable "+vname+": ", "wb "+ host.globalVariables.get(vname).evalValue(null).getPrettyString());
        }
        return 1;
    }

    public static void handleCall(ServerCommandSource source, Supplier<String> call)
    {
        try
        {
            CarpetServer.scriptServer.setChatErrorSnooper(source);
            long start = System.nanoTime();
            String result = call.get();
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
            Messenger.m(source, "wi  = ", "wb "+result, "gi  ("+time+metric+")");
        }
        catch (CarpetExpressionException e)
        {
            Messenger.m(source, "r Exception white evaluating expression at "+new BlockPos(source.getPosition())+": "+e.getMessage());
        }
        CarpetServer.scriptServer.resetErrorSnooper();
    }

    private static int invoke(CommandContext<ServerCommandSource> context, String call, BlockPos pos1, BlockPos pos2,  String args)
    {
        ServerCommandSource source = context.getSource();
        ScriptHost host = getHost(context);
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
        handleCall(source, () ->  host.call(source, call, positions, args));
        return 1;
    }


    private static int compute(CommandContext<ServerCommandSource> context, String expr)
    {
        ServerCommandSource source = context.getSource();
        ScriptHost host = getHost(context);
        handleCall(source, () -> {
            CarpetExpression ex = new CarpetExpression(expr, source, new BlockPos(0, 0, 0));
            return ex.scriptRunCommand(host, new BlockPos(source.getPosition()));
        });
        return 1;
    }

    private static int scriptScan(CommandContext<ServerCommandSource> context, BlockPos origin, BlockPos a, BlockPos b, String expr)
    {
        ServerCommandSource source = context.getSource();
        ScriptHost host = getHost(context);
        MutableIntBoundingBox area = new MutableIntBoundingBox(a, b);
        CarpetExpression cexpr = new CarpetExpression(expr, source, origin);
        int int_1 = area.getBlockCountX() * area.getBlockCountY() * area.getBlockCountZ();
        if (int_1 > CarpetSettings.getInt("fillLimit"))
        {
            Messenger.m(source, "r too many blocks to evaluate: " + int_1);
            return 1;
        }
        int successCount = 0;
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
            Messenger.m(source, "r Error while processing command: "+exc);
            return 0;
        }
        Messenger.m(source, "w Expression successful in " + successCount + " out of " + int_1 + " blocks");
        return successCount;

    }


    private static int scriptFill(CommandContext<ServerCommandSource> context, BlockPos origin, BlockPos a, BlockPos b, String expr,
                                BlockStateArgument block, Predicate<CachedBlockPosition> replacement, String mode)
    {
        ServerCommandSource source = context.getSource();
        ScriptHost host = getHost(context);
        MutableIntBoundingBox area = new MutableIntBoundingBox(a, b);
        CarpetExpression cexpr = new CarpetExpression(expr, source, origin);
        int int_1 = area.getBlockCountX() * area.getBlockCountY() * area.getBlockCountZ();
        if (int_1 > CarpetSettings.getInt("fillLimit"))
        {
            Messenger.m(source, "r too many blocks to evaluate: "+ int_1);
            return 1;
        }

        boolean[][][] volume = new boolean[area.getBlockCountX()][area.getBlockCountY()][area.getBlockCountZ()];

        BlockPos.Mutable mbpos = new BlockPos.Mutable(origin);
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
                        Messenger.m(source, "r Exception while filling the area:\n","l "+e.getMessage());
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
                            
                            if (block.setBlockState(
                                    world,
                                    mbpos,
                                    2 | (CarpetSettings.getBool("fillUpdates") ?0:1024)
                            ))
                            {
                                ++affected;
                            }
                        }
                    }
                }
            }
        }
        if (CarpetSettings.getBool("fillUpdates") && block != null)
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

