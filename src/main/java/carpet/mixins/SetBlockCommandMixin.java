package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.server.command.SetBlockCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SetBlockCommand.class)
public class SetBlockCommandMixin
{
    @Redirect(method = "execute", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;updateNeighbors(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V"
    ))
    private static void conditionalUpdating(ServerWorld serverWorld, BlockPos blockPos_1, Block block_1)
    {
        if (CarpetSettings.fillUpdates) serverWorld.updateNeighbors(blockPos_1, block_1);
    }
}
