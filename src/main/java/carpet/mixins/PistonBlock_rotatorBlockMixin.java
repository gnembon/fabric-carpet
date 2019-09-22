package carpet.mixins;

import carpet.fakes.PistonBlockInterface;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PistonBlock.class)
public abstract class PistonBlock_rotatorBlockMixin implements PistonBlockInterface
{
    @Shadow protected abstract boolean shouldExtend(World world_1, BlockPos blockPos_1, Direction direction_1);

    @Override
    public boolean publicShouldExtend(World world_1, BlockPos blockPos_1, Direction direction_1)
    {
        return shouldExtend(world_1, blockPos_1,direction_1);
    }
}
