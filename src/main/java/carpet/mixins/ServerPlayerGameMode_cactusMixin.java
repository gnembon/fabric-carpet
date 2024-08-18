package carpet.mixins;

import carpet.helpers.BlockRotator;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameMode_cactusMixin
{

    @Redirect(method = "useItemOn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
    ))
    private InteractionResult activateWithOptionalCactus(final BlockState blockState, final ItemStack itemStack, final Level world_1, final Player playerEntity_1, final InteractionHand hand_1, final BlockHitResult blockHitResult_1)
    {
        boolean flipped = BlockRotator.flipBlockWithCactus(blockState, world_1, playerEntity_1, hand_1, blockHitResult_1);
        if (flipped)
            return InteractionResult.SUCCESS;

        return blockState.useItemOn(itemStack, world_1, playerEntity_1, hand_1, blockHitResult_1);
    }
}
