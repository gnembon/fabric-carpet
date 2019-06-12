package carpet.mixins;

import carpet.settings.CarpetSettings;
import carpet.fakes.PortalForcerInterface;
import net.minecraft.block.BlockState;
import net.minecraft.block.PortalBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalBlock.class)
public class PortalBlock_portalCachingMixin
{
    @Inject(method = "createPortalAt", at = @At(
            value =  "INVOKE",
            target = "Lnet/minecraft/block/PortalBlock$AreaHelper;createPortal()V"
    ))
    private void onCreatePortal(IWorld iWorld_1, BlockPos blockPos_1, CallbackInfoReturnable<Boolean> cir)
    {
        if (!iWorld_1.isClient() && CarpetSettings.portalCaching)
        {
            ((PortalForcerInterface)((ServerWorld)iWorld_1).getPortalForcer()).invalidateCache();
        }
    }

    @Inject(method = "getStateForNeighborUpdate", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getDefaultState()Lnet/minecraft/block/BlockState;"
    ))
    private void getAirBlockState(BlockState blockState_1, Direction direction_1, BlockState blockState_2, IWorld iWorld_1, BlockPos blockPos_1, BlockPos blockPos_2, CallbackInfoReturnable<BlockState> cir)
    {
        if (!iWorld_1.isClient() && CarpetSettings.portalCaching)
        {
            ((PortalForcerInterface)((ServerWorld)iWorld_1).getPortalForcer()).invalidateCache();
        }
    }

}
