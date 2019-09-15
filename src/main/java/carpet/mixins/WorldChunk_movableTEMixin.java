package carpet.mixins;

import carpet.settings.CarpetSettings;
import carpet.fakes.WorldChunkInterface;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonExtensionBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

import static net.minecraft.world.chunk.WorldChunk.EMPTY_SECTION;

@Mixin(WorldChunk.class)
public abstract class WorldChunk_movableTEMixin implements WorldChunkInterface
{
    @Shadow
    @Final
    private ChunkSection[] sections;
    @Shadow
    @Final
    private Map<Heightmap.Type, Heightmap> heightmaps;
    @Shadow
    private boolean shouldSave;
    @Shadow
    @Final
    private World world;
    
    @Shadow
    /* @Nullable */
    public abstract BlockEntity getBlockEntity(BlockPos blockPos_1, WorldChunk.CreationType worldChunk$CreationType_1);
    
    // Fix Failure: If a moving BlockEntity is placed while BlockEntities are ticking, this will not find it and then replace it with a new TileEntity!
    // blockEntity_2 = this.getBlockEntity(blockPos_1, WorldChunk.CreationType.CHECK);
    @Redirect(method = "setBlockState", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/world/chunk/WorldChunk;getBlockEntity(Lnet/minecraft/util/math/BlockPos;" + "Lnet/minecraft/world/chunk/WorldChunk$CreationType;)" + "Lnet/minecraft/block/entity/BlockEntity;"))
    private BlockEntity ifGetBlockEntity(WorldChunk worldChunk, BlockPos blockPos_1,
            WorldChunk.CreationType worldChunk$CreationType_1)
    {
        if (!CarpetSettings.movableBlockEntities)
        {
            return this.getBlockEntity(blockPos_1, WorldChunk.CreationType.CHECK);
        }
        else
        {
            return this.world.getBlockEntity(blockPos_1);
        }
    }
    
    
    /**
     * Sets the Blockstate and the BlockEntity.
     * Only sets BlockEntity if Block is BlockEntityProvider, but doesn't check if it actually matches (e.g. can assign beacon to chest entity).
     *
     * @author 2No2Name
     */
    /* @Nullable */
    public BlockState setBlockStateWithBlockEntity(BlockPos blockPos_1, BlockState newBlockState, BlockEntity newBlockEntity,
            boolean boolean_1)
    {
        int x = blockPos_1.getX() & 15;
        int y = blockPos_1.getY();
        int z = blockPos_1.getZ() & 15;
        ChunkSection chunkSection = this.sections[y >> 4];
        if (chunkSection == EMPTY_SECTION)
        {
            if (newBlockState.isAir())
            {
                return null;
            }
            
            chunkSection = new ChunkSection(y >> 4 << 4);
            this.sections[y >> 4] = chunkSection;
        }
        
        boolean boolean_2 = chunkSection.isEmpty();
        BlockState oldBlockState = chunkSection.setBlockState(x, y & 15, z, newBlockState);
        if (oldBlockState == newBlockState)
        {
            return null;
        }
        else
        {
            Block newBlock = newBlockState.getBlock();
            Block oldBlock = oldBlockState.getBlock();
            ((Heightmap) this.heightmaps.get(Heightmap.Type.MOTION_BLOCKING)).trackUpdate(x, y, z, newBlockState);
            ((Heightmap) this.heightmaps.get(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES)).trackUpdate(x, y, z, newBlockState);
            ((Heightmap) this.heightmaps.get(Heightmap.Type.OCEAN_FLOOR)).trackUpdate(x, y, z, newBlockState);
            ((Heightmap) this.heightmaps.get(Heightmap.Type.WORLD_SURFACE)).trackUpdate(x, y, z, newBlockState);
            boolean boolean_3 = chunkSection.isEmpty();
            if (boolean_2 != boolean_3)
            {
                this.world.getChunkManager().getLightingProvider().updateSectionStatus(blockPos_1, boolean_3);
            }
            
            if (!this.world.isClient)
            {
                if (!(oldBlock instanceof PistonExtensionBlock))//this is a movableTE special case, if condition wasn't there it would remove the blockentity that was carried for some reason
                    oldBlockState.onBlockRemoved(this.world, blockPos_1, newBlockState, boolean_1);//this kills it
            }
            else if (oldBlock != newBlock && oldBlock instanceof BlockEntityProvider)
            {
                this.world.removeBlockEntity(blockPos_1);
            }
            
            if (chunkSection.getBlockState(x, y & 15, z).getBlock() != newBlock)
            {
                return null;
            }
            else
            {
                BlockEntity oldBlockEntity = null;
                if (oldBlock instanceof BlockEntityProvider)
                {
                    oldBlockEntity = this.getBlockEntity(blockPos_1, WorldChunk.CreationType.CHECK);
                    if (oldBlockEntity != null)
                    {
                        oldBlockEntity.resetBlock();
                    }
                }
                
                if (newBlock instanceof BlockEntityProvider)
                {
                    if (newBlockEntity == null)
                    {
                        newBlockEntity = ((BlockEntityProvider) newBlock).createBlockEntity(this.world);
                    }
                    if (newBlockEntity != oldBlockEntity && newBlockEntity != null)
                    {
                        newBlockEntity.validate();
                        this.world.setBlockEntity(blockPos_1, newBlockEntity);
                        newBlockEntity.resetBlock();
                    }
                }
                
                if (!this.world.isClient)
                {
                    newBlockState.onBlockAdded(this.world, blockPos_1, oldBlockState, boolean_1); //This can call setblockstate! (e.g. hopper does)
                }
                
                this.shouldSave = true;
                return oldBlockState;
            }
        }
    }
}
