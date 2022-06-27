package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import carpet.fakes.BlockBehaviourInterface;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockBehaviour.class)
public class BlockBehaviour_pistonMoveBehaviorMixin implements BlockBehaviourInterface {

    @Override
    public boolean canOverridePistonMoveBehavior(BlockState state) {
        return true;
    }
}
