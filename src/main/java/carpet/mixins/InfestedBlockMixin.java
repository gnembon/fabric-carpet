package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.InfestedBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InfestedBlock.class)
public abstract class InfestedBlockMixin extends Block
{
    public InfestedBlockMixin(Settings block$Settings_1)
    {
        super(block$Settings_1);
    }

    @Inject(method = "spawnSilverfish", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/entity/mob/SilverfishEntity;playSpawnEffects()V"))
    private void onOnStacksDropped(World world, BlockPos blockPos, CallbackInfo ci)
    {
        if (CarpetSettings.silverFishDropGravel)
        {
            dropStack(world, blockPos, new ItemStack(Blocks.GRAVEL));
        }
    }
}