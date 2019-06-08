package carpet.mixins;

import carpet.helpers.BlockRotator;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBehavior.class)
public interface DispenserBehaviour_cactusMixin
{
    @SuppressWarnings("PublicStaticMixinMember")
    @Inject(method = "registerDefaults", at = @At("HEAD"))
    static void addBlockRotator(CallbackInfo ci)
    {
        DispenserBlock.registerBehavior(Blocks.CACTUS.asItem(), new BlockRotator.CactusDispenserBehaviour());
    }
}
