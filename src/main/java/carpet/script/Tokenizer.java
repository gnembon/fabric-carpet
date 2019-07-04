package carpet.script;

import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Expression tokenizer that allows to iterate over a {@link String}
 * expression token by token. Blank characters will be skipped.
 */
public class Tokenizer implements Iterator<Tokenizer.Token>
{
    /** What character to use for decimal separators. */
    private static final char decimalSeparator = '.';
    /** What character to use for minus sign (negative values). */
    private static final char minusSign = '-';
    /** Actual position in expression string. */
    private int pos = 0;
    private int lineno = 0;
    private int linepos = 0;
    private boolean comments;
    private boolean newLinesMarkers;
    /** The original input expression. */
    private String input;
    /** The previous token or <code>null</code> if none. */
    private Token previousToken;

    private Expression expression;

    Tokenizer(Expression expr, String input, boolean allowComments, boolean allowNewLineMakers)
    {
        this.input = input;
        this.expression = expr;
        this.comments = allowComments;
        this.newLinesMarkers = allowNewLineMakers;
    }

    @Override
    public boolean hasNext()
    {
        return (pos < input.length());
    }

    /**
     * Peek at the next character, without advancing the iterator.
     *
     * @return The next character or character 0, if at end of string.
     */
    private char peekNextChar()
    {
        return (pos < (input.length() - 1)) ? input.charAt(pos + 1) : 0;
    }

