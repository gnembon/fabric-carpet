package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import carpet.helpers.HopperCounter;
import carpet.utils.WoolTool;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends LootableContainerBlockEntity
{
    protected HopperBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Shadow public abstract int size();

    @Shadow public abstract void setStack(int slot, ItemStack stack);

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private static void onInsert(World world, BlockPos blockPos, BlockState blockState, Inventory inventory, CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.hopperCounters) {
            DyeColor wool_color = WoolTool.getWoolColorAtPosition(
                    world,
                    blockPos.offset(blockState.get(HopperBlock.FACING))); // offset
            if (wool_color != null)
            {
                for (int i = 0; i < inventory.size(); ++i)
                {
                    if (!inventory.getStack(i).isEmpty())
                    {
                        ItemStack itemstack = inventory.getStack(i);//.copy();
                        HopperCounter.COUNTERS.get(wool_color).add(world.getServer(), itemstack);
                        inventory.setStack(i, ItemStack.EMPTY);
                    }
                }
                cir.setReturnValue(true);
            }
        }
    }
}