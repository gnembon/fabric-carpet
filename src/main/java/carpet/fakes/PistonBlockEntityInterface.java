package carpet.fakes;

import net.minecraft.block.entity.BlockEntity;

public interface PistonBlockEntityInterface
{
    void setCarriedBlockEntity(BlockEntity blockEntity);
    BlockEntity getCarriedBlockEntity();
    void setRenderCarriedBlockEntity(boolean b);
    boolean getRenderCarriedBlockEntity();
    boolean isRenderModeSet();
}
