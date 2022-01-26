package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StandingAndWallBlockItem.class)
public class StandingAndWallBlockItem_creativeNoClipMixin
{
    @Redirect(method = "getPlacementState", at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/level/LevelReader;isUnobstructed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Z"
    ))
    private boolean canCreativePlayerPlace(
            LevelReader worldView, BlockState state, BlockPos pos, CollisionContext context,
            BlockPlaceContext itemcontext
    )
    {
        Player player = itemcontext.getPlayer();
        if (CarpetSettings.creativeNoClip && player != null && player.isCreative() && player.getAbilities().flying)
        {
            // copy from canPlace
            VoxelShape voxelShape = state.getCollisionShape(worldView, pos, context);
            return voxelShape.isEmpty() || worldView.isUnobstructed(player, voxelShape.move(pos.getX(), pos.getY(), pos.getZ()));

        }
        return worldView.isUnobstructed(state, pos, context);
    }
}
