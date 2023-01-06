package carpet.mixins;

import carpet.fakes.PistonBlockInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlock_rotatorBlockMixin implements PistonBlockInterface
{
    @Shadow protected abstract boolean getNeighborSignal(Level world_1, BlockPos blockPos_1, Direction direction_1);

    @Override
    public boolean publicShouldExtend(Level world_1, BlockPos blockPos_1, Direction direction_1)
    {
        return getNeighborSignal(world_1, blockPos_1,direction_1);
    }
}
