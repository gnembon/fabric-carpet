package carpet.script.exception;

/**
 * A type of {@link RuntimeException} that doesn't spend time producing and filling a stacktrace
 */
public abstract class StacklessRuntimeException extends RuntimeException
{
    public StacklessRuntimeException()
    {
        super();
    }

    public StacklessRuntimeException(String message)
    {
        super(message);
    }

    @Override
    public Throwable fillInStackTrace()
    {
        return this;
    }
}
