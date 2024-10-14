package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.WorldChunkInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
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
            boolean boolean_1)
    {
        int x = blockPos_1.getX() & 15;
        int y = blockPos_1.getY();
        int z = blockPos_1.getZ() & 15;
        LevelChunkSection chunkSection = this.getSection(this.getSectionIndex(y));
        if (chunkSection.hasOnlyAir())
        {
            if (newBlockState.isAir())
            {
                return null;
            }
        }
        
        boolean boolean_2 = chunkSection.hasOnlyAir();
        BlockState oldBlockState = chunkSection.setBlockState(x, y & 15, z, newBlockState);
        if (oldBlockState == newBlockState)
        {
            return null;
        }
        else
        {
            Block newBlock = newBlockState.getBlock();
            Block oldBlock = oldBlockState.getBlock();
            ((Heightmap) this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING)).update(x, y, z, newBlockState);
            ((Heightmap) this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)).update(x, y, z, newBlockState);
            ((Heightmap) this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR)).update(x, y, z, newBlockState);
            ((Heightmap) this.heightmaps.get(Heightmap.Types.WORLD_SURFACE)).update(x, y, z, newBlockState);
            boolean boolean_3 = chunkSection.hasOnlyAir();
            if (boolean_2 != boolean_3)
            {
                this.level.getChunkSource().getLightEngine().updateSectionStatus(blockPos_1, boolean_3);
            }
            
            if (!this.level.isClientSide)
            {
                if (!(oldBlock instanceof MovingPistonBlock))//this is a movableTE special case, if condition wasn't there it would remove the blockentity that was carried for some reason
                    oldBlockState.onRemove(this.level, blockPos_1, newBlockState, boolean_1);//this kills it
            }
            else if (oldBlock != newBlock && oldBlock instanceof EntityBlock)
            {
                this.level.removeBlockEntity(blockPos_1);
            }
            
            if (chunkSection.getBlockState(x, y & 15, z).getBlock() != newBlock)
            {
                return null;
            }
            else
            {
                BlockEntity oldBlockEntity = null;
                if (oldBlockState.hasBlockEntity())
                {
                    oldBlockEntity = this.getBlockEntity(blockPos_1, LevelChunk.EntityCreationType.CHECK);
                    if (oldBlockEntity != null)
                    {
                        oldBlockEntity.setBlockState(oldBlockState);
                        updateBlockEntityTicker(oldBlockEntity);
                    }
                }

                if (oldBlockState.hasBlockEntity())
                {
                    if (newBlockEntity == null)
                    {
                        newBlockEntity = ((EntityBlock) newBlock).newBlockEntity(blockPos_1, newBlockState);
                    }
                    if (newBlockEntity != oldBlockEntity && newBlockEntity != null)
                    {
                        newBlockEntity.clearRemoved();
                        this.level.setBlockEntity(newBlockEntity);
                        newBlockEntity.setBlockState(newBlockState);
                        updateBlockEntityTicker(newBlockEntity);
                    }
                }

                if (!this.level.isClientSide)
                {
                    newBlockState.onPlace(this.level, blockPos_1, oldBlockState, boolean_1); //This can call setblockstate! (e.g. hopper does)
                }
                
                markUnsaved();
                return oldBlockState;
            }
        }
    }
}
