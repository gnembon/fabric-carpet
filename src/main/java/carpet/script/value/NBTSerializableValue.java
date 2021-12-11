package carpet.script.value;

import carpet.fakes.InventoryBearerInterface;
import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.utils.EquipmentInventory;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtList;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtNull;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtElement;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NBTSerializableValue extends Value implements ContainerValueInterface
{
    private String nbtString = null;
    private NbtElement nbtTag = null;
    private Supplier<NbtElement> nbtSupplier = null;
    private boolean owned = false;

    private NBTSerializableValue() {}

    public NBTSerializableValue(String nbtString)
    {
        nbtSupplier = () ->
        {
            try
            {
                return (new StringNbtReader(new StringReader(nbtString))).parseElement();
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException("Incorrect NBT data: "+nbtString);
            }
        };
        owned = true;
    }

    public NBTSerializableValue(NbtElement tag)
    {
        nbtTag = tag;
        owned = true;
    }

    public static Value of(NbtElement tag)
    {
        if (tag == null) return Value.NULL;
        return new NBTSerializableValue(tag);
    }

    public NBTSerializableValue(Supplier<NbtElement> tagSupplier)
    {
        nbtSupplier = tagSupplier;
    }

    public static Value fromStack(ItemStack stack)
    {
        if (stack.hasNbt())
        {
            NBTSerializableValue value = new NBTSerializableValue();
            value.nbtSupplier = stack::getNbt;
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

    public static NBTSerializableValue parseString(String nbtString, boolean fail)
    {
        NbtElement tag;
        try
        {
            tag = (new StringNbtReader(new StringReader(nbtString))).parseElement();
        }
        catch (CommandSyntaxException e)
        {
            if (fail) throw new InternalExpressionException("Incorrect NBT tag: "+ nbtString);
            return null;
        }
        NBTSerializableValue value = new NBTSerializableValue(tag);
        value.nbtString = null;
        return value;
    }


    @Override
    public Value clone()
    {
        // sets only nbttag, even if emtpy;
        NBTSerializableValue copy = new NBTSerializableValue(nbtTag);
        copy.nbtSupplier = this.nbtSupplier;
        copy.nbtString = this.nbtString;
        copy.owned = this.owned;
        return copy;
    }

    @Override
    public Value deepcopy()
    {
        NBTSerializableValue copy = (NBTSerializableValue) clone();
        copy.owned = false;
        ensureOwnership();
        return copy;
    }

    @Override
    public Value fromConstant() {
        return deepcopy();
    }

    // stolen from HopperBlockEntity, adjusted for threaded operation
    public static Inventory getInventoryAt(ServerWorld world, BlockPos blockPos)
    {
        Inventory inventory = null;
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof InventoryProvider) {
            inventory = ((InventoryProvider)block).getInventory(blockState, world, blockPos);
        } else if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity = BlockValue.getBlockEntity(world, blockPos);
            if (blockEntity instanceof Inventory) {
                inventory = (Inventory)blockEntity;
                if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    inventory = ChestBlock.getInventory((ChestBlock)block, blockState, world, blockPos, true);
                }
            }
        }

        if (inventory == null) {
            List<Entity> list = world.getOtherEntities(
                    null,
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

    public static InventoryLocator locateInventory(CarpetContext c, List<Value> params, int offset)
    {
        try
        {
            Value v1 = params.get(offset);
            if (v1.isNull())
            {
                offset ++;
                v1 = params.get(offset);
            }
            else if (v1 instanceof StringValue)
            {
                String strVal = v1.getString().toLowerCase(Locale.ROOT);
                if (strVal.equals("enderchest"))
                {
                    Value v2 = params.get(1 + offset);
                    ServerPlayerEntity player = EntityValue.getPlayerByValue(c.s.getServer(), v2);
                    if (player == null) throw new InternalExpressionException("enderchest inventory requires player argument");
                    return new InventoryLocator(player, player.getBlockPos(), player.getEnderChestInventory(), offset + 2, true);
                }
                if (strVal.equals("equipment"))
                {
                    Value v2 = params.get(1 + offset);
                    if (!(v2 instanceof EntityValue)) throw new InternalExpressionException("Equipment inventory requires a living entity argument");
                    Entity e = ((EntityValue) v2).getEntity();
                    if (!(e instanceof LivingEntity)) throw new InternalExpressionException("Equipment inventory requires a living entity argument");
                    return new InventoryLocator(e, e.getBlockPos(), new EquipmentInventory((LivingEntity) e), offset + 2);
                }
                boolean isEnder = strVal.startsWith("enderchest_");
                if (isEnder) strVal = strVal.substring(11); // len("enderchest_")
                ServerPlayerEntity player = c.s.getServer().getPlayerManager().getPlayer(strVal);
                if (player == null) throw new InternalExpressionException("String description of an inventory should either denote a player or player's enderchest");
                return new InventoryLocator(
                        player,
                        player.getBlockPos(),
                        isEnder ? player.getEnderChestInventory() : player.getInventory(),
                        offset + 1,
                        isEnder
                );
            }
            if (v1 instanceof EntityValue)
            {
                Inventory inv = null;
                Entity e = ((EntityValue) v1).getEntity();
                if (e instanceof PlayerEntity pe) inv = pe.getInventory();
                else if (e instanceof Inventory) inv = (Inventory) e;
                else if (e instanceof InventoryOwner io) inv = io.getInventory();
                else if (e instanceof InventoryBearerInterface ibi) inv = ibi.getCMInventory(); // horse only
                else if (e instanceof LivingEntity le) return new InventoryLocator(e, e.getBlockPos(), new EquipmentInventory(le), offset+1);
                if (inv == null)
                    return null;

                return new InventoryLocator(e, e.getBlockPos(), inv, offset+1);
            }
            if (v1 instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) v1).getPos();
                if (pos == null)
                    throw new InternalExpressionException("Block to access inventory needs to be positioned in the world");
                Inventory inv = getInventoryAt(c.s.getWorld(), pos);
                if (inv == null)
                    return null;
                return new InventoryLocator(pos, pos, inv, offset+1);
            }
            if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                BlockPos pos = new BlockPos(
                        NumericValue.asNumber(args.get(0)).getDouble(),
                        NumericValue.asNumber(args.get(1)).getDouble(),
                        NumericValue.asNumber(args.get(2)).getDouble());
                Inventory inv = getInventoryAt(c.s.getWorld(), pos);
                if (inv == null)
                    return null;
                return new InventoryLocator(pos, pos, inv, offset+1);
            }
            if (v1 instanceof ScreenValue screenValue)
            {
                if(!screenValue.isOpen()) return null;
                return new InventoryLocator(screenValue.getScreenHandler(), screenValue.getPlayer().getBlockPos(), screenValue.getInventory(), offset+1);
            }
            BlockPos pos = new BlockPos(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset)).getDouble());
            Inventory inv = getInventoryAt(c.s.getWorld(), pos);
            if (inv == null)
                return null;
            return new InventoryLocator(pos, pos, inv, offset + 3);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Inventory should be defined either by three coordinates, a block value, an entity, or a screen");
        }
    }

    private static final Map<String,ItemStackArgument> itemCache = new HashMap<>();

    public static ItemStackArgument parseItem(String itemString)
    {
        return parseItem(itemString, null);
    }

    public static ItemStackArgument parseItem(String itemString, NbtCompound customTag)
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
            res = new ItemStackArgument(parser.getItem(), parser.getNbt());
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
            throw new ThrowStatement(itemString, Throwables.UNKNOWN_ITEM);
        }
    }

    public static int validateSlot(int slot, Inventory inv)
    {
        int invSize = inv.size();
        if (slot < 0)
            slot = invSize + slot;
        if (slot < 0 || slot >= invSize)
            return inv.size(); // outside of inventory
        return slot;
    }

    private static Value decodeSimpleTag(NbtElement t)
    {
        if (t instanceof AbstractNbtNumber)
        {
            if (t instanceof NbtLong || t instanceof NbtInt) // short and byte will never exceed float's precision, even int won't
            {
                return NumericValue.of(((AbstractNbtNumber) t).longValue());
            }
            return NumericValue.of(((AbstractNbtNumber) t).numberValue());
        }
        if (t instanceof NbtString)
            return StringValue.of(t.asString());
        if (t instanceof NbtNull)
            return Value.NULL;

        throw new InternalExpressionException("How did we get here: Unknown nbt element class: "+t.getNbtType().getCrashReportName());

    }

    private static Value decodeTag(NbtElement t)
    {
        if (t instanceof NbtCompound || t instanceof AbstractNbtList)
            return new NBTSerializableValue(() -> t);
        return decodeSimpleTag(t);
    }

    private static Value decodeTagDeep(NbtElement t)
    {
        if (t instanceof NbtCompound)
        {
            Map<Value, Value> pairs = new HashMap<>();
            NbtCompound ctag = (NbtCompound)t;
            for (String key: ctag.getKeys())
            {
                pairs.put(new StringValue(key), decodeTagDeep(ctag.get(key)));
            }
            return MapValue.wrap(pairs);
        }
        if (t instanceof AbstractNbtList)
        {
            List<Value> elems = new ArrayList<>();
            AbstractNbtList<? extends NbtElement> ltag = (AbstractNbtList<? extends NbtElement>)t;
            for (NbtElement elem: ltag)
            {
                elems.add(decodeTagDeep(elem));
            }
            return ListValue.wrap(elems);
        }
        return decodeSimpleTag(t);
    }

    public Value toValue()
    {
        return decodeTagDeep(this.getTag());
    }

    public static Value fromValue(Value v)
    {
        if (v instanceof NBTSerializableValue)
            return v;
        if (v instanceof NullValue)
            return Value.NULL;
        return NBTSerializableValue.parseString(v.getString(), true);
    }

    public NbtElement getTag()
    {
        if (nbtTag == null)
            nbtTag = nbtSupplier.get();
        return nbtTag;
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
        NbtElement tag = getTag();
        if (tag instanceof NbtCompound)
            return !((NbtCompound) tag).isEmpty();
        if (tag instanceof AbstractNbtList)
            return ((AbstractNbtList) tag).isEmpty();
        if (tag instanceof AbstractNbtNumber)
            return ((AbstractNbtNumber) tag).doubleValue()!=0.0;
        if (tag instanceof NbtString)
            return tag.asString().isEmpty();
        return true;
    }

    public NbtCompound getCompoundTag()
    {
        try
        {
            ensureOwnership();
            return (NbtCompound) getTag();
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
        ensureOwnership();
        NbtPathArgumentType.NbtPath path = cachePath(where.getString());
        NbtElement tagToInsert = value instanceof NBTSerializableValue ?
                ((NBTSerializableValue) value).getTag() :
                new NBTSerializableValue(value.getString()).getTag();
        boolean modifiedTag;
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
        if (modifiedTag) dirty();
        return modifiedTag;
    }



    private boolean modify_insert(int index, NbtPathArgumentType.NbtPath nbtPath, NbtElement newElement)
    {
        return modify_insert(index, nbtPath, newElement, this.getTag());
    }

    private boolean modify_insert(int index, NbtPathArgumentType.NbtPath nbtPath, NbtElement newElement, NbtElement currentTag)
    {
        Collection<NbtElement> targets;
        try
        {
            targets = nbtPath.getOrInit(currentTag, NbtList::new);
        }
        catch (CommandSyntaxException e)
        {
            return false;
        }

        boolean modified = false;
        for (NbtElement target : targets)
        {
            if (!(target instanceof AbstractNbtList))
            {
                continue;
            }
            try
            {
                AbstractNbtList<?> targetList = (AbstractNbtList) target;
                if (!targetList.addElement(index < 0 ? targetList.size() + index + 1 : index, newElement.copy()))
                    return false;
                modified = true;
            }
            catch (IndexOutOfBoundsException ignored)
            {
            }
        }
        return modified;
    }


    private boolean modify_merge(NbtPathArgumentType.NbtPath nbtPath, NbtElement replacement) //nbtPathArgumentType$NbtPath_1, list_1)
    {
        if (!(replacement instanceof NbtCompound))
        {
            return false;
        }
        NbtElement ownTag = getTag();
        try
        {
            for (NbtElement target : nbtPath.getOrInit(ownTag, NbtCompound::new))
            {
                if (!(target instanceof NbtCompound))
                {
                    continue;
                }
                ((NbtCompound) target).copyFrom((NbtCompound) replacement);
            }
        }
        catch (CommandSyntaxException ignored)
        {
            return false;
        }
        return true;
    }

    private boolean modify_replace(NbtPathArgumentType.NbtPath nbtPath, NbtElement replacement) //nbtPathArgumentType$NbtPath_1, list_1)
    {
        NbtElement tag = getTag();
        String pathText = nbtPath.toString();
        if (pathText.endsWith("]")) // workaround for array replacement or item in the array replacement
        {
            if (nbtPath.remove(tag)==0)
                return false;
            Pattern pattern = Pattern.compile("\\[[^\\[]*]$");
            Matcher matcher = pattern.matcher(pathText);
            if (!matcher.find()) // malformed path
            {
                return false;
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
                    return false;
                }
            }
            NbtPathArgumentType.NbtPath newPath = cachePath(pathText.substring(0, pathText.length()-arrAccess.length()));
            return modify_insert(pos,newPath,replacement, tag);
        }
        try
        {
            nbtPath.put(tag, () -> replacement);
        }
        catch (CommandSyntaxException e)
        {
            return false;
        }
        return true;
    }

    @Override
    public Value get(Value value)
    {
        String valString = value.getString();
        NbtPathArgumentType.NbtPath path = cachePath(valString);
        try
        {
            List<NbtElement> tags = path.get(getTag());
            if (tags.size()==0)
                return Value.NULL;
            if (tags.size()==1 && !valString.endsWith("[]"))
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

    private void ensureOwnership()
    {
        if (!owned)
        {
            nbtTag = getTag().copy();
            nbtString = null;
            nbtSupplier = null;  // just to be sure
            owned = true;
        }
    }

    private void dirty()
    {
        nbtString = null;
    }

    @Override
    public boolean delete(Value where)
    {
        NbtPathArgumentType.NbtPath path = cachePath(where.getString());
        ensureOwnership();
        int removed = path.remove(getTag());
        if (removed > 0)
        {
            dirty();
            return true;
        }
        return false;
    }

    public static record InventoryLocator(Object owner, BlockPos position, Inventory inventory, int offset, boolean isEnder)
    {
        InventoryLocator(Object owner, BlockPos pos, Inventory i, int o)
        {
            this(owner, pos, i, o, false);
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


    @Override
    public NbtElement toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        ensureOwnership();
        return getTag();
    }

    public static class IncompatibleTypeException extends RuntimeException
    {
        private IncompatibleTypeException() {}
        public Value val;
        public IncompatibleTypeException(Value val)
        {
            this.val = val;
        }
    };
}
