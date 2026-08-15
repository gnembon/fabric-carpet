package carpet.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static carpet.script.CarpetEventServer.Event.BLOCK_FORMS;

@Mixin(ConcretePowderBlock.class)
public abstract class ConcretePowderBlock_scarpetEventMixin extends FallingBlock
{
    @Shadow
    @Final
    private Block concrete;

    protected ConcretePowderBlock_scarpetEventMixin(Properties properties)
    {
        super(properties);
    }

    @Redirect(method = "onLand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
    ))
    private boolean onConcreteLand(Level level, BlockPos pos, BlockState newState)
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

    @ModifyExpressionValue(method = "updateShape", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/ConcretePowderBlock;touchesLiquid(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"
    ))
    private boolean onConcreteTouchesLiquid(boolean touchesLiquid, BlockState blockState, LevelReader levelReader, ScheduledTickAccess scheduledTickAccess,
            BlockPos blockPos, Direction direction, BlockPos blockPos2, BlockState blockState2, RandomSource randomSource)
    {
        if (touchesLiquid && BLOCK_FORMS.isNeeded() && levelReader instanceof ServerLevel serverLevel)
        {
            if (BLOCK_FORMS.onBlockForms(serverLevel, blockPos, blockState, this.concrete.defaultBlockState()))
            {
                return false;
            }
        }
        return touchesLiquid;
    }
}
