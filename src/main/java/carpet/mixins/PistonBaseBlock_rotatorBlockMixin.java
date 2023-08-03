package carpet.mixins;

import carpet.fakes.PistonBaseBlockInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlock_rotatorBlockMixin implements PistonBaseBlockInterface
{
    @Shadow protected abstract boolean getNeighborSignal(SignalGetter level, BlockPos pos, Direction facing);

    @Override
    public boolean carpet$getNeighborSignal(Level level, BlockPos pos, Direction facing)
    {
        return getNeighborSignal(level, pos, facing);
    }
}
