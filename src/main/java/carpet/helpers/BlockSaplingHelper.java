package carpet.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Material;

public class BlockSaplingHelper
{
    // Added code for checking water for dead shrub rule
    public static boolean hasWater(LevelAccessor worldIn, BlockPos pos)
    {
        for (BlockPos blockpos$mutableblockpos : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, 1, 4)))
        {
            if (worldIn.getBlockState(blockpos$mutableblockpos).getMaterial() == Material.WATER)
            {
                return true;
            }
        }

        return false;
    }
}
