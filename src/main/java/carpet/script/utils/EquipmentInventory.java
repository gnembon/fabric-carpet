package carpet.script.utils;

import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class EquipmentInventory implements Container
{
    private static final List<EquipmentSlot> slotToSlot = List.of(
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
    public int getContainerSize()
    {
        return 6;
    }

    @Override
    public boolean isEmpty()
    {
        for (EquipmentSlot slot : slotToSlot)
        {
            if (!mob.getItemBySlot(slot).isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot)
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
        return mob.getItemBySlot(slotSlot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount)
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
        return mob.getItemBySlot(slotSlot).split(amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
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
        ItemStack previous = mob.getItemBySlot(slotSlot);
        mob.setItemSlot(slotSlot, ItemStack.EMPTY);
        return previous;
    }

    @Override
    public void setItem(int slot, ItemStack stack)
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
        mob.setItemSlot(slotSlot, stack);
    }

    @Override
    public void setChanged()
    {

    }

    @Override
    public boolean stillValid(Player player)
    {
        return false;
    }

    @Override
    public void clearContent()
    {
        for (EquipmentSlot slot : slotToSlot)
        {
            mob.setItemSlot(slot, ItemStack.EMPTY);
        }
    }
}
