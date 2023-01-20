package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(CloneCommands.class)
public abstract class CloneCommands_fillUpdatesMixin
{
    @Redirect(method = "clone", at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/GameRules;getInt(Lnet/minecraft/world/level/GameRules$Key;)I"
    ))
    private static int redirectCloneGameRuleInt(GameRules instance, GameRules.Key<GameRules.IntegerValue> key)
    {
        int vanilla = instance.getInt(key);
        return Math.max(vanilla, CarpetSettings.fillLimit);
    }

    @Redirect(method = "clone", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;blockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"
    ))
    private static void conditionalUpdating(ServerLevel serverWorld, BlockPos blockPos_1, Block block_1)
    {
        if (CarpetSettings.fillUpdates) serverWorld.blockUpdated(blockPos_1, block_1);
    }
}
