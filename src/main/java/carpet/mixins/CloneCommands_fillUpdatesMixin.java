package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

@Mixin(CloneCommands.class)
public abstract class CloneCommands_fillUpdatesMixin
{
    @ModifyConstant(method = "clone", constant = @Constant(intValue = 32768))
    private static int fillLimit(int original) {
        return CarpetSettings.fillLimit;
    }

    @WrapWithCondition(method = "clone", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;blockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"
    ))
    private static boolean conditionalUpdating(ServerLevel serverWorld, BlockPos blockPos_1, Block block_1)
    {
        return CarpetSettings.fillUpdates;
    }
}
