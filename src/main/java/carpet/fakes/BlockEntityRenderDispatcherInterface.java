package carpet.fakes;

import net.minecraft.block.entity.BlockEntity;

public interface BlockEntityRenderDispatcherInterface
{
    void renderBlockEntityOffset(BlockEntity blockEntity, float partialTicks, int destroyStage, double xOffset, double yOffset, double zOffset);
}
