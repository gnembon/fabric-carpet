package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.block.piston.PistonBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BuddingAmethystBlock.class)
public class BuddingAmethystBlock_getPistonBehaviorMixin {
    @Inject(at = @At("HEAD"), method = "getPistonBehavior(Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/piston/PistonBehavior;", cancellable = true)
    void getPistonBehavior(BlockState state, CallbackInfoReturnable<PistonBehavior> cir) {
        if (CarpetSettings.pushableAmethyst) cir.setReturnValue(PistonBehavior.NORMAL);
    }
}
