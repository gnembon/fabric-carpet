package carpet.script.exception;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer.Token;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

public class ThrowStatement extends InternalExpressionException
{
    private final Throwables thrownExceptionType;
    private final Value exceptionData;
    
    /**
     * Creates a throw exception from a value, and assigns it a specified message.
     * <p>To be used when throwing from Scarpet's {@code throw} function
     * @param data The value to pass
     * @param type Exception type
     */
    public ThrowStatement(Value data, Throwables type)
    {
        super(type.getId());
        exceptionData = data;
        thrownExceptionType = type;
    }

    /**
     * Creates a throw exception.<br>
     * Conveniently creates a value from the {@code value} String
     * to be used easily in Java code
     * @param message The message to display when not handled
     * @param thrownExceptionType An {@link Throwables} containing the inheritance data
     *                  for this exception. When throwing from Java,
     *                  those exceptions should be pre-registered.
     */
    public ThrowStatement(String message, Throwables thrownExceptionType)
    {
        super(message);
        this.exceptionData = StringValue.of(message);
        this.thrownExceptionType = thrownExceptionType;
    }
    
    @Override
    public ExpressionException promote(Context c, Expression e, Token token) {
        return new ProcessedThrowStatement(c, e, token, stack, thrownExceptionType, exceptionData);
    }
}
