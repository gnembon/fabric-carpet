package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviourBlockStateBase_mixin
{
    @Shadow public abstract Block getBlock();

    @Inject(method = "getPistonPushReaction", at = @At("HEAD"), cancellable = true)
    private void onGetPistonPushReaction(CallbackInfoReturnable<PushReaction> cir)
    {
        if (CarpetSettings.movableAmethyst && getBlock() instanceof BuddingAmethystBlock)
        {
            cir.setReturnValue(PushReaction.NORMAL);
        }
    }
}
