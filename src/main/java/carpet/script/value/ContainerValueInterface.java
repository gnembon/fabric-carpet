package carpet.script.value;

public interface ContainerValueInterface
{
    boolean put(Value where, Value value);

    default boolean put(final Value where, final Value value, final Value conditions)
    {
        return put(where, value);
    }

    Value get(Value where);

    boolean has(Value where);

    boolean delete(Value where);
}
