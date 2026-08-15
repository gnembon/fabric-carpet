package carpet.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static carpet.script.CarpetEventServer.Event.BLOCK_FORMS;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlock_scarpetEventMixin
{
    private static boolean onLiquidBlockFormed(Level level, BlockPos pos, BlockState newState)
    {
        if (BLOCK_FORMS.isNeeded() && level instanceof ServerLevel serverLevel)
        {
            BlockState previous = level.getBlockState(pos);
            if (BLOCK_FORMS.onBlockForms(serverLevel, pos, previous, newState))
            {
                return false;
            }
        }
        return level.setBlockAndUpdate(pos, newState);
    }

    @Redirect(method = "shouldSpreadLiquid", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 0
    ))
    private boolean onObsidianOrCobblestoneForm(Level level, BlockPos pos, BlockState newState)
    {
        return onLiquidBlockFormed(level, pos, newState);
    }

    @Redirect(method = "shouldSpreadLiquid", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 1
    ))
    private boolean onBasaltForm(Level level, BlockPos pos, BlockState newState)
    {
        return onLiquidBlockFormed(level, pos, newState);
    }
}
