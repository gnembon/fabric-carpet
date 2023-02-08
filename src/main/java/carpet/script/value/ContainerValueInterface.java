package carpet.script.value;

public interface ContainerValueInterface
{
    boolean put(Value where, Value value);

    default boolean put(Value where, Value value, Value conditions)
    {
        return put(where, value);
    }

    Value get(Value where);

    boolean has(Value where);

    boolean delete(Value where);
}
