package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SetBlockCommand.class)
public class SetBlockCommand_fillUpdatesMixin
{
    @Redirect(method = "setBlock", at = @At(
            value = "INVOKE",
            target =  "Lnet/minecraft/server/level/ServerLevel;updateNeighboursOnBlockSet(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
    ))
    private static void conditionalUpdating(ServerLevel serverWorld, BlockPos blockPos_1, BlockState block_1)
    {
        if (CarpetSettings.fillUpdates) serverWorld.updateNeighboursOnBlockSet(blockPos_1, block_1);
    }
}
