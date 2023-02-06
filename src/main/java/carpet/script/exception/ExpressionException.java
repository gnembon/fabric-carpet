package carpet.script.exception;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer;
import carpet.script.external.Carpet;
import carpet.script.value.FunctionValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/* The expression evaluators exception class. */
public class ExpressionException extends StacklessRuntimeException implements ResolvedException
{
    public final Context context;
    public final Tokenizer.Token token;
    public final List<FunctionValue> stack = new ArrayList<>();
    private final Supplier<String> lazyMessage;
    private String cachedMessage = null;

    public static void prepareForDoom()
    {
        Carpet.Messenger_compose("foo bar");
    }

    public ExpressionException(final Context c, final Expression e, final String message)
    {
        this(c, e, Tokenizer.Token.NONE, message);
    }

    public ExpressionException(final Context c, final Expression e, final Tokenizer.Token t, final String message)
    {
        this(c, e, t, message, Collections.emptyList());
    }

    public ExpressionException(final Context c, final Expression e, final Tokenizer.Token t, final String message, final List<FunctionValue> stack)
    {
        super("Error");
        this.stack.addAll(stack);
        lazyMessage = () -> makeMessage(c, e, t, message);
        token = t;
        context = c;
    }

    public ExpressionException(final Context c, final Expression e, final Tokenizer.Token t, final Supplier<String> messageSupplier, final List<FunctionValue> stack)
    {
        super("Error");
        this.stack.addAll(stack);
        lazyMessage = () -> makeMessage(c, e, t, messageSupplier.get());
        token = t;
        context = c;
    }

    private static List<String> makeError(final Expression expr, /*Nullable*/final Tokenizer.Token token, String errmessage)
    {
        final List<String> errMsg = new ArrayList<>();
        errmessage += expr.getModuleName() == null ? "" : (" in " + expr.getModuleName());
        if (token != null)
        {
            final List<String> snippet = expr.getExpressionSnippet(token);
            errMsg.addAll(snippet);

            if (snippet.size() != 1)
            {
                errmessage += " at line " + (token.lineno + 1) + ", pos " + (token.linepos + 1);
            }
            else
            {
                errmessage += " at pos " + (token.pos + 1);
            }
        }
        errMsg.add(errmessage);
        return errMsg;
    }

    static synchronized String makeMessage(final Context c, final Expression e, final Tokenizer.Token t, final String message) throws ExpressionException
    {
        if (c.getErrorSnooper() != null)
        {
            final List<String> alternative = c.getErrorSnooper().apply(e, t, c, message);
            if (alternative != null)
            {
                return String.join("\n", alternative);
            }
        }
        return String.join("\n", makeError(e, t, message));
    }

    @Override
    public String getMessage()
    {
        if (cachedMessage == null)
        {
            cachedMessage = lazyMessage.get();
        }
        return cachedMessage;
    }
}