    private boolean isHexDigit(char ch)
    {
        return ch == 'x' || ch == 'X' || (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f')
                || (ch >= 'A' && ch <= 'F');
    }

    public static List<Token> simplepass(String input)
    {
        Tokenizer tok = new Tokenizer(null, input, false, false);
        List<Token> res = new ArrayList<>();
        while (tok.hasNext()) res.add(tok.next());
        return res;
    }

    @Override
    public Token next()
    {
        Token token = new Token();

        if (pos >= input.length())
        {
            return previousToken = null;
        }
        char ch = input.charAt(pos);
        while (Character.isWhitespace(ch) && pos < input.length())
        {
            linepos++;
            if (ch=='\n')
            {
                lineno++;
                linepos = 0;
            }
            ch = input.charAt(++pos);
        }
        token.pos = pos;
        token.lineno = lineno;
        token.linepos = linepos;

        boolean isHex = false;

        if (Character.isDigit(ch) || (ch == decimalSeparator && Character.isDigit(peekNextChar())))
        {
            if (ch == '0' && (peekNextChar() == 'x' || peekNextChar() == 'X'))
                isHex = true;
            while ((isHex
                    && isHexDigit(
                    ch))
                    || (Character.isDigit(ch) || ch == decimalSeparator || ch == 'e' || ch == 'E'
                    || (ch == minusSign && token.length() > 0
                    && ('e' == token.charAt(token.length() - 1)
                    || 'E' == token.charAt(token.length() - 1)))
                    || (ch == '+' && token.length() > 0
                    && ('e' == token.charAt(token.length() - 1)
                    || 'E' == token.charAt(token.length() - 1))))
                    && (pos < input.length()))
            {
                token.append(input.charAt(pos++));
                linepos++;
                ch = pos == input.length() ? 0 : input.charAt(pos);
            }
            token.type = isHex ? Token.TokenType.HEX_LITERAL : Token.TokenType.LITERAL;
        }
        else if (ch == '\'')
        {
            pos++;
            linepos++;
            if (pos == input.length())
            {
                token.type = Token.TokenType.STRINGPARAM;
                if (expression != null)
                    throw new ExpressionException(this.expression, token, "Program truncated");
            }
            ch = input.charAt(pos);
            while (ch != '\'')
            {
                if (ch == '\\')
                {
                    pos++;
                    linepos++;
                    if (pos == input.length())
                    {
                        token.type = Token.TokenType.STRINGPARAM;
                        if (expression != null)
                            throw new ExpressionException(this.expression, token, "Program truncated");
                    }
                }
                token.append(input.charAt(pos++));
                linepos++;
                ch = pos == input.length() ? 0 : input.charAt(pos);
            }
            pos++;
            linepos++;
            token.type = Token.TokenType.STRINGPARAM;
        }
        else if (Character.isLetter(ch) || "_".indexOf(ch) >= 0)
        {
            while ((Character.isLetter(ch) || Character.isDigit(ch) || "_".indexOf(ch) >= 0
                    || token.length() == 0 && "_".indexOf(ch) >= 0) && (pos < input.length()))
            {
                token.append(input.charAt(pos++));
                linepos++;
                ch = pos == input.length() ? 0 : input.charAt(pos);
            }
            // Remove optional white spaces after function or variable name
            if (Character.isWhitespace(ch))
            {
                while (Character.isWhitespace(ch) && pos < input.length())
                {
                    ch = input.charAt(pos++);
                    linepos++;
                    if (ch=='\n')
                    {
                        lineno++;
                        linepos = 0;
                    }
                }
                pos--;
                linepos--;
            }
            token.type = ch == '(' ? Token.TokenType.FUNCTION : Token.TokenType.VARIABLE;
        }
        else if (ch == '(' || ch == ')' || ch == ',')
        {
            if (ch == '(')
            {
                token.type = Token.TokenType.OPEN_PAREN;
            }
            else if (ch == ')')
            {
                token.type = Token.TokenType.CLOSE_PAREN;
            }
            else
            {
                token.type = Token.TokenType.COMMA;
            }
            token.append(ch);
            pos++;
            linepos++;
            if (expression != null && previousToken != null && previousToken.type == Token.TokenType.OPERATOR && (ch == ')' || ch == ',')  )
            {
                if (previousToken.surface.equalsIgnoreCase(";"))
                    throw new ExpressionException(this.expression, previousToken,
                            "Cannot have semicolon at the end of the expression");
                throw new ExpressionException(this.expression, previousToken,
                        "Can't have operator "+previousToken.surface+" at the end of a subexpression");
            }
        }
        else
        {
            String greedyMatch = "";
            int initialPos = pos;
            int initialLinePos = linepos;
            ch = input.charAt(pos);
            int validOperatorSeenUntil = -1;
            while (!Character.isLetter(ch) && !Character.isDigit(ch) && "_".indexOf(ch) < 0
                    && !Character.isWhitespace(ch) && ch != '(' && ch != ')' && ch != ','
                    && (pos < input.length()))
            {
                greedyMatch += ch;
                if (comments && "//".equals(greedyMatch))
                {

                    while ( ch != '\n' && pos < input.length())
                    {
                        ch = input.charAt(pos++);
                        linepos++;
                        greedyMatch += ch;
                    }
                    if (ch=='\n')
                    {
                        lineno++;
                        linepos = 0;
                    }
                    token.append(greedyMatch);
                    token.type = Token.TokenType.MARKER;
                    return token; // skipping setting previous
                }
                pos++;
                linepos++;
                if (Expression.none.isAnOperator(greedyMatch))
                {
                    validOperatorSeenUntil = pos;
                }
                ch = pos == input.length() ? 0 : input.charAt(pos);
            }
            if (newLinesMarkers && "$".equals(greedyMatch))
            {
                lineno++;
                linepos = 0;
                token.type = Token.TokenType.MARKER;
                token.append('$');
                return token; // skipping previous token lookback
            }
            if (validOperatorSeenUntil != -1)
            {
                token.append(input.substring(initialPos, validOperatorSeenUntil));
                pos = validOperatorSeenUntil;
                linepos = initialLinePos+validOperatorSeenUntil-initialPos;
            }
            else
            {
                token.append(greedyMatch);
            }

            if (previousToken == null || previousToken.type == Token.TokenType.OPERATOR
                    || previousToken.type == Token.TokenType.OPEN_PAREN || previousToken.type == Token.TokenType.COMMA)
            {
                token.surface += "u";
                token.type = Token.TokenType.UNARY_OPERATOR;
            }
            else
            {
                token.type = Token.TokenType.OPERATOR;
            }
        }
        if (expression != null && previousToken != null &&
            (
                token.type == Token.TokenType.LITERAL ||
                token.type == Token.TokenType.HEX_LITERAL ||
                token.type == Token.TokenType.VARIABLE ||
                token.type == Token.TokenType.STRINGPARAM ||
                token.type == Token.TokenType.FUNCTION
            ) &&(
                previousToken.type == Token.TokenType.VARIABLE ||
                previousToken.type == Token.TokenType.FUNCTION ||
                previousToken.type == Token.TokenType.LITERAL ||
                previousToken.type == Token.TokenType.CLOSE_PAREN ||
                previousToken.type == Token.TokenType.HEX_LITERAL ||
                previousToken.type == Token.TokenType.STRINGPARAM
            )
        )
        {
            throw new ExpressionException(this.expression, previousToken, "'"+token.surface +"' is not allowed after '"+previousToken.surface+"'");
        }
        return previousToken = token;
    }

    @Override
    public void remove()
    {
        throw new InternalExpressionException("remove() not supported");
    }

    public static class Token
    {
        enum TokenType
        {
            VARIABLE, FUNCTION, LITERAL, OPERATOR, UNARY_OPERATOR,
            OPEN_PAREN, COMMA, CLOSE_PAREN, HEX_LITERAL, STRINGPARAM, MARKER
        }
        public String surface = "";
        public TokenType type;
        public int pos;
        public int linepos;
        public int lineno;

        public void append(char c)
        {
            surface += c;
        }

        public void append(String s)
        {
            surface += s;
        }

        public char charAt(int pos)
        {
            return surface.charAt(pos);
        }

        public int length()
        {
            return surface.length();
        }

        @Override
        public String toString()
        {
            return surface;
        }
    }
}
