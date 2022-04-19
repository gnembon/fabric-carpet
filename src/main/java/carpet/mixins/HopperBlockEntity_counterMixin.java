package carpet.mixins;

import carpet.CarpetSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import carpet.helpers.HopperCounter;
import carpet.utils.WoolTool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The {@link Mixin} which removes items in a hopper if it points into a wool counter, and calls {@link HopperCounter#add}
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntity_counterMixin extends RandomizableContainerBlockEntity
{
    protected HopperBlockEntity_counterMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Shadow public abstract int getContainerSize();

    @Shadow public abstract void setItem(int slot, ItemStack stack);

    /**
     * A method to remove items from hoppers pointing into wool and count them via {@link HopperCounter#add} method
     */
    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void onInsert(Level world, BlockPos blockPos, BlockState blockState, Container inventory, CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.hopperCounters) {
            DyeColor wool_color = WoolTool.getWoolColorAtPosition(
                    world,
                    blockPos.relative(blockState.getValue(HopperBlock.FACING))); // offset
            if (wool_color != null)
            {
                for (int i = 0; i < inventory.getContainerSize(); ++i)
                {
                    if (!inventory.getItem(i).isEmpty())
                    {
                        ItemStack itemstack = inventory.getItem(i);//.copy();
                        HopperCounter.COUNTERS.get(wool_color).add(world.getServer(), itemstack);
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                }
                cir.setReturnValue(true);
            }
        }
    }
}
