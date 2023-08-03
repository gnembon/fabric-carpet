package carpet.mixins;

import carpet.fakes.LevelInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.BlockGetter;
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
    public abstract BlockState getBlockState(BlockPos blockPos_1);
    
    //@Shadow
    //public abstract ChunkManager getChunkManager();
    
    @Shadow
    public abstract void setBlocksDirty(BlockPos blockPos_1, BlockState s1, BlockState s2);
    
    @Shadow
    public abstract void sendBlockUpdated(BlockPos var1, BlockState var2, BlockState var3, int var4);
    
    @Shadow
    public abstract void updateNeighborsAt(BlockPos blockPos_1, Block block_1);
    
    @Shadow
    public abstract void onBlockStateChange(BlockPos blockPos_1, BlockState blockState_1, BlockState blockState_2);

    @Shadow public abstract ProfilerFiller getProfiler();

    @Shadow public abstract void updateNeighbourForOutputSignal(BlockPos pos, Block block);

    //@Shadow public abstract boolean setBlockState(BlockPos pos, BlockState state, int flags);

    @Shadow public abstract boolean isDebug();

    /**
     * @author 2No2Name
     */
    @Override
    public boolean carpet$setBlockStateWithBlockEntity(BlockPos pos, BlockState state, BlockEntity blockEntity, int flags)
    {
        if (isOutsideBuildHeight(pos) || !this.isClientSide && isDebug()) return false;
        LevelChunk chunk = this.getChunkAt(pos);
        Block block = state.getBlock();

        BlockState prevState;
        if (blockEntity != null && block instanceof EntityBlock)
        {
            prevState = chunk.carpet$setBlockStateWithBlockEntity(pos, state, blockEntity, (flags & 64) != 0);
            if (blockEntity instanceof LidBlockEntity)
            {
                scheduleTick(pos, block, 5);
            }
        }
        else
        {
            prevState = chunk.setBlockState(pos, state, (flags & 64) != 0);
        }

        if (prevState == null)
        {
            return false;
        }
        else
        {
            BlockState newState = this.getBlockState(pos);

            if (newState != prevState && (newState.getLightBlock((BlockGetter) this, pos) != prevState.getLightBlock((BlockGetter) this, pos) || newState.getLightEmission() != prevState.getLightEmission() || newState.useShapeForLightOcclusion() || prevState.useShapeForLightOcclusion()))
            {
                ProfilerFiller profiler = getProfiler();
                profiler.push("queueCheckLight");
                this.getChunkSource().getLightEngine().checkBlock(pos);
                profiler.pop();
            }

            if (newState == state)
            {
                if (prevState != newState)
                {
                    this.setBlocksDirty(pos, prevState, newState);
                }

                if ((flags & 2) != 0 && (!this.isClientSide || (flags & 4) == 0) && (this.isClientSide || chunk.getFullStatus() != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)))
                {
                    this.sendBlockUpdated(pos, prevState, state, flags);
                }

                if (!this.isClientSide && (flags & 1) != 0)
                {
                    this.updateNeighborsAt(pos, prevState.getBlock());
                    if (state.hasAnalogOutputSignal())
                    {
                        updateNeighbourForOutputSignal(pos, block);
                    }
                }

                if ((flags & 16) == 0)
                {
                    int flagsForNeighborUpdate = flags & -34;
                    prevState.updateIndirectNeighbourShapes(this, pos, flagsForNeighborUpdate);
                    state.updateNeighbourShapes(this, pos, flagsForNeighborUpdate);
                    state.updateIndirectNeighbourShapes(this, pos, flagsForNeighborUpdate);
                }
                this.onBlockStateChange(pos, prevState, newState);
            }
            return true;
        }
    }
}
