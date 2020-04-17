package carpet.script.utils;

import carpet.script.value.FunctionValue;

public class ArgParser
{
    public static class FunctionArgument
    {
        public final FunctionValue function;
        public final int offset;
        FunctionArgument(FunctionValue f, int o)
        {
            function = f;
            offset = o;
        }
    }
}
