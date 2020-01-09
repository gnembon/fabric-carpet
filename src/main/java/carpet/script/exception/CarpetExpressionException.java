package carpet.script.exception;

import carpet.script.value.FunctionValue;
import carpet.utils.Messenger;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public class CarpetExpressionException extends RuntimeException implements ResolvedException
{
    public final List<FunctionValue> stack;

    public CarpetExpressionException(String message, List<FunctionValue> stack)
    {
        super(message);
        this.stack = stack;
    }
    public void printStack(ServerCommandSource source)
    {
        if (stack != null && !stack.isEmpty())
        {
            for (FunctionValue fun : stack)
            {
                Messenger.m(source, "e  ... in "+fun.fullName(), "e /"+(fun.getToken().lineno+1)+":"+(fun.getToken().linepos+1));
            }
        }
    }
}
