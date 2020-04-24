package carpet.commands;

import carpet.CarpetSettings;
import carpet.settings.SettingsManager;
import carpet.utils.Messenger;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.block.Block;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.BlockPredicateArgumentType;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.command.arguments.BlockStateArgumentType;
import static net.minecraft.server.command.CommandSource.suggestMatching;
import net.minecraft.inventory.Inventory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.function.Predicate;
import java.lang.Math;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DrawCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> command = literal("draw").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandDraw)).
                then(literal("sphere").
                     then(argument("center", BlockPosArgumentType.blockPos()).
                        then(argument("radius", IntegerArgumentType.integer(1)).
                                then(argument("block", BlockStateArgumentType.blockState()).
                                        executes((c) -> drawSphere(c.getSource(),
                                                BlockPosArgumentType.getBlockPos(c, "center"),
                                                IntegerArgumentType.getInteger(c, "radius"),
                                                BlockStateArgumentType.getBlockState(c, "block"), null))
                                        .then(literal("replace")
                                                .then(argument("filter", BlockPredicateArgumentType.blockPredicate())
                                                        .executes((c) -> drawSphere(c.getSource(),
                                                                BlockPosArgumentType.getBlockPos(c, "center"),
                                                                IntegerArgumentType.getInteger(c, "radius"),
                                                                BlockStateArgumentType.getBlockState(c, "block"),
                                                                BlockPredicateArgumentType.getBlockPredicate(c,
                                                                        "filter")))))))))
                    .then(literal("diamond").then(argument("center", BlockPosArgumentType.blockPos())
                        .then(argument("radius", IntegerArgumentType.integer(1))
                            .then(argument("block", BlockStateArgumentType.blockState())
                                .executes((c) -> drawDiamond(c.getSource(),
                                    BlockPosArgumentType.getBlockPos(c, "center"),
                                    IntegerArgumentType.getInteger(c, "radius"),
                                    BlockStateArgumentType.getBlockState(c, "block"), null))
                                        .then(literal("replace")
                                            .then(argument("filter", BlockPredicateArgumentType.blockPredicate())
                                                .executes((c) -> drawDiamond(c.getSource(),
                                                   BlockPosArgumentType.getBlockPos(c, "center"),
                                                   IntegerArgumentType.getInteger(c, "radius"),
                                                   BlockStateArgumentType.getBlockState(c, "block"),
                                                   BlockPredicateArgumentType.getBlockPredicate(c,"filter")
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                    )
                    .then(literal("pyramid")
                        .then(argument("center", BlockPosArgumentType.blockPos())
                            .then(argument("radius", IntegerArgumentType.integer(1))
                                .then(argument("height",IntegerArgumentType.integer(1))
                                    .then(argument("pointing up?",BoolArgumentType.bool())
                                        .then(argument("type",StringArgumentType.string())
                                            .suggests( (c, b) -> suggestMatching(new String[]{"square","circle"},b))
                                            .then(argument("orientation",StringArgumentType.string())
                                            .suggests( (c, b) -> suggestMatching(new String[]{"x","y","z"},b))
                                                .then(argument("block", BlockStateArgumentType.blockState())
                                                    .executes((c) -> drawPyramid(c.getSource(),
                                                        BlockPosArgumentType.getBlockPos(c, "center"),
                                                        StringArgumentType.getString(c,"type"),
                                                        IntegerArgumentType.getInteger(c, "radius"),
                                                        IntegerArgumentType.getInteger(c, "height"),
                                                        BoolArgumentType.getBool(c,"pointing up?"),
                                                        StringArgumentType.getString(c,"orientation"),
                                                        BlockStateArgumentType.getBlockState(c, "block"), null)
                                                    )
                                                    .then(literal("replace")
                                                        .then(argument("filter", BlockPredicateArgumentType.blockPredicate())
                                                            .executes((c) -> drawPyramid(c.getSource(),
                                                                BlockPosArgumentType.getBlockPos(c, "center"),
                                                                StringArgumentType.getString(c,"type"),
                                                                IntegerArgumentType.getInteger(c, "radius"),
                                                                IntegerArgumentType.getInteger(c, "height"),
                                                                BoolArgumentType.getBool(c,"pointing up?"),
                                                                StringArgumentType.getString(c,"orientation"),
                                                                BlockStateArgumentType.getBlockState(c, "block"),
                                                                BlockPredicateArgumentType.getBlockPredicate(c,"filter")
                                                                )
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    );
        dispatcher.register(command);
    }

    private static int drawSphere(ServerCommandSource source, BlockPos pos, int radius, BlockStateArgument block,
            Predicate<CachedBlockPosition> replacement)
    {
        return drawSphere(source, pos, (double) radius, (double) radius, (double) radius, block, replacement, false);
    }

    private static int drawSphere(ServerCommandSource source, BlockPos pos, double radiusX, double radiusY,
            double radiusZ, BlockStateArgument block, Predicate<CachedBlockPosition> replacement, boolean solid)
    {
        int affected = 0;
        ServerWorld world = source.getWorld();

        radiusX += 0.5;
        radiusY += 0.5;
        radiusZ += 0.5;

        final double invRadiusX = 1 / radiusX;
        final double invRadiusY = 1 / radiusY;
        final double invRadiusZ = 1 / radiusZ;

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusY = (int) Math.ceil(radiusY);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        BlockPos.Mutable mbpos = new BlockPos.Mutable(pos);
        List<BlockPos> list = Lists.newArrayList();

        double nextXn = 0;

        forX: for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * invRadiusX;
            double nextYn = 0;
            forY: for (int y = 0; y <= ceilRadiusY; ++y) {
                final double yn = nextYn;
                nextYn = (y + 1) * invRadiusY;
                double nextZn = 0;
                forZ: for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    nextZn = (z + 1) * invRadiusZ;

                    double distanceSq = lengthSq(xn, yn, zn);
                    if (distanceSq > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break forZ;
                    }

                    if (!solid) {
                        if (lengthSq(nextXn, yn, zn) <= 1 && lengthSq(xn, nextYn, zn) <= 1 && lengthSq(xn, yn, nextZn) <= 1)
                        {
                            continue;
                        }
                    }

                    CarpetSettings.impendingFillSkipUpdates = !CarpetSettings.fillUpdates;
                    for (int xmod = -1; xmod < 2; xmod += 2)
                    {
                        for (int ymod = -1; ymod < 2; ymod += 2)
                        {
                            for (int zmod = -1; zmod < 2; zmod += 2)
                            {
                                mbpos.set(pos.getX() + xmod * x, pos.getY() + ymod * y, pos.getZ() + zmod * z);
                                if (replacement == null
                                        || replacement.test(new CachedBlockPosition(world, mbpos, true))) 
                                {
                                    BlockEntity tileentity = world.getBlockEntity(mbpos);
                                    if (tileentity instanceof Inventory)
                                    {
                                        ((Inventory) tileentity).clear();
                                    }

                                    if (block.setBlockState(world, mbpos, 2))
                                    {
                                        list.add(mbpos.toImmutable());
                                        ++affected;
                                    }
                                }
                            }
                        }
                    }
                    CarpetSettings.impendingFillSkipUpdates = false;
                }
            }
        }
        if (CarpetSettings.fillUpdates) {

            for (BlockPos blockpos1 : list) {
                Block blokc = world.getBlockState(blockpos1).getBlock();
                world.updateNeighbors(blockpos1, blokc); // or always version????
            }
        }
        Messenger.m(source, "gi Filled " + affected + " blocks");

        return 1;
    }

    private static double lengthSq(double x, double y, double z) {
        return (x * x) + (y * y) + (z * z);
    }

    private static int blockset(ServerCommandSource source, int x, int y, int z, Predicate<CachedBlockPosition> replacement, List<BlockPos> list, 
        BlockPos.Mutable mbpos, BlockStateArgument block){
        ServerWorld world = source.getWorld();
        mbpos.set(x, y, z);
        int success=0;
        if (replacement == null || replacement.test(new CachedBlockPosition(world, mbpos, true))) {
            BlockEntity tileentity = world.getBlockEntity(mbpos);
            if (tileentity instanceof Inventory) {
                ((Inventory) tileentity).clear();
            }
            if (block.setBlockState(world, mbpos, 2)) {
                list.add(mbpos.toImmutable());
                ++success;
            }
        }

        return success;
    }

    private static int drawDiamond(ServerCommandSource source, BlockPos pos, int radius, BlockStateArgument block,
    Predicate<CachedBlockPosition> replacement) {
        return (int) drawDiamond(source, pos, radius, block, replacement, false);
    }

    private static int drawDiamond(ServerCommandSource source, BlockPos pos, int radius,BlockStateArgument block,
            Predicate<CachedBlockPosition> replacement, boolean solid) {
        int affected=0;

        BlockPos.Mutable mbpos = new BlockPos.Mutable(pos);
        List<BlockPos> list = Lists.newArrayList();

        ServerWorld world = source.getWorld();

        CarpetSettings.impendingFillSkipUpdates = !CarpetSettings.fillUpdates;

        for (int r = 0; r < radius; ++r) {
            int y=r-radius+1;
            for (int x = -r; x <= r; ++x) {
                int z=r-Math.abs(x);

                affected+=blockset(source, pos.getX()+x, pos.getY()-y, pos.getZ()+z, replacement, list, mbpos, block);
                affected+=blockset(source, pos.getX()+x, pos.getY()-y, pos.getZ()-z, replacement, list, mbpos, block);
                affected+=blockset(source, pos.getX()+x, pos.getY()+y, pos.getZ()+z, replacement, list, mbpos, block);
                affected+=blockset(source, pos.getX()+x, pos.getY()+y, pos.getZ()-z, replacement, list, mbpos, block);
            }
        }

        CarpetSettings.impendingFillSkipUpdates = false;

        if (CarpetSettings.fillUpdates) {

            for (BlockPos blockpos1 : list) {
                Block blokc = world.getBlockState(blockpos1).getBlock();
                world.updateNeighbors(blockpos1, blokc);
            }
        }

        Messenger.m(source, "gi Filled " + affected + " blocks");

        return 1;
    }

    private static int drawCircle(ServerCommandSource source, BlockPos pos, int offset, int radius,BlockStateArgument block, String orientation,
    Predicate<CachedBlockPosition> replacement, List<BlockPos> list, BlockPos.Mutable mbpos){
        int successes=0;
        for(int axis1=-radius;axis1<=radius;++axis1){

            for(int axis2=-radius;axis2<=radius;++axis2){

                if(axis1*axis1+axis2*axis2<=radius*radius){
                    switch (orientation){
                        case "x":successes+=blockset(source, pos.getX()+offset, pos.getY()+axis1, pos.getZ()+axis2,replacement, list, mbpos, block);break;
                        case "y":successes+=blockset(source, pos.getX()+axis1, pos.getY()+offset, pos.getZ()+axis2,replacement, list, mbpos, block);break;
                        case "z":successes+=blockset(source, pos.getX()+axis2, pos.getY()+axis1, pos.getZ()+offset,replacement, list, mbpos, block);break;
                        default:Messenger.m(source,"gi Invalid orientation, must be x, y or z");return 0;
                    }
                }
            }
        }
        return successes;
    }
    private static int drawSquare(ServerCommandSource source, BlockPos pos, int offset, int radius, BlockStateArgument block, String orientation,
    Predicate<CachedBlockPosition> replacement, List<BlockPos> list, BlockPos.Mutable mbpos){
        int success=0;

        for(int axis1=-radius;axis1<=radius;++axis1){

            for(int axis2=-radius;axis2<=radius;++axis2){
                switch (orientation){
                    case "x":success+=blockset(source, pos.getX()+offset, pos.getY()+axis1, pos.getZ()+axis2,replacement, list, mbpos, block);break;
                    case "y":success+=blockset(source, pos.getX()+axis1, pos.getY()+offset, pos.getZ()+axis2,replacement, list, mbpos, block);break;
                    case "z":success+=blockset(source, pos.getX()+axis2, pos.getY()+axis1, pos.getZ()+offset,replacement, list, mbpos, block);break;
                    default:Messenger.m(source,"gi Invalid orientation, must be x, y or z");return 0;
                }
            }
        }

        return success;
    }
    
    private static int drawPyramid(ServerCommandSource source, BlockPos pos, String shape, int radius, int height, boolean pointup, String orientation,  BlockStateArgument block,
    Predicate<CachedBlockPosition> replacement) {
        return (int) drawPyramid(source, pos, radius, height, block, replacement, pointup, orientation, false, shape);
    }

    private static double drawPyramid(ServerCommandSource source, BlockPos pos, int radius, int height, BlockStateArgument block,
            Predicate<CachedBlockPosition> replacement, boolean pointup, String orientation, boolean solid, String shape) {
        int affected = 0;
        BlockPos.Mutable mbpos = new BlockPos.Mutable(pos);

        List<BlockPos> list = Lists.newArrayList();

        ServerWorld world = source.getWorld();

        CarpetSettings.impendingFillSkipUpdates = !CarpetSettings.fillUpdates;

        int r = radius;

        for(int i =0; i<height;++i) {
            if(pointup){
                r=radius-radius*i/height-1;
            }else if(!pointup){
                r=radius*i/height;
            }
            switch(shape){
                case "circle":affected+=drawCircle(source, pos, i, (int) Math.round(r), block, orientation, replacement, list, mbpos);break;
                
                case "square":affected+=drawSquare(source, pos, i, (int) Math.round(r), block, orientation, replacement, list, mbpos);break;
                
                default:Messenger.m(source,"gi "+shape+" is an incorrect shape, read options for which shapes are available");return 0;
                
            }
        }
        
        CarpetSettings.impendingFillSkipUpdates = false;

        if (CarpetSettings.fillUpdates) {

            for (BlockPos blockpos1 : list) {
                Block blokc = world.getBlockState(blockpos1).getBlock();
                world.updateNeighbors(blockpos1, blokc);
            }
        }

        Messenger.m(source, "gi Filled " + affected + " blocks");

        return 1;
    }
}
