package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

@Mixin(SetBlockCommand.class)
public class SetBlockCommand_fillUpdatesMixin
{
    @WrapWithCondition(method = "setBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;blockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"
    ))
    private static boolean conditionalUpdating(ServerLevel serverWorld, BlockPos blockPos_1, Block block_1)
    {
        return CarpetSettings.fillUpdates;
    }
}
