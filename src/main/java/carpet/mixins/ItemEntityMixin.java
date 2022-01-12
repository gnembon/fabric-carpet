package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.InventoryHelper;
import carpet.fakes.ItemEntityInterface;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements ItemEntityInterface
{
    private static final int SHULKERBOX_MAX_STACK_AMOUNT = 64;

    @Shadow private int itemAge;
    @Shadow private int pickupDelay;

    public ItemEntityMixin(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        if (CarpetSettings.lightningKillsDropsFix) {
            if (this.itemAge > 8) { //Only kill item if its older then 8 ticks
                super.onStruckByLightning(world, lightning);
            }
        } else {
            super.onStruckByLightning(world, lightning);
        }
    }

    @Override
    public int getPickupDelayCM() {
        return this.pickupDelay;
    }

    @Inject(method="<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
    private void removeEmptyShulkerBoxTags(World worldIn, double x, double y, double z, ItemStack stack, CallbackInfo ci)
    {
        if (CarpetSettings.shulkerBoxStackSize > 1
                && stack.getItem() instanceof BlockItem
                && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock)
        {
            if (InventoryHelper.cleanUpShulkerBoxTag(stack)) {
                ((ItemEntity) (Object) this).setStack(stack);
            }
        }
    }

    @Redirect(
            method = "canMerge()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"
            )
    )
    private int getItemStackMaxAmount(ItemStack stack) {
        if (CarpetSettings.shulkerBoxStackSize > 1 && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock)
            return CarpetSettings.shulkerBoxStackSize;

        return stack.getMaxCount();
    }

    @Inject(
            method = "tryMerge(Lnet/minecraft/entity/ItemEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tryStackShulkerBoxes(ItemEntity other, CallbackInfo ci)
    {
        ItemEntity self = (ItemEntity)(Object)this;
        ItemStack selfStack = self.getStack();
        if (CarpetSettings.shulkerBoxStackSize == 1 || !(selfStack.getItem() instanceof BlockItem) || !(((BlockItem)selfStack.getItem()).getBlock() instanceof ShulkerBoxBlock)) {
            return;
        }

        ItemStack otherStack = other.getStack();
        if (selfStack.getItem() == otherStack.getItem()
                && !InventoryHelper.shulkerBoxHasItems(selfStack)
                && !InventoryHelper.shulkerBoxHasItems(otherStack)
                && selfStack.hasNbt() == otherStack.hasNbt()
                && selfStack.getCount() + otherStack.getCount() <= CarpetSettings.shulkerBoxStackSize)
        {
            int amount = Math.min(otherStack.getCount(), CarpetSettings.shulkerBoxStackSize - selfStack.getCount());

            selfStack.increment(amount);
            self.setStack(selfStack);

            this.pickupDelay = Math.max(((ItemEntityInterface)other).getPickupDelayCM(), this.pickupDelay);
            this.itemAge = Math.min(other.getItemAge(), this.itemAge);

            otherStack.decrement(amount);
            if (otherStack.isEmpty())
            {
                other.discard(); // discard remove();
            }
            else
            {
                other.setStack(otherStack);
            }
            ci.cancel();
        }
    }
}
