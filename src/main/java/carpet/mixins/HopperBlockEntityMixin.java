package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import carpet.helpers.HopperCounter;
import carpet.utils.WoolTool;

/**
 * The {@link Mixin} which removes items in a hopper if it points into a wool counter, and calls {@link HopperCounter#add}
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends LootableContainerBlockEntity {

    protected HopperBlockEntityMixin(BlockEntityType<?> blockEntityType_1) {
        super(blockEntityType_1);
    }

    @Shadow
    public abstract double getHopperX();

    @Shadow
    public abstract double getHopperY();

    @Shadow
    public abstract double getHopperZ();

    @Shadow public abstract int size();

    @Shadow public abstract void setStack(int slot, ItemStack stack);

    /**
     * A method to remove items from hoppers pointing into wool and count them via {@link HopperCounter#add} method
     */
    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void onInsert(CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.hopperCounters) {
            DyeColor wool_color = WoolTool.getWoolColorAtPosition(
                    getWorld(),
                    new BlockPos(getHopperX(), getHopperY(), getHopperZ()).offset(this.getCachedState().get(HopperBlock.FACING)));
            if (wool_color != null)
            {
                for (int i = 0; i < size(); ++i)
                {
                    if (!this.getStack(i).isEmpty())
                    {
                        ItemStack itemstack = this.getStack(i);//.copy();
                        HopperCounter.COUNTERS.get(wool_color).add(this.getWorld().getServer(), itemstack);
                        this.setStack(i, ItemStack.EMPTY);
                    }
                }
                cir.setReturnValue(true);
            }
        }
    }
}