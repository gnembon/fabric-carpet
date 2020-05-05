package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockItem.class)
public class BlockItem_creativeNoClipMixin
{
    @Redirect(method = "canPlace", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;canPlace(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/EntityContext;)Z"
    ))
    private boolean canSpectatingPlace(World world, BlockState state, BlockPos pos, EntityContext context,
                                       ItemPlacementContext contextOuter, BlockState stateOuter)
    {
        PlayerEntity player = contextOuter.getPlayer();
        if (CarpetSettings.creativeNoClip && player != null && player.isCreative() && player.abilities.flying)
        {
            // copy from canPlace
            VoxelShape voxelShape = state.getCollisionShape(world, pos, context);
            return voxelShape.isEmpty() || world.intersectsEntities(player, voxelShape.offset(pos.getX(), pos.getY(), pos.getZ()));

        }
        return world.canPlace(state, pos, context);
    }
}
