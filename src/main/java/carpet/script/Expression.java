package carpet.script;

import carpet.CarpetSettings;
import carpet.script.Fluff.AbstractFunction;
import carpet.script.Fluff.AbstractLazyFunction;
import carpet.script.Fluff.AbstractLazyOperator;
import carpet.script.Fluff.AbstractOperator;
import carpet.script.Fluff.AbstractUnaryOperator;
import carpet.script.Fluff.ILazyFunction;
import carpet.script.Fluff.ILazyOperator;
import carpet.script.Fluff.QuadFunction;
import carpet.script.Fluff.QuinnFunction;
import carpet.script.Fluff.SexFunction;
import carpet.script.Fluff.TriFunction;
import carpet.script.exception.BreakStatement;
import carpet.script.exception.ContinueStatement;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ResolvedException;
import carpet.script.exception.ReturnStatement;
import carpet.script.language.Arithmetic;
import carpet.script.language.ControlFlow;
import carpet.script.language.DataStructures;
import carpet.script.language.Functions;
import carpet.script.language.Loops;
import carpet.script.language.Operators;
import carpet.script.language.Sys;
import carpet.script.language.Threading;
import carpet.script.value.FunctionValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Expression
{
    /** The current infix expression */
    private String expression;
    String getCodeString() {return expression;}

    private boolean allowNewlineSubstitutions = true;
    private boolean allowComments = false;

    public Module module = null;

    public String getModuleName()
    {
        return module == null ? "system chat" : module.name();
    }

    public void asATextSource()
    {
        allowNewlineSubstitutions = false;
        allowComments = true;
    }

    public void asAModule(Module mi)
    {
        module = mi;
    }

    /** Cached AST (Abstract Syntax Tree) (root) of the expression */
    private LazyValue ast = null;

    /** script specific operatos and built-in functions */
    private final Map<String, ILazyOperator> operators = new Object2ObjectOpenHashMap<>();
    public boolean isAnOperator(String opname) { return operators.containsKey(opname) || operators.containsKey(opname+"u");}

    private final Map<String, ILazyFunction> functions = new  Object2ObjectOpenHashMap<>();
    public Set<String> getFunctionNames() {return functions.keySet();}

    private final Map<String, String> functionalEquivalence = new Object2ObjectOpenHashMap<>();
    public void addFunctionalEquivalence(String operator, String function)
    {
        assert operators.containsKey(operator);
        assert functions.containsKey(function);
        functionalEquivalence.put(operator, function);
    }

    private final Map<String, Value> constants = Map.of(
            "euler", Arithmetic.euler,
            "pi", Arithmetic.PI,
            "null", Value.NULL,
            "true", Value.TRUE,
            "false", Value.FALSE
    );

    protected Value getConstantFor(String surface)
    {
        return constants.get(surface);
    }

    public List<String> getExpressionSnippet(Tokenizer.Token token)
    {
        String code = this.getCodeString();
        List<String> output = new ArrayList<>(getExpressionSnippetLeftContext(token, code, 1));
        List<String> context = getExpressionSnippetContext(token, code);
        output.add(context.get(0)+" HERE>> "+context.get(1));
        output.addAll(getExpressionSnippetRightContext(token, code, 1));
        return output;
    }

    private static List<String> getExpressionSnippetLeftContext(Tokenizer.Token token, String expr, int contextsize)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length == 1) return output;
        for (int lno=token.lineno-1; lno >=0 && output.size() < contextsize; lno-- )
        {
            output.add(lines[lno]);
        }
        Collections.reverse(output);
        return output;
    }

    private static List<String> getExpressionSnippetContext(Tokenizer.Token token, String expr)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length > 1)
        {
            output.add(lines[token.lineno].substring(0, token.linepos));
            output.add(lines[token.lineno].substring(token.linepos));
        }
        else
        {
            output.add( expr.substring(max(0, token.pos-40), token.pos));
            output.add( expr.substring(token.pos, min(token.pos+1+40, expr.length())));
        }
        return output;
    }

    private static List<String> getExpressionSnippetRightContext(Tokenizer.Token token, String expr, int contextsize)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length == 1) { return output; }
        for (int lno=token.lineno+1; lno < lines.length && output.size() < contextsize; lno++ )
        {
            output.add(lines[lno]);
        }
        return output;
    }


    public void addLazyUnaryOperator(String surface, int precedence, boolean leftAssoc, boolean pure, Function<Context.Type,Context.Type> staticTyper,
                                       TriFunction<Context, Context.Type, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface+"u", new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public boolean pure() {
                return pure;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public Context.Type staticType(Context.Type outerType) {
                return staticTyper.apply(outerType);
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression e, Tokenizer.Token token, LazyValue v, LazyValue v2)
            {
                try
                {
                    return lazyfun.apply(c, t, v);
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, token);
                }
            }
        });
    }


    public void addLazyBinaryOperatorWithDelegation(String surface, int precedence, boolean leftAssoc, boolean pure,
                                       SexFunction<Context, Context.Type, Expression, Tokenizer.Token, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public boolean pure() {
                return pure;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2)
            {
                try
                {
                    return lazyfun.apply(c, type, e, t, v1, v2);
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addCustomFunction(String name, ILazyFunction fun)
    {
        functions.put(name, fun);
    }

    public void addLazyFunctionWithDelegation(String name, int numpar, boolean pure, boolean transitive,
                                                     QuinnFunction<Context, Context.Type, Expression, Tokenizer.Token, List<LazyValue>, LazyValue> lazyfun)
    {
        functions.put(name, new AbstractLazyFunction(numpar, name)
        {
            @Override
            public boolean pure() {
                return pure;
            }

            @Override
            public boolean transitive() {
                return transitive;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type type, Expression e, Tokenizer.Token t, List<LazyValue> lv)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    return lazyfun.apply(c, type, e, t, lv);
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addFunctionWithDelegation(String name, int numpar, boolean pure, boolean transitive,
                                              QuinnFunction<Context, Context.Type, Expression, Tokenizer.Token, List<Value>, Value> fun)
    {
        functions.put(name, new AbstractLazyFunction(numpar, name)
        {
            @Override
            public boolean pure() {
                return pure;
            }

            @Override
            public boolean transitive() {
                return transitive;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type type, Expression e, Tokenizer.Token t, List<LazyValue> lv)
            {
                try
                {
                    Value res = fun.apply(c, type, e, t, unpackArgs(lv, c, Context.NONE));
                    return (cc, tt) -> res;
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addLazyBinaryOperator(String surface, int precedence, boolean leftAssoc, boolean pure, Function<Context.Type, Context.Type> typer,
                                       QuadFunction<Context, Context.Type, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {

            @Override
            public boolean pure() {
                return pure;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public Context.Type staticType(Context.Type outerType)
            {
                return typer.apply(outerType);
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression e, Tokenizer.Token token, LazyValue v1, LazyValue v2)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    return lazyfun.apply(c, t, v1, v2);
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, token);
                }
            }
        });
    }

    public void addBinaryContextOperator(String surface, int precedence, boolean leftAssoc, boolean pure, boolean transitive,
                                      QuadFunction<Context, Context.Type, Value, Value, Value> fun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public boolean pure() {
                return pure;
            }

            @Override
            public boolean transitive() {
                return transitive;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression e, Tokenizer.Token token, LazyValue v1, LazyValue v2)
            {
                try
                {
                    Value ret = fun.apply(c, t, v1.evalValue(c, Context.NONE), v2.evalValue(c, Context.NONE));
                    return (cc, tt) -> ret;
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, token);
                }
            }
        });
    }

    public static RuntimeException handleCodeException(Context c, RuntimeException exc, Expression e, Tokenizer.Token token)
    {
        if (exc instanceof ExitStatement)
            return exc;
        if (exc instanceof IntegrityException)
            return exc;
        if (exc instanceof InternalExpressionException)
            return ((InternalExpressionException) exc).promote(c, e, token);
        if (exc instanceof ArithmeticException)
            return new ExpressionException(c, e, token, "Your math is wrong, "+exc.getMessage());
        if (exc instanceof ResolvedException)
            return exc;
        // unexpected really - should be caught earlier and converted to InternalExpressionException
        CarpetSettings.LOG.error("Unexpected exception while running Scarpet code", exc);
        return new ExpressionException(c, e, token, "Error while evaluating expression: "+exc);
    }

    public void addUnaryOperator(String surface, boolean leftAssoc, Function<Value, Value> fun)
    {
        operators.put(surface+"u", new AbstractUnaryOperator(Operators.precedence.get("unary+-!..."), leftAssoc)
        {
            @Override
            public Value evalUnary(Value v1)
            {
                return fun.apply(v1);
            }
        });
    }

    public void addBinaryOperator(String surface, int precedence, boolean leftAssoc, BiFunction<Value, Value, Value> fun)
    {
        operators.put(surface, new AbstractOperator(precedence, leftAssoc)
        {
            @Override
            public Value eval(Value v1, Value v2) { return fun.apply(v1, v2); }
        });
    }


    public void addUnaryFunction(String name, Function<Value, Value> fun)
    {
        functions.put(name,  new AbstractFunction(1, name)
        {
            @Override
            public Value eval(List<Value> parameters) { return fun.apply(parameters.get(0)); }
        });
    }

    public void addImpureUnaryFunction(String name, Function<Value, Value> fun)
    {
        functions.put(name,  new AbstractFunction(1, name)
        {
            @Override
            public boolean pure() {
                return false;
            }

            @Override
            public Value eval(List<Value> parameters) { return fun.apply(parameters.get(0)); }
        });
    }

    public void addBinaryFunction(String name, BiFunction<Value, Value, Value> fun)
    {
        functions.put(name, new AbstractFunction(2, name)
        {
            @Override
            public Value eval(List<Value> parameters) { return fun.apply(parameters.get(0), parameters.get(1)); }
        });
    }

    public void addFunction(String name, Function<List<Value>, Value> fun)
    {
        functions.put(name, new AbstractFunction(-1, name)
        {
            @Override
            public Value eval(List<Value> parameters) { return fun.apply(parameters); }
        });
    }

    public void addImpureFunction(String name, Function<List<Value>, Value> fun)
    {
        functions.put(name, new AbstractFunction(-1, name)
        {
            @Override
            public boolean pure() {
                return false;
            }

            @Override
            public Value eval(List<Value> parameters) { return fun.apply(parameters); }
        });
    }

    public void addMathematicalUnaryFunction(String name, Function<Double, Double> fun)
    {
        addUnaryFunction(name, (v) -> new NumericValue(fun.apply(NumericValue.asNumber(v).getDouble())));
    }

    public void addMathematicalUnaryIntFunction(String name, Function<Double, Long> fun)
    {
        addUnaryFunction(name, (v) -> new NumericValue(fun.apply(NumericValue.asNumber(v).getDouble())));
    }

    public void addMathematicalBinaryIntFunction(String name, BiFunction<Long, Long, Long> fun)
    {
        addBinaryFunction(name, (w, v) ->
                new NumericValue(fun.apply(NumericValue.asNumber(w).getLong(), NumericValue.asNumber(v).getLong())));
    }
	
    public void addMathematicalBinaryFunction(String name, BiFunction<Double, Double, Double> fun)
    {
        addBinaryFunction(name, (w, v) ->
                new NumericValue(fun.apply(NumericValue.asNumber(w).getDouble(), NumericValue.asNumber(v).getDouble())));
    }


    public void addLazyFunction(String name, int numParams, TriFunction<Context, Context.Type, List<LazyValue>, LazyValue> fun)
    {
        functions.put(name, new AbstractLazyFunction(numParams, name)
        {
            @Override
            public boolean pure() {
                return false;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type i, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
                ILazyFunction.checkInterrupts();
                if (numParams >= 0 && lazyParams.size() != numParams)
                {
                    String error = "Function '"+name+"' requires "+numParams+" arguments, got "+lazyParams.size()+". ";
                    throw new InternalExpressionException(error + (fun instanceof Fluff.UsageProvider up ? up.getUsage() : ""));
                }

                try
                {
                    return fun.apply(c, i, lazyParams);
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addLazyFunction(String name, TriFunction<Context, Context.Type, List<LazyValue>, LazyValue> fun)
    {
        functions.put(name, new AbstractLazyFunction(-1, name)
        {
            @Override
            public boolean pure() {
                return false;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type i, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    return fun.apply(c, i, lazyParams);
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addPureLazyFunction(String name, int num_params, Function<Context.Type, Context.Type> typer, TriFunction<Context, Context.Type, List<LazyValue>, LazyValue> fun)
    {
        functions.put(name, new AbstractLazyFunction(num_params, name)
        {
            @Override
            public boolean pure() {
                return true;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public Context.Type staticType(Context.Type outerType)
            {
                return typer.apply(outerType);
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type i, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    return fun.apply(c, i, lazyParams);
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addContextFunction(String name, int num_params, TriFunction<Context, Context.Type, List<Value>, Value> fun)
    {
        functions.put(name, new AbstractLazyFunction(num_params, name)
        {
            @Override
            public boolean pure() {
                return false;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type i, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    Value ret = fun.apply(c, i, unpackArgs(lazyParams, c, Context.NONE));
                    return (cc, tt) -> ret;
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addTypedContextFunction(String name, int num_params, Context.Type reqType, TriFunction<Context, Context.Type, List<Value>, Value> fun)
    {
        functions.put(name, new AbstractLazyFunction(num_params, name)
        {
            @Override
            public boolean pure() {
                return true;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public Context.Type staticType(Context.Type outerType) {
                return reqType;
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type i, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
                try
                {
                    Value ret = fun.apply(c, i, unpackArgs(lazyParams, c, reqType));
                    return (cc, tt) -> ret;
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public FunctionValue createUserDefinedFunction(Context context, String name, Expression expr, Tokenizer.Token token, List<String> arguments, String varArgs, List<String> outers, LazyValue code)
    {
        if (functions.containsKey(name))
            throw new ExpressionException(context, expr, token, "Function "+name+" would mask a built-in function");
        Map<String, LazyValue> contextValues = new HashMap<>();
        for (String outer : outers)
        {
            LazyValue  lv = context.getVariable(outer);
            if (lv == null)
            {
                throw new InternalExpressionException("Variable "+outer+" needs to be defined in outer scope to be used as outer parameter, and cannot be global");
            }
            else
            {
                contextValues.put(outer, lv);
            }
        }
        if (contextValues.isEmpty()) contextValues = null;

        FunctionValue result =  new FunctionValue(expr, token, name, code, arguments, varArgs, contextValues);
        // do not store lambda definitions
        if (!name.equals("_")) context.host.addUserDefinedFunction(context, module, name, result);
        return result;
    }

    public void alias(String copy, String original)
    {
        ILazyFunction originalFunction = functions.get(original);
        functions.put(copy, new ILazyFunction()
        {
            @Override
            public int getNumParams()
            {
                return originalFunction.getNumParams();
            }

            @Override
            public boolean numParamsVaries()
            {
                return originalFunction.numParamsVaries();
            }

            @Override
            public boolean pure() {
                return originalFunction.pure();
            }

            @Override
            public boolean transitive() {
                return originalFunction.transitive();
            }

            @Override
            public Context.Type staticType(Context.Type outerType) {
                return originalFunction.staticType(outerType);
            }

            @Override
            public LazyValue lazyEval(Context c, Context.Type type, Expression expr, Tokenizer.Token token, List<LazyValue> lazyParams)
            {
                c.host.issueDeprecation(copy+"(...)");
                return originalFunction.lazyEval(c, type, expr, token, lazyParams);
            }
        });
    }


    public void setAnyVariable(Context c, String name, LazyValue lv)
    {
        if (name.startsWith("global_"))
        {
            c.host.setGlobalVariable(module, name, lv);
        }
        else
        {
            c.setVariable(name, lv);
        }
    }

    public LazyValue getOrSetAnyVariable(Context c, String name)
    {
        LazyValue var;
        if (!name.startsWith("global_"))
        {
            var = c.getVariable(name);
            if (var != null) return var;
        }
        var = c.host.getGlobalVariable(module, name);
        if (var != null) return var;
        var = (_c, _t ) -> _c.host.strict ? Value.UNDEF.reboundedTo(name) : Value.NULL.reboundedTo(name);
        setAnyVariable(c, name, var);
        return var;
    }

    public static final Expression none = new Expression("null");
    /**
     * @param expression .
     */
    public Expression(String expression)
    {
        this.expression = expression.stripTrailing().replaceAll("\\r\\n?", "\n").replaceAll("\\t","   ");
        Operators.apply(this);
        ControlFlow.apply(this);
        Functions.apply(this);
        Arithmetic.apply(this);
        Sys.apply(this);
        Threading.apply(this);
        Loops.apply(this);
        DataStructures.apply(this);
    }


    private List<Tokenizer.Token> shuntingYard(Context c)
    {
        List<Tokenizer.Token> outputQueue = new ArrayList<>();
        Stack<Tokenizer.Token> stack = new Stack<>();

        Tokenizer tokenizer = new Tokenizer(c, this, expression, allowComments, allowNewlineSubstitutions);
        // stripping lousy but acceptable semicolons
        List<Tokenizer.Token> cleanedTokens = tokenizer.postProcess();

        Tokenizer.Token lastFunction = null;
        Tokenizer.Token previousToken = null;
        for (Tokenizer.Token token : cleanedTokens)
        {
            switch (token.type)
            {
                case STRINGPARAM:
                    //stack.push(token); // changed that so strings are treated like literals
                    //break;
                case LITERAL:
                case HEX_LITERAL:
                    if (previousToken != null && (
                            previousToken.type == Tokenizer.Token.TokenType.LITERAL ||
                                    previousToken.type == Tokenizer.Token.TokenType.HEX_LITERAL ||
                                    previousToken.type == Tokenizer.Token.TokenType.STRINGPARAM))
                    {
                        throw new ExpressionException(c, this, token, "Missing operator");
                    }
                    outputQueue.add(token);
                    break;
                case VARIABLE:
                    outputQueue.add(token);
                    break;
                case FUNCTION:
                    stack.push(token);
                    lastFunction = token;
                    break;
                case COMMA:
                    if (previousToken != null && previousToken.type == Tokenizer.Token.TokenType.OPERATOR)
                    {
                        throw new ExpressionException(c, this, previousToken, "Missing parameter(s) for operator ");
                    }
                    while (!stack.isEmpty() && stack.peek().type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        if (lastFunction == null)
                        {
                            throw new ExpressionException(c, this, token, "Unexpected comma");
                        }
                        else
                        {
                            throw new ExpressionException(c, this, lastFunction, "Parse error for function");
                        }
                    }
                    break;
                case OPERATOR:
                {
                    if (previousToken != null
                            && (previousToken.type == Tokenizer.Token.TokenType.COMMA || previousToken.type == Tokenizer.Token.TokenType.OPEN_PAREN))
                    {
                        throw new ExpressionException(c, this, token, "Missing parameter(s) for operator '" + token+"'");
                    }
                    ILazyOperator o1 = operators.get(token.surface);
                    if (o1 == null)
                    {
                        throw new ExpressionException(c, this, token, "Unknown operator '" + token + "'");
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case UNARY_OPERATOR:
                {
                    if (previousToken != null && previousToken.type != Tokenizer.Token.TokenType.OPERATOR
                            && previousToken.type != Tokenizer.Token.TokenType.COMMA && previousToken.type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        throw new ExpressionException(c, this, token, "Invalid position for unary operator " + token );
                    }
                    ILazyOperator o1 = operators.get(token.surface);
                    if (o1 == null)
                    {
                        throw new ExpressionException(c, this, token, "Unknown unary operator '" + token.surface.substring(0, token.surface.length() - 1) + "'");
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case OPEN_PAREN:
                    if (previousToken != null)
                    {
                        //if (previousToken.type == Tokenizer.Token.TokenType.LITERAL || previousToken.type == Tokenizer.Token.TokenType.CLOSE_PAREN
                        //        || previousToken.type == Tokenizer.Token.TokenType.VARIABLE
                        //        || previousToken.type == Tokenizer.Token.TokenType.HEX_LITERAL)
                        //{
                            // Implicit multiplication, e.g. 23(a+b) or (a+b)(a-b)
                        //    Tokenizer.Token multiplication = new Tokenizer.Token();
                        //    multiplication.append("*");
                        //    multiplication.type = Tokenizer.Token.TokenType.OPERATOR;
                        //    stack.push(multiplication);
                        //}
                        // if the ( is preceded by a valid function, then it
                        // denotes the start of a parameter list
                        if (previousToken.type == Tokenizer.Token.TokenType.FUNCTION)
                        {
                            outputQueue.add(token);
                        }
                    }
                    stack.push(token);
                    break;
                case CLOSE_PAREN:
                    if (previousToken != null && previousToken.type == Tokenizer.Token.TokenType.OPERATOR)
                    {
                        throw new ExpressionException(c, this, previousToken, "Missing parameter(s) for operator " + previousToken);
                    }
                    while (!stack.isEmpty() && stack.peek().type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        throw new ExpressionException(c, this, "Mismatched parentheses");
                    }
                    stack.pop();
                    if (!stack.isEmpty() && stack.peek().type == Tokenizer.Token.TokenType.FUNCTION)
                    {
                        outputQueue.add(stack.pop());
                    }
                    break;
                case MARKER:
                    if ("$".equals(token.surface))
                    {
                        StringBuilder sb = new StringBuilder(expression);
                        sb.setCharAt(token.pos, '\n');
                        expression = sb.toString();
                    }
                    break;
            }
            if (token.type != Tokenizer.Token.TokenType.MARKER) previousToken = token;
        }

        while (!stack.isEmpty())
        {
            Tokenizer.Token element = stack.pop();
            if (element.type == Tokenizer.Token.TokenType.OPEN_PAREN || element.type == Tokenizer.Token.TokenType.CLOSE_PAREN)
            {
                throw new ExpressionException(c, this, element, "Mismatched parentheses");
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    private void shuntOperators(List<Tokenizer.Token> outputQueue, Stack<Tokenizer.Token> stack, ILazyOperator o1)
    {
        Tokenizer.Token nextToken = stack.isEmpty() ? null : stack.peek();
        while (nextToken != null
                && (nextToken.type == Tokenizer.Token.TokenType.OPERATOR
                || nextToken.type == Tokenizer.Token.TokenType.UNARY_OPERATOR)
                && ((o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(nextToken.surface).getPrecedence())
                || (o1.getPrecedence() < operators.get(nextToken.surface).getPrecedence())))
        {
            outputQueue.add(stack.pop());
            nextToken = stack.isEmpty() ? null : stack.peek();
        }
    }

    public Value eval(Context c)
    {
        if (ast == null)
        {
            ast = getAST(c);
        }
        return evalValue(() -> ast, c, Context.Type.NONE);
    }

    public Value evalValue(Supplier<LazyValue> exprProvider, Context c, Context.Type expectedType)
    {
        try
        {
            return exprProvider.get().evalValue(c, expectedType);
        }
        catch (ContinueStatement | BreakStatement | ReturnStatement exc)
        {
            throw new ExpressionException(c, this, "Control flow functions, like continue, break or return, should only be used in loops, and functions respectively.");
        }
        catch (ExitStatement exit)
        {
            return exit.retval==null?Value.NULL:exit.retval;
        }
        catch (StackOverflowError ignored)
        {
            throw new ExpressionException(c, this, "Your thoughts are too deep");
        }
        catch (InternalExpressionException exc)
        {
            throw new ExpressionException(c, this, "Your expression result is incorrect: "+exc.getMessage());
        }
        catch (ArithmeticException exc)
        {
            throw new ExpressionException(c, this, "The final result is incorrect: "+exc.getMessage());
        }
    }

    public static class ExpressionNode
    {
        public LazyValue op;
        public List<ExpressionNode> args;
        public Tokenizer.Token token;
        public List<Tokenizer.Token> range;
        /**
         * The Value representation of the left parenthesis, used for parsing
         * varying numbers of function parameters.
         */
        public static final ExpressionNode PARAMS_START = new ExpressionNode(null, null, Tokenizer.Token.NONE);
        public ExpressionNode(LazyValue op, List<ExpressionNode> args, Tokenizer.Token token)
        {
            this.op = op;
            this.args = args;
            this.token = token;
            range = new ArrayList<>();
            range.add(token);
        }
        public static ExpressionNode ofConstant(Value val, Tokenizer.Token token)
        {
            return new ExpressionNode(new LazyValue.Constant(val), Collections.emptyList(), token);
        }
    }


    private ExpressionNode RPNToParseTree(List<Tokenizer.Token> tokens, Context context)
    {
        Stack<ExpressionNode> nodeStack = new Stack<>();
        for (final Tokenizer.Token token : tokens)
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                {
                    final ExpressionNode node = nodeStack.pop();
                    LazyValue result = (c, t) -> operators.get(token.surface).lazyEval(c, t, this, token, node.op, null).evalValue(c, t);
                    nodeStack.push(new ExpressionNode(result, Collections.singletonList(node), token));
                    break;
                }
                case OPERATOR:
                    final ExpressionNode v1 = nodeStack.pop();
                    final ExpressionNode v2 = nodeStack.pop();
                    LazyValue result = (c,t) -> operators.get(token.surface).lazyEval(c, t,this, token, v2.op, v1.op).evalValue(c, t);
                    nodeStack.push(new ExpressionNode(result, List.of(v2, v1), token ));
                    break;
                case VARIABLE:
                    Value constant = getConstantFor(token.surface);
                    if (constant != null)
                    {
                        token.morph(Tokenizer.Token.TokenType.CONSTANT, token.surface);
                        nodeStack.push(new ExpressionNode(LazyValue.ofConstant(constant), Collections.emptyList(), token));
                    }
                    else {
                        nodeStack.push(new ExpressionNode(((c, t) -> getOrSetAnyVariable(c, token.surface).evalValue(c, t)), Collections.emptyList(), token));
                    }
                    break;
                case FUNCTION:
                    String name = token.surface;
                    ILazyFunction f;
                    ArrayList<ExpressionNode> p;
                    boolean isKnown = functions.containsKey(name); // globals will be evaluated lazily, not at compile time via .
                    if (isKnown)
                    {
                        f = functions.get(name);
                        p = new ArrayList<>(!f.numParamsVaries() ? f.getNumParams() : 0);
                    }
                    else // potentially unknown function or just unknown function
                    {
                        f = functions.get("call");
                        p = new ArrayList<>();
                    }
                    // pop parameters off the stack until we hit the start of
                    // this function's parameter list
                    while (!nodeStack.isEmpty() && nodeStack.peek() != ExpressionNode.PARAMS_START)
                    {
                        p.add(nodeStack.pop());
                    }
                    if (!isKnown)
                    {
                        p.add(ExpressionNode.ofConstant(new StringValue(name), token.morphedInto(Tokenizer.Token.TokenType.STRINGPARAM, token.surface)));
                        token.morph(Tokenizer.Token.TokenType.FUNCTION, "call");
                    }
                    Collections.reverse(p);

                    if (nodeStack.peek() == ExpressionNode.PARAMS_START)
                    {
                        nodeStack.pop();
                    };
                    List<LazyValue> params = p.stream().map(n -> n.op).collect(Collectors.toList());

                    nodeStack.push(new ExpressionNode(
                            (c, t) -> f.lazyEval(c, t, this, token, params).evalValue(c, t),
                            p,token
                    ));
                    break;
                case OPEN_PAREN:
                    nodeStack.push(ExpressionNode.PARAMS_START);
                    break;
                case LITERAL:
                    Value number;
                    try
                    {
                        number = new NumericValue(token.surface);
                    }
                    catch (NumberFormatException exception)
                    {
                        throw new ExpressionException(context, this, token, "Not a number");
                    }
                    token.morph(Tokenizer.Token.TokenType.CONSTANT, token.surface);
                    nodeStack.push(ExpressionNode.ofConstant(number, token));
                    break;
                case STRINGPARAM:
                    token.morph(Tokenizer.Token.TokenType.CONSTANT, token.surface);
                    nodeStack.push(ExpressionNode.ofConstant(new StringValue(token.surface), token));
                    break;
                case HEX_LITERAL:
                    Value hexNumber;
                    try
                    {
                        hexNumber = new NumericValue(new BigInteger(token.surface.substring(2), 16).longValue());
                    }
                    catch (NumberFormatException exception)
                    {
                        throw new ExpressionException(context, this, token, "Not a number");
                    }
                    token.morph(Tokenizer.Token.TokenType.CONSTANT, token.surface);
                    nodeStack.push(ExpressionNode.ofConstant(hexNumber, token));
                    break;
                default:
                    throw new ExpressionException(context, this, token, "Unexpected token '" + token.surface + "'");
            }
        }
        return nodeStack.pop();
    }

    private LazyValue getAST(Context context)
    {
        //Stack<LazyValue> stack = new Stack<>();
        List<Tokenizer.Token> rpn = shuntingYard(context);
        validate(context, rpn);
        ExpressionNode root = RPNToParseTree(rpn, context);
        if (!CarpetSettings.scriptsOptimization)
            return root.op;

        Context optimizeOnlyContext = new Context.ContextForErrorReporting(context);
        if (CarpetSettings.scriptsDebugging)
            CarpetScriptServer.LOG.info("Input code size for "+getModuleName()+": " + treeSize(root) + " nodes, " + treeDepth(root) + " deep");

        // Defined out here to not need to conditionally assign them with debugging disabled
        int prevTreeSize = -1;
        int prevTreeDepth = -1;

        boolean changed = true;
        while(changed) {
            changed = false;
            while (true) {
                if (CarpetSettings.scriptsDebugging) {
                    prevTreeSize = treeSize(root);
                    prevTreeDepth = treeDepth(root);
                }
                boolean optimized = compactTree(root, Context.Type.NONE, 0);
                if (!optimized) break;
                changed = true;
                if (CarpetSettings.scriptsDebugging)
                    CarpetScriptServer.LOG.info("Compacted from " + prevTreeSize + " nodes, " + prevTreeDepth + " code depth to " + treeSize(root) + " nodes, " + treeDepth(root) + " code depth");
            }
            while (true) {
                if (CarpetSettings.scriptsDebugging) {
                    prevTreeSize = treeSize(root);
                    prevTreeDepth = treeDepth(root);
                }
                boolean optimized = optimizeTree(optimizeOnlyContext, root, Context.Type.NONE, 0);
                if (!optimized) break;
                changed = true;
                if (CarpetSettings.scriptsDebugging)
                    CarpetScriptServer.LOG.info("Optimized from " + prevTreeSize + " nodes, " + prevTreeDepth + " code depth to " + treeSize(root) + " nodes, " + treeDepth(root) + " code depth");
            }
        }
        return extractOp(optimizeOnlyContext, root, Context.Type.NONE);
    }

    private int treeSize(ExpressionNode node)
    {
        if (node.op instanceof LazyValue.ContextFreeLazyValue) return 1;
        return node.args.stream().mapToInt(this::treeSize).sum()+1;
    }
    private int treeDepth(ExpressionNode node)
    {
        if (node.op instanceof LazyValue.ContextFreeLazyValue) return 1;
        return node.args.stream().mapToInt(this::treeDepth).max().orElse(0)+1;
    }


    private boolean compactTree(ExpressionNode node, Context.Type expectedType, int indent) {
        // ctx is just to report errors, not values evaluation
        boolean optimized = false;
        Tokenizer.Token.TokenType token = node.token.type;
        if (!token.isFunctional()) return false;
        // input special cases here, like function signature
        if (node.op instanceof LazyValue.Constant) return false; // optimized already
        // function or operator
        String symbol = node.token.surface;
        Fluff.EvalNode operation = ((token == Tokenizer.Token.TokenType.FUNCTION) ? functions : operators).get(symbol);
        Context.Type requestedType = operation.staticType(expectedType);
        for (ExpressionNode arg : node.args) {
            if (compactTree(arg, requestedType, indent+1)) optimized = true;
        }

        if (expectedType != Context.Type.MAPDEF && symbol.equals("->") && node.args.size() == 2) {
            String rop = node.args.get(1).token.surface;
            ExpressionNode returnNode = null;
            if ((rop.equals(";") || rop.equals("then"))) {
                List<ExpressionNode> thenArgs = node.args.get(1).args;
                if (thenArgs.size() > 1 && thenArgs.get(thenArgs.size() - 1).token.surface.equals("return")) {
                    returnNode = thenArgs.get(thenArgs.size() - 1);
                }
            } else if (rop.equals("return")) {
                returnNode = node.args.get(1);
            }
            if (returnNode != null) // tail return
            {
                if (returnNode.args.size() > 0) {
                    returnNode.op = returnNode.args.get(0).op;
                    returnNode.token = returnNode.args.get(0).token;
                    returnNode.range = returnNode.args.get(0).range;
                    returnNode.args = returnNode.args.get(0).args;
                    if (CarpetSettings.scriptsDebugging)
                        CarpetScriptServer.LOG.info(" - Removed unnecessary tail return of " + returnNode.token.surface + " from function body at line " + (returnNode.token.lineno + 1) + ", node depth " + indent);

                } else {
                    returnNode.op = LazyValue.ofConstant(Value.NULL);
                    returnNode.token.morph(Tokenizer.Token.TokenType.CONSTANT, "");
                    returnNode.args = Collections.emptyList();
                    if (CarpetSettings.scriptsDebugging)
                        CarpetScriptServer.LOG.info(" - Removed unnecessary tail return from function body at line " + (returnNode.token.lineno + 1) + ", node depth " + indent);

                }

            }
        }
        for (Map.Entry<String, String> pair : functionalEquivalence.entrySet()) {
            String operator = pair.getKey();
            String function = pair.getValue();
            if ((symbol.equals(operator) || symbol.equals(function)) && node.args.size() > 0)
            {
                boolean leftOptimizable = operators.get(operator).isLeftAssoc();
                ExpressionNode optimizedChild = node.args.get(leftOptimizable?0:(node.args.size()-1));
                String type = optimizedChild.token.surface;
                if ((type.equals(operator) || type.equals(function)) && (!(optimizedChild.op instanceof LazyValue.ContextFreeLazyValue)))
                {
                    optimized = true;
                    List<ExpressionNode> newargs = new ArrayList<>();
                    if (leftOptimizable)
                    {
                        newargs.addAll(optimizedChild.args);
                        for (int i = 1; i < node.args.size(); i++)
                            newargs.add(node.args.get(i));
                    }
                    else
                    {
                        for (int i = 0; i < node.args.size()-1; i++)
                            newargs.add(node.args.get(i));
                        newargs.addAll(optimizedChild.args);
                    }

                    if (CarpetSettings.scriptsDebugging)
                        CarpetScriptServer.LOG.info(" - " + symbol + "(" + node.args.size() + ") => " + function + "(" + newargs.size() + ") at line " + (node.token.lineno + 1) + ", node depth " + indent);
                    node.token.morph(Tokenizer.Token.TokenType.FUNCTION, function);
                    node.args = newargs;
                }
            }
        }
        return optimized;
    }

    private boolean optimizeTree(Context ctx, ExpressionNode node, Context.Type expectedType, int indent) {
        // ctx is just to report errors, not values evaluation
        boolean optimized = false;
        Tokenizer.Token.TokenType token = node.token.type;
        if (!token.isFunctional()) return false;
        String symbol = node.token.surface;

        // input special cases here, like function signature
        if (node.op instanceof LazyValue.Constant) return false; // optimized already
        // function or operator

        Fluff.EvalNode operation = ((token == Tokenizer.Token.TokenType.FUNCTION) ? functions : operators).get(symbol);
        Context.Type requestedType = operation.staticType(expectedType);
        for (ExpressionNode arg : node.args) {
            if (optimizeTree(ctx, arg, requestedType, indent+1)) optimized = true;
        }

        for (ExpressionNode arg : node.args) {
            if (arg.token.type.isConstant()) continue;
            if (arg.op instanceof LazyValue.ContextFreeLazyValue) continue;
            return optimized;
        }
        // a few exceptions which we don't implement in the framework for simplicity for now
        if (!operation.pure())
        {
            if (symbol.equals("->") && expectedType == Context.Type.MAPDEF)
            {

            }
            else {
                return optimized;
            }
        }
        if (operation.pure())
        {
            // element access with constant elements will always resolve the same way.
            if (symbol.equals(":") && expectedType == Context.Type.LVALUE)
            {
                expectedType = Context.Type.NONE;
            }
        }
        List<LazyValue> args = new ArrayList<>(node.args.size());
        for (ExpressionNode arg : node.args) {
            try {
                if (arg.op instanceof LazyValue.Constant) {
                    Value val = ((LazyValue.Constant) arg.op).get();
                    args.add((c, t) -> val);
                }
                else args.add((c, t) -> arg.op.evalValue(ctx, requestedType));
            } catch (NullPointerException npe) {
                throw new ExpressionException(ctx, this, node.token, "Attempted to evaluate context free expression");
            }
        }
        // applying argument unpacking
        args= AbstractLazyFunction.lazify(AbstractLazyFunction.unpackLazy(args, ctx, requestedType));
        Value result;
        if (operation instanceof ILazyFunction)
        {
            result = ((ILazyFunction) operation).lazyEval(ctx, expectedType, this, node.token, args).evalValue(null, expectedType);
        }
        else if (args.size() == 1)
        {
            result = ((ILazyOperator)operation).lazyEval(ctx, expectedType, this, node.token, args.get(0), null).evalValue(null, expectedType);
        }
        else // args == 2
        {
            result = ((ILazyOperator)operation).lazyEval(ctx, expectedType, this, node.token, args.get(0), args.get(1)).evalValue(null, expectedType);
        }
        node.op = LazyValue.ofConstant(result);
        //node.token.morph(Tokenizer.Token.TokenType.CONSTANT, node.token.surface);
        if (CarpetSettings.scriptsDebugging)
            CarpetScriptServer.LOG.info(" - "+symbol+"("+args.stream().map(a -> a.evalValue(null, requestedType).getString()).collect(Collectors.joining(", "))+") => "+result.getString()+" at line "+(node.token.lineno+1) +", node depth "+indent);
        return true;
    }

    private LazyValue extractOp(Context ctx, ExpressionNode node, Context.Type expectedType)
    {
        if (node.op instanceof LazyValue.Constant)
        {
            // constants are immutable
            if (node.token.type.isConstant())
            {
                Value value = ((LazyValue.Constant) node.op).get();
                return (c, t) -> value;
            }
            return node.op;
        }
        if (node.op instanceof LazyValue.ContextFreeLazyValue)
        {
            Value ret = ((LazyValue.ContextFreeLazyValue) node.op).evalType(expectedType);
            return (c, t) -> ret;
        }
        Tokenizer.Token token = node.token;
        switch (token.type)
        {
            case UNARY_OPERATOR: {
                ILazyOperator op = operators.get(token.surface);
                Context.Type requestedType = op.staticType(expectedType);
                LazyValue arg = extractOp(ctx, node.args.get(0), requestedType);
                return (c, t) -> op.lazyEval(c, t, this, token, arg, null).evalValue(c, t);
            }
            case OPERATOR: {
                ILazyOperator op = operators.get(token.surface);
                Context.Type requestedType = op.staticType(expectedType);
                LazyValue arg = extractOp(ctx, node.args.get(0), requestedType);
                LazyValue arh = extractOp(ctx, node.args.get(1), requestedType);
                return (c, t) -> op.lazyEval(c, t, this, token, arg, arh).evalValue(c, t);
            }
            case VARIABLE:
                return (c, t) -> getOrSetAnyVariable(c, token.surface).evalValue(c, t);
            case FUNCTION: {
                ILazyFunction f = functions.get(token.surface);
                Context.Type requestedType = f.staticType(expectedType);
                List<LazyValue> params = node.args.stream().map(n -> extractOp(ctx, n, requestedType)).collect(Collectors.toList());
                return (c, t) -> f.lazyEval(c, t, this, token, params).evalValue(c, t);
            }
            case CONSTANT:
                return node.op;
            default:
                throw new ExpressionException(ctx, this, node.token, "Unexpected token '" + node.token.type +" "+node.token.surface + "'");

        }
    }

    private void validate(Context c, List<Tokenizer.Token> rpn)
    {
        /*-
         * Thanks to Norman Ramsey:
         * http://http://stackoverflow.com/questions/789847/postfix-notation-validation
         */
        // each push on to this stack is a new function scope, with the value of
        // each
        // layer on the stack being the count of the number of parameters in
        // that scope
        Stack<Integer> stack = new Stack<>();

        // push the 'global' scope
        stack.push(0);

        for (final Tokenizer.Token token : rpn)
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                    if (stack.peek() < 1)
                    {
                        throw new ExpressionException(c, this, token, "Missing parameter(s) for operator " + token);
                    }
                    break;
                case OPERATOR:
                    if (stack.peek() < 2)
                    {
                        if (token.surface.equals(";"))
                        {
                            throw new ExpressionException(c, this, token, "Empty expression found for ';'");
                        }
                        throw new ExpressionException(c, this, token, "Missing parameter(s) for operator " + token);
                    }
                    // pop the operator's 2 parameters and add the result
                    stack.set(stack.size() - 1, stack.peek() - 2 + 1);
                    break;
                case FUNCTION:
                    //ILazyFunction f = functions.get(token.surface);// don't validate global - userdef functions
                    //int numParams = stack.pop();
                    //if (f != null && !f.numParamsVaries() && numParams != f.getNumParams())
                    //{
                    //    throw new ExpressionException(c, this, token, "Function " + token + " expected " + f.getNumParams() + " parameters, got " + numParams);
                    //}
                    stack.pop();
                    // due to unpacking, all functions can have variable number of arguments
                    // we will be checking that at runtime.
                    // TODO try analyze arguments and assess if its possible that they are static
                    if (stack.size() <= 0)
                    {
                        throw new ExpressionException(c, this, token, "Too many function calls, maximum scope exceeded");
                    }
                    // push the result of the function
                    stack.set(stack.size() - 1, stack.peek() + 1);
                    break;
                case OPEN_PAREN:
                    stack.push(0);
                    break;
                default:
                    stack.set(stack.size() - 1, stack.peek() + 1);
            }
        }

        if (stack.size() > 1)
        {
            throw new ExpressionException(c, this, "Too many unhandled function parameter lists");
        }
        else if (stack.peek() > 1)
        {
            throw new ExpressionException(c, this, "Too many numbers or variables");
        }
        else if (stack.peek() < 1)
        {
            throw new ExpressionException(c, this, "Empty expression");
        }
    }
}
