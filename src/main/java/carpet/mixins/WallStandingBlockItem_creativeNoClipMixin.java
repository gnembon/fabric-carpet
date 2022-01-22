package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.WallStandingBlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WallStandingBlockItem.class)
public class WallStandingBlockItem_creativeNoClipMixin
{
    @Redirect(method = "getPlacementState", at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/WorldView;canPlace(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Z"
    ))
    private boolean canCreativePlayerPlace(
            WorldView worldView, BlockState state, BlockPos pos, ShapeContext context,
            ItemPlacementContext itemcontext
    )
    {
        PlayerEntity player = itemcontext.getPlayer();
        if (CarpetSettings.creativeNoClip && player != null && player.isCreative() && player.getAbilities().flying)
        {
            // copy from canPlace
            VoxelShape voxelShape = state.getCollisionShape(worldView, pos, context);
            return voxelShape.isEmpty() || worldView.doesNotIntersectEntities(player, voxelShape.offset(pos.getX(), pos.getY(), pos.getZ()));

        }
        return worldView.canPlace(state, pos, context);
    }
}
