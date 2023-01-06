package carpet.script.value;

import carpet.fakes.InventoryBearerInterface;
import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.utils.EquipmentInventory;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class NBTSerializableValue extends Value implements ContainerValueInterface
{
    private String nbtString = null;
    private Tag nbtTag = null;
    private Supplier<Tag> nbtSupplier = null;
    private boolean owned = false;

    private NBTSerializableValue() {}

    public NBTSerializableValue(String nbtString)
    {
        nbtSupplier = () ->
        {
            try
            {
                return (new TagParser(new StringReader(nbtString))).readValue();
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException("Incorrect NBT data: "+nbtString);
            }
        };
        owned = true;
    }

    public NBTSerializableValue(Tag tag)
    {
        nbtTag = tag;
        owned = true;
    }

    public static Value of(Tag tag)
    {
        if (tag == null) return Value.NULL;
        return new NBTSerializableValue(tag);
    }

    public NBTSerializableValue(Supplier<Tag> tagSupplier)
    {
        nbtSupplier = tagSupplier;
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

    public static String nameFromRegistryId(ResourceLocation id)
    {
        if (id == null) // should be Value.NULL
            return "";
        if (id.getNamespace().equals("minecraft"))
            return id.getPath();
        return id.toString();
    }

    public static NBTSerializableValue parseString(String nbtString, boolean fail)
    {
        Tag tag;
        try
        {
            tag = (new TagParser(new StringReader(nbtString))).readValue();
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
    public static Container getInventoryAt(ServerLevel world, BlockPos blockPos)
    {
        Container inventory = null;
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof WorldlyContainerHolder) {
            inventory = ((WorldlyContainerHolder)block).getContainer(blockState, world, blockPos);
        } else if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity = BlockValue.getBlockEntity(world, blockPos);
            if (blockEntity instanceof Container) {
                inventory = (Container)blockEntity;
                if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    inventory = ChestBlock.getContainer((ChestBlock)block, blockState, world, blockPos, true);
                }
            }
        }

        if (inventory == null) {
            List<Entity> list = world.getEntities(
                    (Entity)null, //TODO check this matches the correct method
                    new AABB(
                            blockPos.getX() - 0.5D, blockPos.getY() - 0.5D, blockPos.getZ() - 0.5D,
                            blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D),
                    EntitySelector.CONTAINER_ENTITY_SELECTOR
            );
            if (!list.isEmpty()) {
                inventory = (Container)list.get(world.random.nextInt(list.size()));
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
                    ServerPlayer player = EntityValue.getPlayerByValue(c.s.getServer(), v2);
                    if (player == null) throw new InternalExpressionException("enderchest inventory requires player argument");
                    return new InventoryLocator(player, player.blockPosition(), player.getEnderChestInventory(), offset + 2, true);
                }
                if (strVal.equals("equipment"))
                {
                    Value v2 = params.get(1 + offset);
                    if (!(v2 instanceof EntityValue)) throw new InternalExpressionException("Equipment inventory requires a living entity argument");
                    Entity e = ((EntityValue) v2).getEntity();
                    if (!(e instanceof LivingEntity)) throw new InternalExpressionException("Equipment inventory requires a living entity argument");
                    return new InventoryLocator(e, e.blockPosition(), new EquipmentInventory((LivingEntity) e), offset + 2);
                }
                boolean isEnder = strVal.startsWith("enderchest_");
                if (isEnder) strVal = strVal.substring(11); // len("enderchest_")
                ServerPlayer player = c.s.getServer().getPlayerList().getPlayerByName(strVal);
                if (player == null) throw new InternalExpressionException("String description of an inventory should either denote a player or player's enderchest");
                return new InventoryLocator(
                        player,
                        player.blockPosition(),
                        isEnder ? player.getEnderChestInventory() : player.getInventory(),
                        offset + 1,
                        isEnder
                );
            }
            if (v1 instanceof EntityValue)
            {
                Container inv = null;
                Entity e = ((EntityValue) v1).getEntity();
                if (e instanceof Player pe) inv = pe.getInventory();
                else if (e instanceof Container) inv = (Container) e;
                else if (e instanceof InventoryCarrier io) inv = io.getInventory();
                else if (e instanceof InventoryBearerInterface ibi) inv = ibi.getCMInventory(); // horse only
                else if (e instanceof LivingEntity le) return new InventoryLocator(e, e.blockPosition(), new EquipmentInventory(le), offset+1);
                if (inv == null)
                    return null;

                return new InventoryLocator(e, e.blockPosition(), inv, offset+1);
            }
            if (v1 instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) v1).getPos();
                if (pos == null)
                    throw new InternalExpressionException("Block to access inventory needs to be positioned in the world");
                Container inv = getInventoryAt(c.s.getLevel(), pos);
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
                Container inv = getInventoryAt(c.s.getLevel(), pos);
                if (inv == null)
                    return null;
                return new InventoryLocator(pos, pos, inv, offset+1);
            }
            if (v1 instanceof ScreenValue screenValue)
            {
                if(!screenValue.isOpen()) return null;
                return new InventoryLocator(screenValue.getPlayer(), screenValue.getPlayer().blockPosition(), screenValue.getInventory(), offset+1);
            }
            BlockPos pos = new BlockPos(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset)).getDouble());
            Container inv = getInventoryAt(c.s.getLevel(), pos);
            if (inv == null)
                return null;
            return new InventoryLocator(pos, pos, inv, offset + 3);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Inventory should be defined either by three coordinates, a block value, an entity, or a screen");
        }
    }

    private static final Map<String,ItemInput> itemCache = new HashMap<>();

    public static ItemInput parseItem(String itemString, RegistryAccess regs)
    {
        return parseItem(itemString, null, regs);
    }

    public static ItemInput parseItem(String itemString, CompoundTag customTag, RegistryAccess regs)
    {
        try
        {
            ItemInput res = itemCache.get(itemString);  // [SCARY SHIT] persistent caches over server reloads
            if (res != null)
                if (customTag == null)
                    return res;
                else
                    return new ItemInput(Holder.direct(res.getItem()), customTag);
            ItemParser.ItemResult parser = ItemParser.parseForItem(regs.lookupOrThrow(Registries.ITEM), new StringReader(itemString));
            res = new ItemInput(parser.item(), parser.nbt());

            itemCache.put(itemString, res);
            if (itemCache.size()>64000)
                itemCache.clear();
            if (customTag == null)
                return res;
            else
                return new ItemInput(Holder.direct(res.getItem()), customTag);
        }
        catch (CommandSyntaxException e)
        {
            throw new ThrowStatement(itemString, Throwables.UNKNOWN_ITEM);
        }
    }

    public static int validateSlot(int slot, Container inv)
    {
        int invSize = inv.getContainerSize();
        if (slot < 0)
            slot = invSize + slot;
        if (slot < 0 || slot >= invSize)
            return inv.getContainerSize(); // outside of inventory
        return slot;
    }

    private static Value decodeSimpleTag(Tag t)
    {
        if (t instanceof NumericTag)
        {
            if (t instanceof LongTag || t instanceof IntTag) // short and byte will never exceed float's precision, even int won't
            {
                return NumericValue.of(((NumericTag) t).getAsLong());
            }
            return NumericValue.of(((NumericTag) t).getAsNumber());
        }
        if (t instanceof StringTag)
            return StringValue.of(t.getAsString());
        if (t instanceof EndTag)
            return Value.NULL;

        throw new InternalExpressionException("How did we get here: Unknown nbt element class: "+t.getType().getName());

    }

    private static Value decodeTag(Tag t)
    {
        if (t instanceof CompoundTag || t instanceof CollectionTag)
            return new NBTSerializableValue(() -> t);
        return decodeSimpleTag(t);
    }

    private static Value decodeTagDeep(Tag t)
    {
        if (t instanceof CompoundTag)
        {
            Map<Value, Value> pairs = new HashMap<>();
            CompoundTag ctag = (CompoundTag)t;
            for (String key: ctag.getAllKeys())
            {
                pairs.put(new StringValue(key), decodeTagDeep(ctag.get(key)));
            }
            return MapValue.wrap(pairs);
        }
        if (t instanceof CollectionTag)
        {
            List<Value> elems = new ArrayList<>();
            CollectionTag<?> ltag = (CollectionTag<?>)t;
            for (Tag elem: ltag)
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
        if (v.isNull())
            return Value.NULL;
        return NBTSerializableValue.parseString(v.getString(), true);
    }

    public Tag getTag()
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
        Tag tag = getTag();
        if (tag instanceof CompoundTag)
            return !((CompoundTag) tag).isEmpty();
        if (tag instanceof CollectionTag)
            return !((CollectionTag<?>) tag).isEmpty();
        if (tag instanceof NumericTag)
            return ((NumericTag) tag).getAsDouble()!=0.0;
        if (tag instanceof StringTag)
            return !tag.getAsString().isEmpty();
        return true;
    }

    public CompoundTag getCompoundTag()
    {
        try
        {
            ensureOwnership();
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
        ensureOwnership();
        NbtPathArgument.NbtPath path = cachePath(where.getString());
        Tag tagToInsert = value instanceof NBTSerializableValue ?
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



    private boolean modify_insert(int index, NbtPathArgument.NbtPath nbtPath, Tag newElement)
    {
        return modify_insert(index, nbtPath, newElement, this.getTag());
    }

    private boolean modify_insert(int index, NbtPathArgument.NbtPath nbtPath, Tag newElement, Tag currentTag)
    {
        Collection<Tag> targets;
        try
        {
            targets = nbtPath.getOrCreate(currentTag, ListTag::new);
        }
        catch (CommandSyntaxException e)
        {
            return false;
        }

        boolean modified = false;
        for (Tag target : targets)
        {
            if (!(target instanceof CollectionTag))
            {
                continue;
            }
            try
            {
                CollectionTag<?> targetList = (CollectionTag<?>) target;
                if (!targetList.addTag(index < 0 ? targetList.size() + index + 1 : index, newElement.copy()))
                    return false;
                modified = true;
            }
            catch (IndexOutOfBoundsException ignored)
            {
            }
        }
        return modified;
    }


    private boolean modify_merge(NbtPathArgument.NbtPath nbtPath, Tag replacement) //nbtPathArgumentType$NbtPath_1, list_1)
    {
        if (!(replacement instanceof CompoundTag))
        {
            return false;
        }
        Tag ownTag = getTag();
        try
        {
            for (Tag target : nbtPath.getOrCreate(ownTag, CompoundTag::new))
            {
                if (!(target instanceof CompoundTag))
                {
                    continue;
                }
                ((CompoundTag) target).merge((CompoundTag) replacement);
            }
        }
        catch (CommandSyntaxException ignored)
        {
            return false;
        }
        return true;
    }

    private boolean modify_replace(NbtPathArgument.NbtPath nbtPath, Tag replacement) //nbtPathArgumentType$NbtPath_1, list_1)
    {
        Tag tag = getTag();
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
            NbtPathArgument.NbtPath newPath = cachePath(pathText.substring(0, pathText.length()-arrAccess.length()));
            return modify_insert(pos,newPath,replacement, tag);
        }
        try
        {
            nbtPath.set(tag, replacement);
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
        NbtPathArgument.NbtPath path = cachePath(valString);
        try
        {
            List<Tag> tags = path.get(getTag());
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
        return cachePath(where.getString()).countMatching(getTag()) > 0;
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
        NbtPathArgument.NbtPath path = cachePath(where.getString());
        ensureOwnership();
        int removed = path.remove(getTag());
        if (removed > 0)
        {
            dirty();
            return true;
        }
        return false;
    }

    public static record InventoryLocator(Object owner, BlockPos position, Container inventory, int offset, boolean isEnder)
    {
        InventoryLocator(Object owner, BlockPos pos, Container i, int o)
        {
            this(owner, pos, i, o, false);
        }
    }

    private static Map<String, NbtPathArgument.NbtPath> pathCache = new HashMap<>();
    private static NbtPathArgument.NbtPath cachePath(String arg)
    {
        NbtPathArgument.NbtPath res = pathCache.get(arg);
        if (res != null)
            return res;
        try
        {
            res = NbtPathArgument.nbtPath().parse(new StringReader(arg));
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
    public Tag toTag(boolean force)
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
