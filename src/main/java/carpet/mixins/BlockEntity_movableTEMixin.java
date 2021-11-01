package carpet.mixins;

import carpet.fakes.BlockEntityInterface;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockEntity.class)
public abstract class BlockEntity_movableTEMixin implements BlockEntityInterface
{
    @Mutable
    @Shadow @Final protected BlockPos pos;

    public void setCMPos(BlockPos newPos)
    {
        pos = newPos;
    };
}
