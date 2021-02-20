package carpet.script.exception;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer.Token;
import carpet.script.value.Value;

import static carpet.script.exception.Throwables.Exception;

public class ThrowStatement extends InternalExpressionException
{
    private final Exception exception;
    /**
     * Creates a throw exception from a value.
     * That value will also be used as the message.<br>
     * To be used when throwing from Scarpet's {@code throw} function with a single argument
     * @param value The value to pass
     */
    public ThrowStatement(Value value)
    {
        this(value, value, Value.NULL);
    }
    
    /**
     * Creates a throw exception from a value, and assigns it a specified message.
     * <p>To be used when throwing from Scarpet's {@code throw} function
     * @param message The message to display if uncaught
     * @param value The value to pass
     * @param parent A parent's name
     */
    public ThrowStatement(Value message, Value value, Value parent)
    {
        super(message.getString());
        exception = new Exception(value, parent);
    }

    /**
     * Creates a throw exception.<br>
     * Conveniently creates a value from the {@code value} String
     * to be used easily in Java code
     * @param message The message to display when not handled
     * @param value A String that will be converted 
     *              to a value to pass to {@code catch}
     *              blocks
     * @param parent This exception's data
     */
    public ThrowStatement(String message, Exception exception)
    {
        super(message);
        this.exception = exception;
    }
    
    @Override
    public ExpressionException promote(Context c, Expression e, Token token) {
        return new ProcessedThrowStatement(c, e, token, getMessage(), stack, exception);
    }
}
