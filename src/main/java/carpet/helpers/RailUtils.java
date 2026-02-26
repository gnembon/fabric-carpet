package carpet.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

public class RailUtils {
    public static void powerRailLine(BlockPos pos, RailShape shape, boolean powerAdjacentLines, Level level) {
        for (Direction dir : getStraightRailDirections(shape)) {
            BlockPos checking = new BlockPos(pos);

            while (true) {
                BlockState state = level.getBlockState(checking);
                if (!(state.getBlock() instanceof PoweredRailBlock)) {
                    break;
                }
                if (state.getValue(PoweredRailBlock.POWERED) && !checking.equals(pos)) {
                    break;
                }

                level.setBlock(checking, level.getBlockState(checking).setValue(PoweredRailBlock.POWERED, true), 2);
                if (powerAdjacentLines) {
                    for (BlockPos adjacentPos : adjacentRails(checking, shape, level)) {
                        if (adjacentPos == null) continue;
                        powerRailLine(adjacentPos,
                                level.getBlockState(adjacentPos).getValue(PoweredRailBlock.SHAPE),
                                true,
                                level);
                    }
                }

                checking = checking.relative(dir);
            }
        }
    }

    private static BlockPos[] adjacentRails(BlockPos pos, RailShape shape, Level level) {
        Direction[] dirs = switch (shape) {
            case NORTH_SOUTH -> new Direction[]{Direction.EAST, Direction.WEST};
            case EAST_WEST -> new Direction[]{Direction.NORTH, Direction.SOUTH};
            default -> throw new IllegalArgumentException("Invalid rail shape, rail shape must be straight");
        };

        BlockPos[] positions = new BlockPos[]{null, null};
        for (int i = 0; i < 2; i++) {
            BlockPos checking = pos.relative(dirs[i]);
            if (level.getBlockState(checking).getBlock() instanceof PoweredRailBlock) {
                positions[i] = checking;
            }
        }

        return positions;
    }

    public static Direction[] getStraightRailDirections(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH -> new Direction[]{Direction.NORTH, Direction.SOUTH};
            case EAST_WEST -> new Direction[]{Direction.EAST, Direction.WEST};
            default -> throw new IllegalArgumentException("Invalid rail shape, rail shape must be straight");
        };
    }
}
