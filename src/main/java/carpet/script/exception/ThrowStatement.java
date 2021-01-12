package carpet.script.exception;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer.Token;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

public class ThrowStatement extends InternalExpressionException
{
    public static final String UNKNOWN_ITEM        = "unknown_item";
    public static final String UNKNOWN_BLOCK       = "unknown_block";
    public static final String UNKNOWN_BIOME       = "unknown_biome";
    public static final String UNKNOWN_SOUND       = "unknown_sound";
    public static final String UNKNOWN_PARTICLE    = "unknown_particle";
    public static final String UNKNOWN_POI_TYPE    = "unknown_poi_type";
    public static final String UNKNOWN_DIMENSION   = "unknown_dimension";
    public static final String UNKNOWN_STRUCTURE   = "unknown_structure";
    public static final String UNKNOWN_CRITERION   = "unknown_criterion";
    public static final String NBT_READ_EXCEPTION  = "nbt_read_exception";
    public static final String JSON_READ_EXCEPTION = "json_read_exception";
    public final Value retval;
    /**
     * Creates a throw exception from a value.
     * That value will also be used as the message.<br>
     * To use when throwing from Scarpet's {@code throw}
     * @param value The value to pass
     */
    public ThrowStatement(Value value)
    {
        super(value.getString());
        retval = value;
    }
    
    /**
     * Creates a throw exception from a value, and 
     * assigns it a specified message.<br>
     * To use when throwing from Scarpet's {@code throw}
     * function
     * @param message The message to display if uncaught
     * @param value The value to pass
     */
    public ThrowStatement(String message, Value value)
    {
        super(message);
        retval = value;
    }

    /**
     * Creates a throw exception.<br>
     * Conveniently creates a value from the {@code value} String
     * to be used easily in Java code
     * @param message The message to display when not handled
     * @param value A String that will be converted 
     *              to a value to pass to {@code catch}
     *              blocks
     */
    public ThrowStatement(String message, String value)
    {
        super(message);
        retval = new StringValue(value);
    }
    
    @Override
    public ExpressionException promote(Context c, Expression e, Token token) {
        return new ProcessedThrowStatement(c, e, token, getMessage(), stack, retval);
    }
}
