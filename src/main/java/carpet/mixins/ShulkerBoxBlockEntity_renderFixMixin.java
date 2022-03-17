package carpet.mixins;


import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShulkerBoxRenderer.class)
public class ShulkerBoxBlockEntity_renderFixMixin
{
    private BlockState _be;
    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 0)
    private ShulkerBoxBlockEntity injected(ShulkerBoxBlockEntity be) {
        _be=be.getBlockState();
        return be;
    }
    @Redirect(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
    ))
    private net.minecraft.world.level.block.state.BlockState rewrite(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos bp)
    {
        return _be;
    }
}
