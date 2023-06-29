package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FillCommand.class)
public abstract class FillCommandMixin
{
    @Redirect(method = "fillBlocks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;blockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"
    ))
    private static void conditionalUpdating(ServerLevel serverWorld, BlockPos blockPos_1, Block block_1)
    {
        if (CarpetSettings.fillUpdates) serverWorld.blockUpdated(blockPos_1, block_1);
    }
}
