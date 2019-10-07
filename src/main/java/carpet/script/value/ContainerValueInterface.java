package carpet.script.value;

public interface ContainerValueInterface
{
    public Value put(Value where, Value value);
    default Value put(Value where, Value value, Value conditions)
    {
        return put(where, value);
    }
    public Value get(Value where);
    public boolean has(Value where);
    public Value delete(Value where);
}
