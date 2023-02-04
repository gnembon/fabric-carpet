package carpet.script.value;

import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import static java.lang.Math.abs;

public class ListValue extends AbstractListValue implements ContainerValueInterface
{
    protected final List<Value> items;

    @Override
    public String getString()
    {
        return "[" + items.stream().map(Value::getString).collect(Collectors.joining(", ")) + "]";
    }

    @Override
    public String getPrettyString()
    {
        return items.size() < 8
                ? "[" + items.stream().map(Value::getPrettyString).collect(Collectors.joining(", ")) + "]"
                : "[" + items.get(0).getPrettyString() + ", " + items.get(1).getPrettyString() + ", ..., " + items.get(items.size() - 2).getPrettyString() + ", " + items.get(items.size() - 1).getPrettyString() + "]";
    }

    @Override
    public boolean getBoolean()
    {
        return !items.isEmpty();
    }

    @Override
    public Value clone()
    {
        return new ListValue(items);
    }

    @Override
    public Value deepcopy()
    {
        final List<Value> copyItems = new ArrayList<>(items.size());
        for (final Value entry : items)
        {
            copyItems.add(entry.deepcopy());
        }
        return new ListValue(copyItems);
    }

    public ListValue(final Collection<? extends Value> list)
    {
        items = new ArrayList<>(list);
    }

    protected ListValue(final List<Value> list)
    {
        items = list;
    }

    public static Value fromTriple(final double a, final double b, final double c)
    {
        return ListValue.of(new NumericValue(a), new NumericValue(b), new NumericValue(c));
    }

    public static Value fromTriple(final int a, final int b, final int c)
    {
        return fromTriple((double) a, b, c);
    }


    public static ListValue wrap(final Stream<Value> stream)
    {
        return wrap(stream.collect(Collectors.toList()));
    }

    public static ListValue wrap(final List<Value> list)
    {
        return new ListValue(list);
    }

    public static ListValue of(final Value... list)
    {
        return new ListValue(new ArrayList<>(Arrays.asList(list)));
    }

    public static ListValue ofNums(final Number... list)
    {
        final List<Value> valList = new ArrayList<>();
        for (final Number i : list)
        {
            valList.add(new NumericValue(i.doubleValue()));
        }
        return new ListValue(valList);
    }

    public static LazyValue lazyEmpty()
    {
        final Value ret = new ListValue();
        return (c, t) -> ret;
    }

    private ListValue()
    {
        items = new ArrayList<>();
    }

    @Override
    public Value add(final Value other)
    {
        final ListValue output = new ListValue();
        if (other instanceof final ListValue list)
        {
            final List<Value> otherItems = list.items;
            if (otherItems.size() == items.size())
            {
                for (int i = 0, size = items.size(); i < size; i++)
                {
                    output.items.add(items.get(i).add(otherItems.get(i)));
                }
            }
            else
            {
                throw new InternalExpressionException("Cannot add two lists of uneven sizes");
            }
        }
        else
        {
            for (final Value v : items)
            {
                output.items.add(v.add(other));
            }
        }
        return output;
    }

    @Override
    public void append(final Value v)
    {
        items.add(v);
    }

    @Override
    public Value subtract(final Value other)
    {
        final ListValue output = new ListValue();
        if (other instanceof final ListValue list)
        {
            final List<Value> otherItems = list.items;
            if (otherItems.size() == items.size())
            {
                for (int i = 0, size = items.size(); i < size; i++)
                {
                    output.items.add(items.get(i).subtract(otherItems.get(i)));
                }
            }
            else
            {
                throw new InternalExpressionException("Cannot subtract two lists of uneven sizes");
            }
        }
        else
        {
            for (final Value v : items)
            {
                output.items.add(v.subtract(other));
            }
        }
        return output;
    }

    public void subtractFrom(final Value v) // if I ever do -= then it wouod remove items
    {
        throw new UnsupportedOperationException(); // TODO
    }


    @Override
    public Value multiply(final Value other)
    {
        final ListValue output = new ListValue();
        if (other instanceof final ListValue list)
        {
            final List<Value> otherItems = list.items;
            if (otherItems.size() == items.size())
            {
                for (int i = 0, size = items.size(); i < size; i++)
                {
                    output.items.add(items.get(i).multiply(otherItems.get(i)));
                }
            }
            else
            {
                throw new InternalExpressionException("Cannot multiply two lists of uneven sizes");
            }
        }
        else
        {
            for (final Value v : items)
            {
                output.items.add(v.multiply(other));
            }
        }
        return output;
    }

