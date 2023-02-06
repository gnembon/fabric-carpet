package carpet.script;

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
import carpet.script.external.Vanilla;
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
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Expression
{
    /**
     * The current infix expression
     */
    private String expression;

    String getCodeString()
    {
        return expression;
    }

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

    public void asAModule(final Module mi)
    {
        module = mi;
    }

    /**
     * Cached AST (Abstract Syntax Tree) (root) of the expression
     */
    private LazyValue ast = null;

    /**
     * script specific operatos and built-in functions
     */
    private final Map<String, ILazyOperator> operators = new Object2ObjectOpenHashMap<>();

    public boolean isAnOperator(final String opname)
    {
        return operators.containsKey(opname) || operators.containsKey(opname + "u");
    }

    private final Map<String, ILazyFunction> functions = new Object2ObjectOpenHashMap<>();

    public Set<String> getFunctionNames()
    {
        return functions.keySet();
    }

    private final Map<String, String> functionalEquivalence = new Object2ObjectOpenHashMap<>();

    public void addFunctionalEquivalence(final String operator, final String function)
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

    protected Value getConstantFor(final String surface)
    {
        return constants.get(surface);
    }

    public List<String> getExpressionSnippet(final Tokenizer.Token token)
    {
        final String code = this.getCodeString();
        final List<String> output = new ArrayList<>(getExpressionSnippetLeftContext(token, code, 1));
        final List<String> context = getExpressionSnippetContext(token, code);
        output.add(context.get(0) + " HERE>> " + context.get(1));
        output.addAll(getExpressionSnippetRightContext(token, code, 1));
        return output;
    }

    private static List<String> getExpressionSnippetLeftContext(final Tokenizer.Token token, final String expr, final int contextsize)
    {
        final List<String> output = new ArrayList<>();
        final String[] lines = expr.split("\n");
        if (lines.length == 1)
        {
            return output;
        }
        for (int lno = token.lineno - 1; lno >= 0 && output.size() < contextsize; lno--)
        {
            output.add(lines[lno]);
        }
        Collections.reverse(output);
        return output;
    }

    private static List<String> getExpressionSnippetContext(final Tokenizer.Token token, final String expr)
    {
        final List<String> output = new ArrayList<>();
        final String[] lines = expr.split("\n");
        if (lines.length > 1)
        {
            output.add(lines[token.lineno].substring(0, token.linepos));
            output.add(lines[token.lineno].substring(token.linepos));
        }
        else
        {
            output.add(expr.substring(Math.max(0, token.pos - 40), token.pos));
            output.add(expr.substring(token.pos, Math.min(token.pos + 1 + 40, expr.length())));
        }
        return output;
    }

    private static List<String> getExpressionSnippetRightContext(final Tokenizer.Token token, final String expr, final int contextsize)
    {
        final List<String> output = new ArrayList<>();
        final String[] lines = expr.split("\n");
        if (lines.length == 1)
        {
            return output;
        }
        for (int lno = token.lineno + 1; lno < lines.length && output.size() < contextsize; lno++)
        {
            output.add(lines[lno]);
        }
        return output;
    }


    public void addLazyUnaryOperator(final String surface, final int precedence, final boolean leftAssoc, final boolean pure, final Function<Context.Type, Context.Type> staticTyper,
                                     final TriFunction<Context, Context.Type, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface + "u", new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public boolean pure()
            {
                return pure;
            }

            @Override
            public boolean transitive()
            {
                return false;
            }

            @Override
            public Context.Type staticType(final Context.Type outerType)
            {
                return staticTyper.apply(outerType);
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type t, final Expression e, final Tokenizer.Token token, final LazyValue v, final LazyValue v2)
            {
                try
                {
                    return lazyfun.apply(c, t, v);
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, token);
                }
            }
        });
    }


    public void addLazyBinaryOperatorWithDelegation(final String surface, final int precedence, final boolean leftAssoc, final boolean pure,
                                                    final SexFunction<Context, Context.Type, Expression, Tokenizer.Token, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public boolean pure()
            {
                return pure;
            }

            @Override
            public boolean transitive()
            {
                return false;
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type type, final Expression e, final Tokenizer.Token t, final LazyValue v1, final LazyValue v2)
            {
                try
                {
                    return lazyfun.apply(c, type, e, t, v1, v2);
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addCustomFunction(final String name, final ILazyFunction fun)
    {
        functions.put(name, fun);
    }

    public void addLazyFunctionWithDelegation(final String name, final int numpar, final boolean pure, final boolean transitive,
                                              final QuinnFunction<Context, Context.Type, Expression, Tokenizer.Token, List<LazyValue>, LazyValue> lazyfun)
    {
        functions.put(name, new AbstractLazyFunction(numpar, name)
        {
            @Override
            public boolean pure()
            {
                return pure;
            }

            @Override
            public boolean transitive()
            {
                return transitive;
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type type, final Expression e, final Tokenizer.Token t, final List<LazyValue> lv)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    return lazyfun.apply(c, type, e, t, lv);
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addFunctionWithDelegation(final String name, final int numpar, final boolean pure, final boolean transitive,
                                          final QuinnFunction<Context, Context.Type, Expression, Tokenizer.Token, List<Value>, Value> fun)
    {
        functions.put(name, new AbstractLazyFunction(numpar, name)
        {
            @Override
            public boolean pure()
            {
                return pure;
            }

            @Override
            public boolean transitive()
            {
                return transitive;
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type type, final Expression e, final Tokenizer.Token t, final List<LazyValue> lv)
            {
                try
                {
                    final Value res = fun.apply(c, type, e, t, unpackArgs(lv, c, Context.NONE));
                    return (cc, tt) -> res;
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addLazyBinaryOperator(final String surface, final int precedence, final boolean leftAssoc, final boolean pure, final Function<Context.Type, Context.Type> typer,
                                      final QuadFunction<Context, Context.Type, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {

            @Override
            public boolean pure()
            {
                return pure;
            }

            @Override
            public boolean transitive()
            {
                return false;
            }

            @Override
            public Context.Type staticType(final Context.Type outerType)
            {
                return typer.apply(outerType);
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type t, final Expression e, final Tokenizer.Token token, final LazyValue v1, final LazyValue v2)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    return lazyfun.apply(c, t, v1, v2);
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, token);
                }
            }
        });
    }

    public void addBinaryContextOperator(final String surface, final int precedence, final boolean leftAssoc, final boolean pure, final boolean transitive,
                                         final QuadFunction<Context, Context.Type, Value, Value, Value> fun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public boolean pure()
            {
                return pure;
            }

            @Override
            public boolean transitive()
            {
                return transitive;
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type t, final Expression e, final Tokenizer.Token token, final LazyValue v1, final LazyValue v2)
            {
                try
                {
                    final Value ret = fun.apply(c, t, v1.evalValue(c, Context.NONE), v2.evalValue(c, Context.NONE));
                    return (cc, tt) -> ret;
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, token);
                }
            }
        });
    }

    public static RuntimeException handleCodeException(final Context c, final RuntimeException exc, final Expression e, final Tokenizer.Token token)
    {
        if (exc instanceof ExitStatement)
        {
            return exc;
        }
        if (exc instanceof IntegrityException)
        {
            return exc;
        }
        if (exc instanceof final InternalExpressionException iee)
        {
            return iee.promote(c, e, token);
        }
        if (exc instanceof ArithmeticException)
        {
            return new ExpressionException(c, e, token, "Your math is wrong, " + exc.getMessage());
        }
        if (exc instanceof ResolvedException)
        {
            return exc;
        }
        // unexpected really - should be caught earlier and converted to InternalExpressionException
        CarpetScriptServer.LOG.error("Unexpected exception while running Scarpet code", exc);
        return new ExpressionException(c, e, token, "Internal error (please report this issue to Carpet) while evaluating: " + exc);
    }

    public void addUnaryOperator(final String surface, final boolean leftAssoc, final Function<Value, Value> fun)
    {
        operators.put(surface + "u", new AbstractUnaryOperator(Operators.precedence.get("unary+-!..."), leftAssoc)
        {
            @Override
            public Value evalUnary(final Value v1)
            {
                return fun.apply(v1);
            }
        });
    }

    public void addBinaryOperator(final String surface, final int precedence, final boolean leftAssoc, final BiFunction<Value, Value, Value> fun)
    {
        operators.put(surface, new AbstractOperator(precedence, leftAssoc)
        {
            @Override
            public Value eval(final Value v1, final Value v2)
            {
                return fun.apply(v1, v2);
            }
        });
    }


    public void addUnaryFunction(final String name, final Function<Value, Value> fun)
    {
        functions.put(name, new AbstractFunction(1, name)
        {
            @Override
            public Value eval(final List<Value> parameters)
            {
                return fun.apply(parameters.get(0));
            }
        });
    }

    public void addImpureUnaryFunction(final String name, final Function<Value, Value> fun)
    {
        functions.put(name, new AbstractFunction(1, name)
        {
            @Override
            public boolean pure()
            {
                return false;
            }

            @Override
            public Value eval(final List<Value> parameters)
            {
                return fun.apply(parameters.get(0));
            }
        });
    }

    public void addBinaryFunction(final String name, final BiFunction<Value, Value, Value> fun)
    {
        functions.put(name, new AbstractFunction(2, name)
        {
            @Override
            public Value eval(final List<Value> parameters)
            {
                return fun.apply(parameters.get(0), parameters.get(1));
            }
        });
    }

    public void addFunction(final String name, final Function<List<Value>, Value> fun)
    {
        functions.put(name, new AbstractFunction(-1, name)
        {
            @Override
            public Value eval(final List<Value> parameters)
            {
                return fun.apply(parameters);
            }
        });
    }

    public void addImpureFunction(final String name, final Function<List<Value>, Value> fun)
    {
        functions.put(name, new AbstractFunction(-1, name)
        {
            @Override
            public boolean pure()
            {
                return false;
            }

            @Override
            public Value eval(final List<Value> parameters)
            {
                return fun.apply(parameters);
            }
        });
    }

    public void addMathematicalUnaryFunction(final String name, final DoubleUnaryOperator fun)
    {
        addUnaryFunction(name, (v) -> new NumericValue(fun.applyAsDouble(NumericValue.asNumber(v).getDouble())));
    }

    public void addMathematicalUnaryIntFunction(final String name, final DoubleToLongFunction fun)
    {
        addUnaryFunction(name, (v) -> new NumericValue(fun.applyAsLong(NumericValue.asNumber(v).getDouble())));
    }

    public void addMathematicalBinaryIntFunction(final String name, final BinaryOperator<Long> fun)
    {
        addBinaryFunction(name, (w, v) ->
                new NumericValue(fun.apply(NumericValue.asNumber(w).getLong(), NumericValue.asNumber(v).getLong())));
    }

    public void addMathematicalBinaryFunction(final String name, final BinaryOperator<Double> fun)
    {
        addBinaryFunction(name, (w, v) ->
                new NumericValue(fun.apply(NumericValue.asNumber(w).getDouble(), NumericValue.asNumber(v).getDouble())));
    }


    public void addLazyFunction(final String name, final int numParams, final TriFunction<Context, Context.Type, List<LazyValue>, LazyValue> fun)
    {
        functions.put(name, new AbstractLazyFunction(numParams, name)
        {
            @Override
            public boolean pure()
            {
                return false;
            }

            @Override
            public boolean transitive()
            {
                return false;
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type i, final Expression e, final Tokenizer.Token t, final List<LazyValue> lazyParams)
            {
                ILazyFunction.checkInterrupts();
                if (numParams >= 0 && lazyParams.size() != numParams)
                {
                    final String error = "Function '" + name + "' requires " + numParams + " arguments, got " + lazyParams.size() + ". ";
                    throw new InternalExpressionException(error + (fun instanceof Fluff.UsageProvider up ? up.getUsage() : ""));
                }

                try
                {
                    return fun.apply(c, i, lazyParams);
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addLazyFunction(final String name, final TriFunction<Context, Context.Type, List<LazyValue>, LazyValue> fun)
    {
        functions.put(name, new AbstractLazyFunction(-1, name)
        {
            @Override
            public boolean pure()
            {
                return false;
            }

            @Override
            public boolean transitive()
            {
                return false;
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type i, final Expression e, final Tokenizer.Token t, final List<LazyValue> lazyParams)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    return fun.apply(c, i, lazyParams);
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addPureLazyFunction(final String name, final int num_params, final Function<Context.Type, Context.Type> typer, final TriFunction<Context, Context.Type, List<LazyValue>, LazyValue> fun)
    {
        functions.put(name, new AbstractLazyFunction(num_params, name)
        {
            @Override
            public boolean pure()
            {
                return true;
            }

            @Override
            public boolean transitive()
            {
                return false;
            }

            @Override
            public Context.Type staticType(final Context.Type outerType)
            {
                return typer.apply(outerType);
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type i, final Expression e, final Tokenizer.Token t, final List<LazyValue> lazyParams)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    return fun.apply(c, i, lazyParams);
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addContextFunction(final String name, final int num_params, final TriFunction<Context, Context.Type, List<Value>, Value> fun)
    {
        functions.put(name, new AbstractLazyFunction(num_params, name)
        {
            @Override
            public boolean pure()
            {
                return false;
            }

            @Override
            public boolean transitive()
            {
                return false;
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type i, final Expression e, final Tokenizer.Token t, final List<LazyValue> lazyParams)
            {
                ILazyFunction.checkInterrupts();
                try
                {
                    final Value ret = fun.apply(c, i, unpackArgs(lazyParams, c, Context.NONE));
                    return (cc, tt) -> ret;
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addTypedContextFunction(final String name, final int num_params, final Context.Type reqType, final TriFunction<Context, Context.Type, List<Value>, Value> fun)
    {
        functions.put(name, new AbstractLazyFunction(num_params, name)
        {
            @Override
            public boolean pure()
            {
                return true;
            }

            @Override
            public boolean transitive()
            {
                return false;
            }

            @Override
            public Context.Type staticType(final Context.Type outerType)
            {
                return reqType;
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type i, final Expression e, final Tokenizer.Token t, final List<LazyValue> lazyParams)
            {
                try
                {
                    final Value ret = fun.apply(c, i, unpackArgs(lazyParams, c, reqType));
                    return (cc, tt) -> ret;
                }
                catch (final RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public FunctionValue createUserDefinedFunction(final Context context, final String name, final Expression expr, final Tokenizer.Token token, final List<String> arguments, final String varArgs, final List<String> outers, final LazyValue code)
    {
        if (functions.containsKey(name))
        {
            throw new ExpressionException(context, expr, token, "Function " + name + " would mask a built-in function");
        }
        Map<String, LazyValue> contextValues = new HashMap<>();
        for (final String outer : outers)
        {
            final LazyValue lv = context.getVariable(outer);
            if (lv == null)
            {
                throw new InternalExpressionException("Variable " + outer + " needs to be defined in outer scope to be used as outer parameter, and cannot be global");
            }
            else
            {
                contextValues.put(outer, lv);
            }
        }
        if (contextValues.isEmpty())
        {
            contextValues = null;
        }

        final FunctionValue result = new FunctionValue(expr, token, name, code, arguments, varArgs, contextValues);
        // do not store lambda definitions
        if (!name.equals("_"))
        {
            context.host.addUserDefinedFunction(context, module, name, result);
        }
        return result;
    }

    public void alias(final String copy, final String original)
    {
        final ILazyFunction originalFunction = functions.get(original);
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
            public boolean pure()
            {
                return originalFunction.pure();
            }

            @Override
            public boolean transitive()
            {
                return originalFunction.transitive();
            }

            @Override
            public Context.Type staticType(final Context.Type outerType)
            {
                return originalFunction.staticType(outerType);
            }

            @Override
            public LazyValue lazyEval(final Context c, final Context.Type type, final Expression expr, final Tokenizer.Token token, final List<LazyValue> lazyParams)
            {
                c.host.issueDeprecation(copy + "(...)");
                return originalFunction.lazyEval(c, type, expr, token, lazyParams);
            }
        });
    }


    public void setAnyVariable(final Context c, final String name, final LazyValue lv)
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

    public LazyValue getOrSetAnyVariable(final Context c, final String name)
    {
        LazyValue variable;
        if (!name.startsWith("global_"))
        {
            variable = c.getVariable(name);
            if (variable != null)
            {
                return variable;
            }
        }
        variable = c.host.getGlobalVariable(module, name);
        if (variable != null)
        {
            return variable;
        }
        variable = (_c, _t) -> _c.host.strict ? Value.UNDEF.reboundedTo(name) : Value.NULL.reboundedTo(name);
        setAnyVariable(c, name, variable);
        return variable;
    }

    public static final Expression none = new Expression("null");

    /**
     * @param expression .
     */
    public Expression(final String expression)
    {
        this.expression = expression.stripTrailing().replaceAll("\\r\\n?", "\n").replaceAll("\\t", "   ");
        Operators.apply(this);
        ControlFlow.apply(this);
        Functions.apply(this);
        Arithmetic.apply(this);
        Sys.apply(this);
        Threading.apply(this);
        Loops.apply(this);
        DataStructures.apply(this);
    }


    private List<Tokenizer.Token> shuntingYard(final Context c)
    {
        final List<Tokenizer.Token> outputQueue = new ArrayList<>();
        final Stack<Tokenizer.Token> stack = new ObjectArrayList<>();

        final Tokenizer tokenizer = new Tokenizer(c, this, expression, allowComments, allowNewlineSubstitutions);
        // stripping lousy but acceptable semicolons
        final List<Tokenizer.Token> cleanedTokens = tokenizer.postProcess();

        Tokenizer.Token lastFunction = null;
        Tokenizer.Token previousToken = null;
        for (final Tokenizer.Token token : cleanedTokens)
        {
            switch (token.type)
            {
                case STRINGPARAM:
                    //stack.push(token); // changed that so strings are treated like literals
                    //break;
                case LITERAL, HEX_LITERAL:
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
                    while (!stack.isEmpty() && stack.top().type != Tokenizer.Token.TokenType.OPEN_PAREN)
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
                        throw new ExpressionException(c, this, token, "Missing parameter(s) for operator '" + token + "'");
                    }
                    final ILazyOperator o1 = operators.get(token.surface);
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
                        throw new ExpressionException(c, this, token, "Invalid position for unary operator " + token);
                    }
                    final ILazyOperator o1 = operators.get(token.surface);
                    if (o1 == null)
                    {
                        throw new ExpressionException(c, this, token, "Unknown unary operator '" + token.surface.substring(0, token.surface.length() - 1) + "'");
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case OPEN_PAREN:
                    // removed implicit multiplication in this missing code block
                    if (previousToken != null && previousToken.type == Tokenizer.Token.TokenType.FUNCTION)
                    {
                        outputQueue.add(token);
                    }
                    stack.push(token);
                    break;
                case CLOSE_PAREN:
                    if (previousToken != null && previousToken.type == Tokenizer.Token.TokenType.OPERATOR)
                    {
                        throw new ExpressionException(c, this, previousToken, "Missing parameter(s) for operator " + previousToken);
                    }
                    while (!stack.isEmpty() && stack.top().type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        throw new ExpressionException(c, this, "Mismatched parentheses");
                    }
                    stack.pop();
                    if (!stack.isEmpty() && stack.top().type == Tokenizer.Token.TokenType.FUNCTION)
                    {
                        outputQueue.add(stack.pop());
                    }
                    break;
                case MARKER:
                    if ("$".equals(token.surface))
                    {
                        final StringBuilder sb = new StringBuilder(expression);
                        sb.setCharAt(token.pos, '\n');
                        expression = sb.toString();
                    }
                    break;
            }
            if (token.type != Tokenizer.Token.TokenType.MARKER)
            {
                previousToken = token;
            }
        }

        while (!stack.isEmpty())
        {
            final Tokenizer.Token element = stack.pop();
            if (element.type == Tokenizer.Token.TokenType.OPEN_PAREN || element.type == Tokenizer.Token.TokenType.CLOSE_PAREN)
            {
                throw new ExpressionException(c, this, element, "Mismatched parentheses");
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    private void shuntOperators(final List<Tokenizer.Token> outputQueue, final Stack<Tokenizer.Token> stack, final ILazyOperator o1)
    {
        Tokenizer.Token nextToken = stack.isEmpty() ? null : stack.top();
        while (nextToken != null
                && (nextToken.type == Tokenizer.Token.TokenType.OPERATOR
                || nextToken.type == Tokenizer.Token.TokenType.UNARY_OPERATOR)
                && ((o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(nextToken.surface).getPrecedence())
                || (o1.getPrecedence() < operators.get(nextToken.surface).getPrecedence())))
        {
            outputQueue.add(stack.pop());
            nextToken = stack.isEmpty() ? null : stack.top();
        }
    }

    public Value eval(final Context c)
    {
        if (ast == null)
        {
            ast = getAST(c);
        }
        return evalValue(() -> ast, c, Context.Type.NONE);
    }

    public Value evalValue(final Supplier<LazyValue> exprProvider, final Context c, final Context.Type expectedType)
    {
        try
        {
            return exprProvider.get().evalValue(c, expectedType);
        }
        catch (final ContinueStatement | BreakStatement | ReturnStatement exc)
        {
            throw new ExpressionException(c, this, "Control flow functions, like continue, break or return, should only be used in loops, and functions respectively.");
        }
        catch (final ExitStatement exit)
        {
            return exit.retval == null ? Value.NULL : exit.retval;
        }
        catch (final StackOverflowError ignored)
        {
            throw new ExpressionException(c, this, "Your thoughts are too deep");
        }
        catch (final InternalExpressionException exc)
        {
            throw new ExpressionException(c, this, "Your expression result is incorrect: " + exc.getMessage());
        }
        catch (final ArithmeticException exc)
        {
            throw new ExpressionException(c, this, "The final result is incorrect: " + exc.getMessage());
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

        public ExpressionNode(final LazyValue op, final List<ExpressionNode> args, final Tokenizer.Token token)
        {
            this.op = op;
            this.args = args;
            this.token = token;
            range = new ArrayList<>();
            range.add(token);
        }

        public static ExpressionNode ofConstant(final Value val, final Tokenizer.Token token)
        {
            return new ExpressionNode(new LazyValue.Constant(val), Collections.emptyList(), token);
        }
    }


    private ExpressionNode RPNToParseTree(final List<Tokenizer.Token> tokens, final Context context)
    {
        final Stack<ExpressionNode> nodeStack = new ObjectArrayList<>();
        for (final Tokenizer.Token token : tokens)
        {
            switch (token.type) {
                case UNARY_OPERATOR -> {
                    final ExpressionNode node = nodeStack.pop();
                    final LazyValue result = (c, t) -> operators.get(token.surface).lazyEval(c, t, this, token, node.op, null).evalValue(c, t);
                    nodeStack.push(new ExpressionNode(result, Collections.singletonList(node), token));
                }
                case OPERATOR -> {
                    final ExpressionNode v1 = nodeStack.pop();
                    final ExpressionNode v2 = nodeStack.pop();
                    final LazyValue result = (c, t) -> operators.get(token.surface).lazyEval(c, t, this, token, v2.op, v1.op).evalValue(c, t);
                    nodeStack.push(new ExpressionNode(result, List.of(v2, v1), token));
                }
                case VARIABLE -> {
                    final Value constant = getConstantFor(token.surface);
                    if (constant != null)
                    {
                        token.morph(Tokenizer.Token.TokenType.CONSTANT, token.surface);
                        nodeStack.push(new ExpressionNode(LazyValue.ofConstant(constant), Collections.emptyList(), token));
                    }
                    else
                    {
                        nodeStack.push(new ExpressionNode(((c, t) -> getOrSetAnyVariable(c, token.surface).evalValue(c, t)), Collections.emptyList(), token));
                    }
                }
                case FUNCTION -> {
                    final String name = token.surface;
                    final ILazyFunction f;
                    final ArrayList<ExpressionNode> p;
                    final boolean isKnown = functions.containsKey(name); // globals will be evaluated lazily, not at compile time via .
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
                    while (!nodeStack.isEmpty() && nodeStack.top() != ExpressionNode.PARAMS_START)
                    {
                        p.add(nodeStack.pop());
                    }
                    if (!isKnown)
                    {
                        p.add(ExpressionNode.ofConstant(new StringValue(name), token.morphedInto(Tokenizer.Token.TokenType.STRINGPARAM, token.surface)));
                        token.morph(Tokenizer.Token.TokenType.FUNCTION, "call");
                    }
                    Collections.reverse(p);
                    if (nodeStack.top() == ExpressionNode.PARAMS_START)
                    {
                        nodeStack.pop();
                    }
                    final List<LazyValue> params = p.stream().map(n -> n.op).collect(Collectors.toList());
                    nodeStack.push(new ExpressionNode(
                            (c, t) -> f.lazyEval(c, t, this, token, params).evalValue(c, t),
                            p, token
                    ));
                }
                case OPEN_PAREN -> nodeStack.push(ExpressionNode.PARAMS_START);
                case LITERAL -> {
                    final Value number;
                    try
                    {
                        number = new NumericValue(token.surface);
                    }
                    catch (final NumberFormatException exception)
                    {
                        throw new ExpressionException(context, this, token, "Not a number");
                    }
                    token.morph(Tokenizer.Token.TokenType.CONSTANT, token.surface);
                    nodeStack.push(ExpressionNode.ofConstant(number, token));
                }
                case STRINGPARAM -> {
                    token.morph(Tokenizer.Token.TokenType.CONSTANT, token.surface);
                    nodeStack.push(ExpressionNode.ofConstant(new StringValue(token.surface), token));
                }
                case HEX_LITERAL -> {
                    final Value hexNumber;
                    try
                    {
                        hexNumber = new NumericValue(new BigInteger(token.surface.substring(2), 16).longValue());
                    }
                    catch (final NumberFormatException exception)
                    {
                        throw new ExpressionException(context, this, token, "Not a number");
                    }
                    token.morph(Tokenizer.Token.TokenType.CONSTANT, token.surface);
                    nodeStack.push(ExpressionNode.ofConstant(hexNumber, token));
                }
                default -> throw new ExpressionException(context, this, token, "Unexpected token '" + token.surface + "'");
            }
        }
        return nodeStack.pop();
    }

    private LazyValue getAST(final Context context)
    {
        final List<Tokenizer.Token> rpn = shuntingYard(context);
        validate(context, rpn);
        final ExpressionNode root = RPNToParseTree(rpn, context);
        if (!Vanilla.ScriptServer_scriptOptimizations(((CarpetScriptServer)context.scriptServer()).server))
        {
            return root.op;
        }

        final Context optimizeOnlyContext = new Context.ContextForErrorReporting(context);
        final boolean scriptsDebugging = Vanilla.ScriptServer_scriptDebugging(((CarpetScriptServer)context.scriptServer()).server);
        if (scriptsDebugging)
        {
            CarpetScriptServer.LOG.info("Input code size for " + getModuleName() + ": " + treeSize(root) + " nodes, " + treeDepth(root) + " deep");
        }

        // Defined out here to not need to conditionally assign them with debugging disabled
        int prevTreeSize = -1;
        int prevTreeDepth = -1;

        boolean changed = true;
        while (changed)
        {
            changed = false;
            while (true)
            {
                if (scriptsDebugging)
                {
                    prevTreeSize = treeSize(root);
                    prevTreeDepth = treeDepth(root);
                }
                final boolean optimized = compactTree(root, Context.Type.NONE, 0, scriptsDebugging);
                if (!optimized)
                {
                    break;
                }
                changed = true;
                if (scriptsDebugging)
                {
                    CarpetScriptServer.LOG.info("Compacted from " + prevTreeSize + " nodes, " + prevTreeDepth + " code depth to " + treeSize(root) + " nodes, " + treeDepth(root) + " code depth");
                }
            }
            while (true)
            {
                if (scriptsDebugging)
                {
                    prevTreeSize = treeSize(root);
                    prevTreeDepth = treeDepth(root);
                }
                final boolean optimized = optimizeTree(optimizeOnlyContext, root, Context.Type.NONE, 0, scriptsDebugging);
                if (!optimized)
                {
                    break;
                }
                changed = true;
                if (scriptsDebugging)
                {
                    CarpetScriptServer.LOG.info("Optimized from " + prevTreeSize + " nodes, " + prevTreeDepth + " code depth to " + treeSize(root) + " nodes, " + treeDepth(root) + " code depth");
                }
            }
        }
        return extractOp(optimizeOnlyContext, root, Context.Type.NONE);
    }

    private int treeSize(final ExpressionNode node)
    {
        return node.op instanceof LazyValue.ContextFreeLazyValue ? 1 : node.args.stream().mapToInt(this::treeSize).sum() + 1;
    }

    private int treeDepth(final ExpressionNode node)
    {
        return node.op instanceof LazyValue.ContextFreeLazyValue ? 1 : node.args.stream().mapToInt(this::treeDepth).max().orElse(0) + 1;
    }


    private boolean compactTree(final ExpressionNode node, final Context.Type expectedType, final int indent, final boolean scriptsDebugging)
    {
        // ctx is just to report errors, not values evaluation
        boolean optimized = false;
        final Tokenizer.Token.TokenType token = node.token.type;
        if (!token.isFunctional())
        {
            return false;
        }
        // input special cases here, like function signature
        if (node.op instanceof LazyValue.Constant)
        {
            return false; // optimized already
        }
        // function or operator
        final String symbol = node.token.surface;
        final Fluff.EvalNode operation = ((token == Tokenizer.Token.TokenType.FUNCTION) ? functions : operators).get(symbol);
        final Context.Type requestedType = operation.staticType(expectedType);
        for (final ExpressionNode arg : node.args)
        {
            if (compactTree(arg, requestedType, indent + 1, scriptsDebugging))
            {
                optimized = true;
            }
        }

        if (expectedType != Context.Type.MAPDEF && symbol.equals("->") && node.args.size() == 2)
        {
            final String rop = node.args.get(1).token.surface;
            ExpressionNode returnNode = null;
            if ((rop.equals(";") || rop.equals("then")))
            {
                final List<ExpressionNode> thenArgs = node.args.get(1).args;
                if (thenArgs.size() > 1 && thenArgs.get(thenArgs.size() - 1).token.surface.equals("return"))
                {
                    returnNode = thenArgs.get(thenArgs.size() - 1);
                }
            }
            else if (rop.equals("return"))
            {
                returnNode = node.args.get(1);
            }
            if (returnNode != null) // tail return
            {
                if (!returnNode.args.isEmpty())
                {
                    returnNode.op = returnNode.args.get(0).op;
                    returnNode.token = returnNode.args.get(0).token;
                    returnNode.range = returnNode.args.get(0).range;
                    returnNode.args = returnNode.args.get(0).args;
                    if (scriptsDebugging)
                    {
                        CarpetScriptServer.LOG.info(" - Removed unnecessary tail return of " + returnNode.token.surface + " from function body at line " + (returnNode.token.lineno + 1) + ", node depth " + indent);
                    }
                }
                else
                {
                    returnNode.op = LazyValue.ofConstant(Value.NULL);
                    returnNode.token.morph(Tokenizer.Token.TokenType.CONSTANT, "");
                    returnNode.args = Collections.emptyList();
                    if (scriptsDebugging)
                    {
                        CarpetScriptServer.LOG.info(" - Removed unnecessary tail return from function body at line " + (returnNode.token.lineno + 1) + ", node depth " + indent);
                    }
                }

            }
        }
        for (final Map.Entry<String, String> pair : functionalEquivalence.entrySet())
        {
            final String operator = pair.getKey();
            final String function = pair.getValue();
            if ((symbol.equals(operator) || symbol.equals(function)) && node.args.size() > 0)
            {
                final boolean leftOptimizable = operators.get(operator).isLeftAssoc();
                final ExpressionNode optimizedChild = node.args.get(leftOptimizable ? 0 : (node.args.size() - 1));
                final String type = optimizedChild.token.surface;
                if ((type.equals(operator) || type.equals(function)) && (!(optimizedChild.op instanceof LazyValue.ContextFreeLazyValue)))
                {
                    optimized = true;
                    final List<ExpressionNode> newargs = new ArrayList<>();
                    if (leftOptimizable)
                    {
                        newargs.addAll(optimizedChild.args);
                        for (int i = 1; i < node.args.size(); i++)
                        {
                            newargs.add(node.args.get(i));
                        }
                    }
                    else
                    {
                        for (int i = 0; i < node.args.size() - 1; i++)
                        {
                            newargs.add(node.args.get(i));
                        }
                        newargs.addAll(optimizedChild.args);
                    }

                    if (scriptsDebugging)
                    {
                        CarpetScriptServer.LOG.info(" - " + symbol + "(" + node.args.size() + ") => " + function + "(" + newargs.size() + ") at line " + (node.token.lineno + 1) + ", node depth " + indent);
                    }
                    node.token.morph(Tokenizer.Token.TokenType.FUNCTION, function);
                    node.args = newargs;
                }
            }
        }
        return optimized;
    }

    private boolean optimizeTree(final Context ctx, final ExpressionNode node, Context.Type expectedType, final int indent, final boolean scriptsDebugging)
    {
        // ctx is just to report errors, not values evaluation
        boolean optimized = false;
        final Tokenizer.Token.TokenType token = node.token.type;
        if (!token.isFunctional())
        {
            return false;
        }
        final String symbol = node.token.surface;

        // input special cases here, like function signature
        if (node.op instanceof LazyValue.Constant)
        {
            return false; // optimized already
        }
        // function or operator

        final Fluff.EvalNode operation = ((token == Tokenizer.Token.TokenType.FUNCTION) ? functions : operators).get(symbol);
        final Context.Type requestedType = operation.staticType(expectedType);
        for (final ExpressionNode arg : node.args)
        {
            if (optimizeTree(ctx, arg, requestedType, indent + 1, scriptsDebugging))
            {
                optimized = true;
            }
        }

        for (final ExpressionNode arg : node.args)
        {
            if (arg.token.type.isConstant())
            {
                continue;
            }
            if (arg.op instanceof LazyValue.ContextFreeLazyValue)
            {
                continue;
            }
            return optimized;
        }
        // a few exceptions which we don't implement in the framework for simplicity for now
        if (!operation.pure())
        {
            if (!symbol.equals("->") || expectedType != Context.Type.MAPDEF)
            {
                return optimized;
            }
        }
        // element access with constant elements will always resolve the same way.
        if (operation.pure() && symbol.equals(":") && expectedType == Context.Type.LVALUE)
        {
            expectedType = Context.Type.NONE;
        }
        List<LazyValue> args = new ArrayList<>(node.args.size());
        for (final ExpressionNode arg : node.args)
        {
            try
            {
                if (arg.op instanceof LazyValue.Constant)
                {
                    final Value val = ((LazyValue.Constant) arg.op).get();
                    args.add((c, t) -> val);
                }
                else
                {
                    args.add((c, t) -> arg.op.evalValue(ctx, requestedType));
                }
            }
            catch (final NullPointerException npe)
            {
                throw new ExpressionException(ctx, this, node.token, "Attempted to evaluate context free expression");
            }
        }
        // applying argument unpacking
        args = AbstractLazyFunction.lazify(AbstractLazyFunction.unpackLazy(args, ctx, requestedType));
        final Value result;
        if (operation instanceof ILazyFunction)
        {
            result = ((ILazyFunction) operation).lazyEval(ctx, expectedType, this, node.token, args).evalValue(null, expectedType);
        }
        else if (args.size() == 1)
        {
            result = ((ILazyOperator) operation).lazyEval(ctx, expectedType, this, node.token, args.get(0), null).evalValue(null, expectedType);
        }
        else // args == 2
        {
            result = ((ILazyOperator) operation).lazyEval(ctx, expectedType, this, node.token, args.get(0), args.get(1)).evalValue(null, expectedType);
        }
        node.op = LazyValue.ofConstant(result);
        if (scriptsDebugging)
        {
            CarpetScriptServer.LOG.info(" - " + symbol + "(" + args.stream().map(a -> a.evalValue(null, requestedType).getString()).collect(Collectors.joining(", ")) + ") => " + result.getString() + " at line " + (node.token.lineno + 1) + ", node depth " + indent);
        }
        return true;
    }

    private LazyValue extractOp(final Context ctx, final ExpressionNode node, final Context.Type expectedType)
    {
        if (node.op instanceof LazyValue.Constant)
        {
            // constants are immutable
            if (node.token.type.isConstant())
            {
                final Value value = ((LazyValue.Constant) node.op).get();
                return (c, t) -> value;
            }
            return node.op;
        }
        if (node.op instanceof LazyValue.ContextFreeLazyValue)
        {
            final Value ret = ((LazyValue.ContextFreeLazyValue) node.op).evalType(expectedType);
            return (c, t) -> ret;
        }
        final Tokenizer.Token token = node.token;
        switch (token.type)
        {
            case UNARY_OPERATOR:
            {
                final ILazyOperator op = operators.get(token.surface);
                final Context.Type requestedType = op.staticType(expectedType);
                final LazyValue arg = extractOp(ctx, node.args.get(0), requestedType);
                return (c, t) -> op.lazyEval(c, t, this, token, arg, null).evalValue(c, t);
            }
            case OPERATOR:
            {
                final ILazyOperator op = operators.get(token.surface);
                final Context.Type requestedType = op.staticType(expectedType);
                final LazyValue arg = extractOp(ctx, node.args.get(0), requestedType);
                final LazyValue arh = extractOp(ctx, node.args.get(1), requestedType);
                return (c, t) -> op.lazyEval(c, t, this, token, arg, arh).evalValue(c, t);
            }
            case VARIABLE:
                return (c, t) -> getOrSetAnyVariable(c, token.surface).evalValue(c, t);
            case FUNCTION:
            {
                final ILazyFunction f = functions.get(token.surface);
                final Context.Type requestedType = f.staticType(expectedType);
                final List<LazyValue> params = node.args.stream().map(n -> extractOp(ctx, n, requestedType)).collect(Collectors.toList());
                return (c, t) -> f.lazyEval(c, t, this, token, params).evalValue(c, t);
            }
            case CONSTANT:
                return node.op;
            default:
                throw new ExpressionException(ctx, this, node.token, "Unexpected token '" + node.token.type + " " + node.token.surface + "'");

        }
    }

    private void validate(final Context c, final List<Tokenizer.Token> rpn)
    {
        /*-
         * Thanks to Norman Ramsey:
         * http://http://stackoverflow.com/questions/789847/postfix-notation-validation
         */
        // each push on to this stack is a new function scope, with the value of
        // each
        // layer on the stack being the count of the number of parameters in
        // that scope
        final IntArrayList stack = new IntArrayList(); // IntArrayList instead of just IntStack because we need to query the size

        // push the 'global' scope
        stack.push(0);

        for (final Tokenizer.Token token : rpn)
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                    if (stack.topInt() < 1)
                    {
                        throw new ExpressionException(c, this, token, "Missing parameter(s) for operator " + token);
                    }
                    break;
                case OPERATOR:
                    if (stack.topInt() < 2)
                    {
                        if (token.surface.equals(";"))
                        {
                            throw new ExpressionException(c, this, token, "Empty expression found for ';'");
                        }
                        throw new ExpressionException(c, this, token, "Missing parameter(s) for operator " + token);
                    }
                    // pop the operator's 2 parameters and add the result
                    stack.set(stack.size() - 1, stack.topInt() - 2 + 1);
                    break;
                case FUNCTION:
                    //ILazyFunction f = functions.get(token.surface);// don't validate global - userdef functions
                    //int numParams = stack.pop();
                    //if (f != null && !f.numParamsVaries() && numParams != f.getNumParams())
                    //{
                    //    throw new ExpressionException(c, this, token, "Function " + token + " expected " + f.getNumParams() + " parameters, got " + numParams);
                    //}
                    stack.popInt();
                    // due to unpacking, all functions can have variable number of arguments
                    // we will be checking that at runtime.
                    // TODO try analyze arguments and assess if its possible that they are static
                    if (stack.size() <= 0)
                    {
                        throw new ExpressionException(c, this, token, "Too many function calls, maximum scope exceeded");
                    }
                    // push the result of the function
                    stack.set(stack.size() - 1, stack.topInt() + 1);
                    break;
                case OPEN_PAREN:
                    stack.push(0);
                    break;
                default:
                    stack.set(stack.size() - 1, stack.topInt() + 1);
            }
        }

        if (stack.size() > 1)
        {
            throw new ExpressionException(c, this, "Too many unhandled function parameter lists");
        }
        else if (stack.topInt() > 1)
        {
            throw new ExpressionException(c, this, "Too many numbers or variables");
        }
        else if (stack.topInt() < 1)
        {
            throw new ExpressionException(c, this, "Empty expression");
        }
    }
}
