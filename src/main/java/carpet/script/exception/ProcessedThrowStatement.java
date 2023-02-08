package carpet.script.exception;

import java.util.List;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer.Token;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

public class ProcessedThrowStatement extends ExpressionException
{
    public final Throwables thrownExceptionType;
    public final Value data;

    public ProcessedThrowStatement(Context c, Expression e, Token token, List<FunctionValue> stack, Throwables thrownExceptionType, Value data)
    {
        super(c, e, token, () -> "Unhandled " + thrownExceptionType.getId() + " exception: " + data.getString(), stack);
        this.thrownExceptionType = thrownExceptionType;
        this.data = data;
    }
}
