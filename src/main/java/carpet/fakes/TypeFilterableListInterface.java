package carpet.fakes;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public interface TypeFilterableListInterface<T>
{
    public List<T> getAtIndices(IntList indices);
    public T getAtIndex(int index);
}
