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
import carpet.script.argument.FunctionArgument;
import carpet.script.bundled.Module;
import carpet.script.exception.BreakStatement;
import carpet.script.exception.ContinueStatement;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ResolvedException;
import carpet.script.exception.ReturnStatement;
import carpet.script.exception.ThrowStatement;
import carpet.script.utils.PerlinNoiseSampler;
import carpet.script.utils.SimplexNoiseSampler;
import carpet.script.value.AbstractListValue;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.FunctionSignatureValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.GlobalValue;
import carpet.script.value.LContainerValue;
import carpet.script.value.LazyListValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang3.text.WordUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class Expression
{
    private static final Map<String, Integer> precedence = new HashMap<String,Integer>() {{
        put("attribute~:", 80);
        put("unary+-!", 60);
        put("exponent^", 40);
        put("multiplication*/%", 30);
        put("addition+-", 20);
        put("compare>=><=<", 10);
        put("equal==!=", 7);
        put("and&&", 5);
        put("or||", 4);
        put("assign=<>", 3);
        put("def->", 2);
        put("nextop;", 1);
    }};
    public static final Random randomizer = new Random();

    public static final Value PI = new NumericValue(
            "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

    public static final Value euler = new NumericValue(
            "2.71828182845904523536028747135266249775724709369995957496696762772407663");

    // %[argument_index$][flags][width][.precision][t]conversion
    private static final Pattern formatPattern = Pattern.compile("%(\\d+\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

    /** The current infix expression */
    private String expression;
    String getCodeString() {return expression;}

    private boolean allowNewlineSubstitutions = true;
    private boolean allowComments = false;

    public Module module = null;

    public String getModuleName()
    {
        return module == null?null:module.getName();
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

    public List<String> getExpressionSnippet(Tokenizer.Token token)
    {
        String code = this.getCodeString();
        List<String> output = new ArrayList<>();
        for (String line: getExpressionSnippetLeftContext(token, code, 1))
        {
            output.add(line);
        }
        List<String> context = getExpressionSnippetContext(token, code);
        output.add(context.get(0)+" HERE>> "+context.get(1));
        for (String line: getExpressionSnippetRightContext(token, code, 1))
        {
            output.add(line);
        }
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


    public void addLazyUnaryOperator(String surface, int precedence, boolean leftAssoc,
                                       TriFunction<Context, Integer, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface+"u", new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer t, Expression e, Tokenizer.Token token, LazyValue v, LazyValue v2)
            {
                try
                {
                    if (v2 != null)
                    {
                        throw new ExpressionException(c, e, token, "Did not expect a second parameter for unary operator");
                    }
                    Value.assertNotNull(v);
                    return lazyfun.apply(c, t, v);
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, token);
                }
            }
        });
    }


    public void addLazyBinaryOperatorWithDelegation(String surface, int precedence, boolean leftAssoc,
                                       SexFunction<Context, Integer, Expression, Tokenizer.Token, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2)
            {
                try
                {
                    Value.assertNotNull(v1, v2);
                    return lazyfun.apply(c, type, e, t, v1, v2);
                }
                catch (RuntimeException exc)
                {
                    throw handleCodeException(c, exc, e, t);
                }
            }
        });
    }

    public void addLazyFunctionWithDelegation(String name, int numpar,
                                                     QuinnFunction<Context, Integer, Expression, Tokenizer.Token, List<LazyValue>, LazyValue> lazyfun)
    {
        functions.put(name, new AbstractLazyFunction(numpar)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, List<LazyValue> lv)
            {
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

    public void addLazyBinaryOperator(String surface, int precedence, boolean leftAssoc,
                                       QuadFunction<Context, Integer, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer t, Expression e, Tokenizer.Token token, LazyValue v1, LazyValue v2)
            {
                try
                {
                    Value.assertNotNull(v1, v2);
                    return lazyfun.apply(c, t, v1, v2);
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
        if (exc instanceof InternalExpressionException)
            return new ExpressionException(c, e, token, exc.getMessage(), ((InternalExpressionException) exc).stack);
        if (exc instanceof ArithmeticException)
            return new ExpressionException(c, e, token, "Your math is wrong, "+exc.getMessage());
        if (exc instanceof ResolvedException)
            return exc;
        // unexpected really - should be caught earlier and converted to InternalExpressionException
        exc.printStackTrace();
        return new ExpressionException(c, e, token, "Error while evaluating expression: "+exc);
    }

    public void addUnaryOperator(String surface, boolean leftAssoc, Function<Value, Value> fun)
    {
        operators.put(surface+"u", new AbstractUnaryOperator(precedence.get("unary+-!"), leftAssoc)
        {
            @Override
            public Value evalUnary(Value v1)
            {
                return fun.apply(Value.assertNotNull(v1));
            }
        });
    }

    public void addBinaryOperator(String surface, int precedence, boolean leftAssoc, BiFunction<Value, Value, Value> fun)
    {
        operators.put(surface, new AbstractOperator(precedence, leftAssoc)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                Value.assertNotNull(v1, v2);
                return fun.apply(v1, v2);
            }
        });
    }


    public void addUnaryFunction(String name, Function<Value, Value> fun)
    {
        functions.put(name,  new AbstractFunction(1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return fun.apply(Value.assertNotNull(parameters.get(0)));
            }
        });
    }

    public void addBinaryFunction(String name, BiFunction<Value, Value, Value> fun)
    {
        functions.put(name, new AbstractFunction(2)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                Value v1 = parameters.get(0);
                Value v2 = parameters.get(1);
                Value.assertNotNull(v1, v2);
                return fun.apply(v1, v2);
            }
        });
    }

    public void addFunction(String name, Function<List<Value>, Value> fun)
    {
        functions.put(name, new AbstractFunction(-1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                for (Value v: parameters)
                    Value.assertNotNull(v);
                return fun.apply(parameters);
            }
        });
    }

    public void addMathematicalUnaryFunction(String name, Function<Double, Double> fun)
    {
        addUnaryFunction(name, (v) -> new NumericValue(fun.apply(NumericValue.asNumber(v).getDouble())));
    }

    public void addMathematicalBinaryFunction(String name, BiFunction<Double, Double, Double> fun)
    {
        addBinaryFunction(name, (w, v) ->
                new NumericValue(fun.apply(NumericValue.asNumber(w).getDouble(), NumericValue.asNumber(v).getDouble())));
    }


    public void addLazyFunction(String name, int num_params, TriFunction<Context, Integer, List<LazyValue>, LazyValue> fun)
    {
        functions.put(name, new AbstractLazyFunction(num_params)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer i, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
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
    public FunctionValue addContextFunction(Context context, String name, Expression expr, Tokenizer.Token token, List<String> arguments, List<String> outers, LazyValue code)
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

        FunctionValue result =  new FunctionValue(expr, token, name, code, arguments, contextValues);
        // do not store lambda definitions
        if (!name.equals("_")) context.host.addUserDefinedFunction(context, module, name, result);
        return result;
    }

    public void alias(String copy, String original)
    {
        functions.put(copy, functions.get(original));
    }


    private void UserDefinedFunctionsAndControlFlow() // public just to get the javadoc right
    {
        // artificial construct to handle user defined functions and function definitions
        addLazyFunction("import", -1, (c, t, lv) ->
        {
            if (lv.size() < 1) throw new InternalExpressionException("'import' needs at least a module name to import, and list of values to import");
            String moduleName = lv.get(0).evalValue(c).getString();
            c.host.importModule(c, moduleName);
            moduleName = moduleName.toLowerCase(Locale.ROOT);
            if (lv.size() > 1)
                c.host.importNames(c, module, moduleName, lv.subList(1, lv.size()).stream().map((l) -> l.evalValue(c).getString()).collect(Collectors.toList()));
            if (t == Context.VOID)
                return LazyValue.NULL;
            ListValue list = ListValue.wrap(c.host.availableImports(moduleName).map(StringValue::new).collect(Collectors.toList()));
            return (cc, tt) -> list;
        });

        addLazyFunctionWithDelegation("call",-1, (c, t, expr, tok, lv) -> { // adjust based on c
            if (lv.size() == 0)
                throw new InternalExpressionException("'call' expects at least function name to call");
            //lv.remove(lv.size()-1); // aint gonna cut it // maybe it will because of the eager eval changes
            if (t != Context.SIGNATURE) // just call the function
            {
                FunctionArgument functionArgument = FunctionArgument.findIn(c, module, lv, 0, false, true);
                FunctionValue fun = functionArgument.function;
                Value retval = fun.callInContext(expr, c, t, fun.getExpression(), fun.getToken(), functionArgument.args).evalValue(c);
                return (cc, tt) -> retval; ///!!!! dono might need to store expr and token in statics? (e? t?)
            }
            // gimme signature
            String name = lv.get(0).evalValue(c).getString();
            List<String> args = new ArrayList<>();
            List<String> globals = new ArrayList<>();
            for (int i = 1; i < lv.size(); i++)
            {
                Value v = lv.get(i).evalValue(c, Context.LOCALIZATION);
                if (!v.isBound())
                {
                    throw new InternalExpressionException("Only variables can be used in function signature, not  " + v.getString());
                }
                if (v instanceof GlobalValue)
                {
                    globals.add(v.boundVariable);
                }
                else
                {
                    args.add(v.boundVariable);
                }
            }
            Value retval = new FunctionSignatureValue(name, args, globals);
            return (cc, tt) -> retval;
        });
        addLazyFunction("outer", 1, (c, t, lv) -> {
            if (t != Context.LOCALIZATION)
                throw new InternalExpressionException("Outer scoping of variables is only possible in function signatures");
            return (cc, tt) -> new GlobalValue(lv.get(0).evalValue(c));
        });

        addLazyBinaryOperator(";",precedence.get("nextop;"), true, (c, t, lv1, lv2) ->
        {
            lv1.evalValue(c, Context.VOID);
            Value v2 = lv2.evalValue(c, t);
            return (cc, tt) -> v2;
        });

        //assigns const procedure to the lhs, returning its previous value
        addLazyBinaryOperatorWithDelegation("->", precedence.get("def->"), false, (c, type, e, t, lv1, lv2) ->
        {
            if (type == Context.MAPDEF)
            {
                Value result = ListValue.of(lv1.evalValue(c), lv2.evalValue(c));
                return (cc, tt) -> result;
            }
            Value v1 = lv1.evalValue(c, Context.SIGNATURE);
            if (!(v1 instanceof FunctionSignatureValue))
                throw new InternalExpressionException("'->' operator requires a function signature on the LHS");
            FunctionSignatureValue sign = (FunctionSignatureValue) v1;
            Value result = addContextFunction(c, sign.getName(), e, t, sign.getArgs(), sign.getGlobals(), lv2);
            return (cc, tt) -> result;
        });

        addFunction("exit", (lv) -> { throw new ExitStatement(lv.size()==0?Value.NULL:lv.get(0)); });
        addFunction("return", (lv) -> { throw new ReturnStatement(lv.size()==0?Value.NULL:lv.get(0));} );
        addFunction("throw", (lv)-> {throw new ThrowStatement(lv.size()==0?Value.NULL:lv.get(0)); });

        addLazyFunction("try", -1, (c, t, lv) ->
        {
            if (lv.size()==0)
                throw new InternalExpressionException("'try' needs at least an expression block");
            try
            {
                Value retval = lv.get(0).evalValue(c, t);
                return (c_, t_) -> retval;
            }
            catch (ThrowStatement ret)
            {
                if (lv.size() == 1)
                    return (c_, t_) -> Value.NULL;
                LazyValue __ = c.getVariable("_");
                c.setVariable("_", (__c, __t) -> ret.retval.reboundedTo("_"));
                Value val = lv.get(1).evalValue(c, t);
                c.setVariable("_",__);
                return (c_, t_) -> val;
            }
        });

        // if(cond1, expr1, cond2, expr2, ..., ?default) => value
        addLazyFunction("if", -1, (c, t, lv) ->
        {
            if ( lv.size() < 2 )
                throw new InternalExpressionException("'if' statement needs to have at least one condition and one case");
            for (int i=0; i<lv.size()-1; i+=2)
            {
                if (lv.get(i).evalValue(c, Context.BOOLEAN).getBoolean())
                {
                    //int iFinal = i;
                    Value ret = lv.get(i+1).evalValue(c, t);
                    return (cc, tt) -> ret;
                }
            }
            if (lv.size()%2 == 1)
            {
                Value ret = lv.get(lv.size() - 1).evalValue(c, t);
                return (cc, tt) -> ret;
            }
            return (cc, tt) -> Value.NULL;
        });
    }

    private void Operators()
    {
        addBinaryOperator("+", precedence.get("addition+-"), true, Value::add);
        addBinaryOperator("-", precedence.get("addition+-"), true, Value::subtract);
        addBinaryOperator("*", precedence.get("multiplication*/%"), true, Value::multiply);
        addBinaryOperator("/", precedence.get("multiplication*/%"), true, Value::divide);
        addBinaryOperator("%", precedence.get("multiplication*/%"), true, (v1, v2) ->
                new NumericValue(NumericValue.asNumber(v1).getDouble() % NumericValue.asNumber(v2).getDouble()));
        addBinaryOperator("^", precedence.get("exponent^"), false, (v1, v2) ->
                new NumericValue(Math.pow(NumericValue.asNumber(v1).getDouble(), NumericValue.asNumber(v2).getDouble())));

        addLazyBinaryOperator("&&", precedence.get("and&&"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.BOOLEAN);
            if (!v1.getBoolean()) return (cc, tt) -> v1;
            return lv2;
        });

        addLazyBinaryOperator("||", precedence.get("or||"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.BOOLEAN);
            if (v1.getBoolean()) return (cc, tt) -> v1;
            return lv2;
        });

        addBinaryOperator("~", precedence.get("attribute~:"), true, Value::in);

        addBinaryOperator(">", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) > 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator(">=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) >= 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("<", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) < 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("<=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) <= 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("==", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.TRUE : Value.FALSE);
        addBinaryOperator("!=", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.FALSE : Value.TRUE);

        addLazyBinaryOperator("=", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.LVALUE);
            Value v2 = lv2.evalValue(c);
            if (v1 instanceof ListValue.ListConstructorValue && v2 instanceof ListValue)
            {
                List<Value> ll = ((ListValue)v1).getItems();
                List<Value> rl = ((ListValue)v2).getItems();
                if (ll.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                if (ll.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                for (Value v: ll) v.assertAssignable();
                Iterator<Value> li = ll.iterator();
                Iterator<Value> ri = rl.iterator();
                while(li.hasNext())
                {
                    String lname = li.next().getVariable();
                    Value vval = ri.next().reboundedTo(lname);
                    setAnyVariable(c, lname, (cc, tt) -> vval);
                }
                return (cc, tt) -> Value.TRUE;
            }
            if (v1 instanceof LContainerValue)
            {
                ContainerValueInterface container = ((LContainerValue) v1).getContainer();
                if (container == null)
                    return (cc, tt) -> Value.NULL;
                Value address = ((LContainerValue) v1).getAddress();
                if (!(container.put(address, v2))) return (cc, tt) -> Value.NULL;
                return (cc, tt) -> v2;
            }
            v1.assertAssignable();
            String varname = v1.getVariable();
            Value copy = v2.reboundedTo(varname);
            LazyValue boundedLHS = (cc, tt) -> copy;
            setAnyVariable(c, varname, boundedLHS);
            return boundedLHS;
        });

        addLazyBinaryOperator("+=", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.LVALUE);
            Value v2 = lv2.evalValue(c);
            if (v1 instanceof ListValue.ListConstructorValue && v2 instanceof ListValue)
            {
                List<Value> ll = ((ListValue)v1).getItems();
                List<Value> rl = ((ListValue)v2).getItems();
                if (ll.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                if (ll.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                for (Value v: ll) v.assertAssignable();
                Iterator<Value> li = ll.iterator();
                Iterator<Value> ri = rl.iterator();
                while(li.hasNext())
                {
                    Value lval = li.next();
                    String lname = lval.getVariable();
                    Value result = lval.add(ri.next()).bindTo(lname);
                    setAnyVariable(c, lname, (cc, tt) -> result);
                }
                return (cc, tt) -> Value.TRUE;
            }
            if (v1 instanceof LContainerValue)
            {
                ContainerValueInterface cvi = ((LContainerValue) v1).getContainer();
                if (cvi == null)
                {
                    throw new InternalExpressionException("Failed to resolve left hand side of the += operation");
                }
                Value key = ((LContainerValue) v1).getAddress();
                Value value = cvi.get(key);
                if (value instanceof ListValue || value instanceof MapValue)
                {
                    ((AbstractListValue) value).append(v2);
                    return (cc, tt) -> value;
                }
                else
                {
                    Value res = value.add(v2);
                    cvi.put(key, res);
                    return (cc, tt) -> res;
                }
            }
            v1.assertAssignable();
            String varname = v1.getVariable();
            LazyValue boundedLHS;
            if (v1 instanceof ListValue || v1 instanceof MapValue)
            {
                ((AbstractListValue) v1).append(v2);
                boundedLHS = (cc, tt)-> v1;
            }
            else
            {
                Value result = v1.add(v2).bindTo(varname);
                boundedLHS = (cc, tt) -> result;
            }
            setAnyVariable(c, varname, boundedLHS);
            return boundedLHS;
        });

        addLazyBinaryOperator("<>", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c);
            Value v2 = lv2.evalValue(c);
            if (v1 instanceof ListValue.ListConstructorValue && v2 instanceof ListValue.ListConstructorValue)
            {
                List<Value> ll = ((ListValue)v1).getItems();
                List<Value> rl = ((ListValue)v2).getItems();
                if (ll.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                if (ll.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                for (Value v: ll) v.assertAssignable();
                for (Value v: rl) v.assertAssignable();
                Iterator<Value> li = ll.iterator();
                Iterator<Value> ri = rl.iterator();
                while(li.hasNext())
                {
                    Value lval = li.next();
                    Value rval = ri.next();
                    String lname = lval.getVariable();
                    String rname = rval.getVariable();
                    lval.reboundedTo(rname);
                    rval.reboundedTo(lname);
                    setAnyVariable(c, lname, (cc, tt) -> rval);
                    setAnyVariable(c, rname, (cc, tt) -> lval);
                }
                return (cc, tt) -> Value.TRUE;
            }
            v1.assertAssignable();
            v2.assertAssignable();
            String lvalvar = v1.getVariable();
            String rvalvar = v2.getVariable();
            Value lval = v2.reboundedTo(lvalvar);
            Value rval = v1.reboundedTo(rvalvar);
            setAnyVariable(c, lvalvar, (cc, tt) -> lval);
            setAnyVariable(c, rvalvar, (cc, tt) -> rval);
            return (cc, tt) -> lval;
        });

        addUnaryOperator("-",  false, (v) -> new NumericValue(-NumericValue.asNumber(v).getDouble()));

        addUnaryOperator("+", false, (v) -> new NumericValue(NumericValue.asNumber(v).getDouble()));

        addLazyUnaryOperator("!", precedence.get("unary+-!"), false, (c, t, lv)-> lv.evalValue(c, Context.BOOLEAN).getBoolean() ? (cc, tt)-> Value.FALSE : (cc, tt) -> Value.TRUE); // might need context boolean

    }

    private void ArithmeticOperations()
    {
        addLazyFunction("not", 1, (c, t, lv) -> lv.get(0).evalValue(c, Context.BOOLEAN).getBoolean() ? ((cc, tt) -> Value.FALSE) : ((cc, tt) -> Value.TRUE));

        addUnaryFunction("fact", (v) ->
        {
            long number = NumericValue.asNumber(v).getLong();
            long factorial = 1;
            for (int i = 1; i <= number; i++)
            {
                factorial = factorial * i;
            }
            return new NumericValue(factorial);
        });
        addMathematicalUnaryFunction("sin",    (d) -> Math.sin(Math.toRadians(d)));
        addMathematicalUnaryFunction("cos",    (d) -> Math.cos(Math.toRadians(d)));
        addMathematicalUnaryFunction("tan",    (d) -> Math.tan(Math.toRadians(d)));
        addMathematicalUnaryFunction("asin",   (d) -> Math.toDegrees(Math.asin(d)));
        addMathematicalUnaryFunction("acos",   (d) -> Math.toDegrees(Math.acos(d)));
        addMathematicalUnaryFunction("atan",   (d) -> Math.toDegrees(Math.atan(d)));
        addMathematicalBinaryFunction("atan2", (d, d2) -> Math.toDegrees(Math.atan2(d, d2)) );
        addMathematicalUnaryFunction("sinh",   Math::sinh );
        addMathematicalUnaryFunction("cosh",   Math::cosh  );
        addMathematicalUnaryFunction("tanh",   Math::tanh );
        addMathematicalUnaryFunction("sec",    (d) ->  1.0 / Math.cos(Math.toRadians(d)) ); // Formula: sec(x) = 1 / cos(x)
        addMathematicalUnaryFunction("csc",    (d) ->  1.0 / Math.sin(Math.toRadians(d)) ); // Formula: csc(x) = 1 / sin(x)
        addMathematicalUnaryFunction("sech",   (d) ->  1.0 / Math.cosh(d) );                // Formula: sech(x) = 1 / cosh(x)
        addMathematicalUnaryFunction("csch",   (d) -> 1.0 / Math.sinh(d)  );                // Formula: csch(x) = 1 / sinh(x)
        addMathematicalUnaryFunction("cot",    (d) -> 1.0 / Math.tan(Math.toRadians(d))  ); // Formula: cot(x) = cos(x) / sin(x) = 1 / tan(x)
        addMathematicalUnaryFunction("acot",   (d) ->  Math.toDegrees(Math.atan(1.0 / d)) );// Formula: acot(x) = atan(1/x)
        addMathematicalUnaryFunction("coth",   (d) ->  1.0 / Math.tanh(d) );                // Formula: coth(x) = 1 / tanh(x)
        addMathematicalUnaryFunction("asinh",  (d) ->  Math.log(d + (Math.sqrt(Math.pow(d, 2) + 1))));  // Formula: asinh(x) = ln(x + sqrt(x^2 + 1))
        addMathematicalUnaryFunction("acosh",  (d) ->  Math.log(d + (Math.sqrt(Math.pow(d, 2) - 1))));  // Formula: acosh(x) = ln(x + sqrt(x^2 - 1))
        addMathematicalUnaryFunction("atanh",  (d) ->                                       // Formula: atanh(x) = 0.5*ln((1 + x)/(1 - x))
        {
            if (Math.abs(d) > 1 || Math.abs(d) == 1)
                throw new InternalExpressionException("Number must be |x| < 1");
            return 0.5 * Math.log((1 + d) / (1 - d));
        });
        addMathematicalUnaryFunction("rad",  Math::toRadians);
        addMathematicalUnaryFunction("deg", Math::toDegrees);
        addMathematicalUnaryFunction("ln", Math::log);
        addMathematicalUnaryFunction("ln1p", Math::log1p);
        addMathematicalUnaryFunction("log10", Math::log10);
        addMathematicalUnaryFunction("log", a -> Math.log(a)/Math.log(2));
        addMathematicalUnaryFunction("log1p", x -> Math.log1p(x)/Math.log(2));
        addMathematicalUnaryFunction("sqrt", Math::sqrt);
        addMathematicalUnaryFunction("abs", Math::abs);
        addMathematicalUnaryFunction("round", (d) -> (double)Math.round(d));
        addMathematicalUnaryFunction("floor", Math::floor);
        addMathematicalUnaryFunction("ceil", Math::ceil);

        addLazyFunction("mandelbrot", 3, (c, t, lv) -> {
            double a0 = NumericValue.asNumber(lv.get(0).evalValue(c)).getDouble();
            double b0 = NumericValue.asNumber(lv.get(1).evalValue(c)).getDouble();
            long maxiter = NumericValue.asNumber(lv.get(2).evalValue(c)).getLong();
            double a = 0.0D;
            double b = 0.0D;
            long iter = 0;
            while(a*a+b*b<4 && iter < maxiter)
            {
                double temp = a*a-b*b+a0;
                b = 2*a*b+b0;
                a = temp;
                iter++;
            }
            long iFinal = iter;
            return (cc, tt) -> new NumericValue(iFinal);
        });

        addFunction("max", (lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'max' requires at least one parameter");
            Value max = null;
            if (lv.size()==1 && lv.get(0) instanceof ListValue)
                lv = ((ListValue) lv.get(0)).getItems();
            for (Value parameter : lv)
            {
                if (max == null || parameter.compareTo(max) > 0) max = parameter;
            }
            return max;
        });

        addFunction("min", (lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'min' requires at least one parameter");
            Value min = null;
            if (lv.size()==1 && lv.get(0) instanceof ListValue)
                lv = ((ListValue) lv.get(0)).getItems();
            for (Value parameter : lv)
            {
                if (min == null || parameter.compareTo(min) < 0) min = parameter;
            }
            return min;
        });

        addUnaryFunction("relu", (v) -> v.compareTo(Value.ZERO) < 0 ? Value.ZERO : v);
    }

    private void LoopsAndHigherOrderFunctions()
    {
        // condition and expression will get a bound '_i'
        // returns last successful expression or false
        // while(cond, limit, expr) => ??
        addFunction("break", lv -> {
            if (lv.size()==0) throw new BreakStatement(null);
            if (lv.size()==1) throw new BreakStatement(lv.get(0));
            throw new InternalExpressionException("'break' can only be called with zero or one argument");
        });

        addFunction("continue", lv -> {
            if (lv.size()==0) throw new ContinueStatement(null);
            if (lv.size()==1) throw new ContinueStatement(lv.get(0));
            throw new InternalExpressionException("'continue' can only be called with zero or one argument");
        });

        addLazyFunction("while", 3, (c, t, lv) ->
        {
            long limit = NumericValue.asNumber(lv.get(1).evalValue(c)).getLong();
            LazyValue condition = lv.get(0);
            LazyValue expr = lv.get(2);
            long i = 0;
            Value lastOne = Value.NULL;
            //scoping
            LazyValue _val = c.getVariable("_");
            c.setVariable("_",(cc, tt) -> new NumericValue(0).bindTo("_"));
            while (i<limit && condition.evalValue(c, Context.BOOLEAN).getBoolean() )
            {
                try
                {
                    lastOne = expr.evalValue(c, t);
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null) lastOne = stmt.retval;
                    if (stmt instanceof BreakStatement) break;
                }
                i++;
                long seriously = i;
                c.setVariable("_", (cc, tt) -> new NumericValue(seriously).bindTo("_"));
            }
            //revering scope
            c.setVariable("_", _val);
            Value lastValueNoKidding = lastOne;
            return (cc, tt) -> lastValueNoKidding;
        });

        // loop(Num, expr) => last_value
        // expr receives bounded variable '_' indicating iteration
        addLazyFunction("loop", 2, (c, t, lv) ->
        {
            long limit = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
            Value lastOne = Value.NULL;
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            for (long i=0; i < limit; i++)
            {
                long whyYouAsk = i;
                c.setVariable("_", (cc, tt) -> new NumericValue(whyYouAsk).bindTo("_"));
                try
                {
                    lastOne = expr.evalValue(c, t);
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null) lastOne = stmt.retval;
                    if (stmt instanceof BreakStatement) break;
                }
            }
            //revering scope
            c.setVariable("_", _val);
            Value trulyLastOne = lastOne;
            return (cc, tt) -> trulyLastOne;
        });

        // map(list or Num, expr) => list_results
        // receives bounded variable '_' with the expression
        addLazyFunction("map", 2, (c, t, lv) ->
        {
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof AbstractListValue))
                throw new InternalExpressionException("First argument of 'map' function should be a list or iterator");
            Iterator<Value> iterator = ((AbstractListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                String var = next.boundVariable;
                next.bindTo("_");
                int doYouReally = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(doYouReally).bindTo("_i"));
                try
                {
                    result.add(expr.evalValue(c));
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null) result.add(stmt.retval);
                    if (stmt instanceof BreakStatement)
                    {
                        next.boundVariable = var;
                        break;
                    }
                }
                next.boundVariable = var;
            }
            ((AbstractListValue) rval).fatality();
            Value ret = ListValue.wrap(result);
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return (cc, tt) ->  ret;
        });

        // grep(list or num, expr) => list
        // receives bounded variable '_' with the expression, and "_i" with index
        // produces list of values for which the expression is true
        addLazyFunction("filter", 2, (c, t, lv) ->
        {
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof AbstractListValue))
                throw new InternalExpressionException("First argument of 'filter' function should be a list or iterator");
            Iterator<Value> iterator = ((AbstractListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                String var = next.boundVariable;
                next.bindTo("_");
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                try
                {
                    if(expr.evalValue(c, Context.BOOLEAN).getBoolean())
                        result.add(next);
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null && stmt.retval.getBoolean()) result.add(next);
                    if (stmt instanceof BreakStatement)
                    {
                        next.boundVariable = var;
                        break;
                    }
                }
                next.boundVariable = var;
            }
            ((AbstractListValue) rval).fatality();
            Value ret = ListValue.wrap(result);
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return (cc, tt) -> ret;
        });

        // first(list, expr) => elem or null
        // receives bounded variable '_' with the expression, and "_i" with index
        // returns first element on the list for which the expr is true
        addLazyFunction("first", 2, (c, t, lv) ->
        {

            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof AbstractListValue))
                throw new InternalExpressionException("First argument of 'first' function should be a list or iterator");
            Iterator<Value> iterator = ((AbstractListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            Value result = Value.NULL;
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                String var = next.boundVariable;
                next.bindTo("_");
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                try
                {
                    if(expr.evalValue(c, Context.BOOLEAN).getBoolean())
                    {
                        result = next;
                        next.boundVariable = var;
                        break;
                    }
                }
                catch (BreakStatement  stmt)
                {
                    result = stmt.retval == null? next : stmt.retval;
                    next.boundVariable = var;
                    break;
                }
                catch (ContinueStatement ignored)
                {
                    throw new InternalExpressionException("'continue' inside 'first' function has no sense");
                }
                next.boundVariable = var;
            }
            //revering scope
            ((AbstractListValue) rval).fatality();
            Value whyWontYouTrustMeJava = result;
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return (cc, tt) -> whyWontYouTrustMeJava;
        });

        // all(list, expr) => boolean
        // receives bounded variable '_' with the expression, and "_i" with index
        // returns true if expr is true for all items
        addLazyFunction("all", 2, (c, t, lv) ->
        {
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof AbstractListValue))
                throw new InternalExpressionException("First argument of 'all' function should be a list or iterator");
            Iterator<Value> iterator = ((AbstractListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            LazyValue result = LazyValue.TRUE;
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                String var = next.boundVariable;
                next.bindTo("_");
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                if(!expr.evalValue(c, Context.BOOLEAN).getBoolean())
                {
                    result = LazyValue.FALSE;
                    next.boundVariable = var;
                    break;
                }
                next.boundVariable = var;
            }
            //revering scope
            ((AbstractListValue) rval).fatality();
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return result;
        });

        // runs traditional for(init, condition, increment, body) tri-argument for loop with body in between
        addLazyFunction("c_for", 4, (c, t, lv) -> {
            LazyValue initial = lv.get(0);
            LazyValue condition = lv.get(1);
            LazyValue increment = lv.get(2);
            LazyValue body = lv.get(3);
            int iterations = 0;
            for (initial.evalValue(c, Context.VOID); condition.evalValue(c, Context.BOOLEAN).getBoolean(); increment.evalValue(c, Context.VOID))
            {
                try
                {
                    body.evalValue(c, Context.VOID);
                }
                catch (BreakStatement stmt)
                {
                    break;
                }
                catch (ContinueStatement ignored)
                {
                }
                iterations++;
            }
            int finalIterations = iterations;
            return (cc, tt) -> new NumericValue(finalIterations);
        });

        // similar to map, but returns total number of successes
        // for(list, expr) => success_count
        // can be substituted for first and all, but first is more efficient and all doesn't require knowing list size
        addLazyFunction("for", 2, (c, t, lv) ->
        {
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof AbstractListValue))
                throw new InternalExpressionException("First argument of 'for' function should be a list or iterator");
            Iterator<Value> iterator = ((AbstractListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _ite = c.getVariable("_i");
            int successCount = 0;
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                String var = next.boundVariable;
                next.bindTo("_");
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                Value result = Value.FALSE;
                try
                {
                    result = expr.evalValue(c, t);
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null) result = stmt.retval;
                    if (stmt instanceof BreakStatement)
                    {
                        next.boundVariable = var;
                        break;
                    }
                }
                if(t != Context.VOID && result.getBoolean())
                    successCount++;
                next.boundVariable = var;
            }
            //revering scope
            ((AbstractListValue) rval).fatality();
            c.setVariable("_", _val);
            c.setVariable("_i", _ite);
            long promiseWontChange = successCount;
            return (cc, tt) -> new NumericValue(promiseWontChange);
        });


        // reduce(list, expr, ?acc) => value
        // reduces values in the list with expression that gets accumulator
        // each iteration expr receives acc - accumulator, and '_' - current list value
        // returned value is substituted to the accumulator
        addLazyFunction("reduce", 3, (c, t, lv) ->
        {
            LazyValue expr = lv.get(1);

            Value acc = lv.get(2).evalValue(c);
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof AbstractListValue))
                throw new InternalExpressionException("First argument of 'reduce' should be a list or iterator");
            Iterator<Value> iterator = ((AbstractListValue) rval).iterator();

            if (!iterator.hasNext())
            {
                Value seriouslyWontChange = acc;
                return (cc, tt) -> seriouslyWontChange;
            }

            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _acc = c.getVariable("_a");
            LazyValue _ite = c.getVariable("_i");

            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                String var = next.boundVariable;
                next.bindTo("_");
                Value promiseWontChangeYou = acc;
                int seriously = i;
                c.setVariable("_a", (cc, tt) -> promiseWontChangeYou.bindTo("_a"));
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                try
                {
                    acc = expr.evalValue(c, t);
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null) acc = stmt.retval;
                    if (stmt instanceof BreakStatement)
                    {
                        next.boundVariable = var;
                        break;
                    }
                }
                next.boundVariable = var;
            }
            //reverting scope
            ((AbstractListValue) rval).fatality();
            c.setVariable("_a", _acc);
            c.setVariable("_", _val);
            c.setVariable("_i", _ite);

            Value hopeItsEnoughPromise = acc;
            return (cc, tt) -> hopeItsEnoughPromise;
        });
    }

    private void BasicDataStructures()
    {
        addFunction("l", lv ->
        {
            if (lv.size() == 1 && lv.get(0) instanceof LazyListValue)
                return ListValue.wrap(((LazyListValue) lv.get(0)).unroll());
            return new ListValue.ListConstructorValue(lv);
        });

        addFunction("join", (lv) ->
        {
            if (lv.size() < 2)
                throw new InternalExpressionException("'join' takes at least 2 arguments");
            String delimiter = lv.get(0).getString();
            List<Value> toJoin;
            if (lv.size()==2 && lv.get(1) instanceof LazyListValue)
            {
                toJoin = ((LazyListValue) lv.get(1)).unroll();

            }
            else if (lv.size() == 2 && lv.get(1) instanceof ListValue)
            {
                toJoin = new ArrayList<>(((ListValue)lv.get(1)).getItems());
            }
            else
            {
                toJoin = lv.subList(1,lv.size());
            }
            return new StringValue(toJoin.stream().map(Value::getString).collect(Collectors.joining(delimiter)));
        });

        addBinaryFunction("split", (d, v) -> {
            String delimiter = d.getString();
            String hwat = v.getString();
            return ListValue.wrap(Arrays.stream(hwat.split(delimiter)).map(StringValue::new).collect(Collectors.toList()));
        });

        addFunction("slice", (lv) -> {

            if (lv.size() != 2 && lv.size() != 3)
                throw new InternalExpressionException("'slice' takes 2 or 3 arguments");
            Value hwat = lv.get(0);
            long from = NumericValue.asNumber(lv.get(1)).getLong();
            long to = -1;
            if (lv.size()== 3)
                to = NumericValue.asNumber(lv.get(2)).getLong();
            return hwat.slice(from, to);
        });

        addFunction("sort", (lv) ->
        {
            List<Value> toSort = lv;
            if (lv.size()==1 && lv.get(0) instanceof ListValue)
            {
                toSort = new ArrayList<>(((ListValue)lv.get(0)).getItems());
            }
            Collections.sort(toSort);
            return ListValue.wrap(toSort);
        });

        addLazyFunction("sort_key", -1, (c, t, lv) ->  //get working with iterators
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("First argument for 'sort_key' should be a List");
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof ListValue))
                throw new InternalExpressionException("First argument for 'sort_key' should be a List");
            List<Value> toSort = new ArrayList<>(((ListValue) v).getItems());
            if (lv.size()==1)
            {
                Collections.shuffle(toSort);
                Value ret = ListValue.wrap(toSort);
                return (_c, _t) -> ret;
            }
            LazyValue sortKey = lv.get(1);
            //scoping
            LazyValue __ = c.getVariable("_");
            Collections.sort(toSort,(v1, v2) -> {
                c.setVariable("_",(cc, tt) -> v1);
                Value ev1 = sortKey.evalValue(c);
                c.setVariable("_",(cc, tt) -> v2);
                Value ev2 = sortKey.evalValue(c);
                return ev1.compareTo(ev2);
            });
            //revering scope
            c.setVariable("_", __);
            return (cc, tt) -> ListValue.wrap(toSort);
        });

        addFunction("range", (lv) ->
        {
            long from = 0;
            long to = 0;
            long step = 1;
            int argsize = lv.size();
            if (argsize == 0 || argsize > 3)
                throw new InternalExpressionException("'range' accepts from 1 to 3 arguments, not "+argsize);
            to = NumericValue.asNumber(lv.get(0)).getLong();
            if (lv.size() > 1)
            {
                from = to;
                to = NumericValue.asNumber(lv.get(1)).getLong();
                if (lv.size() > 2)
                {
                    step = NumericValue.asNumber(lv.get(2)).getLong();
                }
            }
            return LazyListValue.range(from, to, step);
        });

        addLazyFunction("m", -1, (c, t, llv) ->
        {
            List<Value> lv = new ArrayList<>();
            for (LazyValue lazyParam : llv) {
                lv.add(lazyParam.evalValue(c, Context.MAPDEF)); // none type default by design
            }
            Value ret;
            if (lv.size() == 1 && lv.get(0) instanceof LazyListValue)
                ret = new MapValue(((LazyListValue) lv.get(0)).unroll());
            else
                ret = new MapValue(lv);
            return (cc, tt) -> ret;
        });

        addUnaryFunction("keys", v -> {
            if (v instanceof MapValue)
                return new ListValue(((MapValue) v).getMap().keySet());
            return Value.NULL;
        });

        addUnaryFunction("values", v -> {
            if (v instanceof MapValue)
                return new ListValue(((MapValue) v).getMap().values());
            return Value.NULL;
        });

        addUnaryFunction("pairs", v -> {
            if (v instanceof MapValue)
                return ListValue.wrap(((MapValue) v).getMap().entrySet().stream().map(
                        (p) -> ListValue.of(p.getKey(), p.getValue())
                ).collect(Collectors.toList()));
            return Value.NULL;
        });

        addLazyBinaryOperator(":", precedence.get("attribute~:"),true, (c, t, container_lv, key_lv) ->
        {
            Value container = container_lv.evalValue(c);
            if (container instanceof LContainerValue)
            {
                ContainerValueInterface outerContainer = ((LContainerValue) container).getContainer();
                if (outerContainer == null)
                {
                    Value innerLValue = new LContainerValue(null, null);
                    return (cc, tt) -> innerLValue;
                }
                Value innerContainer = outerContainer.get(((LContainerValue) container).getAddress());
                if (!(innerContainer instanceof  ContainerValueInterface))
                {
                    Value innerLValue = new LContainerValue(null, null);
                    return (cc, tt) -> innerLValue;
                }
                Value innerLValue = new LContainerValue((ContainerValueInterface) innerContainer, key_lv.evalValue(c));
                return (cc, tt) -> innerLValue;
            }
            if (!(container instanceof ContainerValueInterface))
                if (t == Context.LVALUE)
                    return (cc, tt) -> new LContainerValue(null, null);
                else
                    return (cc, tt) -> Value.NULL;
            Value address = key_lv.evalValue(c);
            if (t != Context.LVALUE)
            {
                Value retVal = ((ContainerValueInterface) container).get(address);
                return (cc, ct) -> retVal;
            }
            Value retVal = new LContainerValue((ContainerValueInterface) container, address);
            return (cc, ct) -> retVal;
        });

        addLazyFunction("get", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'get' requires parameters");
            if (lv.size() == 1)
            {
                Value v = lv.get(0).evalValue(c, Context.LVALUE);
                if (!(v  instanceof LContainerValue))
                    return (cc, tt) -> Value.NULL;
                ContainerValueInterface container = ((LContainerValue) v).getContainer();
                if (container == null)
                    return (cc, tt) -> Value.NULL;
                Value ret = container.get(((LContainerValue) v).getAddress());
                return (cc, tt) -> ret;
            }
            Value container = lv.get(0).evalValue(c);
            for (int i = 1; i < lv.size(); i++)
            {
                if (!(container instanceof ContainerValueInterface)) return (cc, tt) -> Value.NULL;
                container = ((ContainerValueInterface) container).get(lv.get(i).evalValue(c));
            }
            if (container == null)
                return (cc, tt) -> Value.NULL;
            Value finalContainer = container;
            return (cc, tt) -> finalContainer;
        });

        addLazyFunction("has", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'has' requires parameters");
            if (lv.size() == 1)
            {
                Value v = lv.get(0).evalValue(c, Context.LVALUE);
                if (!(v  instanceof LContainerValue))
                    return (cc, tt) -> Value.NULL;
                ContainerValueInterface container = ((LContainerValue) v).getContainer();
                if (container == null)
                    return (cc, tt) -> Value.NULL;
                Value ret = new NumericValue(container.has(((LContainerValue) v).getAddress()));
                return (cc, tt) -> ret;
            }
            Value container = lv.get(0).evalValue(c);
            for (int i = 1; i < lv.size()-1; i++)
            {
                if (!(container instanceof ContainerValueInterface)) return (cc, tt) -> Value.NULL;
                container = ((ContainerValueInterface) container).get(lv.get(i).evalValue(c));
            }
            if (!(container instanceof ContainerValueInterface))
                return (cc, tt) -> Value.NULL;
            Value ret = new NumericValue(((ContainerValueInterface) container).has(lv.get(lv.size()-1).evalValue(c)));
            return (cc, tt) -> ret;
        });

        addLazyFunction("put", -1, (c, t, lv) ->
        {
            if(lv.size()<2)
            {
                throw new InternalExpressionException("'put' takes at least three arguments, a container, address, and values to insert at that index");
            }
            Value container = lv.get(0).evalValue(c, Context.LVALUE);
            if (container instanceof LContainerValue)
            {
                ContainerValueInterface internalContainer = ((LContainerValue) container).getContainer();
                if (internalContainer == null)
                {
                    return (cc, tt) -> Value.NULL;
                }
                Value address = ((LContainerValue) container).getAddress();
                Value what = lv.get(1).evalValue(c);
                Value retVal = new NumericValue( (lv.size() > 2)
                        ? internalContainer.put(address, what, lv.get(2).evalValue(c))
                        : internalContainer.put(address, what));
                return (cc, tt) -> retVal;

            }
            if(lv.size()<3)
            {
                throw new InternalExpressionException("'put' takes at least three arguments, a container, address, and values to insert at that index");
            }
            if (!(container instanceof ContainerValueInterface))
            {
                return (cc, tt) -> Value.NULL;
            }
            Value where = lv.get(1).evalValue(c);
            Value what = lv.get(2).evalValue(c);
            Value retVal = new NumericValue( (lv.size()>3)
                    ? ((ContainerValueInterface) container).put(where, what, lv.get(3).evalValue(c))
                    : ((ContainerValueInterface) container).put(where, what));
            return (cc, tt) -> retVal;
        });

        addLazyFunction("delete", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'delete' requires parameters");
            if (lv.size() == 1)
            {
                Value v = lv.get(0).evalValue(c, Context.LVALUE);
                if (!(v  instanceof LContainerValue))
                    return (cc, tt) -> Value.NULL;
                ContainerValueInterface container = ((LContainerValue) v).getContainer();
                if (container == null)
                    return (cc, tt) -> Value.NULL;
                Value ret = new NumericValue(container.delete(((LContainerValue) v).getAddress()));
                return (cc, tt) -> ret;
            }
            Value container = lv.get(0).evalValue(c);
            for (int i = 1; i < lv.size()-1; i++)
            {
                if (!(container instanceof ContainerValueInterface)) return (cc, tt) -> Value.NULL;
                container = ((ContainerValueInterface) container).get(lv.get(i).evalValue(c));
            }
            if (!(container instanceof ContainerValueInterface))
                return (cc, tt) -> Value.NULL;
            Value ret = new NumericValue(((ContainerValueInterface) container).delete(lv.get(lv.size()-1).evalValue(c)));
            return (cc, tt) -> ret;
        });
    }

    private void SystemFunctions()
    {
        addUnaryFunction("hash_code", v -> new NumericValue(v.hashCode()));

        addUnaryFunction("copy", Value::deepcopy);

        addLazyFunction("bool", 1, (c, t, lv) -> {
            Value v = lv.get(0).evalValue(c, Context.BOOLEAN);
            if (v instanceof StringValue)
            {
                String str = v.getString();
                if ("false".equalsIgnoreCase(str) || "null".equalsIgnoreCase(str))
                {
                    return (cc, tt) -> Value.FALSE;
                }
            }
            Value retval = new NumericValue(v.getBoolean());
            return (cc, tt) -> retval;
        });
        addUnaryFunction("number", v -> {
            if (v instanceof NumericValue)
                return v;
            double res = v.readNumber();
            if (Double.isNaN(res))
                return Value.NULL;
            return new NumericValue(v.readNumber());
        });
        addFunction("str", lv ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'str' requires at least one argument");
            String format = lv.get(0).getString();
            if (lv.size() == 1)
                return new StringValue(format);
            int argIndex = 1;
            if (lv.get(1) instanceof ListValue && lv.size() == 2)
            {
                lv = ((ListValue) lv.get(1)).getItems();
                argIndex = 0;
            }
            List<Object> args = new ArrayList<>();
            Matcher m = formatPattern.matcher(format);

            for (int i = 0, len = format.length(); i < len; ) {
                if (m.find(i)) {
                    // Anything between the start of the string and the beginning
                    // of the format specifier is either fixed text or contains
                    // an invalid format string.
                    // [[scarpet]] but we skip it and let the String.format fail
                    char fmt = m.group(6).toLowerCase().charAt(0);
                    if (fmt == 's')
                    {
                        if (argIndex >= lv.size())
                            throw new InternalExpressionException("Not enough arguments for "+m.group(0));
                        args.add(lv.get(argIndex).getString());
                        argIndex++;
                    }
                    else if (fmt == 'd' || fmt == 'o' || fmt == 'x')
                    {
                        if (argIndex >= lv.size())
                            throw new InternalExpressionException("Not enough arguments for "+m.group(0));
                        args.add(lv.get(argIndex).readInteger());
                        argIndex++;
                    }
                    else if (fmt == 'a' || fmt == 'e' || fmt == 'f' || fmt == 'g')
                    {
                        if (argIndex >= lv.size())
                            throw new InternalExpressionException("Not enough arguments for "+m.group(0));
                        args.add(lv.get(argIndex).readNumber());
                        argIndex++;
                    }
                    else if (fmt == 'b')
                    {
                        if (argIndex >= lv.size())
                            throw new InternalExpressionException("Not enough arguments for "+m.group(0));
                        args.add(lv.get(argIndex).getBoolean());
                        argIndex++;
                    }
                    else if (fmt == '%')
                    {
                        //skip /%%
                    }
                    else
                    {
                        throw new InternalExpressionException("Format not supported: "+m.group(6));
                    }

                    i = m.end();
                } else {
                    // No more valid format specifiers.  Check for possible invalid
                    // format specifiers.
                    // [[scarpet]] but we skip it and let the String.format fail
                    break;
                }
            }
            try
            {
                return new StringValue(String.format(Locale.ROOT, format, args.toArray()));
            }
            catch (IllegalFormatException ife)
            {
                throw new InternalExpressionException("Illegal string format: "+ife.getMessage());
            }
        });

        addUnaryFunction("lower", v -> new StringValue(v.getString().toLowerCase(Locale.ROOT)));

        addUnaryFunction("upper", v -> new StringValue(v.getString().toUpperCase(Locale.ROOT)));

        addUnaryFunction("title", v -> new StringValue(WordUtils.capitalizeFully(v.getString())));

        addFunction("replace", (lv) ->
        {
            if (lv.size() != 3 && lv.size() !=2)
                throw new InternalExpressionException("'replace' expects string to read, pattern regex, and optional replacement string");
            String data = lv.get(0).getString();
            String regex = lv.get(1).getString();
            String replacement = "";
            if (lv.size() == 3)
                replacement = lv.get(2).getString();
            return new StringValue(data.replaceAll(regex, replacement));
        });

        addFunction("replace_first", (lv) ->
        {
            if (lv.size() != 3 && lv.size() !=2)
                throw new InternalExpressionException("'replace_first' expects string to read, pattern regex, and optional replacement string");
            String data = lv.get(0).getString();
            String regex = lv.get(1).getString();
            String replacement = "";
            if (lv.size() == 3)
                replacement = lv.get(2).getString();
            return new StringValue(data.replaceFirst(regex, replacement));
        });

        addUnaryFunction("type", v -> new StringValue(v.getTypeString()));

        addUnaryFunction("length", v -> new NumericValue(v.length()));
        addLazyFunction("rand", -1, (c, t, lv) -> {
            int argsize = lv.size();
            Random randomizer = Expression.randomizer;
            if (argsize != 1 && argsize != 2)
                throw new InternalExpressionException("'rand' takes one (range) or two arguments (range and seed)");
            if (argsize == 2) randomizer = c.host.getRandom(NumericValue.asNumber(lv.get(1).evalValue(c)).getLong());
            Value argument = lv.get(0).evalValue(c);
            if (argument instanceof ListValue)
            {
                List<Value> list = ((ListValue) argument).getItems();
                Value retval = list.get(randomizer.nextInt(list.size()));
                return (cc, tt) -> retval;
            }
            if (t == Context.BOOLEAN)
            {
                double rv = NumericValue.asNumber(argument).getDouble()*randomizer.nextFloat();
                Value retval = rv<1.0D?Value.FALSE:Value.TRUE;
                return (cc, tt) -> retval;
            }
            Value retval = new NumericValue(NumericValue.asNumber(argument).getDouble()*randomizer.nextDouble());
            return (cc, tt) -> retval;
        });

        addLazyFunction("perlin", -1, (c, t, lv) -> {
            PerlinNoiseSampler sampler;
            Value x, y, z;

            if (lv.size() >= 4)
            {
                x = lv.get(0).evalValue(c);
                y = lv.get(1).evalValue(c);
                z = lv.get(2).evalValue(c);
                sampler = PerlinNoiseSampler.getPerlin(NumericValue.asNumber(lv.get(3).evalValue(c)).getLong());
            }
            else
            {
                sampler = PerlinNoiseSampler.instance;
                y = Value.NULL;
                z = Value.NULL;
                if (lv.size() == 0 )
                    throw new InternalExpressionException("'perlin' requires at least one dimension to sample from");
                x = NumericValue.asNumber(lv.get(0).evalValue(c));
                if (lv.size() > 1)
                {
                    y = NumericValue.asNumber(lv.get(1).evalValue(c));
                    if (lv.size() > 2)
                        z = NumericValue.asNumber(lv.get(2).evalValue(c));
                }
            }

            double result;

            if (z instanceof NullValue)
                if (y instanceof NullValue)
                    result = sampler.sample1d(NumericValue.asNumber(x).getDouble());
                else
                    result = sampler.sample2d(NumericValue.asNumber(x).getDouble(), NumericValue.asNumber(y).getDouble());
            else
                result = sampler.sample3d(
                        NumericValue.asNumber(x).getDouble(),
                        NumericValue.asNumber(y).getDouble(),
                        NumericValue.asNumber(z).getDouble());
            Value ret = new NumericValue(result);
            return (cc, tt) -> ret;
        });

        addLazyFunction("simplex", -1, (c, t, lv) -> {
            SimplexNoiseSampler sampler;
            Value x, y, z;

            if (lv.size() >= 4)
            {
                x = lv.get(0).evalValue(c);
                y = lv.get(1).evalValue(c);
                z = lv.get(2).evalValue(c);
                sampler = SimplexNoiseSampler.getSimplex(NumericValue.asNumber(lv.get(3).evalValue(c)).getLong());
            }
            else
            {
                sampler = SimplexNoiseSampler.instance;
                z = Value.NULL;
                if (lv.size() < 2 )
                    throw new InternalExpressionException("'simplex' requires at least two dimensions to sample from");
                x = NumericValue.asNumber(lv.get(0).evalValue(c));
                y = NumericValue.asNumber(lv.get(1).evalValue(c));
                if (lv.size() > 2)
                    z = NumericValue.asNumber(lv.get(2).evalValue(c));
            }
            double result;

            if (z instanceof NullValue)
                result = sampler.sample2d(NumericValue.asNumber(x).getDouble(), NumericValue.asNumber(y).getDouble());
            else
                result = sampler.sample3d(
                        NumericValue.asNumber(x).getDouble(),
                        NumericValue.asNumber(y).getDouble(),
                        NumericValue.asNumber(z).getDouble());
            Value ret = new NumericValue(result);
            return (cc, tt) -> ret;
        });

        addUnaryFunction("print", (v) ->
        {
            System.out.println(v.getString());
            return v; // pass through for variables
        });
        addUnaryFunction("sleep", (v) ->
        {
            long time = NumericValue.asNumber(v).getLong();
            try
            {
                Thread.sleep(time);
                Thread.yield();
            }
            catch (InterruptedException ignored) { }
            return v; // pass through for variables
        });
        addLazyFunction("time", 0, (c, t, lv) ->
        {
            Value time = new NumericValue((System.nanoTime() / 1000) / 1000.0);
            return (cc, tt) -> time;
        });

        addLazyFunction("unix_time", 0, (c, t, lv) ->
        {
            Value time = new NumericValue(System.currentTimeMillis());
            return (cc, tt) -> time;
        });

        addFunction("convert_date", lv ->
        {
            int argsize = lv.size();
            if (lv.size() == 0) throw new InternalExpressionException("'convert_date' requires at least one parameter");
            Value value = lv.get(0);
            if (argsize == 1 && !(value instanceof ListValue))
            {
                Calendar cal = new GregorianCalendar(Locale.ROOT);
                cal.setTimeInMillis(NumericValue.asNumber(value, "timestamp").getLong());
                int weekday = cal.get(Calendar.DAY_OF_WEEK)-1;
                if (weekday == 0) weekday = 7;
                Value retVal = ListValue.ofNums(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH)+1,
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND),
                        weekday,
                        cal.get(Calendar.DAY_OF_YEAR),
                        cal.get(Calendar.WEEK_OF_YEAR)
                );
                return retVal;
            }
            else if(value instanceof ListValue)
            {
                lv = ((ListValue) value).getItems();
                argsize = lv.size();
            }
            Calendar cal = new GregorianCalendar(0, 0, 0, 0, 0, 0);

            if (argsize == 3)
            {
                cal.set(
                        NumericValue.asNumber(lv.get(0)).getInt(),
                        NumericValue.asNumber(lv.get(1)).getInt()-1,
                        NumericValue.asNumber(lv.get(2)).getInt()
                );
            }
            else if (argsize == 6)
            {
                cal.set(
                        NumericValue.asNumber(lv.get(0)).getInt(),
                        NumericValue.asNumber(lv.get(1)).getInt()-1,
                        NumericValue.asNumber(lv.get(2)).getInt(),
                        NumericValue.asNumber(lv.get(3)).getInt(),
                        NumericValue.asNumber(lv.get(4)).getInt(),
                        NumericValue.asNumber(lv.get(5)).getInt()
                );
            }
            else throw new InternalExpressionException("Date conversion requires 3 arguments for Dates or 6 arguments, for time");
            return new NumericValue(cal.getTimeInMillis());
        });

        addLazyFunction("profile_expr", 1, (c, t, lv) ->
        {
            LazyValue lazy = lv.get(0);
            long end = System.nanoTime()+50000000L;
            long it = 0;
            while (System.nanoTime()<end)
            {
                lazy.evalValue(c);
                it++;
            }
            Value res = new NumericValue(it);
            return (cc, tt) -> res;
        });

        addLazyFunction("var", 1, (c, t, lv) -> {
            String varname = lv.get(0).evalValue(c).getString();
            return getOrSetAnyVariable(c, varname);
        });

        addLazyFunction("undef", 1, (c, t, lv) ->
        {
            Value remove = lv.get(0).evalValue(c);
            if (remove instanceof FunctionValue)
            {
                c.host.delFunction(module, remove.getString());
                return (cc, tt) -> Value.NULL;
            }
            String varname = remove.getString();
            boolean isPrefix = varname.endsWith("*");
            if (isPrefix)
                varname = varname.replaceAll("\\*+$", "");
            if (isPrefix)
            {
                c.host.delFunctionWithPrefix(module, varname);
                if (varname.startsWith("global_"))
                {
                    c.host.delGlobalVariableWithPrefix(module, varname);
                }
                else if (!varname.startsWith("_"))
                {
                    c.removeVariablesMatching(varname);
                }
            }
            else
            {
                c.host.delFunction(module, varname);
                if (varname.startsWith("global_"))
                {
                    c.host.delGlobalVariable(module, varname);
                }
                else if (!varname.startsWith("_"))
                {
                    c.delVariable(varname);
                }
            }
            return (cc, tt) -> Value.NULL;
        });

        //deprecate
        addLazyFunction("vars", 1, (c, t, lv) -> {
            String prefix = lv.get(0).evalValue(c).getString();
            List<Value> values = new ArrayList<>();
            if (prefix.startsWith("global"))
            {
                c.host.globaVariableNames(module, (s) -> s.startsWith(prefix)).forEach(s -> values.add(new StringValue(s)));
            }
            else
            {
                c.getAllVariableNames().stream().filter(s -> s.startsWith(prefix)).forEach(s -> values.add(new StringValue(s)));
            }
            Value retval = ListValue.wrap(values);
            return (cc, tt) -> retval;
        });

        addLazyFunctionWithDelegation("task", -1, (c, t, expr, tok, lv) -> {
            if (lv.size() == 0)
                throw new InternalExpressionException("'task' requires at least function to call as a parameter");
            FunctionArgument functionArgument = FunctionArgument.findIn(c, module, lv, 0, true, false);
            Value queue = Value.NULL;
            if (lv.size() > functionArgument.offset) queue = lv.get(functionArgument.offset).evalValue(c);
            ThreadValue thread = new ThreadValue(queue, functionArgument.function, expr, tok, c, functionArgument.args);
            Thread.yield();
            return (cc, tt) -> thread;
        });

        addFunction("task_count", (lv) ->
        {
            if (lv.size() > 0)
            {
                return new NumericValue(ThreadValue.taskCount(lv.get(0)));
            }
            return new NumericValue(ThreadValue.taskCount());
        });

        addUnaryFunction("task_value", (v) -> {
            if (!(v instanceof ThreadValue))
                throw new InternalExpressionException("'task_value' could only be used with a task value");
            return ((ThreadValue) v).getValue();
        });

        addUnaryFunction("task_join", (v) -> {
            if (!(v instanceof ThreadValue))
                throw new InternalExpressionException("'task_join' could only be used with a task value");
            return ((ThreadValue) v).join();
        });

        addLazyFunction("task_dock", 1, (c, t, lv) -> {
            // pass through placeholder
            // implmenetation should dock the task on the main thread.
            return lv.get(0);
        });

        addUnaryFunction("task_completed", (v) -> {
            if (!(v instanceof ThreadValue))
                throw new InternalExpressionException("'task_completed' could only be used with a task value");
            return new NumericValue(((ThreadValue) v).isFinished());
        });

        addLazyFunction("synchronize", -1, (c, t, lv) ->
        {
            if (lv.size() == 0) throw new InternalExpressionException("'synchronize' require at least an expression to synchronize");
            Value lockValue = Value.NULL;
            int ind = 0;
            if (lv.size() == 2)
            {
                lockValue = lv.get(0).evalValue(c);
                ind = 1;
            }
            synchronized (ThreadValue.getLock(lockValue))
            {
                Value ret = lv.get(ind).evalValue(c, t);
                return (_c, _t) -> ret;
            }
        });

        addLazyFunction("system_variable_get", -1, (c, t, lv) ->
        {
            if (lv.size() == 0) throw new InternalExpressionException("'system_variable_get' expects at least a key to be fetched");
            Value key = lv.get(0).evalValue(c);
            if (lv.size() > 1)
            {
                ScriptHost.systemGlobals.computeIfAbsent(key, k -> lv.get(1).evalValue(c));
            }
            Value res = ScriptHost.systemGlobals.get(key);
            if (res!=null) return (cc, tt) -> res;
            return (cc, tt) -> Value.NULL;
        });

        addLazyFunction("system_variable_set", 2, (c, t, lv) ->
        {
            Value key = lv.get(0).evalValue(c);
            Value value = lv.get(1).evalValue(c);
            Value res = ScriptHost.systemGlobals.put(key, value);
            if (res!=null) return (cc, tt) -> res;
            return (cc, tt) -> Value.NULL;
        });


    }

    private void setAnyVariable(Context c, String name, LazyValue lv)
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
        LazyValue var = null;
        if (!name.startsWith("global_"))
        {
            var = c.getVariable(name);
            if (var != null) return var;
        }
        var = c.host.getGlobalVariable(module, name);
        if (var != null) return var;
        var = (_c, _t ) -> Value.ZERO.reboundedTo(name);
        setAnyVariable(c, name, var);
        return var;
    }

    public static final Expression none = new Expression("null");
    /**
     * @param expression .
     */
    public Expression(String expression)
    {
        this.expression = expression.trim().replaceAll("\\r\\n?", "\n").replaceAll("\\t","   ");
        UserDefinedFunctionsAndControlFlow();
        Operators();
        ArithmeticOperations();
        SystemFunctions();
        LoopsAndHigherOrderFunctions();
        BasicDataStructures();
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
                        if (previousToken.type == Tokenizer.Token.TokenType.LITERAL || previousToken.type == Tokenizer.Token.TokenType.CLOSE_PAREN
                                || previousToken.type == Tokenizer.Token.TokenType.VARIABLE
                                || previousToken.type == Tokenizer.Token.TokenType.HEX_LITERAL)
                        {
                            // Implicit multiplication, e.g. 23(a+b) or (a+b)(a-b)
                            Tokenizer.Token multiplication = new Tokenizer.Token();
                            multiplication.append("*");
                            multiplication.type = Tokenizer.Token.TokenType.OPERATOR;
                            stack.push(multiplication);
                        }
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
        return eval(c, Context.NONE);
    }
    public Value eval(Context c, Integer expectedType)
    {
        if (ast == null)
        {
            ast = getAST(c);
        }
        return evalValue(() -> ast, c, expectedType);
    }

    public Value evalValue(Supplier<LazyValue> exprProvider, Context c, Integer expectedType)
    {
        try
        {
            return exprProvider.get().evalValue(c, expectedType);
        }
        catch (ContinueStatement | BreakStatement | ReturnStatement exc)
        {
            throw new ExpressionException(c, this, "Control flow functions, like continue, break, or return, should only be used in loops and functions respectively.");
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

    private LazyValue getAST(Context context)
    {
        Stack<LazyValue> stack = new Stack<>();
        List<Tokenizer.Token> rpn = shuntingYard(context);
        validate(context, rpn);
        for (final Tokenizer.Token token : rpn)
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                {
                    final LazyValue value = stack.pop();
                    LazyValue result = (c, t) -> operators.get(token.surface).lazyEval(c, t, this, token, value, null).evalValue(c);
                    stack.push(result);
                    break;
                }
                case OPERATOR:
                    final LazyValue v1 = stack.pop();
                    final LazyValue v2 = stack.pop();
                    LazyValue result = (c,t) -> operators.get(token.surface).lazyEval(c, t,this, token, v2, v1).evalValue(c);
                    stack.push(result);
                    break;
                case VARIABLE:
                    stack.push((c, t) -> getOrSetAnyVariable(c, token.surface).evalValue(c));
                    break;
                case FUNCTION:
                    String name = token.surface;
                    ILazyFunction f;
                    ArrayList<LazyValue> p;
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
                    while (!stack.isEmpty() && stack.peek() != LazyValue.PARAMS_START)
                    {
                        p.add(stack.pop());
                    }
                    if (!isKnown) p.add( (c, t) -> new StringValue(name));
                    Collections.reverse(p);

                    if (stack.peek() == LazyValue.PARAMS_START)
                    {
                        stack.pop();
                    }

                    stack.push((c, t) -> f.lazyEval(c, t, this, token, p).evalValue(c));
                    break;
                case OPEN_PAREN:
                    stack.push(LazyValue.PARAMS_START);
                    break;
                case LITERAL:
                    stack.push((c, t) ->
                    {
                        try
                        {
                            return new NumericValue(token.surface);
                        }
                        catch (NumberFormatException exception)
                        {
                            throw new ExpressionException(c, this, token, "Not a number");
                        }
                    });
                    break;
                case STRINGPARAM:
                    stack.push((c, t) -> new StringValue(token.surface) ); // was originally null
                    break;
                case HEX_LITERAL:
                    stack.push((c, t) -> new NumericValue(new BigInteger(token.surface.substring(2), 16).doubleValue()));
                    break;
                default:
                    throw new ExpressionException(context, this, token, "Unexpected token '" + token.surface + "'");
            }
        }
        return stack.pop();
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
                            throw new ExpressionException(c, this, token, "Unnecessary semicolon");
                        }
                        throw new ExpressionException(c, this, token, "Missing parameter(s) for operator " + token);
                    }
                    // pop the operator's 2 parameters and add the result
                    stack.set(stack.size() - 1, stack.peek() - 2 + 1);
                    break;
                case FUNCTION:
                    ILazyFunction f = functions.get(token.surface);// don't validate global - userdef functions
                    int numParams = stack.pop();
                    if (f != null && !f.numParamsVaries() && numParams != f.getNumParams())
                    {
                        throw new ExpressionException(c, this, token, "Function " + token + " expected " + f.getNumParams() + " parameters, got " + numParams);
                    }
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
