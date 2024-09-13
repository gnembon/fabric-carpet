package carpet.mixins;

import carpet.fakes.WorldChunkInterface;
import carpet.fakes.LevelInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Level.class)
public abstract class Level_movableBEMixin implements LevelInterface, LevelAccessor
{
    @Shadow
    @Final
    public boolean isClientSide;

    @Shadow
    public abstract LevelChunk getChunkAt(BlockPos blockPos_1);
    
    @Shadow
    public abstract void setBlocksDirty(BlockPos blockPos_1, BlockState s1, BlockState s2);
    
    @Shadow
    public abstract void sendBlockUpdated(BlockPos var1, BlockState var2, BlockState var3, int var4);
    
    @Shadow
    public abstract void updateNeighborsAt(BlockPos blockPos_1, Block block_1);
    
    @Shadow
    public abstract void onBlockStateChange(BlockPos blockPos_1, BlockState blockState_1, BlockState blockState_2);

    @Shadow public abstract void updateNeighbourForOutputSignal(BlockPos pos, Block block);

    @Shadow public abstract boolean isDebug();

    /**
     * @author 2No2Name
     */
    @Override
    public boolean setBlockStateWithBlockEntity(BlockPos blockPos_1, BlockState blockState_1, BlockEntity newBlockEntity, int int_1)
    {
        if (isOutsideBuildHeight(blockPos_1) || !this.isClientSide && isDebug()) return false;
        LevelChunk worldChunk_1 = this.getChunkAt(blockPos_1);
        Block block_1 = blockState_1.getBlock();

        BlockState blockState_2;
        if (newBlockEntity != null && block_1 instanceof EntityBlock)
        {
            blockState_2 = ((WorldChunkInterface) worldChunk_1).setBlockStateWithBlockEntity(blockPos_1, blockState_1, newBlockEntity, (int_1 & 64) != 0);
            if (newBlockEntity instanceof LidBlockEntity)
            {
                scheduleTick(blockPos_1, block_1, 5);
            }
        }
        else
        {
            blockState_2 = worldChunk_1.setBlockState(blockPos_1, blockState_1, (int_1 & 64) != 0);
        }

        if (blockState_2 == null)
        {
            return false;
        }
        else
        {
            BlockState blockState_3 = this.getBlockState(blockPos_1);

            if (blockState_3 != blockState_2 && (blockState_3.getLightBlock() != blockState_2.getLightBlock() || blockState_3.getLightEmission() != blockState_2.getLightEmission() || blockState_3.useShapeForLightOcclusion() || blockState_2.useShapeForLightOcclusion()))
            {
                ProfilerFiller profiler = Profiler.get();
                profiler.push("queueCheckLight");
                this.getChunkSource().getLightEngine().checkBlock(blockPos_1);
                profiler.pop();
            }

            if (blockState_3 == blockState_1)
            {
                if (blockState_2 != blockState_3)
                {
                    this.setBlocksDirty(blockPos_1, blockState_2, blockState_3);
                }

                if ((int_1 & 2) != 0 && (!this.isClientSide || (int_1 & 4) == 0) && (this.isClientSide || worldChunk_1.getFullStatus() != null && worldChunk_1.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)))
                {
                    this.sendBlockUpdated(blockPos_1, blockState_2, blockState_1, int_1);
                }

                if (!this.isClientSide && (int_1 & 1) != 0)
                {
                    this.updateNeighborsAt(blockPos_1, blockState_2.getBlock());
                    if (blockState_1.hasAnalogOutputSignal())
                    {
                        updateNeighbourForOutputSignal(blockPos_1, block_1);
                    }
                }

                if ((int_1 & 16) == 0)
                {
                    int int_2 = int_1 & -34;
                    blockState_2.updateIndirectNeighbourShapes(this, blockPos_1, int_2); // prepare
                    blockState_1.updateNeighbourShapes(this, blockPos_1, int_2); // updateNeighbours
                    blockState_1.updateIndirectNeighbourShapes(this, blockPos_1, int_2); // prepare
                }
                this.onBlockStateChange(blockPos_1, blockState_2, blockState_3);
            }
            return true;
        }
    }
}
