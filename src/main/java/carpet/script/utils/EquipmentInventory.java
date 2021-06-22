package carpet.script.utils;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public class EquipmentInventory implements Inventory
{
    private static final List<EquipmentSlot> slotToSlot = Arrays.asList(
            EquipmentSlot.MAINHAND,
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD,
            EquipmentSlot.OFFHAND
    );

    LivingEntity mob;
    public EquipmentInventory(LivingEntity mob)
    {
        this.mob = mob;
    }

    @Override
    public int size()
    {
        return 6;
    }

    @Override
    public boolean isEmpty()
    {
        for (EquipmentSlot slot: slotToSlot)
            if (!mob.getEquippedStack(slot).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot)
    {
        EquipmentSlot slotSlot;
        try
        {
            slotSlot = slotToSlot.get(slot);
        }
        catch (IndexOutOfBoundsException ignored)
        {
            //going out of the index should be really exceptional
            return ItemStack.EMPTY;
        }
        return mob.getEquippedStack(slotSlot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount)
    {
        EquipmentSlot slotSlot;
        try
        {
            slotSlot = slotToSlot.get(slot);
        }
        catch (IndexOutOfBoundsException ignored)
        {
            //going out of the index should be really exceptional
            return ItemStack.EMPTY;
        }
        return mob.getEquippedStack(slotSlot).split(amount);
    }

    @Override
    public ItemStack removeStack(int slot)
    {
        EquipmentSlot slotSlot;
        try
        {
            slotSlot = slotToSlot.get(slot);
        }
        catch (IndexOutOfBoundsException ignored)
        {
            //going out of the index should be really exceptional
            return ItemStack.EMPTY;
        }
        ItemStack previous = mob.getEquippedStack(slotSlot);
        mob.equipStack(slotSlot, ItemStack.EMPTY);
        return previous;
    }

    @Override
    public void setStack(int slot, ItemStack stack)
    {
        EquipmentSlot slotSlot;
        try
        {
            slotSlot = slotToSlot.get(slot);
        }
        catch (IndexOutOfBoundsException ignored)
        {
            //going out of the index should be really exceptional
            return;
        }
        mob.equipStack(slotSlot, stack);
    }

    @Override
    public void markDirty()
    {

    }

    @Override
    public boolean canPlayerUse(PlayerEntity player)
    {
        return false;
    }

    @Override
    public void clear()
    {
        for (EquipmentSlot slot: slotToSlot)
            mob.equipStack(slot, ItemStack.EMPTY);
    }
}
