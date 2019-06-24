package carpet.script.value;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractTraderEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.Supplier;

public class NBTSerializableValue extends Value
{
    private Object what;
    private String nbtString = null;
    private CompoundTag nbtTag = null;
    private Supplier<CompoundTag> nbtSupplier;

    public NBTSerializableValue(ItemStack stack)
    {
        what = stack;
        nbtSupplier = () -> ((ItemStack)what).toTag(new CompoundTag());
    }

    public static InventoryLocator locateInventory(CarpetContext c, List<LazyValue> params, int offset)
    {
        try
        {
            Inventory inv = null;
            Value v1 = params.get(0 + offset).evalValue(c);
            if (v1 instanceof EntityValue)
            {
                Entity e = ((EntityValue) v1).getEntity();
                if (e instanceof PlayerEntity) inv = ((PlayerEntity) e).inventory;
                else if (e instanceof Inventory) inv = (Inventory)e;
                else if (e instanceof VillagerEntity) inv = ((VillagerEntity) e).getInventory();

                if (inv == null)
                    throw new InternalExpressionException("Entity "+e+" has no inventory");

                return new InventoryLocator(e, e.getBlockPos(), inv, offset+1);
            }
            else if (v1 instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) v1).getPos();
                if (pos == null)
                    throw new InternalExpressionException("Block to acess inventory needs to be positioned in the world");
                inv = HopperBlockEntity.getInventoryAt(c.s.getWorld(), pos);
                if (inv == null)
                    throw new InternalExpressionException("Block at "+pos+" has no inventory");
                return new InventoryLocator(pos, pos, inv, offset+1);
            }
            else if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                BlockPos pos = new BlockPos(
                        NumericValue.asNumber(args.get(0)).getDouble(),
                        NumericValue.asNumber(args.get(1)).getDouble(),
                        NumericValue.asNumber(args.get(2)).getDouble());
                inv = HopperBlockEntity.getInventoryAt(c.s.getWorld(), pos);
                if (inv == null)
                    throw new InternalExpressionException("Block at "+pos+" has no inventory");
                return new InventoryLocator(pos, pos, inv, offset+1);
            }
            BlockPos pos = new BlockPos(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset).evalValue(c)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset).evalValue(c)).getDouble());
            inv = HopperBlockEntity.getInventoryAt(c.s.getWorld(), pos);
            if (inv == null)
                throw new InternalExpressionException("Block at "+pos+" has no inventory");
            return new InventoryLocator(pos, pos, inv, offset+3);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Inventory should be defined either by three coordinates, a block value, or an entity");
        }
    }

    private CompoundTag getTag()
    {
        if (nbtTag == null)
            nbtTag = nbtSupplier.get();
        return nbtTag;
    }


    @Override
    public String getString()
    {
        if (nbtString == null)
            nbtString = getTag().toString();
        return nbtString;
    }

    @Override
    public boolean getBoolean()
    {
        return true;
    }

    public static class InventoryLocator
    {
        public Object owner;
        public BlockPos position;
        public Inventory inventory;
        public int offset;
        InventoryLocator(Object owner, BlockPos pos, Inventory i, int o)
        {
            this.owner = owner;
            position = pos;
            inventory = i;
            offset = o;
        }
    }
}
