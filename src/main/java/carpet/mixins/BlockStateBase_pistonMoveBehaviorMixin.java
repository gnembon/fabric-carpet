package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import carpet.fakes.BlockBehaviourInterface;
import carpet.fakes.BlockStateBaseInterface;
import carpet.helpers.PistonMoveBehaviorManager.PistonMoveBehavior;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

@Mixin(BlockStateBase.class)
public class BlockStateBase_pistonMoveBehaviorMixin implements BlockStateBaseInterface {

    private PistonMoveBehavior pistonMoveBehaviorOverride = PistonMoveBehavior.NONE;
    private PistonMoveBehavior defaultPistonMoveBehaviorOverride = PistonMoveBehavior.NONE;

    @Shadow private Block getBlock() { return null; }
    @Shadow private BlockState asState() { return null; }

    @Inject(
        method = "getPistonPushReaction",
        cancellable = true,
        at = @At(
            value = "HEAD"
        )
    )
    private void overridePushReaction(CallbackInfoReturnable<PushReaction> cir) {
        if (pistonMoveBehaviorOverride.isPresent()) {
            cir.setReturnValue(pistonMoveBehaviorOverride.getPushReaction());
        }
    }

    @Override
    public boolean canChangePistonMoveBehaviorOverride() {
        return ((BlockBehaviourInterface)getBlock()).canOverridePistonMoveBehavior(asState());
    }

    @Override
    public PistonMoveBehavior getPistonMoveBehaviorOverride() {
        return pistonMoveBehaviorOverride;
    }

    @Override
    public void setPistonMoveBehaviorOverride(PistonMoveBehavior behavior) {
        pistonMoveBehaviorOverride = getOverrideOrNone(behavior);
    }

    @Override
    public PistonMoveBehavior getDefaultPistonMoveBehaviorOverride() {
        return defaultPistonMoveBehaviorOverride;
    }

    @Override
    public void setDefaultPistonMoveBehaviorOverride(PistonMoveBehavior behavior) {
        defaultPistonMoveBehaviorOverride = getOverrideOrNone(behavior);
    }

    private PistonMoveBehavior getOverrideOrNone(PistonMoveBehavior behavior) {
        PushReaction vanillaBehavior = getBlock().getPistonPushReaction(asState());
        return behavior.is(vanillaBehavior) ? PistonMoveBehavior.NONE : behavior;
    }
}