    @Override
    public Value divide(final Value other)
    {
        final ListValue output = new ListValue();
        if (other instanceof final ListValue list)
        {
            final List<Value> otherItems = list.items;
            if (otherItems.size() == items.size())
            {
                for (int i = 0, size = items.size(); i < size; i++)
                {
                    output.items.add(items.get(i).divide(otherItems.get(i)));
                }
            }
            else
            {
                throw new InternalExpressionException("Cannot divide two lists of uneven sizes");
            }
        }
        else
        {
            for (final Value v : items)
            {
                output.items.add(v.divide(other));
            }
        }
        return output;
    }

    @Override
    public int compareTo(final Value o)
    {
        if (o instanceof final ListValue ol)
        {
            final int size = this.getItems().size();
            final int otherSize = ol.getItems().size();
            if (size != otherSize)
            {
                return size - otherSize;
            }
            if (size == 0)
            {
                return 0;
            }
            for (int i = 0; i < size; i++)
            {
                final int res = this.items.get(i).compareTo(ol.items.get(i));
                if (res != 0)
                {
                    return res;
                }
            }
            return 0;
        }
        return getString().compareTo(o.getString());
    }

    @Override
    public boolean equals(final Object o)
    {
        return o instanceof final ListValue list && getItems().equals(list.getItems());
    }

    public List<Value> getItems()
    {
        return items;
    }

    @Override
    public Iterator<Value> iterator()
    {
        return new ArrayList<>(items).iterator();
    } // should be thread safe

    @Override
    public List<Value> unpack()
    {
        return new ArrayList<>(items);
    }

    public void extend(final List<Value> subList)
    {
        items.addAll(subList);
    }

    /**
     * Finds a proper list index >=0 and < len that correspont to the rolling index value of idx
     *
     * @param idx
     * @param len
     * @return
     */
    public static int normalizeIndex(long idx, final int len)
    {
        if (idx >= 0 && idx < len)
        {
            return (int) idx;
        }
        final long range = abs(idx) / len;
        idx += (range + 2) * len;
        idx = idx % len;
        return (int) idx;
    }

    public static class ListConstructorValue extends ListValue
    {
        public ListConstructorValue(final Collection<? extends Value> list)
        {
            super(list);
        }
    }

    @Override
    public int length()
    {
        return items.size();
    }

    @Override
    public Value in(final Value value1)
    {
        for (int i = 0; i < items.size(); i++)
        {
            final Value v = items.get(i);
            if (v.equals(value1))
            {
                return new NumericValue(i);
            }
        }
        return Value.NULL;
    }

    @Override
    public Value slice(final long fromDesc, final Long toDesc)
    {
        final List<Value> items = getItems();
        final int size = items.size();
        final int from = normalizeIndex(fromDesc, size);
        if (toDesc == null)
        {
            return new ListValue(new ArrayList<>(getItems().subList(from, size)));
        }
        final int to = normalizeIndex(toDesc, size + 1);
        if (from > to)
        {
            return ListValue.of();
        }
        return new ListValue(new ArrayList<>(getItems().subList(from, to)));
    }

    @Override
    public Value split(final Value delimiter)
    {
        final ListValue result = new ListValue();
        if (delimiter == null)
        {
            this.forEach(item -> result.items.add(of(item)));
            return result;
        }
        int startIndex = 0;
        int index = 0;
        for (final Value val : this.items)
        {
            index++;
            if (val.equals(delimiter))
            {
                result.items.add(new ListValue(new ArrayList<>(this.items.subList(startIndex, index - 1))));
                startIndex = index;
            }
        }
        result.items.add(new ListValue(new ArrayList<>(this.items.subList(startIndex, length()))));
        return result;
    }

    @Override
    public double readDoubleNumber()
    {
        return items.size();
    }

    @Override
    public boolean put(final Value where, final Value value, final Value conditionValue)
    {
        final String condition = conditionValue.getString();
        if (condition.equalsIgnoreCase("insert"))
        {
            return put(where, value, false, false);
        }
        if (condition.equalsIgnoreCase("extend"))
        {
            return put(where, value, false, true);
        }
        if (condition.equalsIgnoreCase("replace"))
        {
            return put(where, value, true, false);
        }
        throw new InternalExpressionException("List 'put' modifier could be either 'insert', 'replace', or extend");
    }

