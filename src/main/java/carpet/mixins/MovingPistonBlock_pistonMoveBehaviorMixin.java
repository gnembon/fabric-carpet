package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import carpet.fakes.BlockBehaviourInterface;

import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(MovingPistonBlock.class)
public class MovingPistonBlock_pistonMoveBehaviorMixin implements BlockBehaviourInterface {

    @Override
    public boolean canOverridePistonMoveBehavior(BlockState state) {
        return false;
    }
}
