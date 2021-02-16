package carpet.mixins;

import carpet.CarpetSettings;
import carpet.network.ClientNetworkHandler;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(StructureBlockScreen.class)
public abstract class StructureBlockScreen_structureBlockLimitMixin
{
	@Shadow @Final private StructureBlockBlockEntity structureBlock;

	@Inject(method = "method_2516", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void sendCarpetStructureBlockPacket(StructureBlockBlockEntity.Action action, CallbackInfoReturnable<Boolean> cir, BlockPos blockPos, BlockPos blockPos2)
    {
        if (CarpetSettings.structureBlockLimit != CarpetSettings.VANILLA_STRUCTURE_BLOCK_LIMIT)
        {
            ClientNetworkHandler.sendCarpetStructureBlockPacket(this.structureBlock.getPos(), blockPos, blockPos2);
        }
    }
}
