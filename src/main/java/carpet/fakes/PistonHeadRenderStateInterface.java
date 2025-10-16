package carpet.fakes;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public interface PistonHeadRenderStateInterface {
    public BlockEntityRenderState getMovedBERenderState();
    public void setMovedBERenderState(BlockEntityRenderState state);
}
