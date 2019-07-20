package carpet.script.value;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.command.arguments.ItemStackArgument;
import net.minecraft.command.arguments.ItemStringReader;
import net.minecraft.command.arguments.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNumberTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.nbt.Tag;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NBTSerializableValue extends Value
{
    private String nbtString = null;
    private Tag nbtTag = null;
    private Supplier<CompoundTag> nbtSupplier = null;

    public NBTSerializableValue(ItemStack stack)
    {
        nbtSupplier = () -> stack.hasTag() ? stack.getTag(): new CompoundTag();
    }

    public NBTSerializableValue(String nbtString)
    {
        nbtSupplier = () ->
        {
            try
            {
                return (new StringNbtReader(new StringReader(nbtString))).parseCompoundTag();
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException("Incorrect nbt data: "+nbtString);
            }
        };
    }

    public NBTSerializableValue(Tag tag)
    {
        nbtTag = tag;
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
                else if (e instanceof Inventory) inv = (Inventory) e;
                else if (e instanceof VillagerEntity) inv = ((VillagerEntity) e).getInventory();

                if (inv == null)
                    return null;

                return new InventoryLocator(e, e.getBlockPos(), inv, offset + 1);
            }
            else if (v1 instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) v1).getPos();
                if (pos == null)
                    throw new InternalExpressionException("Block to access inventory needs to be positioned in the world");
                inv = HopperBlockEntity.getInventoryAt(c.s.getWorld(), pos);
                if (inv == null)
                    return null;
                return new InventoryLocator(pos, pos, inv, offset + 1);
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
                    return null;
                return new InventoryLocator(pos, pos, inv, offset + 1);
            }
            BlockPos pos = new BlockPos(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset).evalValue(c)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset).evalValue(c)).getDouble());
            inv = HopperBlockEntity.getInventoryAt(c.s.getWorld(), pos);
            if (inv == null)
                return null;
            return new InventoryLocator(pos, pos, inv, offset + 3);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Inventory should be defined either by three coordinates, a block value, or an entity");
        }
    }

    private static Map<String,ItemStackArgument> itemCache = new HashMap<>();

    public static ItemStackArgument parseItem(String itemString)
    {
        return parseItem(itemString, null);
    }

    public static ItemStackArgument parseItem(String itemString, CompoundTag customTag)
    {
        try
        {
            ItemStackArgument res = itemCache.get(itemString);
            if (res != null)
                if (customTag == null)
                    return res;
                else
                    return new ItemStackArgument(res.getItem(), customTag);

            ItemStringReader parser = (new ItemStringReader(new StringReader(itemString), false)).consume();
            res = new ItemStackArgument(parser.getItem(), parser.getTag());
            itemCache.put(itemString, res);
            if (itemCache.size()>64000)
                itemCache.clear();
            if (customTag == null)
                return res;
            else
                return new ItemStackArgument(res.getItem(), customTag);
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException("Incorrect item: "+itemString);
        }
    }

    public static int validateSlot(int slot, Inventory inv)
    {
        int invSize = inv.getInvSize();
        if (slot < 0)
            slot = invSize + slot;
        if (slot < 0 || slot >= invSize)
            return inv.getInvSize(); // outside of inventory
        return slot;
    }

    public static Value decodeTag(Tag t)
    {
        if (t instanceof CompoundTag)
            return new NBTSerializableValue(t);
        if (t instanceof AbstractNumberTag)
            return new NumericValue(((AbstractNumberTag) t).getDouble());
        // more can be done here
        return new StringValue(t.asString());
    }

    public Tag getTag()
    {
        if (nbtTag == null)
            nbtTag = nbtSupplier.get();
        return nbtTag;
    }

    @Override
    public boolean equals(final Value o)
    {
        if (o instanceof NBTSerializableValue)
            return getTag().equals(((NBTSerializableValue) o).getTag());
        return super.equals(o);
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

    public CompoundTag getCompoundTag()
    {
        try
        {
            return (CompoundTag) getTag();
        }
        catch (ClassCastException e)
        {
            throw new InternalExpressionException(getString()+" is not a valid compound tag");
        }
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

    private static Map<String, NbtPathArgumentType.NbtPath> pathCache = new HashMap<>();
    public static NbtPathArgumentType.NbtPath cachePath(String arg)
    {
        NbtPathArgumentType.NbtPath res = pathCache.get(arg);
        if (res != null)
            return res;
        try
        {
            res = NbtPathArgumentType.nbtPath().method_9362(new StringReader(arg));
        }
        catch (CommandSyntaxException exc)
        {
            throw new InternalExpressionException("Incorrect nbt path: "+arg);
        }
        if (pathCache.size() > 1024)
            pathCache.clear();
        pathCache.put(arg, res);
        return res;
    }

    @Override
    public Value getElementAt(Value value)
    {
        NbtPathArgumentType.NbtPath path = cachePath(value.getString());
        try
        {
            List<Tag> tags = path.get(getTag());
            if (tags.size()==0)
                return Value.NULL;
            if (tags.size()==1)
                return NBTSerializableValue.decodeTag(tags.get(0));
            return ListValue.wrap(tags.stream().map(NBTSerializableValue::decodeTag).collect(Collectors.toList()));
        }
        catch (CommandSyntaxException ignored) { }
        return Value.NULL;
    }

    @Override
    public String getTypeString()
    {
        return "nbt";
    }
}
