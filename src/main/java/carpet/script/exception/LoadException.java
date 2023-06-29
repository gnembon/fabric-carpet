package carpet.script.exception;

/**
 * A Scarpet exception that indicates that load of the app has failed.
 * <p>
 * Goes up the stack to the point of app load and gets caught there, preventing the app from loading with
 * the given message.
 */
public class LoadException extends RuntimeException implements ResolvedException
{
    public LoadException()
    {
        super();
    }
    public LoadException(String message)
    {
        super(message);
    }
}
