package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InfestedBlock.class)
public abstract class InfestedBlock_gravelMixin extends Block
{
    public InfestedBlock_gravelMixin(Properties block$Settings_1)
    {
        super(block$Settings_1);
    }

    @Inject(method = "spawnInfestation", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/entity/monster/Silverfish;spawnAnim()V"))
    private void onOnStacksDropped(ServerLevel serverWorld, BlockPos pos, CallbackInfo ci)
    {
        if (CarpetSettings.silverFishDropGravel)
        {
            popResource(serverWorld, pos, new ItemStack(Blocks.GRAVEL));
        }
    }
}