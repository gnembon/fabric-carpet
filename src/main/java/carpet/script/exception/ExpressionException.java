package carpet.script.exception;

import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.Tokenizer;

import java.util.ArrayList;
import java.util.List;

import static carpet.script.ExpressionInspector.Expression_getExpressionSnippet;
import static carpet.script.ExpressionInspector.Expression_getName;

/* The expression evaluators exception class. */
public class ExpressionException extends RuntimeException
{
    public ExpressionException(String message)
    {
        super(message);
    }

    private static Fluff.TriFunction<Expression, Tokenizer.Token, String, List<String>> errorMaker = (expr, token, errmessage) ->
    {

        List<String> snippet = Expression_getExpressionSnippet(token, expr);
        List<String> errMsg = new ArrayList<>(snippet);
        if (snippet.size() != 1)
        {
            errmessage+= " at line "+(token.lineno+1)+", pos "+(token.linepos+1);
        }
        else
        {
            errmessage += " at pos "+(token.pos+1);
        }
        if (Expression_getName(expr) != null)
        {
            errmessage += " ("+Expression_getName(expr)+")";
        }
        errMsg.add(errmessage);
        return errMsg;
    };
    public static Fluff.TriFunction<Expression, Tokenizer.Token, String, List<String>> errorSnooper = null;

    static String makeMessage(Expression e, Tokenizer.Token t, String message) throws ExpressionException
    {
        if (errorSnooper != null)
        {
            List<String> alternative = errorSnooper.apply(e, t, message);
            if (alternative!= null)
            {
                return String.join("\n", alternative);
            }
        }
        return String.join("\n", errorMaker.apply(e, t, message));
    }

    public ExpressionException(Expression e, Tokenizer.Token t, String message)
    {
        super(makeMessage(e, t, message));
    }
}
