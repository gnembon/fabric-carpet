package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CactusBlock.class)
public abstract class AbstractCactusBlock_zeroTickPlantsMixin {
    @Shadow
    public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {}
    
    /**
     * @author silverisntgold
     *
     * @reason Instead of tick() always terminating after it has checked
     * if a plant needs to be broken (current vanilla behavior),
     * call the plants randomTick() if the attempt to break the block fails
     */
    @Overwrite
    public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        if (!blockState.canSurvive(serverLevel, blockPos)) {
            serverLevel.destroyBlock(blockPos, true);
        } else if (CarpetSettings.zeroTickPlants) {
            this.randomTick(blockState, serverLevel, blockPos, randomSource);
        }
    }
}
