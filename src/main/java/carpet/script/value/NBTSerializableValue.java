package carpet.script.value;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.arguments.ItemStackArgument;
import net.minecraft.command.arguments.ItemStringReader;
import net.minecraft.command.arguments.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractListTag;
import net.minecraft.nbt.AbstractNumberTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NBTSerializableValue extends Value implements ContainerValueInterface
{
    private String nbtString = null;
    private Tag nbtTag = null;
    private Supplier<Tag> nbtSupplier = null;

    private NBTSerializableValue() {}

    public NBTSerializableValue(String nbtString)
    {
        nbtSupplier = () ->
        {
            try
            {
                return (new StringNbtReader(new StringReader(nbtString))).parseTag();
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException("Incorrect NBT data: "+nbtString);
            }
        };
    }

    public NBTSerializableValue(Tag tag)
    {
        nbtTag = tag;
    }

    public static Value fromStack(ItemStack stack)
    {
        if (stack.hasTag())
        {
            NBTSerializableValue value = new NBTSerializableValue();
            value.nbtSupplier = stack::getTag;
            return value;
        }
        return Value.NULL;
    }

    public static String nameFromRegistryId(Identifier id)
    {
        if (id == null) // should be Value.NULL
            return "";
        if (id.getNamespace().equals("minecraft"))
            return id.getPath();
        return id.toString();
    }

    public static NBTSerializableValue parseString(String nbtString)
    {
        Tag tag;
        try
        {
            tag = (new StringNbtReader(new StringReader(nbtString))).parseTag();
        }
        catch (CommandSyntaxException e)
        {
            return null;
        }
        NBTSerializableValue value = new NBTSerializableValue(tag);
        value.nbtString = nbtString;
        return value;
    }


    @Override
    public Value clone()
    {
        // sets only nbttag, even if emtpy;
        NBTSerializableValue copy = new NBTSerializableValue(nbtTag);
        copy.nbtSupplier = this.nbtSupplier;
        copy.nbtString = this.nbtString;
        return copy;
    }

    @Override
    public Value deepcopy()
    {
        NBTSerializableValue copy = (NBTSerializableValue) clone();
        // same fields except when tag is set - need to copy it.
        if (copy.nbtTag != null)
            copy.nbtTag = copy.getTag().copy();
        return copy;
    }

    // stolen from HopperBlockEntity, adjusted for threaded operation
    public static Inventory getInventoryAt(ServerWorld world, BlockPos blockPos)
    {
        Inventory inventory = null;
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof InventoryProvider) {
            inventory = ((InventoryProvider)block).getInventory(blockState, world, blockPos);
        } else if (block.hasBlockEntity()) {
            BlockEntity blockEntity = BlockValue.getBlockEntity(world, blockPos);
            if (blockEntity instanceof Inventory) {
                inventory = (Inventory)blockEntity;
                if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    inventory = ChestBlock.getInventory((ChestBlock)block, blockState, world, blockPos, true);
                }
            }
        }

        if (inventory == null) {
            List<Entity> list = world.getEntities(
                    (Entity)null,
                    new Box(
                            blockPos.getX() - 0.5D, blockPos.getY() - 0.5D, blockPos.getZ() - 0.5D,
                            blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D),
                    EntityPredicates.VALID_INVENTORIES
            );
            if (!list.isEmpty()) {
                inventory = (Inventory)list.get(world.random.nextInt(list.size()));
            }
        }

        return inventory;
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
                inv = getInventoryAt(c.s.getWorld(), pos);
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
                inv = getInventoryAt(c.s.getWorld(), pos);
                if (inv == null)
                    return null;
                return new InventoryLocator(pos, pos, inv, offset + 1);
            }
            else if (v1.getString().equalsIgnoreCase("enderchest"))
            {
                Value v2 = params.get(1 + offset).evalValue(c);
                if (!(v2 instanceof EntityValue) || !(((EntityValue) v2).getEntity() instanceof PlayerEntity))
                {
                    throw new InternalExpressionException("enderchest inventory requires player argument");
                }
                PlayerEntity e = (PlayerEntity)((EntityValue) v2).getEntity();
                inv = e.getEnderChestInventory();
                return new InventoryLocator(e, e.getBlockPos(), inv, offset + 2, true);
            }
            BlockPos pos = new BlockPos(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset).evalValue(c)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset).evalValue(c)).getDouble());
            inv = getInventoryAt(c.s.getWorld(), pos);
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

    private static Value decodeTag(Tag t)
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

    private void replaceTag(Tag newTag)
    {
        nbtTag = newTag;
        nbtString = null;
        nbtSupplier = null;  // just to be sure
    }

    @Override
    public boolean equals(final Object o)
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
        Tag tag = getTag();
        if (tag instanceof CompoundTag)
            return !((CompoundTag) tag).isEmpty();
        if (tag instanceof AbstractListTag)
            return ((AbstractListTag) tag).isEmpty();
        if (tag instanceof AbstractNumberTag)
            return ((AbstractNumberTag) tag).getDouble()!=0.0;
        if (tag instanceof StringTag)
            return tag.asString().isEmpty();
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

    @Override
    public boolean put(Value where, Value value)
    {
        return put(where, value, new StringValue("replace"));
    }

    @Override
    public boolean put(Value where, Value value, Value conditions)
    {
        /// WIP

        NbtPathArgumentType.NbtPath path = cachePath(where.getString());
        Tag tagToInsert = value instanceof NBTSerializableValue ?
                ((NBTSerializableValue) value).getTag() :
                new NBTSerializableValue(value.getString()).getTag();
        Tag modifiedTag;
        if (conditions instanceof NumericValue)
        {
            modifiedTag = modify_insert((int)((NumericValue) conditions).getLong(), path, tagToInsert);
        }
        else
        {
            String ops = conditions.getString();
            if (ops.equalsIgnoreCase("merge"))
            {
                modifiedTag = modify_merge(path, tagToInsert);
            }
            else if (ops.equalsIgnoreCase("replace"))
            {
                modifiedTag = modify_replace(path, tagToInsert);
            }
            else
            {
                return false;
            }
        }
        if (modifiedTag != null)
        {
            replaceTag(modifiedTag);
            return  true;
        }
        return false;
    }



    private Tag modify_insert(int index, NbtPathArgumentType.NbtPath nbtPath, Tag newElement)
    {
        return modify_insert(index, nbtPath, newElement, this.getTag().copy());
    }

    private Tag modify_insert(int index, NbtPathArgumentType.NbtPath nbtPath, Tag newElement, Tag currentTag)
    {
        Collection<Tag> targets;
        try
        {
            targets = nbtPath.getOrInit(currentTag, ListTag::new);
        }
        catch (CommandSyntaxException e)
        {
            return null;
        }

        boolean modified = false;
        for (Tag target : targets)
        {
            if (!(target instanceof AbstractListTag))
            {
                continue;
            }
            try
            {
                AbstractListTag<?> targetList = (AbstractListTag) target;
                if (!targetList.addTag(index < 0 ? targetList.size() + index + 1 : index, newElement.copy()))
                    return null;
                modified = true;
            }
            catch (IndexOutOfBoundsException ignored)
            {
            }
        }
        return modified?currentTag:null;
    }


    private Tag modify_merge(NbtPathArgumentType.NbtPath nbtPath, Tag replacement) //nbtPathArgumentType$NbtPath_1, list_1)
    {
        if (!(replacement instanceof CompoundTag))
        {
            return getTag();
        }
        Tag originalTag = getTag().copy();
        try
        {
            for (Tag target : nbtPath.getOrInit(originalTag, CompoundTag::new))
            {
                if (!(target instanceof CompoundTag))
                {
                    continue;
                }
                ((CompoundTag) target).copyFrom((CompoundTag) replacement);
            }
        }
        catch (CommandSyntaxException ignored) { }
        return originalTag;
    }

    private Tag modify_replace(NbtPathArgumentType.NbtPath nbtPath, Tag replacement) //nbtPathArgumentType$NbtPath_1, list_1)
    {
        Tag originalTag = getTag().copy();
        String pathText = nbtPath.toString();
        if (pathText.endsWith("]")) // workaround for array replacement or item in the array replacement
        {
            if (nbtPath.remove(originalTag)==0)
                return null;
            Pattern pattern = Pattern.compile("\\[[^\\[]*]$");
            Matcher matcher = pattern.matcher(pathText);
            if (!matcher.find()) // malformed path
            {
                return null;
            }
            String arrAccess = matcher.group();
            int pos;
            if (arrAccess.length()==2) // we just removed entire array
                pos = 0;
            else
            {
                try
                {
                    pos = Integer.parseInt(arrAccess.substring(1, arrAccess.length() - 1));
                }
                catch (NumberFormatException e)
                {
                    return null;
                }
            }
            NbtPathArgumentType.NbtPath newPath = cachePath(pathText.substring(0, pathText.length()-arrAccess.length()));
            return modify_insert(pos,newPath,replacement, originalTag);
        }
        try
        {
            nbtPath.put(originalTag, () -> replacement);
        }
        catch (CommandSyntaxException e)
        {
            return null;
        }
        return originalTag;
    }

    @Override
    public Value get(Value value)
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
    public boolean has(Value where)
    {
        return cachePath(where.getString()).count(getTag()) > 0;
    }

    @Override
    public boolean delete(Value where)
    {
        NbtPathArgumentType.NbtPath path = cachePath(where.getString());
        Tag tag = getTag().copy();
        int removed = path.remove(tag);
        if (removed > 0) replaceTag(tag);
        return removed > 0;
    }

    public static class InventoryLocator
    {
        public Object owner;
        public BlockPos position;
        public Inventory inventory;
        public int offset;
        public boolean isEnder;
        InventoryLocator(Object owner, BlockPos pos, Inventory i, int o)
        {
            this(owner, pos, i, o, false);
        }

        InventoryLocator(Object owner, BlockPos pos, Inventory i, int o, boolean isEnder)
        {
            this.owner = owner;
            position = pos;
            inventory = i;
            offset = o;
            this.isEnder = isEnder;
        }
    }

    private static Map<String, NbtPathArgumentType.NbtPath> pathCache = new HashMap<>();
    private static NbtPathArgumentType.NbtPath cachePath(String arg)
    {
        NbtPathArgumentType.NbtPath res = pathCache.get(arg);
        if (res != null)
            return res;
        try
        {
            res = NbtPathArgumentType.nbtPath().parse(new StringReader(arg));
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
    public String getTypeString()
    {
        return "nbt";
    }
}
