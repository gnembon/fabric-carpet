package carpet.script;

import java.util.List;

/**
 * sole purpose of this package is to provide public access to package-private methods of Expression and CarpetExpression
 * classes so they don't leave garbage in javadocs
 */
public class ExpressionInspector
{
    public static List<String> Expression_getExpressionSnippet(Tokenizer.Token token, Expression expr)
    {
        return Expression.getExpressionSnippet(token, expr);
    }

    public static String Expression_getName(Expression e)
    {
        return e.getName();
    }
}
