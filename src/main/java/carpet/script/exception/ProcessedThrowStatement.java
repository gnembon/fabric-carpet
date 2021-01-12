package carpet.script.exception;

import java.util.List;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer.Token;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

public class ProcessedThrowStatement extends ExpressionException {
    public final Value exceptionName;
    public final String message;
    
    public ProcessedThrowStatement(Context c, Expression e, Token token, String message, List<FunctionValue> stack, Value exceptionName) {
        super(c, e, token, "Unhandled exception: "+message, stack);
        this.exceptionName = exceptionName;
        this.message = message;
    }
}
