package carpet.script.exception;

import java.util.HashMap;
import java.util.Map;

import carpet.script.value.NullValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

/**
 * This class contains default Scarpet catchable exception's ids, as well as their inheritance and 
 * methods to check whether filters are compatible with those.
 * 
 * Exceptions here cannot be thrown as an actual {@link Throwable}, they are just part of the metadata for
 * {@link ThrowStatement} and {@link ProcessedThrowStatement}
 */
public class Throwables {
    protected static final Map<Value, Exception> byValue = new HashMap<>();
    public static final Exception EXCEPTION               = register("exception", null);
    public static final Exception   VALUE_EXCEPTION       = register("value_exception", EXCEPTION);
    public static final Exception     UNKNOWN_ITEM        = register("unknown_item", VALUE_EXCEPTION);
    public static final Exception     UNKNOWN_BLOCK       = register("unknown_block", VALUE_EXCEPTION);
    public static final Exception     UNKNOWN_BIOME       = register("unknown_biome", VALUE_EXCEPTION);
    public static final Exception     UNKNOWN_SOUND       = register("unknown_sound", VALUE_EXCEPTION);
    public static final Exception     UNKNOWN_PARTICLE    = register("unknown_particle", VALUE_EXCEPTION);
    public static final Exception     UNKNOWN_POI_TYPE    = register("unknown_poi_type", VALUE_EXCEPTION);
    public static final Exception     UNKNOWN_DIMENSION   = register("unknown_dimension", VALUE_EXCEPTION);
    public static final Exception     UNKNOWN_STRUCTURE   = register("unknown_structure", VALUE_EXCEPTION);
    public static final Exception     UNKNOWN_CRITERION   = register("unknown_criterion", VALUE_EXCEPTION);
    public static final Exception   FILE_READ_EXCEPTION   = register("file_read_exception", EXCEPTION);
    public static final Exception     NBT_READ            = register("nbt_read_exception", FILE_READ_EXCEPTION);
    public static final Exception     JSON_READ           = register("json_read_exception", FILE_READ_EXCEPTION);
    public static final Exception   USER_DEFINED          = register("user_defined_exception", EXCEPTION);
    protected static final Exception ERROR                = register("error", null);
    
    private Throwables() {}
    
    /**
     * Creates an exception and registers it to be used as parent for
     * user defined exceptions in Scarpet's throw function.
     * <p>Scarpet exceptions should, in general, have a top-level parent being {@link Throwables#EXCEPTION} 
     * @param value The value for the exception as a {@link String}. Will be converted into a {@link StringValue}
     * @param parent The parent of the exception being created, or <code>null</code> if top-level
     * @return The created exception
     */
    public static Exception register(String value, Exception parent)
    {
        Exception exc = new Exception(value, parent);
        byValue.put(exc.getValue(), exc);
        return exc;
    }
    
    /**
     * The data of a {@link ThrowStatement} exception. Supports creating with inheritance and
     * checking whether a filter is compatible with them.
     */
    public static class Exception {
        private final Value value;
        private final Exception parent;
        private final boolean isError;
        
        /**
         * Creates a new exception, with an optional parent.
         * <p>To be used in Scarpet's throw command
         * <p>Parent will default to {@link Throwables#USER_DEFINED} if provided
         * parentValue meets Value#isNull, else it will create a new {@link Exception},
         * with its parent being {@link Throwables#USER_DEFINED}.
         * <p>If passed parent matches {@link Throwables#ERROR}, it will create an error
         * exception, which cannot be caught.
         * @param value The {@link Value} of the exception
         * @param parentValue An optional {@link Value} matching the name of a parent exception.
         *               <br>Accepts a {@link NullValue}, but not a {@code null}
         */
        public Exception(Value value, Value parentValue)
        {
            if (byValue.containsKey(value))
                throw new InternalExpressionException("Exception value can't be the same as existing exception. Use it as parent instead");
            this.value = value;
            if (!parentValue.isNull())
            {
                Exception possibleParent = byValue.get(parentValue);
                parent = possibleParent == null ? new Exception(parentValue, Value.NULL) : possibleParent;
            } else
                parent = USER_DEFINED;
            isError = this.parent == ERROR;
        }
        
        /**
         * Creates a new exception.
         * <p>Not suitable for creating exceptions that can't be caught.
         * Use an {@link InternalExpressionException} for that
         * @param value The exception's value as a {@link String}
         * @param parent The parent exception, or <code>null</code> if no parent is available
         */
        protected Exception(String value, Exception parent)
        {
            this.value = StringValue.of(value);
            this.parent = parent;
            this.isError = false;
        }
        
        /**
         * Checks whether the given filter matches an instance of this exception, by checking equality
         * with itself and possible parents.
         * @param filter The {@link Value} to check against
         * @return Whether or not the given value matches this exception's hierarchy
         */
        public boolean isInstance(Value filter) {
            return getValue().equals(filter) || (parent != null && parent.isInstance(filter));
        }
        
        /**
         * Returns the {@link Value} provided when creating this exception
         * @return The {@link Value} of this exception
         */
        public Value getValue() {
            return value;
        }
        
        /**
         * Returns whether this exception has been defined as an error.
         * <p> Error exceptions cannot be caught in Scarpet
         * @return Whether this exception has been defined as an error
         */
        public boolean isError() {
            return isError;
        }
    }
}
