package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.InventoryHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import carpet.fakes.ItemEntityInterface;
import java.util.Objects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements ItemEntityInterface
{
    @Shadow private int age;
    @Shadow private int pickupDelay;

    public ItemEntityMixin(EntityType<?> entityType_1, Level world_1) {
        super(entityType_1, world_1);
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        if (CarpetSettings.lightningKillsDropsFix) {
            if (this.age > 8) { //Only kill item if it's older than 8 ticks
                super.thunderHit(world, lightning);
            }
        } else {
            super.thunderHit(world, lightning);
        }
    }

    @Override
    public int getPickupDelayCM() {
        return this.pickupDelay;
    }

    @Inject(method="<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void removeEmptyShulkerBoxTags(Level worldIn, double x, double y, double z, ItemStack stack, CallbackInfo ci)
    {
        if (CarpetSettings.shulkerBoxStackSize > 1
                && stack.getItem() instanceof BlockItem bi
                && bi.getBlock() instanceof ShulkerBoxBlock)
        {
            InventoryHelper.cleanUpShulkerBoxTag(stack);
        }
    }

    @Redirect(
            method = "isMergable()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"
            )
    )
    private int getItemStackMaxAmount(ItemStack stack) {
        if (CarpetSettings.shulkerBoxStackSize > 1 && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock)
            return CarpetSettings.shulkerBoxStackSize;

        return stack.getMaxStackSize();
    }

    @Inject(
            method = "tryToMerge(Lnet/minecraft/world/entity/item/ItemEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tryStackShulkerBoxes(ItemEntity other, CallbackInfo ci)
    {
        ItemEntity self = (ItemEntity)(Object)this;
        ItemStack selfStack = self.getItem();
        if (CarpetSettings.shulkerBoxStackSize == 1 || !(selfStack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ShulkerBoxBlock)) {
            return;
        }

        ItemStack otherStack = other.getItem();
        if (selfStack.getItem() == otherStack.getItem()
                && !InventoryHelper.shulkerBoxHasItems(selfStack)
                && !InventoryHelper.shulkerBoxHasItems(otherStack)
                && Objects.equals(selfStack.getTag(), otherStack.getTag()) // empty block entity tags are cleaned up when spawning
                && selfStack.getCount() != CarpetSettings.shulkerBoxStackSize)
        {
            int amount = Math.min(otherStack.getCount(), CarpetSettings.shulkerBoxStackSize - selfStack.getCount());

            selfStack.grow(amount);
            self.setItem(selfStack);

            this.pickupDelay = Math.max(((ItemEntityInterface)other).getPickupDelayCM(), this.pickupDelay);
            this.age = Math.min(other.getAge(), this.age);

            otherStack.shrink(amount);
            if (otherStack.isEmpty())
            {
                other.discard();
            }
            else
            {
                other.setItem(otherStack);
            }
            ci.cancel();
        }
    }
}
