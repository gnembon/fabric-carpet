package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import carpet.CarpetSettings;

import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

@Mixin(BarrierBlock.class)
public class BarrierBlock_movableBarriersMixin extends Block {

    private BarrierBlock_movableBarriersMixin(Properties properties) {
        super(properties);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return CarpetSettings.movableBarriers ? PushReaction.NORMAL : super.getPistonPushReaction(state);
    }
}
