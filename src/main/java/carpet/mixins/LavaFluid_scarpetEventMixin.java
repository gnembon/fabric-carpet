package carpet.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static carpet.script.CarpetEventServer.Event.BLOCK_FORMS;

@Mixin(LavaFluid.class)
public abstract class LavaFluid_scarpetEventMixin
{
    @Redirect(method = "spreadTo", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
    ))
    private boolean onStoneForm(LevelAccessor level, BlockPos pos, BlockState newState)
    {
        if (BLOCK_FORMS.isNeeded() && level instanceof ServerLevel serverLevel)
        {
            BlockState previous = serverLevel.getBlockState(pos);
            if (BLOCK_FORMS.onBlockForms(serverLevel, pos, previous, newState))
            {
                return false;
            }
        }
        return level.setBlockAndUpdate(pos, newState);
    }
}