    @Override
    public boolean put(final Value ind, final Value value)
    {
        return put(ind, value, true, false);
    }

    private boolean put(final Value ind, final Value value, final boolean replace, final boolean extend)
    {
        if (ind.isNull())
        {
            if (extend && value instanceof AbstractListValue)
            {
                ((AbstractListValue) value).iterator().forEachRemaining(items::add);
            }
            else
            {
                items.add(value);
            }
        }
        else
        {
            final int numitems = items.size();
            if (!(ind instanceof NumericValue))
            {
                return false;
            }
            int index = (int) ((NumericValue) ind).getLong();
            if (index < 0)
            {// only for values < 0
                index = normalizeIndex(index, numitems);
            }
            if (replace)
            {
                while (index >= items.size())
                {
                    items.add(Value.NULL);
                }
                items.set(index, value);
                return true;
            }
            while (index > items.size())
            {
                items.add(Value.NULL);
            }

            if (extend && value instanceof AbstractListValue)
            {
                final Iterable<Value> iterable = ((AbstractListValue) value)::iterator;
                final List<Value> appendix = StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
                items.addAll(index, appendix);
                return true;
            }
            items.add(index, value);
        }
        return true;
    }

    @Override
    public Value get(final Value value)
    {
        final int size = items.size();
        return size == 0 ? Value.NULL : items.get(normalizeIndex(NumericValue.asNumber(value, "'address' to a list index").getLong(), size));
    }

    @Override
    public boolean has(final Value where)
    {
        final long index = NumericValue.asNumber(where, "'address' to a list index").getLong();
        return index >= 0 && index < items.size();
    }

    @Override
    public boolean delete(final Value where)
    {
        if (!(where instanceof NumericValue) || items.isEmpty())
        {
            return false;
        }
        final long index = ((NumericValue) where).getLong();
        items.remove(normalizeIndex(index, items.size()));
        return true;
    }

    @Override
    public String getTypeString()
    {
        return "list";
    }

    @Override
    public int hashCode()
    {
        return items.hashCode();
    }

    private enum TagTypeCompat
    {
        INT,
        LONG,
        DBL,
        LIST,
        MAP,
        STRING;

        private static TagTypeCompat getType(final Tag tag)
        {
            if (tag instanceof IntTag)
            {
                return INT;
            }
            if (tag instanceof LongTag)
            {
                return LONG;
            }
            if (tag instanceof DoubleTag)
            {
                return DBL;
            }
            if (tag instanceof ListTag)
            {
                return LIST;
            }
            if (tag instanceof CompoundTag)
            {
                return MAP;
            }
            return STRING;
        }
    }


    @Override
    public Tag toTag(final boolean force)
    {
        final int argSize = items.size();
        if (argSize == 0)
        {
            return new ListTag();
        }
        final ListTag tag = new ListTag();
        if (argSize == 1)
        {
            tag.add(items.get(0).toTag(force));
            return tag;
        }
        // figuring out the types
        final List<Tag> tags = new ArrayList<>();
        items.forEach(v -> tags.add(v.toTag(force)));
        final Set<TagTypeCompat> cases = EnumSet.noneOf(TagTypeCompat.class);
        tags.forEach(t -> cases.add(TagTypeCompat.getType(t)));
        if (cases.size() == 1) // well, one type of items
        {
            tag.addAll(tags);
            return tag;
        }
        if (cases.contains(TagTypeCompat.LIST)
                || cases.contains(TagTypeCompat.MAP)
                || cases.contains(TagTypeCompat.STRING)) // incompatible types
        {
            if (!force)
            {
                throw new NBTSerializableValue.IncompatibleTypeException(this);
            }
            tags.forEach(t -> tag.add(StringTag.valueOf(t.getAsString())));
            return tag;
        }
        // only numbers / mixed types
        tags.forEach(cases.contains(TagTypeCompat.DBL)
                ? (t -> tag.add(DoubleTag.valueOf(((NumericTag) t).getAsDouble())))
                : (t -> tag.add(LongTag.valueOf(((NumericTag) t).getAsLong()))));
        return tag;
    }

    @Override
    public JsonElement toJson()
    {
        final JsonArray array = new JsonArray();
        for (final Value el : items)
        {
            array.add(el.toJson());
        }
        return array;
    }
}
