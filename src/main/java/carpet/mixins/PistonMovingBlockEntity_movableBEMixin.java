package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.PistonMovingBlockEntityInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntity_movableBEMixin extends BlockEntity implements PistonMovingBlockEntityInterface
{
    @Shadow
    private boolean isSourcePiston;
    @Shadow
    private BlockState movedState;
    
    private BlockEntity carriedBlockEntity;
    private boolean renderCarriedBlockEntity = false;
    private boolean renderSet = false;

    public PistonMovingBlockEntity_movableBEMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }


    /**
     * @author 2No2Name
     */
    public BlockEntity carpet$getCarriedBlockEntity()
    {
        return carriedBlockEntity;
    }

    @Override
    public void setLevel(Level world) {
        super.setLevel(world);
        if (carriedBlockEntity != null) carriedBlockEntity.setLevel(world);
    }

    public void carpet$setCarriedBlockEntity(BlockEntity blockEntity)
    {
        this.carriedBlockEntity = blockEntity;
        if (this.carriedBlockEntity != null)
        {
            carriedBlockEntity.carpet$setPos(worldPosition);
            // this might be little dangerous since pos is final for a hashing reason?
            if (level != null) carriedBlockEntity.setLevel(level);
        }
        //    this.carriedBlockEntity.setPos(this.pos);
    }
    
    public boolean carpet$isRenderModeSet()
    {
        return renderSet;
    }
    
    public boolean carpet$getRenderCarriedBlockEntity()
    {
        return renderCarriedBlockEntity;
    }
    
    public void carpet$setRenderCarriedBlockEntity(boolean b)
    {
        renderCarriedBlockEntity = b;
        renderSet = true;
    }
    
    /**
     * @author 2No2Name
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean movableTEsetBlockState0(
            Level level, BlockPos blockPos_1, BlockState blockAState_2, int int_1,
            Level world2, BlockPos blockPos, BlockState blockState, PistonMovingBlockEntity pistonBlockEntity)
    {
        if (!CarpetSettings.movableBlockEntities)
            return level.setBlock(blockPos_1, blockAState_2, int_1);
        else
            return level.carpet$setBlockStateWithBlockEntity(blockPos_1, blockAState_2, pistonBlockEntity.carpet$getCarriedBlockEntity(), int_1);
    }
    
    @Redirect(method = "finalTick", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean movableTEsetBlockState1(Level level, BlockPos blockPos_1, BlockState blockState_2, int int_1)
    {
        if (!CarpetSettings.movableBlockEntities)
            return level.setBlock(blockPos_1, blockState_2, int_1);
        else
        {
            boolean ret = level.carpet$setBlockStateWithBlockEntity(blockPos_1, blockState_2, this.carriedBlockEntity, int_1);
            this.carriedBlockEntity = null; //this will cancel the finishHandleBroken
            return ret;
        }
    }
    
    @Inject(method = "finalTick", at = @At(value = "RETURN"))
    private void finishHandleBroken(CallbackInfo cir)
    {
        //Handle TNT Explosions or other ways the moving Block is broken
        //Also /setblock will cause this to be called, and drop e.g. a moving chest's contents.
        // This is MC-40380 (BlockEntities that aren't Inventories drop stuff when setblock is called )
        if (CarpetSettings.movableBlockEntities && this.carriedBlockEntity != null && !this.level.isClientSide && this.level.getBlockState(this.worldPosition).getBlock() == Blocks.AIR)
        {
            BlockState blockState_2;
            if (this.isSourcePiston)
                blockState_2 = Blocks.AIR.defaultBlockState();
            else
                blockState_2 = Block.updateFromNeighbourShapes(this.movedState, this.level, this.worldPosition);
            this.level.carpet$setBlockStateWithBlockEntity(this.worldPosition, blockState_2, this.carriedBlockEntity, 3);
            this.level.destroyBlock(this.worldPosition, false, null);
        }
    }
    
    @Inject(method = "load", at = @At(value = "TAIL"))
    private void onFromTag(CompoundTag NbtCompound_1, CallbackInfo ci)
    {
        if (CarpetSettings.movableBlockEntities && NbtCompound_1.contains("carriedTileEntityCM", 10))
        {
            if (this.movedState.getBlock() instanceof EntityBlock)
                this.carriedBlockEntity = ((EntityBlock) (this.movedState.getBlock())).newBlockEntity(worldPosition, movedState);//   this.world);
            if (carriedBlockEntity != null) //Can actually be null, as BlockPistonMoving.createNewTileEntity(...) returns null
                this.carriedBlockEntity.load(NbtCompound_1.getCompound("carriedTileEntityCM"));
            carpet$setCarriedBlockEntity(carriedBlockEntity);
        }
    }
    
    @Inject(method = "saveAdditional", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void onToTag(CompoundTag NbtCompound_1, CallbackInfo ci)
    {
        if (CarpetSettings.movableBlockEntities && this.carriedBlockEntity != null)
        {
            //Leave name "carriedTileEntityCM" instead of "carriedBlockEntityCM" for upgrade compatibility with 1.13.2 movable TE
            NbtCompound_1.put("carriedTileEntityCM", this.carriedBlockEntity.saveWithoutMetadata());
        }
    }
}
