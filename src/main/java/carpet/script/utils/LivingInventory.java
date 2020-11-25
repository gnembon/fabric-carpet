package carpet.script.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class LivingInventory implements Inventory
{
    private static final Int2ObjectMap<EquipmentSlot> slotToSlot = new Int2ObjectOpenHashMap<EquipmentSlot>(){{
                put(0, EquipmentSlot.MAINHAND);
                put(1, EquipmentSlot.FEET);
                put(2, EquipmentSlot.LEGS);
                put(3, EquipmentSlot.CHEST);
                put(4, EquipmentSlot.HEAD);
                put(5, EquipmentSlot.OFFHAND);
    }};

    LivingEntity mob;
    public LivingInventory(LivingEntity mob)
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
        for (EquipmentSlot slot: EquipmentSlot.values())
            if (!mob.getEquippedStack(slot).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot)
    {
        EquipmentSlot slotSlot = slotToSlot.getOrDefault(slot, null);
        if (slotSlot == null) return null;
        return mob.getEquippedStack(slotToSlot.get(slot));
    }

    @Override
    public ItemStack removeStack(int slot, int amount)
    {
        EquipmentSlot slotSlot = slotToSlot.getOrDefault(slot, null);
        if (slotSlot == null) return null;
        ItemStack stack = mob.getEquippedStack(slotSlot);
        stack.decrement(amount);
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot)
    {
        EquipmentSlot slotSlot = slotToSlot.getOrDefault(slot, null);
        if (slotSlot == null) return null;
        ItemStack previous = mob.getEquippedStack(slotSlot);
        mob.equipStack(slotSlot, ItemStack.EMPTY);
        return previous;
    }

    @Override
    public void setStack(int slot, ItemStack stack)
    {
        EquipmentSlot slotSlot = slotToSlot.getOrDefault(slot, null);
        if (slotSlot == null) return;
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
        for (EquipmentSlot slot: EquipmentSlot.values())
            mob.equipStack(slot, ItemStack.EMPTY);
    }
}
