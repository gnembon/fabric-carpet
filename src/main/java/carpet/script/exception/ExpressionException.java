package carpet.script.exception;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.Tokenizer;
import carpet.script.value.FunctionValue;

import java.util.ArrayList;
import java.util.List;

import static carpet.script.ExpressionInspector.Expression_getExpressionSnippet;
import static carpet.script.ExpressionInspector.Expression_getModuleName;

/* The expression evaluators exception class. */
public class ExpressionException extends RuntimeException implements ResolvedException
{
    public final Context context;
    public final List<FunctionValue> stack = new ArrayList<>();

    public ExpressionException(Context c, Expression e, String message)
    {
        super(makeMessage(c, e, null, message));
        context = c;
    }

    public ExpressionException(Context c, Expression e, Tokenizer.Token t, String message)
    {
        super(makeMessage(c, e, t, message));
        context = c;
    }
    public ExpressionException(Context c, Expression e, Tokenizer.Token t, String message, List<FunctionValue> stack)
    {
        super(makeMessage(c, e, t, message));
        this.stack.addAll(stack);
        context = c;
    }

    private static final Fluff.TriFunction<Expression, Tokenizer.Token, String, List<String>> errorMaker = (expr, /*Nullable*/ token, errmessage) ->
    {

        List<String> errMsg = new ArrayList<>();
        errmessage += Expression_getModuleName(expr) == null?"":(" in "+Expression_getModuleName(expr));
        if (token != null)
        {
            List<String> snippet = Expression_getExpressionSnippet(token, expr);
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
    };

    static String makeMessage(Context c, Expression e, Tokenizer.Token t, String message) throws ExpressionException
    {
        if (c.host.errorSnooper != null)
        {
            List<String> alternative = c.host.errorSnooper.apply(e, t, message);
            if (alternative!= null)
            {
                return String.join("\n", alternative);
            }
        }
        return String.join("\n", errorMaker.apply(e, t, message));
    }
}
