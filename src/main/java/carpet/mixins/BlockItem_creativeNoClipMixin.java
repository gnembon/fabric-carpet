package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockItem.class)
public class BlockItem_creativeNoClipMixin
{
    @Redirect(method = "canPlace", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isUnobstructed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Z"
    ))
    private boolean canSpectatingPlace(Level world, BlockState state, BlockPos pos, CollisionContext context,
                                       BlockPlaceContext contextOuter, BlockState stateOuter)
    {
        Player player = contextOuter.getPlayer();
        if (CarpetSettings.creativeNoClip && player != null && player.isCreative() && player.getAbilities().flying)
        {
            // copy from canPlace
            VoxelShape voxelShape = state.getCollisionShape(world, pos, context);
            return voxelShape.isEmpty() || world.isUnobstructed(player, voxelShape.move(pos.getX(), pos.getY(), pos.getZ()));

        }
        return world.isUnobstructed(state, pos, context);
    }
}
