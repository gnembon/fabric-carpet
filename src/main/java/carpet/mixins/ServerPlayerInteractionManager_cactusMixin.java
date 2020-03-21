package carpet.mixins;

import carpet.helpers.BlockRotator;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManager_cactusMixin
{

    @Redirect(method = "interactBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;onUse(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"
    ))
    private ActionResult activateWithOptionalCactus(BlockState blockState, World world_1, PlayerEntity playerEntity_1, Hand hand_1, BlockHitResult blockHitResult_1)
    {
        boolean flipped = BlockRotator.flipBlockWithCactus(blockState, world_1, playerEntity_1, hand_1, blockHitResult_1);
        if (flipped)
            return ActionResult.SUCCESS;

        return blockState.onUse(world_1, playerEntity_1, hand_1, blockHitResult_1);
    }
}
