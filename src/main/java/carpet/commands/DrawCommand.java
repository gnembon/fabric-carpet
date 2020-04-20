package carpet.commands;

import carpet.CarpetSettings;
import carpet.settings.SettingsManager;
import carpet.utils.Messenger;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.block.Block;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.BlockPredicateArgumentType;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.command.arguments.BlockStateArgumentType;
import net.minecraft.inventory.Inventory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.function.Predicate;
import java.lang.Math;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DrawCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> command = literal("draw")
                .requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandDraw))
                .then(literal("sphere").then(argument("center", BlockPosArgumentType.blockPos())
                        .then(argument("radius", IntegerArgumentType.integer(1))
                                .then(argument("block", BlockStateArgumentType.blockState())
                                        .executes((c) -> drawSphere(c.getSource(),
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
                        .then(literal("cone")
                            .then(argument("center", BlockPosArgumentType.blockPos())
                                .then(argument("radius", IntegerArgumentType.integer(1))
                                    .then(argument("height",IntegerArgumentType.integer(1))
                                        .then(argument("pointing up?",BoolArgumentType.bool())
                                            .then(literal("x")
                                                .then(argument("block", BlockStateArgumentType.blockState())
                                                    .executes((c) -> drawCone(c.getSource(),
                                                        BlockPosArgumentType.getBlockPos(c, "center"),
                                                        IntegerArgumentType.getInteger(c, "radius"),
                                                        IntegerArgumentType.getInteger(c, "height"),
                                                        BoolArgumentType.getBool(c,"pointing up?"),"x",
                                                        BlockStateArgumentType.getBlockState(c, "block"), null)
                                                    )
                                                    .then(literal("replace")
                                                        .then(argument("filter", BlockPredicateArgumentType.blockPredicate())
                                                            .executes((c) -> drawCone(c.getSource(),
                                                                BlockPosArgumentType.getBlockPos(c, "center"),
                                                                IntegerArgumentType.getInteger(c, "radius"),
                                                                IntegerArgumentType.getInteger(c, "height"),
                                                                BoolArgumentType.getBool(c,"pointing up?"),"x",
                                                                BlockStateArgumentType.getBlockState(c, "block"),
                                                                BlockPredicateArgumentType.getBlockPredicate(c,"filter")
                                                                )
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                            .then(literal("y")
                                                .then(argument("block", BlockStateArgumentType.blockState())
                                                    .executes((c) -> drawCone(c.getSource(),
                                                        BlockPosArgumentType.getBlockPos(c, "center"),
                                                        IntegerArgumentType.getInteger(c, "radius"),
                                                        IntegerArgumentType.getInteger(c, "height"),
                                                        BoolArgumentType.getBool(c,"pointing up?"),"y",
                                                        BlockStateArgumentType.getBlockState(c, "block"), null)
                                                    )
                                                    .then(literal("replace")
                                                        .then(argument("filter", BlockPredicateArgumentType.blockPredicate())
                                                            .executes((c) -> drawCone(c.getSource(),
                                                                BlockPosArgumentType.getBlockPos(c, "center"),
                                                                IntegerArgumentType.getInteger(c, "radius"),
                                                                IntegerArgumentType.getInteger(c, "height"),
                                                                BoolArgumentType.getBool(c,"pointing up?"),"y",
                                                                BlockStateArgumentType.getBlockState(c, "block"),
                                                                BlockPredicateArgumentType.getBlockPredicate(c,"filter")
                                                                )
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                            .then(literal("z")
                                                .then(argument("block", BlockStateArgumentType.blockState())
                                                    .executes((c) -> drawCone(c.getSource(),
                                                        BlockPosArgumentType.getBlockPos(c, "center"),
                                                        IntegerArgumentType.getInteger(c, "radius"),
                                                        IntegerArgumentType.getInteger(c, "height"),
                                                        BoolArgumentType.getBool(c,"pointing up?"),"z",
                                                        BlockStateArgumentType.getBlockState(c, "block"), null)
                                                    )
                                                    .then(literal("replace")
                                                        .then(argument("filter", BlockPredicateArgumentType.blockPredicate())
                                                            .executes((c) -> drawCone(c.getSource(),
                                                                BlockPosArgumentType.getBlockPos(c, "center"),
                                                                IntegerArgumentType.getInteger(c, "radius"),
                                                                IntegerArgumentType.getInteger(c, "height"),
                                                                BoolArgumentType.getBool(c,"pointing up?"),"z",
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
            Predicate<CachedBlockPosition> replacement) {
        return drawSphere(source, pos, (double) radius, (double) radius, (double) radius, block, replacement, false);
    }

    private static int drawSphere(ServerCommandSource source, BlockPos pos, double radiusX, double radiusY,
            double radiusZ, BlockStateArgument block, Predicate<CachedBlockPosition> replacement, boolean solid) {
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
                        if (lengthSq(nextXn, yn, zn) <= 1 && lengthSq(xn, nextYn, zn) <= 1
                                && lengthSq(xn, yn, nextZn) <= 1) {
                            continue;
                        }
                    }

                    CarpetSettings.impendingFillSkipUpdates = !CarpetSettings.fillUpdates;
                    for (int xmod = -1; xmod < 2; xmod += 2) {
                        for (int ymod = -1; ymod < 2; ymod += 2) {
                            for (int zmod = -1; zmod < 2; zmod += 2) {
                                mbpos.set(pos.getX() + xmod * x, pos.getY() + ymod * y, pos.getZ() + zmod * z);
                                if (replacement == null
                                        || replacement.test(new CachedBlockPosition(world, mbpos, true))) {
                                    BlockEntity tileentity = world.getBlockEntity(mbpos);
                                    if (tileentity instanceof Inventory) {
                                        ((Inventory) tileentity).clear();
                                    }

                                    if (block.setBlockState(world, mbpos, 2)) {
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

    private static int drawCircle(ServerCommandSource source, BlockPos pos, int offset, int radius, int height, boolean pointup, BlockStateArgument block, String orientation,
    Predicate<CachedBlockPosition> replacement, List<BlockPos> list, BlockPos.Mutable mbpos){
        
        if(orientation=="x"){
            for(int y=-radius;y<=radius;++y){

                for(int z=-radius;z<=radius;++z){

                    if(y*y+z*z<=radius*radius){
                        blockset(source, pos.getX()+offset, pos.getY()+y, pos.getZ()+z, replacement, list, mbpos, block);
                    }
                }
            }
        }

        if(orientation=="y"){
            for(int x=-radius;x<=radius;++x){

                for(int z=-radius;z<=radius;++z){

                    if(x*x+z*z<=radius*radius){
                        blockset(source, pos.getX()+x, pos.getY()+offset, pos.getZ()+z, replacement, list, mbpos, block);
                    }
                }
            }
        }

        if(orientation=="z"){
            for(int y=-radius;y<=radius;++y){

                for(int x=-radius;x<=radius;++x){

                    if(y*y+x*x<=radius*radius){
                        blockset(source, pos.getX()+x, pos.getY()+y, pos.getZ()+offset, replacement, list, mbpos, block);
                    }
                }
            }
        }

        return 1;
    }
    
    private static int drawCone(ServerCommandSource source, BlockPos pos, int radius, int height, boolean pointup, String orientation,  BlockStateArgument block,
    Predicate<CachedBlockPosition> replacement) {
        return (int) drawCone(source, pos, radius, height, block, replacement, pointup, orientation, false);
    }

    private static double drawCone(ServerCommandSource source, BlockPos pos, int radius, int height,BlockStateArgument block,
            Predicate<CachedBlockPosition> replacement, boolean pointup, String orientation, boolean solid) {
        int affected = 0;
        BlockPos.Mutable mbpos = new BlockPos.Mutable(pos);

        List<BlockPos> list = Lists.newArrayList();

        ServerWorld world = source.getWorld();

        CarpetSettings.impendingFillSkipUpdates = !CarpetSettings.fillUpdates;

        int r = radius;

        for(int i =0; i<height;++i){
            if(pointup==true){
                r=radius-radius*i/height-1;
            }
            if(pointup==false){
                r=radius*i/height;
            }
            drawCircle(source, pos, i, (int) Math.round(r), height, pointup, block, orientation, replacement, list, mbpos);
            //circle(cx+_,cy,cz,'x',ceil(r),block) in scarpet implementation, _ is i here, and is added in offset parameter
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
