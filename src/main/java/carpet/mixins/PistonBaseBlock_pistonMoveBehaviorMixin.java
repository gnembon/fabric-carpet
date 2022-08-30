package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import carpet.helpers.PistonMoveBehaviorManager;
import carpet.helpers.PistonMoveBehaviorManager.PistonMoveBehavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlock_pistonMoveBehaviorMixin {

    @Redirect(
        method = "isPushable",
        slice = @Slice(
            from = @At(
                value = "RETURN",
                ordinal = 1
            ),
            to = @At(
                value = "RETURN",
                ordinal = 2
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
        )
    )
    private static boolean overrideObsidianPushReaction(BlockState state, Block block) {
        // Several blocks are made immovable with explicit checks. To override the
        // push reaction of these blocks we make these checks fail.
        if (state.is(block)) {
            PistonMoveBehavior override = PistonMoveBehaviorManager.getOverride(state);

            if (!override.isPresent()) {
                return true;
            }
        }

        return false;
    }

    @Redirect(
        method = "isPushable",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"
        )
    )
    private static float overrideBedrockPushReaction(BlockState state, BlockGetter level, BlockPos pos) {
        // Several blocks are made immovable due to having "negative mining speed".
        // To override the push reaction of these blocks we return a non-negative
        // mining speed instead.
        float destroySpeed = state.getDestroySpeed(level, pos);

        if (destroySpeed == -1.0F) {
            PistonMoveBehavior override = PistonMoveBehaviorManager.getOverride(state);

            if (override.isPresent()) {
                return 0.0F;
            }
        }

        return destroySpeed;
    }

    @Redirect(
        method = "isPushable",
        slice = @Slice(
            from = @At(
                value = "RETURN",
                ordinal = 4
            ),
            to = @At(
                value = "RETURN",
                ordinal = 5
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
        )
    )
    private static boolean overridePistonPushReaction(BlockState blockState, Block block, BlockState state, Level level, BlockPos pos, Direction moveDir, boolean allowDestroy, Direction pistonFacing) {
        return state.is(block) && !PistonMoveBehaviorManager.getOverride(state).isPresent();
    }
}
