package carpet.mixins;

import carpet.fakes.PistonHeadRenderStateInterface;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PistonHeadRenderState.class)
public class PistonHeadRenderState_movableBEMixin implements PistonHeadRenderStateInterface {
    private BlockEntityRenderState blockEntityRenderState;
    @Override
    public BlockEntityRenderState getMovedBERenderState() {
        return blockEntityRenderState;
    }

    @Override
    public void setMovedBERenderState(BlockEntityRenderState state) {
        this.blockEntityRenderState = state;
    }
}
