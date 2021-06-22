package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndGatewayBlockEntity.class)
public class EndGatewayBlockEntity_creativeNoClipMixin
{
    @Inject(method = "method_30276", cancellable = true, at = @At("HEAD"))
    private static void checkFlyingCreative(Entity entity, CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.isCreativeFlying(entity)) cir.setReturnValue(false);
    }
}
