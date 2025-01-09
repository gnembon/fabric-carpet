package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.WorldChunkInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LightEngine;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelChunk.class)
public abstract class LevelChunk_movableBEMixin extends ChunkAccess implements WorldChunkInterface
{
    @Shadow
    @Final
    Level level;

    public LevelChunk_movableBEMixin(ChunkPos pos, UpgradeData upgradeData, LevelHeightAccessor heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable LevelChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biome, inhabitedTime, sectionArrayInitializer, blendingData);
    }

    @Shadow
    /* @Nullable */
    public abstract BlockEntity getBlockEntity(BlockPos blockPos_1, LevelChunk.EntityCreationType worldChunk$CreationType_1);

    @Shadow protected abstract <T extends BlockEntity> void updateBlockEntityTicker(T blockEntity);

    @Shadow public abstract void addAndRegisterBlockEntity(BlockEntity blockEntity);

    // Fix Failure: If a moving BlockEntity is placed while BlockEntities are ticking, this will not find it and then replace it with a new TileEntity!
    // blockEntity_2 = this.getBlockEntity(blockPos_1, WorldChunk.CreationType.CHECK);
    // question is - with the changes in the BE handling this might not be a case anymore
    @Redirect(method = "setBlockState", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity ifGetBlockEntity(LevelChunk worldChunk, BlockPos blockPos_1,
            LevelChunk.EntityCreationType worldChunk$CreationType_1)
    {
        if (!CarpetSettings.movableBlockEntities)
        {
            return this.getBlockEntity(blockPos_1, LevelChunk.EntityCreationType.CHECK);
        }
        else
        {
            return this.level.getBlockEntity(blockPos_1);
        }
    }
    
    
    /**
     * Sets the Blockstate and the BlockEntity.
     * Only sets BlockEntity if Block is BlockEntityProvider, but doesn't check if it actually matches (e.g. can assign beacon to chest entity).
     *
     * @author 2No2Name
     */
    /* @Nullable */
    // todo update me to the new version
    @Override
    public BlockState setBlockStateWithBlockEntity(BlockPos blockPos_1, BlockState newBlockState, BlockEntity newBlockEntity,
            int flags)
    {
        boolean boolean_1 = (flags & 64) != 0;
        int y = blockPos_1.getY();

        LevelChunkSection chunkSection = this.getSection(this.getSectionIndex(y));

        boolean hadOnlyAir = chunkSection.hasOnlyAir();

        if (hadOnlyAir && newBlockState.isAir())
        {
            return null;
        }

        int x = blockPos_1.getX() & 15;
        int chunkY = blockPos_1.getY() & 15;
        int z = blockPos_1.getZ() & 15;
        BlockState oldBlockState = chunkSection.setBlockState(x, chunkY, z, newBlockState);
        if (oldBlockState == newBlockState)
        {
            return null;
        }
        Block newBlock = newBlockState.getBlock();
        this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(x, y, z, newBlockState);
        this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(x, y, z, newBlockState);
        this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(x, y, z, newBlockState);
        this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(x, y, z, newBlockState);
        boolean hasOnlyAir = chunkSection.hasOnlyAir();
        if (hadOnlyAir != hasOnlyAir)
        {
            this.level.getChunkSource().getLightEngine().updateSectionStatus(blockPos_1, hasOnlyAir);
            this.level.getChunkSource().onSectionEmptinessChanged(chunkPos.x, SectionPos.blockToSectionCoord(y), chunkPos.z, hasOnlyAir);
        }

        if (LightEngine.hasDifferentLightProperties(oldBlockState, newBlockState)) {
            ProfilerFiller profiler = Profiler.get();
            profiler.push("updateSkyLightSources");
            skyLightSources.update(this, x, chunkY, z);
            profiler.popPush("queueCheckLight");
            level.getChunkSource().getLightEngine().checkBlock(blockPos_1);
            profiler.pop();
        }

        boolean hadBlockEntity = oldBlockState.hasBlockEntity();
        boolean blockChanged = !oldBlockState.is(newBlock);

        boolean sideEffects = (flags & Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS) == 0;

        if (blockChanged) {
            if (level instanceof ServerLevel serverLevel && !(oldBlockState.getBlock() instanceof MovingPistonBlock)) {
                if (hadBlockEntity && sideEffects) {
                    final BlockEntity blockEntity = level.getBlockEntity(blockPos_1);
                    if (blockEntity != null) {
                        blockEntity.preRemoveSideEffects(blockPos_1, oldBlockState, boolean_1);
                    }
                }

                if (hadBlockEntity) {
                    removeBlockEntity(blockPos_1);
                }
                if ((flags & Block.UPDATE_NEIGHBORS) != 0) { // scary change // so many other places that used to call this regardless if the UPDATE_NEIGHBORS flag was set
                    oldBlockState.affectNeighborsAfterRemoval(serverLevel, blockPos_1, boolean_1);
                }
            } else if (hadBlockEntity) {
                removeBlockEntity(blockPos_1);
            }
        }






        if (chunkSection.getBlockState(x, chunkY, z).getBlock() != newBlock)
        {
            return null;
        }

        if (!level.isClientSide && sideEffects) {
            // this updates stuff, schedule ticks - do we want that since its only be called from MovingPistonBlock really?
            newBlockState.onPlace(level, blockPos_1, oldBlockState, boolean_1);
        }

        BlockEntity oldBlockEntity = null;
        if (oldBlockState.hasBlockEntity())
        {
            oldBlockEntity = this.getBlockEntity(blockPos_1, LevelChunk.EntityCreationType.CHECK);
            if (oldBlockEntity != null && !oldBlockEntity.isValidBlockState(oldBlockState))
            {
                oldBlockEntity.setBlockState(oldBlockState);
                updateBlockEntityTicker(oldBlockEntity);
            }
            else
            {
                if (newBlockEntity == null)
                {
                    newBlockEntity = ((EntityBlock) newBlock).newBlockEntity(blockPos_1, newBlockState);
                    if (newBlockEntity != null)
                    {
                        addAndRegisterBlockEntity(newBlockEntity);
                    }
                }
                if (newBlockEntity != oldBlockEntity && newBlockEntity != null)
                {
                    newBlockEntity.clearRemoved();
                    this.level.setBlockEntity(newBlockEntity);
                    newBlockEntity.setBlockState(newBlockState);
                    updateBlockEntityTicker(newBlockEntity);
                }
            }
        }

        markUnsaved();
        return oldBlockState;
    }
}
