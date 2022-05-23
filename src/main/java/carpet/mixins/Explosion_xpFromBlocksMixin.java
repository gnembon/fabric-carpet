package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Explosion.class)
public class Explosion_xpFromBlocksMixin {

    @Redirect(method = "finalizeExplosion", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"
    ))
    private void spawnXPAfterBreak(BlockState instance, ServerLevel serverLevel, BlockPos blockPos, ItemStack itemStack, boolean b)
    {
        instance.spawnAfterBreak(serverLevel, blockPos, itemStack, b || CarpetSettings.xpFromExplosions);
    }
}
