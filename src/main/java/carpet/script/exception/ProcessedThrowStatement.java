package carpet.script.exception;

import java.util.List;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer.Token;
import carpet.script.value.FunctionValue;

public class ProcessedThrowStatement extends ExpressionException {
    public final Throwables.Exception exception;
    public final String message;
    
    public ProcessedThrowStatement(Context c, Expression e, Token token, String message, List<FunctionValue> stack, Throwables.Exception exception) {
        super(c, e, token, "Unhandled exception: "+message, stack);
        this.exception = exception;
        this.message = message;
    }
}
