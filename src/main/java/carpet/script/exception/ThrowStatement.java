package carpet.script.exception;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer.Token;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

public class ThrowStatement extends InternalExpressionException
{
    private final Throwables type;
    private final Value data;

    /**
     * Creates a throw exception from a value, and assigns it a specified message.
     * <p>To be used when throwing from Scarpet's {@code throw} function
     *
     * @param data The value to pass
     * @param type Exception type
     */
    public ThrowStatement(Value data, Throwables type)
    {
        super(type.getId());
        this.data = data;
        this.type = type;
    }

    public ThrowStatement(Value data, Throwables parent, String subtype)
    {
        super(subtype);
        this.data = data;
        this.type = new Throwables(subtype, parent);
    }

    /**
     * Creates a throw exception.<br>
     * Conveniently creates a value from the {@code value} String
     * to be used easily in Java code
     *
     * @param message The message to display when not handled
     * @param type    An {@link Throwables} containing the inheritance data
     *                for this exception. When throwing from Java,
     *                those exceptions should be pre-registered.
     */
    public ThrowStatement(String message, Throwables type)
    {
        super(type.getId());
        this.data = StringValue.of(message);
        this.type = type;
    }

    @Override
    public ExpressionException promote(Context c, Expression e, Token token)
    {
        return new ProcessedThrowStatement(c, e, token, stack, type, data);
    }
}
