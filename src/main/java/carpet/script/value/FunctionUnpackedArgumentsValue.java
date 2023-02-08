package carpet.script.value;

import java.util.List;

public class FunctionUnpackedArgumentsValue extends ListValue
{
    public FunctionUnpackedArgumentsValue(List<Value> list)
    {
        super(list);
    }

    @Override
    public Value clone()
    {
        return new FunctionUnpackedArgumentsValue(items);
    }

    @Override
    public Value deepcopy()
    {
        return new FunctionUnpackedArgumentsValue(((ListValue) super.deepcopy()).items);
    }
}
