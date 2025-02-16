package carpet.script;

public class Token {
    enum TokenType {
        FUNCTION(true, false), OPERATOR(true, false), UNARY_OPERATOR(true, false),
        VARIABLE(false, false), CONSTANT(false, true),
        LITERAL(false, true), HEX_LITERAL(false, true), STRINGPARAM(false, true),
        OPEN_PAREN(false, true), COMMA(false, true), CLOSE_PAREN(false, true), MARKER(false, true);

        final boolean functional;
        final boolean constant;

        TokenType(boolean functional, boolean constant) {
            this.functional = functional;
            this.constant = constant;
        }

        public boolean isFunctional() {
            return functional;
        }

        public boolean isConstant() {
            return constant;
        }
    }

    public String surface = "";
    public TokenType type;
    public int pos;
    public int linepos;
    public int lineno;
    public static final Token NONE = new Token();

    public Token morphedInto(TokenType newType, String newSurface) {
        Token created = new Token();
        created.surface = newSurface;
        created.type = newType;
        created.pos = pos;
        created.linepos = linepos;
        created.lineno = lineno;
        return created;
    }

    public void morph(TokenType type, String s) {
        this.type = type;
        this.surface = s;
    }

    public void append(char c) {
        surface += c;
    }

    public void append(String s) {
        surface += s;
    }

    public char charAt(int pos) {
        return surface.charAt(pos);
    }

    public int length() {
        return surface.length();
    }

    @Override
    public String toString() {
        return surface;
    }
}
