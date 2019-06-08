package carpet.mixins;

import carpet.helpers.BlockRotator;
import net.minecraft.Bootstrap;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public abstract class Bootstrap_cactusMixin
{
    @Inject(method = "initialize", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/dispenser/DispenserBehavior;registerDefaults()V",
            shift = At.Shift.AFTER
    ))
    private static void registerCactus(CallbackInfo ci)
    {
        DispenserBlock.registerBehavior(Blocks.CACTUS.asItem(), new BlockRotator.CactusDispenserBehaviour());
    }

}
