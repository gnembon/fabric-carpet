package carpet.script.value;

import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static carpet.script.value.NBTSerializableValue.nameFromRegistryId;
import static java.lang.Math.abs;

public class ListValue extends AbstractListValue implements ContainerValueInterface
{
    protected List<Value> items;
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
        items = new ArrayList<>();
        items.addAll(list);
    }

    private ListValue(List<Value> list)
    {
        items = list;
    }

    public static Value fromItemStack(ItemStack itemstack)
    {
        if (itemstack == null || itemstack.isEmpty())
            return Value.NULL;
        return ListValue.of(
                new StringValue(nameFromRegistryId(Registry.ITEM.getId(itemstack.getItem()))),
                new NumericValue(itemstack.getCount()),
                NBTSerializableValue.fromStack(itemstack)
        );
    }


    public static ListValue wrap(List<Value> list)
    {
        ListValue created = new ListValue();
        created.items = list;
        return created;
    }
    public static ListValue of(Value ... list)
    {
        return ListValue.wrap(Arrays.asList(list));
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
                throw new InternalExpressionException("Cannot subtract two lists of uneven sizes");
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
                throw new InternalExpressionException("Cannot subtract two lists of uneven sizes");
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
                throw new InternalExpressionException("Cannot subtract two lists of uneven sizes");
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

    public Iterator<Value> iterator() { return new ArrayList<>(items).iterator(); } // should be thread safe

    public void extend(List<Value> subList)
    {
        for (Value v: subList)
            items.add(v);
    }

    public void addAtIndex(int index, List<Value> subList)
    {
        int numitems = items.size();
        long range = abs(index)/numitems;
        index += (range+2)*numitems;
        index = index % numitems;
        for (Value v: subList)
        {
            if (index < numitems)
            {
                items.set(index, v);
            }
            else
            {
                items.add(v);
            }
            index++;
        }
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
    public Value slice(long from, long to)
    {
        List<Value> items = getItems();
        int size = items.size();
        if (to < 0 || to > size) to = size;
        if (from < 0 || from > size) from = size;
        if (from > to)
            return ListValue.of();
        return new ListValue((Collection<? extends Value>) getItems().subList((int)from, (int) to));
    }

    @Override
    public double readNumber()
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
        throw new  InternalExpressionException("List 'put' modifier could be either 'insert', or 'replace'");
    }

    @Override
    public boolean put(Value ind, Value value)
    {
        return put(ind, value, true, false);
    }

    private boolean put(Value ind, Value value, boolean replace, boolean extend)
    {
        if (ind == Value.NULL)
        {
            if (extend && value instanceof AbstractListValue)
            {
                ((AbstractListValue) value).iterator().forEachRemaining((v)-> items.add(v));
            }
            else
            {
                items.add(value);
            }
            return true;
        }
        else
        {
            int numitems = items.size();
            if (!(ind instanceof NumericValue))
                return false;
            int index = (int)((NumericValue) ind).getLong();
            if (index < 0)
            {
                long range = abs(index) / numitems;
                index += (range + 2) * numitems;
                index = index % numitems;
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
            return true;
        }
    }

    @Override
    public Value get(Value value)
    {
        long index = NumericValue.asNumber(value).getLong();
        int numitems = items.size();
        long range = abs(index)/numitems;
        index += (range+2)*numitems;
        index = index % numitems;
        return items.get((int)index);
    }

    @Override
    public boolean has(Value where)
    {
        long index = NumericValue.asNumber(where).getLong();
        return index >= 0 && index < items.size();
    }

    @Override
    public boolean delete(Value where)
    {
        if (!(where instanceof NumericValue) || items.isEmpty()) return false;
        long index = ((NumericValue) where).getLong();
        int numitems = items.size();
        long range = abs(index)/numitems;
        index += (range+2)*numitems;
        index = index % numitems;
        items.remove((int)index);
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
}
