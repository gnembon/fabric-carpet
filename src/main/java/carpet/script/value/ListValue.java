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
        return "["+items.stream().map(Value::getString).collect(Collectors.joining(", "))+"]";
    }

    @Override
    public String getPrettyString()
    {
        if (items.size()<8)
            return "["+items.stream().map(Value::getPrettyString).collect(Collectors.joining(", "))+"]";
        return "["+items.get(0).getPrettyString()+", "+items.get(1).getPrettyString()+", ..., "+
                items.get(items.size()-2).getPrettyString()+", "+items.get(items.size()-1).getPrettyString()+"]";
    }

    @Override
    public boolean getBoolean() {
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
        List<Value> copyItems = new ArrayList<>(items.size());
        for (Value entry: items) copyItems.add(entry.deepcopy());
        return new ListValue(copyItems);
    }

    public ListValue(Collection<? extends Value> list)
    {
        items = new ArrayList<>(list);
    }

    protected ListValue(List<Value> list)
    {
        items = list;
    }

    public static Value fromTriple(double a, double b, double c)
    {
        return ListValue.of(new NumericValue(a), new NumericValue(b), new NumericValue(c));
    }

    public static Value fromTriple(int a, int b, int c)
    {
        return fromTriple((double) a, b, c);
    }


    public static ListValue wrap(Stream<Value> stream)
    {
        return wrap(stream.collect(Collectors.toList()));
    }

    public static ListValue wrap(List<Value> list)
    {
        return new ListValue(list);
    }
    public static ListValue of(Value ... list)
    {
        return new ListValue(new ArrayList<>(Arrays.asList(list)));
    }
    public static ListValue ofNums(Number ... list)
    {
        List<Value> valList = new ArrayList<>();
        for (Number i : list)
            valList.add(new NumericValue(i.doubleValue()));
        return new ListValue(valList);
    }

    public static LazyValue lazyEmpty()
    {
        Value ret = new ListValue();
        return (c, t) -> ret;
    }

    private ListValue()
    {
        items = new ArrayList<>();
    }

    @Override
    public Value add(Value other) {
        ListValue output = new ListValue();
        if (other instanceof ListValue)
        {
            List<Value> other_list = ((ListValue) other).items;
            if (other_list.size() == items.size())
            {
                for(int i = 0, size = items.size(); i < size; i++)
                {
                    output.items.add(items.get(i).add(other_list.get(i)));
                }
            }
            else
            {
                throw new InternalExpressionException("Cannot add two lists of uneven sizes");
            }
        }
        else
        {
            for (Value v : items)
            {
                output.items.add(v.add(other));
            }
        }
        return output;
    }
    @Override
    public void append(Value v)
    {
        items.add(v);
    }

    @Override
    public Value subtract(Value other)
    {
        ListValue output = new ListValue();
        if (other instanceof ListValue)
        {
            List<Value> other_list = ((ListValue) other).items;
            if (other_list.size() == items.size())
            {
                for(int i = 0, size = items.size(); i < size; i++)
                {
                    output.items.add(items.get(i).subtract(other_list.get(i)));
                }
            }
            else
            {
                throw new InternalExpressionException("Cannot subtract two lists of uneven sizes");
            }
        }
        else
        {
            for (Value v : items)
            {
                output.items.add(v.subtract(other));
            }
        }
        return output;
    }
    public void subtractFrom(Value v) // if I ever do -= then it wouod remove items
    {
        throw new UnsupportedOperationException(); // TODO
    }


    @Override
    public Value multiply(Value other)
    {
        ListValue output = new ListValue();
        if (other instanceof ListValue)
        {
            List<Value> other_list = ((ListValue) other).items;
            if (other_list.size() == items.size())
            {
                for(int i = 0, size = items.size(); i < size; i++)
                {
                    output.items.add(items.get(i).multiply(other_list.get(i)));
                }
            }
            else
            {
                throw new InternalExpressionException("Cannot multiply two lists of uneven sizes");
            }
        }
        else
        {
            for (Value v : items)
            {
                output.items.add(v.multiply(other));
            }
        }
        return output;
    }

    @Override
    public Value divide(Value other)
    {
        ListValue output = new ListValue();
        if (other instanceof ListValue)
        {
            List<Value> other_list = ((ListValue) other).items;
            if (other_list.size() == items.size())
            {
                for(int i = 0, size = items.size(); i < size; i++)
                {
                    output.items.add(items.get(i).divide(other_list.get(i)));
                }
            }
            else
            {
                throw new InternalExpressionException("Cannot divide two lists of uneven sizes");
            }
        }
        else
        {
            for (Value v : items)
            {
                output.items.add(v.divide(other));
            }
        }
        return output;
    }

    @Override
    public int compareTo(Value o)
    {
        if (o instanceof ListValue)
        {
            ListValue ol = (ListValue)o;
            int this_size = this.getItems().size();
            int o_size = ol.getItems().size();
            if (this_size != o_size) return this_size - o_size;
            if (this_size == 0) return 0;
            for (int i = 0; i < this_size; i++)
            {
                int res = this.items.get(i).compareTo(ol.items.get(i));
                if (res != 0) return res;
            }
            return 0;
        }
        return getString().compareTo(o.getString());
    }

    @Override
    public boolean equals(final Object o)
    {
        if (o instanceof ListValue)
        {
            return getItems().equals(((ListValue) o).getItems());
        }
        return false;
    }

    public List<Value> getItems()
    {
        return items;
    }

    @Override
    public Iterator<Value> iterator() { return new ArrayList<>(items).iterator(); } // should be thread safe

    @Override
    public List<Value> unpack()
    {
        return new ArrayList<>(items);
    }

    public void extend(List<Value> subList)
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
    public static int normalizeIndex(long idx, int len)
    {
        if (idx >=0 && idx < len) return (int)idx;
        long range = abs(idx)/len;
        idx += (range+2)*len;
        idx = idx % len;
        return (int)idx;
    }

    public static class ListConstructorValue extends ListValue
    {
        public ListConstructorValue(Collection<? extends Value> list)
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
    public Value in(Value value1)
    {
        for (int i = 0; i < items.size(); i++)
        {
            Value v = items.get(i);
            if (v.equals(value1))
            {
                return new NumericValue(i);
            }
        }
        return Value.NULL;
    }

    @Override
    public Value slice(long fromDesc, Long toDesc)
    {
        List<Value> items = getItems();
        int size = items.size();
        int from = normalizeIndex(fromDesc, size);
        if (toDesc == null)
            return new ListValue(new ArrayList<>(getItems().subList(from, size)));
        int to = normalizeIndex(toDesc, size+1);
        if (from > to)
            return ListValue.of();
        return new ListValue(new ArrayList<>(getItems().subList(from, to)));
    }

    @Override
    public Value split(Value delimiter) {
        ListValue result = new ListValue();
        if (delimiter == null)
        {
            this.forEach(item -> result.items.add(of(item)));
            return result;
        }
        int startIndex = 0;
        int index = 0;
        for (Value val : this.items)
        {
            index++;
            if (val.equals(delimiter))
            {
                result.items.add(new ListValue(new ArrayList<>(this.items.subList(startIndex, index-1))));
                startIndex = index;
            }
        }
        result.items.add(new ListValue(new ArrayList<>(this.items.subList(startIndex, length()))));
        return result;
    }

    @Override
    public double readDoubleNumber()
    {
        return (double)items.size();
    }

    @Override
    public boolean put(Value where, Value value, Value conditionValue)
    {
        String condition = conditionValue.getString();
        if (condition.equalsIgnoreCase("insert"))
            return put(where, value, false, false);
        if (condition.equalsIgnoreCase("extend"))
            return put(where, value, false, true);
        if (condition.equalsIgnoreCase("replace"))
            return put(where, value, true, false);
        throw new  InternalExpressionException("List 'put' modifier could be either 'insert', 'replace', or extend");
    }

    @Override
    public boolean put(Value ind, Value value)
    {
        return put(ind, value, true, false);
    }

    private boolean put(Value ind, Value value, boolean replace, boolean extend)
    {
        if (ind.isNull())
        {
            if (extend && value instanceof AbstractListValue)
            {
                ((AbstractListValue) value).iterator().forEachRemaining((v)-> items.add(v));
            }
            else
            {
                items.add(value);
            }
        }
        else
        {
            int numitems = items.size();
            if (!(ind instanceof NumericValue))
                return false;
            int index = (int)((NumericValue) ind).getLong();
            if (index < 0)
            {// only for values < 0
                index = normalizeIndex(index, numitems);
            }
            if (replace)
            {
                while (index >= items.size()) items.add(Value.NULL);
                items.set(index, value);
                return true;
            }
            while (index > items.size()) items.add(Value.NULL);

            if (extend && value instanceof AbstractListValue)
            {
                Iterable<Value> iterable = ((AbstractListValue) value)::iterator;
                List<Value> appendix = StreamSupport.stream( iterable.spliterator(), false).collect(Collectors.toList());
                items.addAll(index, appendix );
                return true;
            }
            items.add(index, value);
        }
        return true;
    }

    @Override
    public Value get(Value value)
    {
        int size = items.size();
        if (size == 0) return Value.NULL;
        return items.get(normalizeIndex(NumericValue.asNumber(value, "'address' to a list index").getLong(), size));
    }

    @Override
    public boolean has(Value where)
    {
        long index = NumericValue.asNumber(where, "'address' to a list index").getLong();
        return index >= 0 && index < items.size();
    }

    @Override
    public boolean delete(Value where)
    {
        if (!(where instanceof NumericValue) || items.isEmpty()) return false;
        long index = ((NumericValue) where).getLong();
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
        private static TagTypeCompat getType(Tag tag)
        {
            if (tag instanceof IntTag) return INT;
            if (tag instanceof LongTag) return LONG;
            if (tag instanceof DoubleTag) return DBL;
            if (tag instanceof ListTag) return LIST;
            if (tag instanceof CompoundTag) return MAP;
            return STRING;
        }
    }


    @Override
    public Tag toTag(boolean force)
    {
        int argSize = items.size();
        if (argSize == 0) return new ListTag();
        ListTag tag = new ListTag();
        if (argSize ==1)
        {
            tag.add(items.get(0).toTag(force));
            return tag;
        }
        // figuring out the types
        List<Tag> tags= new ArrayList<>();
        items.forEach(v -> tags.add(v.toTag(force)));
        Set<TagTypeCompat> cases = EnumSet.noneOf(TagTypeCompat.class);
        tags.forEach(t -> cases.add(TagTypeCompat.getType(t)));
        if (cases.size()==1) // well, one type of items
        {
            tag.addAll(tags);
            return tag;
        }
        if (cases.contains(TagTypeCompat.LIST)
                || cases.contains(TagTypeCompat.MAP)
                || cases.contains(TagTypeCompat.STRING)) // incompatible types
        {
            if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
            tags.forEach(t -> tag.add(StringTag.valueOf(t.getAsString())));
            return tag;
        }
        // only numbers / mixed types
        if (cases.contains(TagTypeCompat.DBL))
        {
            tags.forEach(t -> tag.add(DoubleTag.valueOf(((NumericTag)t).getAsDouble())));
        }
        else
        {
            tags.forEach(t -> tag.add(LongTag.valueOf(((NumericTag)t).getAsLong())));
        }
        return tag;
    }

    @Override
    public JsonElement toJson()
    {
        JsonArray array = new JsonArray();
        for (Value el: items)
            array.add(el.toJson());
        return array;
    }
}
