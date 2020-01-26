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
import java.util.Collections;
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


/**
 * <h1>Fundamental components of <code>scarpet</code> programming language (towards version 1.7).</h1>
 *
 * <p>Scarpet (a.k.a. Carpet Script, or Script for Carpet) is a programming language designed to provide
 * the ability to write custom programs to run within Minecraft and
 * interact with the world.</p>
 *
 * <p>The project was initially built based on the EvalEx project,
 * however it now diverged so far away from the original implementation,
 * it would be hard to tell it without this mention.
 * EvalEx is a handy expression evaluator for Java, that
 * allows to evaluate simple mathematical and boolean expressions.
 * For more information, see:
 * <a href="https://github.com/uklimaschewski/EvalEx">EvalEx GitHub
 * repository</a></p>
 *
 * <p>This specification is divided into two sections: this one is agnostic
 * to any Minecraft related features and could function on its own, and CarpetExpression for
 * Minecraft specific routines and world manipulation functions.</p>
 *
 * <h1>Synopsis</h1>
 *
 * <pre>
 * script run print('Hello World!')
 * </pre>
 * <p>or an OVERLY complex example:</p>
 * <pre>
 * /script run
 *     block_check(x1, y1, z1, x2, y2, z2, block_to_check) -&gt;
 *     (
 *         l(minx, maxx) = sort(l(x1, x2));
 *         l(miny, maxy) = sort(l(y1, y2));
 *         l(minz, maxz) = sort(l(z1, z2));
 *         'Need to compute the size of the area of course';
 *         'Cause this language doesn\'t support comments in the command mode';
 *         xsize = maxx - minx + 1;
 *         ysize = maxy - miny + 1;
 *         zsize = maxz - minz + 1;
 *         total_count = 0;
 *         loop(xsize,
 *             xx = minx + _ ;
 *             loop(ysize,
 *                 yy = miny + _ ;
 *                 loop(zsize,
 *                     zz = minz + _ ;
 *                     if ( block(xx,yy,zz) == block_to_check,
 *                         total_count += ceil(rand(1))
 *                     )
 *                 )
 *             )
 *         );
 *         total_count
 *     );
 *     check_area_around_closest_player_for_block(block_to_check) -&gt;
 *     (
 *         closest_player = player();
 *         l(posx, posy, posz) = query(closest_player, 'pos');
 *         total_count = block_check( posx-8,1,posz-8, posx+8,17,posz+8, block_to_check);
 *         print('There is '+total_count+' of '+block_to_check+' around you')
 *     )
 *
 * /script invoke check_area_around_closest_player_for_block 'diamond_ore'
 *
 * </pre>
 * <p>or simply</p>
 * <pre>
 * script run print('There is'+for(rect(x,9,z,8,8,8), _ == 'diamond_ore')+' diamond ore around you')
 * </pre>
 * <p>It definitely pays to check what higher level <code>scarpet</code> functions have to offer.</p>
 * <h1>Programs</h1>
 * <p>
 * You can think of an program like a mathematical expression, like
 * <code>"2.4*sin(45)/(2-4)"</code> or  <code>"sin(y)&gt;0 &amp; max(z, 3)&gt;3"</code>
 * Writing a program, is like writing a <code>2+3</code>, just a bit longer</p>
 *
 * <h2>Basic language components</h2>
 * <p>Programs consist of constants, like <code>2</code>, <code>3.14</code>, <code>pi</code>, or <code>'foo'</code>,
 * operators like <code>+</code>, <code>/</code>, <code>-&gt;</code>, variables which you can define, like <code>foo</code>
 * or special ones that will be defined for you, like <code>_x</code>, or <code>_</code> , which I specific to
 * a each built in function, and functions with name, and arguments in the form of <code>f(a,b,c)</code>, where
 * <code>f</code> is the function name, and <code>a, b, c</code> are the arguments which can be any other expression.
 * And that's all the parts of the language, so all in all - sounds quite simple.</p>
 *
 * <h2>Code flow</h2>
 * <p>
 *     Like any other proper programming language, <code>scarpet</code> needs brackets, basically to identify
 *     where stuff begins and where it ends. In the languages that uses much more complicated constructs, like Java,
 *     they tend to use all sort of them, round ones to indicate function calls, curly to indicate section of code,
 *     square to access lists, pointy for generic types etc... I mean - there is no etc, cause they have exhausted
 *     all the bracket options...
 * </p>
 * <p><code>Scarpet</code> is different, since it runs everything based on functions (although its not per se a functional language like lisp)
 * only needs the round brackets for everything, and it is up to the programmer to organize its code so its readable,
 * as adding more brackets does not have any effect on the performance of the programs as they are compiled before they are executed.
 * Look at the following example usage of <code>if()</code> function:
 * </p>
 * <pre>
 * if(x&lt;y+6,set(x,8+y,z,'air');plop(x,top('surface',x,z),z,'birch'),sin(query(player(),'yaw'))&gt;0.5,plop(0,0,0,'boulder'),particle('fire',x,y,z))
 * </pre>
 * <p>Would you prefer to read</p>
 * <pre>
 * if(   x&lt;y+6,
 *            set(x,8+y,z,'air');
 *            plop(x,top('surface',x,z),z,'birch'),
 *       sin(query(player(),'yaw'))&gt;0.5,
 *            plop(0,0,0,'boulder'),
 *       particle('fire',x,y,z)
 * )
 * </pre>
 * <p>Or rather:</p>
 * <pre>
 * if
 * (
 *     x&lt;y+6,
 *     (
 *         set(x,8+y,z,'air');
 *         plop(x,top('surface',x,z),z,'birch')
 *     ),
 *
 *     sin(query(player(),'yaw'))&gt;0.5,
 *     (
 *         plop(0,0,0,'boulder')
 *     ),
 *
 *     particle('fire',x,y,z)
 * )
 * </pre>
 * <p>Whichever style you prefer it doesn't matter. It typically depends on the situation and the complexity of the
 * subcomponents. No matter how many whitespaces and extra brackets you add - the code will evaluate to exactly the
 * same expression, and will run exactly the same, so make sure your programs are nice and clean so others don't
 * have problems with them</p>
 *
 * <h2>Functions and scoping</h2>
 * <p>
 * Users can define functions in the form <code>fun(args....) -&gt; expression </code> and they are compiled and saved
 * for further execution in this but also subsequent calls of /script command. Functions can also be assigned to variables,
 * passed as arguments, called with <code>call</code> function, but in most cases you would want to call them directly
 * by name, in the form of <code>fun(args...)</code>.
 * This means that once defined functions
 * are saved with the world for further use. For variables, there are two types of them,
 * global - which are shared anywhere in the code, and those are all which name starts with 'global_', and
 * local variables which is everything else and those are only visible inside each function.
 * This also means that all the parameters in functions are passed 'by value', not 'by reference'.
 * </p>
 *
 * <h2>Outer variables</h2>
 * <p>Functions can still 'borrow' variables from the outer scope,
 * by adding them to the function signature wrapped around built-in function <code>outer</code>.
 * It adds the specified value to the function call stack so they behave exactly like capturing lambdas in Java, but
 * unlike java captured variables don't need to be final. Scarpet will just attach their new values at the time of the
 * function definition, even if they change later. Most value will be copied, but mutable values, like maps or lists, allow
 * to keep the 'state' with the function, allowing them to have memory and act like objects so to speak.
 * . Check <code>outer(var)</code> for details.</p>
 *
 *
 * <h2>Code delivery, line indicators</h2>
 * <p>Note that this should only apply to pasting your code to execute with commandblock. Scarpet recommends placing your
 * code in apps (files with <code>.sc</code> extension that can be placed inside "/scripts" folder in the world files
 * and loaded as a scarpet app with command <code>/script load [app_name]</code>. Scarpet apps loaded from disk should only
 * contain code, no need to start with "/script run" prefix</p>
 * <p>The following is the code that could be provided in a <code>foo.sc</code> app file located in world <code>/scripts</code> folder</p>
 *
 * <pre>
 * run_program() -&gt; (
 *   loop( 10,
 *     // looping 10 times
 *     // comments are allowed in scripts located in world files
 *     // since we can tell where that line ends
 *     foo = floor(rand(10));
 *     check_not_zero(foo);
 *     print(_+' - foo: '+foo);
 *     print('  reciprocal: '+  _/foo )
 *   )
 * );
 * check_not_zero(foo) -&gt; (
 *   if (foo==0, foo = 1)
 * )
 * </pre>
 * <p>Which we then call in-game with:</p>
 * <pre>
 *     /script load foo
 *     /script in foo invoke run_program
 * </pre>
 * <p>However the following code can also be input as a command, or in a command block.</p>
 *
 * <p>Since the maximum command that can be input to the chat is limited in length, you will be probably inserting your
 * programs by pasting them to command blocks or reading from world files, however pasting to command blocks will remove some whitespaces and squish
 * your newlines making the code not readable. If you are pasting a program that is perfect and will never cause an error,
 * I salute you, but for the most part it is quite likely that your program might break, either at compile time, when
 * its initially analyzed, or at execute time, when you suddenly attempt to divide something by zero. In these cases
 * you would want to get a meaningful error message, but for that you would need to indicate for the compiler where
 * did you put these new lines, since command block would squish them. For that, place  at the beginning
 * of the line to let the compiler know where are you. This makes so that <code>$</code> is the only character that is
 * illegal in programs, since it will be replaced with new lines. As far as I know, <code>$</code> is not used
 * anywhere inside Minecraft identifiers, so this shouldn't hinder the abilities of your programs.</p>
 * <p>Consider the following program executed as command block command:</p>
 * <pre>
 * /script run
 * run_program() -&gt; (
 *   loop( 10,
 *     foo = floor(rand(_));
 *     check_not_zero(foo);
 *     print(_+' - foo: '+foo);
 *     print('  reciprocal: '+  _/foo )
 *   )
 * );
 * check_not_zero(foo) -&gt; (
 *    if (foo==0, foo = 1)
 * )
 *
 * </pre>
 * <p>Lets say that the intention was to check if the bar is zero and prevent division by zero in print,
 * but because the <code>foo</code> is passed as a variable, it never changes the original foo value.
 * Because of the inevitable division by zero, we get the following message:
 * </p>
 * <pre>
 * Your math is wrong, Incorrect number format for NaN at pos 98
 * run_program() -> ( loop( 10, foo = floor(rand(_)); check_not_zero(foo); print(_+' - foo: '+foo);
 * HERE>> print(' reciprocal: '+ _/foo ) ));check_not_zero(foo) -> ( if (foo==0, foo = 1))
 * </pre>
 *
 * As we can see, we got our problem where the result of the mathematical operation was not a number (infinity, so not a number),
 * however by pasting our program
 * into the command made it squish the newlines so while it is clear where the error happened and we still can track the error down,
 * the position of the error (98) is not very helpful and wouldn't be useful if the program gets significantly longer.
 * To combat this issue we can precede every line of the script with dollar signs <code>$</code>:
 * <pre>
 * /script run
 * $run_program() -&gt; (
 * $  loop( 10,
 * $    foo = floor(rand(_));
 * $    check_not_zero(foo);
 * $    print(_+' - foo: '+foo);
 * $    print('  reciprocal: '+  _/foo )
 * $  )
 * $);
 * $check_not_zero(foo) -&gt; (
 * $   if (foo==0, foo = 1)
 * $)
 * </pre>
 *
 * <p>Then we get the following error message</p>
 *
 * <pre>
 * Your math is wrong, Incorrect number format for NaN at line 7, pos 2
 *   print(_+' - foo: '+foo);
 *    HERE>> print(' reciprocal: '+ _/foo )
 *   )
 * </pre>
 *
 *
 * <p>As we can note not only we get much more concise snippet, but also information about the line
 * number and position, so means its way easier to locate the potential problems problem</p>
 *
 * <p>Obviously that's not the way we intended this program to work. To get it <code>foo</code> modified via
 * a function call, we would either return it as a result and assign it to the new variable:
 * </p>
 * <pre>
 *     foo = check_not_zero(foo);
 *     ...
 *     check_not_zero(foo) -&gt; if(foo == 0, 1, foo)
 * </pre>
 * <p>.. or convert it to a global variable, which in this case passing as an argument is not required</p>
 * <pre>
 *     global_foo = floor(rand(10));
 *     check_foo_not_zero();
 *     ...
 *     check_foo_not_zero() -&gt; if(global_foo == 0, global_foo = 1)
 * </pre>
 * </p>
 */

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
    protected static final Random randomizer = new Random();

    static final Value PI = new NumericValue(
            "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

    static final Value euler = new NumericValue(
            "2.71828182845904523536028747135266249775724709369995957496696762772407663");

    // %[argument_index$][flags][width][.precision][t]conversion
    private static final Pattern formatPattern = Pattern.compile("%(\\d+\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

    /** The current infix expression */
    private String expression;
    String getCodeString() {return expression;}

    private boolean allowNewlineSubstitutions = true;
    private boolean allowComments = false;

    public Module module = null;

    void asATextSource()
    {
        allowNewlineSubstitutions = false;
        allowComments = true;
    }

    void asAModule(Module mi)
    {

        module = mi;
    }

    /** Cached AST (Abstract Syntax Tree) (root) of the expression */
    private LazyValue ast = null;

    /** script specific operatos and built-in functions */
    private final Map<String, ILazyOperator> operators = new Object2ObjectOpenHashMap<>();
    boolean isAnOperator(String opname) { return operators.containsKey(opname) || operators.containsKey(opname+"u");}

    private final Map<String, ILazyFunction> functions = new  Object2ObjectOpenHashMap<>();
    Set<String> getFunctionNames() {return functions.keySet();}

    static List<String> getExpressionSnippet(Tokenizer.Token token, Expression expr)
    {
        String code = expr.getCodeString();
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


    private void addLazyUnaryOperator(String surface, int precedence, boolean leftAssoc,
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


    private void addLazyBinaryOperatorWithDelegation(String surface, int precedence, boolean leftAssoc,
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

    private void addLazyFunctionWithDelegation(String name, int numpar,
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

    private void addLazyBinaryOperator(String surface, int precedence, boolean leftAssoc,
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

    static RuntimeException handleCodeException(Context c, RuntimeException exc, Expression e, Tokenizer.Token token)
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

    private void addUnaryOperator(String surface, boolean leftAssoc, Function<Value, Value> fun)
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

    private void addBinaryOperator(String surface, int precedence, boolean leftAssoc, BiFunction<Value, Value, Value> fun)
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


    private void addUnaryFunction(String name, Function<Value, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name,  new AbstractFunction(1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return fun.apply(Value.assertNotNull(parameters.get(0)));
            }
        });
    }

    void addBinaryFunction(String name, BiFunction<Value, Value, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
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

    private void addFunction(String name, Function<List<Value>, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
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

    private void addMathematicalUnaryFunction(String name, Function<Double, Double> fun)
    {
        addUnaryFunction(name, (v) -> new NumericValue(fun.apply(NumericValue.asNumber(v).getDouble())));
    }

    private void addMathematicalBinaryFunction(String name, BiFunction<Double, Double, Double> fun)
    {
        addBinaryFunction(name, (w, v) ->
                new NumericValue(fun.apply(NumericValue.asNumber(w).getDouble(), NumericValue.asNumber(v).getDouble())));
    }


    void addLazyFunction(String name, int num_params, TriFunction<Context, Integer, List<LazyValue>, LazyValue> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
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
    private FunctionValue addContextFunction(Context context, String name, Expression expr, Tokenizer.Token token, List<String> arguments, List<String> outers, LazyValue code)
    {
        name = name.toLowerCase(Locale.ROOT);
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
        if (!name.equals("_")) context.host.addUserDefinedFunction(module, name, result);
        return result;
    }

    /**
     * <h1>Variables and Constants</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p><code>scarpet</code> provides a number of constants that can be used literally in scripts</p>
     * <ul>
     *     <li><code>null</code>: nothing, zilch, not even false</li>
     *     <li><code>true</code>: pure true, or just 1 (one)</li>
     *     <li><code>false</code>: false truth, or true falsth, 0 (zero) actually</li>
     *     <li><code>pi</code>: for the fans of perimeters, its a perimeter of an apple pi of diameter 1. About 3.14</li>
     *     <li><code>euler</code>: clever guy. Derivative of its exponent is goto 1. About 2.72</li>
     * </ul>
     * <p>Apart from that, there is a bunch of system variables, that start with <code>_</code> that are set by
     * <code>scarpet</code> built-ins, like <code>_</code>, typically each consecutive value in loops,
     * <code>_i</code> indicating iteration, or <code>_a</code> like an accumulator for <code>reduce</code>
     * function. Certain calls to Minecraft specific calls would also set <code>_x</code>,
     * <code>_y</code>, <code>_z</code>, indicating block positions. All variables starting with
     * <code>_</code> are read-only, and cannot be declared and modified in client code.</p>
     *
     * <h2>Literals</h2>
     * <p><code>scarpet</code> accepts numeric and string liters constants.
     * Numbers look like <code>1, 2.5, -3e-7, 0xff, </code> and are internally represented as Java's <code>double</code>
     * but <code>scarpet</code> will try to trim trailing zeros as much as possible so if you need to use them as intergers,
     * you can. Strings using single quoting, for multiple reasons, but primarily to allow for easier use of strings inside
     * doubly quoted command arguments (when passing a script as a parameter of <code>/script fill</code> for example,
     * or when typing in jsons inside scarpet to feed back into a <code>/data merge</code> command for example. Strings also
     * use backslashes <code>\</code> for quoting special characters, in both plain strings and regular expressions</p>
     * <pre>
     * 'foo'
     * print('This doesn\'t work')
     * nbt ~ '\\.foo'   // matching '.' as a '.', not 'any character match'
     * </pre>
     * </div>
     */

    public void VariablesAndConstants() // public just to get the Javadocs right
    {
        // all declared as global variables to save on switching scope cost
    }

    /**
     * <h1>User-defined functions and program control flow</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <h2>Writing programs with more than 1 line</h2>
     * <h3><code>Operator ;</code></h3>
     * <p>To effectively write programs that have more than one line, a programmer needs way to specify a sequence of
     * commands that execute one after another. In <code>scarpet</code> this can be achieved with <code>;</code>. Its an operator,
     * and by separating statements with semicolons. And since whitespaces and <code>$</code> signs are all treats as
     * whitespaces, how you layout your code doesn't matter, as long as it is readable to everyone involved.</p>
     * <pre>
     * expr;
     * expr;
     * expr;
     * expr
     * </pre>
     * <p>Notice that the last expression is not followed by a semicolon. Since instruction separation is functional
     * in <code>scarpet</code>, and not barely an instruction delimiter, terminating the code with a dangling operator
     * wouldn't be valid. Having said that, since many programming languages don't care about the number of op terminators
     * programmers use, carpet preprocessor will remove all unnecessary semicolons from scripts when compiled.</p>
     * <p>In general <code>expr; expr; expr; expr</code> is equivalent to
     * <code>(((expr ; expr) ; expr) ; expr)</code>.</p>
     * <p>Result of the evaluated expression is the same as the result of the second expression, but first expression is
     * also evaluated for sideeffects</p>
     * <pre>
     * expr1 ; expr2 =&gt; expr2  // with expr1 as a sideeffect
     * </pre>
     * <h2>Global variables</h2>
     * <p>All defined functions are compiled, stored persistently, and available globally -
     * accessible to all other scripts. Functions can only be undefined via call to <code>undef('fun')</code>, which
     * would erase global entry for function <code>fun</code>. Since all variables have local scope inside each function,
     * one way to share large objects is via global variables
     * </p>
     * <p>Any variable that is used with a name that starts with <code>'global_'</code> will be stored and accessible globally,
     * not, inside current scope. It will also persist across scripts, so if a procedure needs to use its own construct, it needs to
     * define it, or initialize it explicitly, or undefine it via <code>undef</code></p>
     * <pre>
     * a() -&gt; global_list+=1; global_list = l(1,2,3); a(); a(); global_list  // =&gt; [1,2,3,1,1]
     * </pre>
     * <h3><code>Operator -&gt;</code></h3>
     * <p>To organize code better than a flat sequence of operations, one can define functions. Definition is correct if
     * has the following form</p>
     * <pre>
     *     fun(args, ...) -&gt; expr
     * </pre>
     * <p>Where <code>fun(args, ...)</code> is a function signature indicating function name, number of arguments,
     * and their names, and expr is an expression (can be complex) that is evaluated when <code>fun</code> is called.
     * Names in the signature don't need to be used anywhere else, other occurrences of these names
     * will be masked in this function scope.
     * Function call creates new scope for variables inside <code>expr</code>, so all non-global variables are not
     * visible from the caller scope. All parameters are passed by value to the new scope, including lists and other
     * containers, however their copy will be shallow.</p>
     * <p>The function returns its name as a string, which means it can be used to call it later with the <code>call</code> function</p>
     * <p>Using <code>_</code> as the function name creates anonymous function, so each time <code>_</code> function is defined,
     * it will be given a unique name, which you can pass somewhere else to get this function <code>call</code>ed.</p>
     * <pre>
     * a(lst) -&gt; lst+=1; list = l(1,2,3); a(list); a(list); list  // =&gt; [1,2,3]
     * </pre>
     * <p>In case the inner function wants to operate and modify larger objects, lists from the outer
     * scope, but not global, it needs to use <code>outer</code> function in function signature</p>
     * <h3><code>outer(arg)</code></h3>
     * <p><code>outer</code> function can only be used in the function signature, and it will
     * cause an error everywhere else. It saves the value of that variable from the outer scope and allows
     * its use in the inner scope. This is a similar behaviour to using outer variables in lambda function definitions
     * from Java, except here you have to specify which variables you want to use, and borrow</p>
     * <p>This mechanism can be used to use static mutable objects without the need of using <code>global_...</code> variables</p>
     * <pre>
     * list = l(1,2,3); a(outer(list)) -&gt; list+=1;  a(); a(); list  // =&gt; [1,2,3,1,1]
     * </pre>
     * <p>The return value of a function is the value of the last expression. This as the same effect as using outer
     * or global lists, but is more expensive</p>
     * <pre>
     * a(lst) -&gt; lst+=1; list = l(1,2,3); list=a(list); list=a(list); list  // =&gt; [1,2,3,1,1]
     * </pre>
     * <p>Ability to combine more statements into one expression, with functions, passing parameters, and global and outer
     * scoping allow to organize even larger scripts</p>
     * <h3><code>import(module_name, symbols ...)</code></h3>
     * <p>Imports symbols from other apps and libraries into the current one: global variables or functions, allowing
     * to use them in the current app. This include
     * other symbols imported by these modules. Scarpet supports cicular dependencies, but if symbols are used directly in
     * the module body rather than functions, it may not be able to retrieve them. Returns full list of available symbols
     * that could be imported from this module, which can be used to debug import issues, and list contents of libraries.
     * </p>
     * <h3><code>call(function, args.....)</code></h3>
     * <p>calls a user defined function with specified arguments. It is equivalent to calling <code>function(args...)</code>
     * directly except you can use it with function value, or name instead. This means you can pass functions to other user defined
     * functions as arguments and call them with <code>call</code> internally. And since function definitions return the
     * defined function, they can be defined in place as anonymous functions.</p>
     * <p>Little technical note: the use of <code>_</code> in expression passed to built in functions is much more efficient due to
     * not creating new call stacks for each invoked function, but anonymous functions is the only mechanism available
     * for programmers with their own lambda arguments</p>
     * <pre>
     * my_map(list, function) -&gt; map(list, call(function, _));
     * my_map(l(1,2,3), _(x) -&gt; x*x);    // =&gt; [1,4,9]
     *
     * profile_expr(my_map(l(1,2,3), _(x) -&gt; x*x));   // =&gt; ~32000
     * sq(x) -&gt; x*x; profile_expr(my_map(l(1,2,3), 'sq'));   // =&gt; ~36000
     * sq = (_(x) -&gt; x*x); profile_expr(my_map(l(1,2,3), sq));   // =&gt; ~36000
     * profile_expr(map(l(1,2,3), _*_));   // =&gt; ~80000
     *
     * </pre>
     * <h2>Control flow</h2>
     * <h3><code>return(expr?)</code></h3>
     * <p>Sometimes its convenient to break the organized control flow, or it is not practical to pass
     * the final result value of a function to the last statement, in this case a return statement can be used</p>
     * <p>If no argument is provided - returns null value.</p>
     * <pre>
     * def() -&gt; (
     *  expr1;
     *  expr2;
     *  return(expr3); // function terminates returning expr3
     *  expr4;     // skipped
     *  expr5      // skipped
     * )
     * </pre>
     * <p>In general its cheaper to leave the last expression as a return value, rather than calling returns everywhere,
     * but it would often lead to a messy code.</p>
     * <h3><code>exit(expr?)</code></h3>
     * <p>It terminates entire program passing <code>expr</code> as the result of the program execution, or null if omitted.</p>
     * <h3><code>try(expr, catch_expr(_)?) ... throw(value?)</code></h3>
     * <p><code>try</code> function evaluates expression, and continues further unless <code>throw</code> function is called
     * anywhere inside <code>expr</code>. In that case the <code>catch_expr</code> is evaluates with <code>_</code> set
     * to the argument <code>throw</code> was called with.
     * This mechanic accepts skipping thrown value - it throws null instead, and catch expression - then try returns null as well
     * This mechanism allows to terminate large portion of a convoluted
     * call stack and continue program execution. There is only one level of exceptions currently in carpet, so if the inner
     * function also defines the <code>try</code> catchment area, it will received the exception first, but it can technically
     * rethrow the value its getting for the outer scope. Unhandled throw acts like an exit statement.</p>
     * <h3><code>if(cond, expr, cond?, expr?, ..., default?)</code></h3>
     * <p>If statement is a function that takes a number of conditions that are evaluated one after another and if
     * any of them turns out true, its <code>expr</code> gets returned, otherwise, if all conditions fail, the return value is
     * <code>default</code> expression, or <code>null</code> if default is skipped</p>
     * <p><code>if</code> function is equivalent to <code>if (cond) expr; else if (cond) expr; else default;</code>
     * from Java, just in a functional form </p>
     * </div>
     */

    public void UserDefinedFunctionsAndControlFlow() // public just to get the javadoc right
    {
        // artificial construct to handle user defined functions and function definitions
        addLazyFunction("import", -1, (c, t, lv) ->
        {
            if (lv.size() < 1) throw new InternalExpressionException("'import' needs at least a module name to import, and list of values to import");
            String moduleName = lv.get(0).evalValue(c).getString();
            c.host.importModule(c, moduleName);
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
                Value functionValue = lv.get(0).evalValue(c);
                if (!(functionValue instanceof FunctionValue))
                {
                    String name = functionValue.getString();
                    functionValue = c.host.getAssertFunction(module, name);
                }
                List<LazyValue> lvargs = new ArrayList<>(lv.size()-1);
                for (int i=1; i< lv.size(); i++)
                {
                    lvargs.add(lv.get(i));
                }
                FunctionValue fun = (FunctionValue)functionValue;
                Value retval = fun.callInContext(expr, c, t, fun.getExpression(), fun.getToken(), lvargs).evalValue(c);
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

    /**
     * <h1>Operators</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     *
     * <p>There is a number of operators you can use inside the expressions. Those could be considered
     * generic type operators that apply to most data types. They also follow standard operator
     * precedence, i.e. <code>2+2*2</code> is understood as <code>2+(2*2)</code>,
     * not <code>(2+2)*2</code>, otherwise they are applied from left to right, i.e.
     * <code>2+4-3</code> is interpreted as <code>(2+4)-3</code>, which in case of numbers
     * doesn't matter, but since <code>scarpet</code> allows for mixing all value types
     * the associativity would matter, and may lead to unintended effects: </p>
     * <p>Important operator is function definition <code>-&gt;</code> operator. It will be covered
     * in {@link carpet.script.Expression#UserDefinedFunctionsAndControlFlow}</p>
     * <pre>
     * '123'+4-2 =&gt; ('123'+4)-2 =&gt; '1234'-2 =&gt; '134'
     * '123'+(4-2) =&gt; '123'+2 =&gt; '1232'
     * 3*'foo' =&gt; 'foofoofoo'
     * 1357-5 =&gt; 1352
     * 1357-'5' =&gt; 137
     * 3*'foo'-'o' =&gt; 'fff'
     * l(1,3,5)+7 =&gt; l(8,10,12)
     * </pre>
     * <p>As you can see, values can behave differently when mixed with other types
     * in the same expression. in case values are of the same types, the result
     * tends to be obvious, but <code>Scarpet</code> tries to make sense of whatever it
     * has to deal with</p>
     * <h2>Operator Precedence</h2>
     * <p>
     * Here is the complete list of operators in <code>scarpet</code> including control flow operators.
     * Note, that commas and brackets are not technically operators, but part of the language,
     * even if they look like them:
     * </p>
     * <ul>
     *     <li>Match, Get <code>~ :</code></li>
     *     <li>Unary <code>+ - !</code></li>
     *     <li>Exponent <code>^</code></li>
     *     <li>Multiplication <code>* / %</code></li>
     *     <li>Addition <code>+ -</code></li>
     *     <li>Comparison <code>== != &gt; &gt;= &lt;= &lt;</code></li>
     *     <li>Logical And<code>&amp;&amp;</code></li>
     *     <li>Logical Or <code>||</code></li>
     *     <li>Assignment <code>= += &lt;&gt;</code></li>
     *     <li>Definition <code>-&gt;</code></li>
     *     <li>Next statement<code>;</code></li>
     *     <li>Comma <code>,</code></li>
     *     <li>Bracket <code>( )</code></li>
     * </ul>
     *
     * <h3><code>Get, Accessor Operator  :</code></h3>
     * <p>Operator version of the <code>get(...)</code> function to access elements of lists, maps, and potentially other
     * containers (i.e. NBTs). It is important to distinguish from <code>~</code> operator, which is a matching operator,
     * which is expected to perform some extra computations to retrieve the result, while <code>:</code> should be
     * straightforward and immediate, and the source object should behave like a container and support full container
     * API, meaning <code>get(...)</code>, <code>put(...)</code>, <code>delete(...)</code>,
     * and <code>has(...)</code> functions</p>
     * <p>For certain operators and functions (get, put, delete, has, =, +=) objects can use <code>:</code> annotated
     * fields as l-values, meaning construct like <code>foo:0 = 5</code>, would act like <code>put(foo, 0, 5)</code>,
     * rather than <code>get(foo, 0) = 5</code>, which would result in an error.</p>
     * <p>TODO: add more information about l-value behaviour.</p>
     *
     * <h3><code>Matching Operator  ~</code></h3>
     * <p>This operator should be understood as 'matches', 'contains', 'is_in',
     * or 'find me some stuff about something else. For strings it matches the right operand as a regular
     * expression to the left, returning the first match. This can be used to extract information from unparsed nbt's
     * in a more convoluted way (use <code>get(...)</code> for more appropriate way of doing it).
     * For lists it checks if an element is in the list, and returns the index of that element,
     * or <code>null</code> if no such element was found, especially that the use of <code>first(...)</code> function will not
     * return the index. Currently it doesn't have any special behaviour for numbers - it checks for existence of characters
     * in string representation of the left operand with respect of the regular expression on the right hand side.
     * string</p>
     * <p>in Minecraft API portion <code>entity ~ feature</code> is a shortcode for <code>query(entity,feature)</code>
     * for queries that do not take any extra arguments.</p>
     * <pre>
     * l(1,2,3) ~ 2  =&gt; 1
     * l(1,2,3) ~ 4  =&gt; null
     * 'foobar' ~ '.b'  =&gt; 'ob'
     * player('*') ~ 'gnembon'  // null unless player gnembon is logged in (better to use player('gnembon') instead
     * p ~ 'sneaking' // if p is an entity returns whether p is sneaking
     * </pre>
     * <p>Or a longer example of an ineffective way to searching for a squid</p>
     * <pre>
     * entities = entities_area('all',x,y,z,100,10,100);
     * sid = entities ~ 'Squid';
     * if(sid != null, run('execute as '+query(get(entities,sid),'id')+' run say I am here '+query(get(entities,sid),'pos') ) )
     * </pre>
     * <p>Or an example to find if a player has specific enchantment on a held axe (either hand) and get its level
     * (not using proper NBTs query support via <code>get(...)</code>):</p>
     * <pre>
     * global_get_enchantment(p, ench) -&gt; (
     * $   for(l('main','offhand'),
     * $      holds = query(p, 'holds', _);
     * $      if( holds,
     * $         l(what, count, nbt) = holds;
     * $         if( what ~ '_axe' &amp;&amp; nbt ~ ench,
     * $            lvl = max(lvl, number(nbt ~ '(?&lt;=lvl:)\\d') )
     * $         )
     * $      )
     * $   );
     * $   lvl
     * $);
     * /script run global_get_enchantment(players(), 'sharpness')
     * </pre>
     *
     * <h3><code>Basic Arithmetic Operators  +  -  *  /</code></h3>
     * <p>Allows to add the results of two expressions. If the operands resolve to numbers, the result is
     * arithmetic operation.
     * In case of strings, adding or subtracting from a string results in string concatenation and
     * removal of substrings from that string. Multiplication of strings and numbers results in repeating the
     * string N times and division results in taking the first k'th part of the string, so that <code>str*n/n ~ str</code>
     * In case first operand is a list, either it results in a new list
     * with all elements modified one by one with the other operand, or if the operand is a list with the same number of
     * items - elementwise addition/subtraction</p>
     * <p>Examples:</p>
     * <pre>
     * 2+3 =&gt; 5
     * 'foo'+3+2 =&gt; 'foo32'
     * 'foo'+(3+2) =&gt; 'foo5'
     * 3+2+'bar' =&gt; '5bar'
     * 'foo'*3 =&gt; 'foofoofoo'
     * 'foofoofoo' / 3 =&gt; 'foo'
     * 'foofoofoo'-'o' =&gt; 'fff'
     * l(1,2,3)+1  =&gt; l(2,3,4)
     * b = l(100,63,100); b+l(10,0,10)  =&gt; l(110,63,110)
     * </pre>
     *
     * <h3><code>Just Operators  %  ^</code></h3>
     * <p>The modulo and exponent (power) operators work only if both operands are numbers</p>
     * <pre>
     * pi^pi%euler  =&gt; 1.124....
     * -9 % 4  =&gt; -1
     * 9 % -4  =&gt; 0 ¯\_(ツ)_/¯ Java
     * -3 ^ 2  =&gt; 9
     * -3 ^ pi =&gt; // Error
     * </pre>
     *
     * <h3><code>Comparison Operators  ==  !=  &lt;  &gt;  &lt;=  &gt;=</code></h3>
     * <p>Allows to compare the results of two expressions.
     * For numbers it is considers arithmetic order of numbers, for strings - lexicographical,
     * nulls are always 'less' than everything else, and lists check their elements - if the sizes
     * are different, the size matters, otherwise, pairwise comparisons for each elements are performed.
     * The same order rules than with all these operators are used with the default sortographical order as
     * used by <code>sort</code> function. All of these are true:
     * </p>
     * <pre>
     * null == null
     * null != false
     * 0 == false
     * 1 == true
     * null &lt; 0
     * null &lt; -1000
     * 1000 &lt; 'a'
     * 'bar' &lt; 'foo'
     * 3 == 3.0
     * </pre>
     *
     * <h3><code>Logical Operators  &amp;&amp;   ||</code></h3>
     * <p>These operator compute respective boolean operation on the operands. What it important is that if calculating
     * of the second operand is not necessary, it won't be evaluated, which means one can use them as conditional
     * statements. In case of success returns first positive operand (<code>||</code>) or last one (<code>&amp;&amp;</code>).</p>
     * <pre>
     * true || false  =&gt; 1
     * null || false =&gt; 0
     * null != false || run('kill gnembon')  =&gt; 1 // gnembon survives
     * null != false &amp;&amp; run('kill gnembon')  =&gt; 0 // when cheats not allowed
     * null != false &amp;&amp; run('kill gnembon')  =&gt; 1 // gnembon dies, cheats allowed
     * </pre>
     *

     * <h3><code>Assignment Operators  =  &lt;&gt;  +=</code></h3>
     * <p>A set of assignment operators. All require bounded variable on the LHS, <code>&lt;&gt;</code> requires
     * bounded arguments on the right hand side as well (bounded, meaning being variables). Additionally they can also
     * handle list constructors with all bounded variables, and work then as list assignment operators.
     * When <code>+=</code> is used on a list, it extends that list of that element, and returns the list (old == new).
     * <code>scarpet</code> doesn't support currently removal of items. Removal of items can be obtaine via
     * <code>filter</code> command, and reassigning it fo the same variable. Both operations would require rewriting of the
     * array anyways.</p>
     * <pre>
     * a = 5  =&gt; a == 5
     * l(a,b,c) = l(3,4,5) =&gt; a==3, b==4, c==5
     * l(minx,maxx) = sort(xi,xj);  // minx assumes min(xi, xj) and maxx, max(xi, xj)
     * l(a,b,c,d,e,f) = l(range(6)); l(a,b,c) &lt;&gt; l(d,e,f); l(a,b,c,d,e,f)  =&gt; [3,4,5,0,1,2]
     * a = l(1,2,3); a += 4  =&gt; [1,2,3,4]
     * a = l(1,2,3,4); a = filter(a,_!=2)  =&gt; [1,3,4]
     * </pre>
     * <h3><code>Unary Operators  -  +</code></h3>
     * <p>Require a number, flips the sign. One way to assert it's a number is by crashing the script. gg.</p>
     * <pre>
     * -4  =&gt; -4
     * +4  =&gt; 4
     * +'4'  // Error message
     * </pre>
     *
     * <h3><code>Negation Operator  !</code></h3>
     * <p>flips boolean condition of the expression. Equivalent of <code>bool(expr)==false</code></p>
     * <pre>
     * !true  =&gt; 0
     * !false  =&gt; 1
     * !null  =&gt; 1
     * !5  =&gt; 0
     * !l() =&gt; 1
     * !l(null) =&gt; 0
     * </pre>
     * </div>
     */
    public void Operators()
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
                Value res = cvi.get(key).add(v2);
                cvi.put(key, res);
                return (cc, tt) -> res;
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

    /**
     * <h1>Arithmetic operations</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <h2>Basic Arithmetic Functions</h2>
     * <p>There is bunch of them - they require a number and spit out a number,
     * doing what you would expect them to do.</p>
     * <h3><code>fact(n)</code></h3>
     * <p>Factorial of a number, a.k.a <code>n!</code>, just not in <code>scarpet</code>. Gets big... quick...</p>
     * <h3><code>sqrt(n)</code></h3>
     * <p>Square root. For other fancy roots, use <code>^</code>, math and yo noggin. Imagine square roots on a tree...</p>
     * <h3><code>abs(n)</code></h3>
     * <p>Absolut value.</p>
     * <h3><code>round(n)</code></h3>
     * <p>Closest integer value. Did you know the earth is also round?</p>
     * <h3><code>floor(n)</code></h3>
     * <p>Highest integer that is still no larger then <code>n</code>. Insert a floor pun here.</p>
     * <h3><code>ceil(n)</code></h3>
     * <p>First lucky integer that is not smalller than <code>n</code>. As you would expect, ceiling is typically
     * right above the floor.</p>
     * <h3><code>ln(n)</code></h3>
     * <p>Natural logarithm of <code>n</code>. Naturally.</p>
     * <h3><code>ln1p(n)</code></h3>
     * <p>Natural logarithm of <code>n+1</code>. Very optimistic.</p>
     * <h3><code>log10(n)</code></h3>
     * <p>Decimal logarithm of <code>n</code>. Its ceiling is the length of its floor.</p>
     * <h3><code>log(n)</code></h3>
     * <p>Binary logarithm of <code>n</code>. Finally, a proper one, not like the previous 11.</p>
     * <h3><code>log1p(n)</code></h3>
     * <p>Binary logarithm of <code>n+1</code>. Also always positive.</p>
     * <h3><code>mandelbrot(a, b, limit)</code></h3>
     * <p>Computes the value of the mandelbrot set, for set <code>a</code> and <code>b</code> spot.
     * Spot the beetle. Why not.</p>
     * <h3><code>min(arg, ...), min(list), max(arg, ...), max(list)</code></h3>
     * <p>Compute minimum or maximum of supplied arguments assuming default sorthoraphical order. In case you are
     * missing <code>argmax</code>, just use <code>a ~ max(a)</code>, little less efficient, but still fun.
     * </p>
     * <p>
     * Interesting bit - <code>min</code> and <code>max</code> don't remove variable associations from arguments, which
     * means can be used as LHS of assignments (obvious case), or argument spec in function definitions (far less obvious).
     * </p>
     * <pre>
     * a = 1; b = 2; min(a,b) = 3; l(a,b)  =&gt; [3, 2]
     * a = 1; b = 2; fun(x, min(a,b)) -&gt; l(a,b); fun(3,5)  =&gt; [5, 0]
     * </pre>
     * <p>Absolutely no idea, how the latter might be useful in practice. But since it compiles, can ship it.</p>
     *
     * <h3><code>relu(n)</code></h3>
     * <p>Linear rectifier of <code>n</code>. 0 below 0, n above. Why not. <code>max(0,n)</code>
     * with less moral repercussions.</p>
     * <h2>Trigonometric / Geometric Functions</h2>
     * <h3><code>sin(x)</code></h3>
     * <h3><code>cos(x)</code></h3>
     * <h3><code>tan(x)</code></h3>
     * <h3><code>asin(x)</code></h3>
     * <h3><code>acos(x)</code></h3>
     * <h3><code>atan(x)</code></h3>
     * <h3><code>atan2(x,y)</code></h3>
     * <h3><code>sinh(x)</code></h3>
     * <h3><code>cosh(x)</code></h3>
     * <h3><code>tanh(x)</code></h3>
     * <h3><code>sec(x)</code></h3>
     * <h3><code>csc(x)</code></h3>
     * <h3><code>sech(x)</code></h3>
     * <h3><code>csch(x)</code></h3>
     * <h3><code>cot(x)</code></h3>
     * <h3><code>acot(x)</code></h3>
     * <h3><code>coth(x)</code></h3>
     * <h3><code>asinh(x)</code></h3>
     * <h3><code>acosh(x)</code></h3>
     * <h3><code>atanh(x)</code></h3>
     * <h3><code>rad(deg)</code></h3>
     * <h3><code>deg(rad)</code></h3>
     * <p>Use as you wish</p>
     *
     * </div>
     *
     */

    public void ArithmeticOperations()
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

    /**
     * <h1>Loops, and higher order functions</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>Efficient use of these functions can greatly simplify your programs and speed them up, as these functions
     * will internalize most of the operations that need to be applied on multiple values at the same time. Most
     * of them take a <code>list</code> argument which can be any iterable structure in scarpet, including
     * generators, like <code>rect</code>, or <code>range</code>, and maps, where the iterator returns all the map keys</p>
     * <h2>Loops</h2>
     *
     * <h3><code>break(), break(expr), continue(), continue(expr)</code></h3>
     * <p>These allow to control execution of a loop either skipping current iteration code, using <code>continue</code>, or finishing
     * the current loop, using <code>break</code>. <code>break</code> and <code>continue</code> can only be used inside
     * <code>for, while, loop, map, filter and reduce</code> functions, while <code>break</code> can be used in <code>first</code> as well.
     * Outside of the internal expressions of these functions, calling <code>break</code> or <code>continue</code> will cause an
     * error. In case of the nexted loops, and more complex setups, use custom <code>try</code> and <code>throw</code> setup.</p>
     * <p>Please check corresponding loop function description what <code>continue</code> and <code>break</code> do in their contexts,
     * but in general case, passed values to <code>break</code> and <code>continue</code> will be used in place of the return value of the
     * internal iteration expression.</p>
     *
     *
     * <h3><code>for(list,expr(_,_i))</code></h3>
     * <p>Evaluates expression over list of items from the <code>list</code>. Supplies
     * <code>_</code>(value) and <code>_i</code>(iteration number) to the <code>expr</code>.</p>
     * <p>Returns the number of times <code>expr</code> was successful. Uses <code>continue</code> and <code>break</code> argument
     * in place of the returned value from the <code>expr</code>(if supplied), to determine if the iteration was successful.</p>
     * <pre>
     *     check_prime(n) -&gt; !first( range(2, sqrt(n)+1), !(n % _) );
     *     for(range(1000000,1100000),check_prime(_))  =&gt; 7216
     * </pre>
     * <p>From which we can learn that there is 7216 primes between 1M and 1.1M</p>
     *
     * <h3><code>while(cond, limit)</code></h3>
     * <p>Evaluates expression <code>expr</code> repeatedly until condition <code>cond</code> becomes false,
     * but not more than <code>limit</code> times. Returns the result of the last <code>expr</code> evaluation,
     * or <code>null</code> if nothing was successful. Both <code>expr</code> and <code>cond</code> will recveived a
     * bound variable <code>_</code> indicating current iteration, so its a number.</p>
     * <pre>
     *  while(a&lt;100,10,a=_*_)  =&gt; 81 // loop exhausted via limit
     *  while(a&lt;100,20,a=_*_)  =&gt; 100 // loop stopped at condition, but a has already been assigned
     *  while(_*_&lt;100,20,a=_*_)  =&gt; 81 // loop stopped at condition, before a was assigned a value
     * </pre>
     *
     * <h3><code>loop(num,expr(_),exit(_)?)</code></h3>
     * <p>Evaluates expression <code>expr</code>, <code>num</code> number of times.
     *<code>expr</code> receives <code>_</code> system variable indicating the iteration.</p>
     * <pre>
     *     loop(5, game_tick())  =&gt; repeat tick 5 times
     *     list = l(); loop(5, x = _; loop(5, list += l(x, _) ) ); list
     *       // double loop, produces: [[0, 0], [0, 1], [0, 2], [0, 3], [0, 4], [1, 0], [1, 1], ... , [4, 2], [4, 3], [4, 4]]
     * </pre>
     * <p>In this small example we will search for first 10 primes, apparently including 0:</p>
     * <pre>
     *     check_prime(n) -&gt; !first( range(2, sqrt(n)+1), !(n % _) );
     *     primes = l();
     *     loop(10000, if(check_prime(_), primes += _ ; if (length(primes) &gt;= 10, break())));
     *     primes
     *
     *     // outputs: [0, 1, 2, 3, 5, 7, 11, 13, 17, 19]
     * </pre>
     *
     * <h2>Higher Order Functions</h2>
     * <h3><code>map(list,expr(_,_i))</code></h3>
     * <p>Converts a <code>list</code> of values, to another list where each value is result of an expression
     * <code>v = expr(_, _i)</code> where <code>_</code> is passed as each element of the list, and <code>_i</code> is
     * the index of such element. If <code>break</code> is called the map returns whatever collected thus far. If
     * <code>continue</code> and <code>break</code> are used with supplied argument, it is used in place of the resulting
     * map element, otherwise current element is skipped.</p>
     * <pre>
     *     map(range(10), _*_)  =&gt; [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
     *     map(players('*'), _+' is stoopid') [gnembon is stoopid, herobrine is stoopid]
     * </pre>
     *
     * <h3><code>filter(list,expr(_,_i))</code></h3>
     * <p>filters <code>list</code> elements returning only these that return positive result of the <code>expr</code>.
     * With <code>break</code> and <code>continue</code> statements, the supplied value can be used as a boolean check instead.</p>
     * <pre>
     *     filter(range(100), !(_%5), _*_&gt;1000)  =&gt; [0, 5, 10, 15, 20, 25, 30]
     *     map(filter(entity_list('*'),_=='Witch'), query(_,'pos') )  =&gt; [[1082.5, 57, 1243.5]]
     * </pre>
     *
     * <h3><code>first(list,expr(_,_i))</code></h3>
     * <p>Finds and returns the first item in the list that satisfies <code>expr</code>. It sets <code>_</code> for current element value,
     * and <code>_i</code> for index of that element. <code>break</code> can be called inside the iteration code, using its argument
     * value instead of the current item. <code>continue</code> has no sense and cannot be called inside <code>first</code> call.</p>
     * <pre>
     *     first(range(1000,10000), n=_; !first( range(2, sqrt(n)+1), !(n % _) ) )  =&gt; 1009 // first prime after 1000
     * </pre>
     * <p>Notice in the example above, that we needed to rename the outer <code>_</code> to be albe to use in in the inner
     * <code>first</code> call</p>
     *
     *
     * <h3><code>all(list,expr(_,_i))</code></h3>
     * <p>Returns <code>true</code> if all elements on the list satisfy the condition. Its roughly equivalent to
     * <code>all(list,expr) &lt;=&gt; for(list,expr)==length(list)</code>. <code>expr</code>
     * also receives bound <code>_</code> and <code>_i</code> variables. <code>break</code> and <code>continue</code> have no sense
     * and cannot be used inside of <code>expr</code> body.</p>
     *
     * <pre>
     *     all([1,2,3], check_prime(_))  =&gt; 1
     *     all(neighbours(x,y,z), _=='stone')  =&gt; 1 // if all neighbours of [x, y, z] are stone
     *     map(filter(rect(0,4,0,1000,0,1000), l(x,y,z)=pos(_); all(rect(x,y,z,1,0,1),_=='bedrock') ), pos(_) )
     *       =&gt; [[-298, 4, -703], [-287, 4, -156], [-269, 4, 104], [242, 4, 250], [-159, 4, 335], [-208, 4, 416], [-510, 4, 546], [376, 4, 806]]
     *         // find all 3x3 bedrock structures in the top bedrock layer
     *     map( filter( rect(0,4,0,1000,1,1000,1000,0,1000), l(x,y,z)=pos(_);
     *             all(rect(x,y,z,1,0,1),_=='bedrock') &amp;&amp; for(rect(x,y-1,z,1,1,1,1,0,1),_=='bedrock')&lt;8),
     *        pos(_) )  =&gt; [[343, 3, -642], [153, 3, -285], [674, 3, 167], [-710, 3, 398]]
     *         // ditto, but requiring at most 7 bedrock block in the 18 blocks below them
     * </pre>
     *
     * <h3><code>reduce(list,expr(_a,_,_i), initial)</code></h3>
     * <p>Applies <code>expr</code> for each element of the list and saves the result in <code>_a</code> accumulator.
     * Consecutive calls to <code>expr</code> can access that value to apply more values. You also need to specify
     * the initial value to apply for the accumulator. <code>break</code> can be used to terminate reduction prematurely.
     * If a value is provided to <code>break</code> or <code>continue</code>, it will be used from now on as a new value for the
     * accumulator.</p>
     * <pre>
     *     reduce([1,2,3,4],_a+_,0)  =&gt; 10
     *     reduce([1,2,3,4],_a*_,1)  =&gt; 24
     * </pre>
     * </div>
     */
    public void LoopsAndHigherOrderFunctions()
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

        // similar to map, but returns total number of successes
        // for(list, expr) => success_count
        // can be substituted for first and all, but first is more efficient and all doesn't require knowing list size
        addLazyFunction("for", 2, (c, t, lv) ->
        {
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof AbstractListValue))
                throw new InternalExpressionException("Second argument of 'for' function should be a list or iterator");
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

    /**
     * <h1>Lists, Maps and API support for Containers</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>Scarpet supports basic container types: lists and maps (aka hashmaps, dicts etc..)</p>
     * <h2>Container manipulation</h2>
     * <p>Here is a list of operations that work on all types of containers: lists, maps, as well as other Minecraft
     * specific modifyable containers, like NBTs</p>
     *
     * <h3><code>get(container, address, ...), get(lvalue), ':' operator</code></h3>
     * <p>Returns the value at <code>address</code> element from the <code>value</code>.
     * For lists it indicates an index, use negative numbers to reach elements from the end of the list. <code>get</code>
     * call will always be able to find the index. In case there is few items, it will loop over </p>
     * <p>for maps, retrieves the value under the key specified in the <code>address</code> or null otherwise</p>
     * <p>[Minecraft specific usecase]: In case <code>value</code> is of <code>nbt</code> type, uses address as the
     * nbt path to query,
     * returning null, if path is not found, one value if there was one match, or list of values if result is a list.
     * Returned elements can be of numerical type, string texts, or another compound nbt tags</p>
     * <p>In case to simplify the access with nested objects, you can add chain of addresses to the arguments of
     * <code>get</code> rather than calling it multiple times. <code>get(get(foo,a),b)</code> is equivalent to
     * <code>get(foo, a, b)</code>, or <code>foo:a:b</code>.</p>
     * <pre>
     *     get(l(range(10)), 5)  =&gt; 5
     *     get(l(range(10)), -1)  =&gt; 9
     *     get(l(range(10)), 10)  =&gt; 0
     *     l(range(10)):93  =&gt; 3
     *
     *     get(player() ~ 'nbt', 'Health') =&gt; 20 // inefficient way to get player health, use player() ~ 'health' instead
     *
     *     get(m( l('foo',2), l('bar',3), l('baz',4) ), 'bar')  =&gt; 3
     * </pre>
     *
     * <h3><code>has(container, address, ...), has(lvalue)</code></h3>
     * <p>Similar to <code>get</code>, but returns boolean value indicating if the given index / key / path is in the
     * container. Can be used to determine if <code>get(...)==null</code> means the element doesn't exist, or the stored value
     * for this address is <code>null</code>, and is cheaper to run than <code>get</code>.</p>
     * <p>Like get, it can accept multiple addresses for chains in nested containers. In this case <code>has(foo:a:b)</code>
     * is equivalent to <code>has(get(foo,a), b)</code> or <code>has(foo, a, b)</code></p>
     *
     * <h3><code>delete(container, address, ...), delete(lvalue)</code></h3>
     * <p>Removes specific entry from the container. For the lists - removes the element and shrinks it. For maps, it
     * removes the key from the map, and for nbt - removes content from a given path. For lists and maps returns previous
     * entry at the address, for nbt's - number of removed objects, with 0 indicating that the original value was unaffected.</p>
     * <p>Like with the <code>get</code> and <code>has</code>, <code>delete</code> can accept chained addresses, as well as
     * l-value container access, removing the value
     * from the leaf of the path provided, so <code>delete(foo, a, b)</code> is the same as <code>delete(get(foo,a),b)</code>
     * or <code>delete(foo:a:b)</code></p>
     * <p>Returns true, if container was changed, false, if it was left unchanged, and null if operation was invalid.</p>
     *
     * <h3><code>put(container, address, value), put(container, address, value, mode), put(lvalue, value)</code></h3>
     * <p><u><b>Lists</b></u></p>
     * <p>Modifies the container by replacing the value under the address with the supplied <code>value</code>.
     * For lists, a valid index is required, but can be negative as well to indicate positions from the end of the list.
     * If <code>null</code> is supplied as the address, it always means - add to the end of the list. </p>
     * <p>There are three modes that lists can have items added to them:
     * <ul>
     *     <li><code>replace</code>(default): Replaces item under given index(address). Doesn't change the size of the array
     *     unless <code>null</code> address is used, which is an exception and then it appends to the end</li>
     *     <li><code>insert</code>: Inserts given element at a specified index, shifting the rest of the array to make space
     *     for the item. Note that index of -1 points to the last element of the list, thus inserting at that position and
     *     moving the previous last element to the new last element position. To insert at the end, use <code>+=</code>
     *     operator, or <code>null</code> address in put</li>
     *     <li><code>extend</code>: treats the supplied value as an iterable set of values to insert at a given index,
     *     extending the list by this amount of items. Again use <code>null</code> address/index to point to the end of
     *     the list</li>
     * </ul>
     * <p>Due to the extra mode parameter, there is no chaining for <code>put</code>, but you can still use l-value
     * container access to indicate container and address, so <code>put(foo, key, value)</code> is the same as
     * <code>put(foo:key, value)</code> or <code>foo:key=value</code></p>
     * <p>Returns true, if container got modified, false otherwise, and null if operation was invalid.</p>
     *
     * <p><u><b>Maps</b></u></p>
     * <p>For maps there are no modes available (yet, seems there is no reason to). It replaces the value under the supplied
     * key (address), or sets it if not currently present.</p>
     * <p><u><b>NBT Tags</b></u></p>
     * <p>
     * The address for nbt values is a valid nbt path that you would use with <code>/data</code> command, and tag is any
     * tag that would be applicable for a given insert operation. Note that to distinguish between proper types (like integer
     * types, you need to use command notation, i.e. regular ints is <code>123</code>, while byte size int would be <code>123b</code>
     * and an explicit string would be <code>"5"</code>, so it helps that scarpet uses single quotes in his strings. Unlike
     * for lists and maps, it returns the number of affected nodes, or 0 if none were affected.
     * </p>
     * <p>There are three modes that NBT tags can have items added to them:
     * <ul>
     *     <li><code>replace</code>(default): Replaces item under given path(address). Removes them first if possible, and then
     *     adds given element to the supplied position. The target path can indicate compound tag keys, lists, or individual
     *     elements of the lists.</li>
     *     <li><code>&lt;N&gt;</code>: Index for list insertions. Inserts given element at a specified index, inside a list
     *     specified with the path address. Fails if list is not specified. It behaves like <code>insert</code> mode for lists,
     *     i.e. it is not removing any of the existing elements. Use <code>replace</code> to remove and replace existing element.
     *     </li>
     *     <li><code>merge</code>: assumes that both path and replacement target are of compound type (dictionaries, maps,
     *     <code>{}</code> types), and merges keys from <code>value</code> with the compound tag under the path</li>
     * </ul>
     *
     * <pre>
     *     a = l(1, 2, 3); put(a, 1, 4); a  =&gt; [1, 4, 3]
     *     a = l(1, 2, 3); put(a, null, 4); a  =&gt; [1, 2, 3, 4]
     *     a = l(1, 2, 3); put(a, 1, 4, 'insert'); a  =&gt; [1, 4, 2, 3]
     *     a = l(1, 2, 3); put(a, null, l(4, 5, 6), 'extend'); a  =&gt; [1, 2, 3, 4, 5, 6]
     *     a = l(1, 2, 3); put(a, 1, l(4, 5, 6), 'extend'); a  =&gt; [1, 4, 5, 6, 2, 3]
     *     a = l(l(0,0,0),l(0,0,0),l(0,0,0)); put(a.1, 1, 1); a  =&gt; [[0, 0, 0], [0, 1, 0], [0, 0, 0]]
     *     a = m(1,2,3,4); put(a, 5, null); a  =&gt; {1: null, 2: null, 3: null, 4: null, 5: null}
     *     tag = nbt('{}'); put(tag, 'BlockData.Properties', '[1,2,3,4]'); tag  =&gt; {BlockData:{Properties:[1,2,3,4]}}
     *     tag = nbt('{a:[{lvl:3},{lvl:5},{lvl:2}]}'); put(tag, 'a[].lvl', 1); tag  =&gt; {a:[{lvl:1},{lvl:1},{lvl:1}]}
     *     tag = nbt('{a:[{lvl:[1,2,3]},{lvl:[3,2,1]},{lvl:[4,5,6]}]}'); put(tag, 'a[].lvl', 1, 2); tag
     *          =&gt; {a:[{lvl:[1,2,1,3]},{lvl:[3,2,1,1]},{lvl:[4,5,1,6]}]}
     *     tag = nbt('{a:[{lvl:[1,2,3]},{lvl:[3,2,1]},{lvl:[4,5,6]}]}'); put(tag, 'a[].lvl[1]', 1); tag
     *          =&gt; {a:[{lvl:[1,1,3]},{lvl:[3,1,1]},{lvl:[4,1,6]}]}
     * </pre>
     *
     * <h2>List operations</h2>
     * <h3><code>l(values ...), l(iterator) </code></h3>
     * <p>Creates a list of values of the expressions passed as parameters. It can be used as an L-value and if all
     * elements are variables, you coujld use it to return multiple results from one function call, if that function returns
     * a list of results with the same size as the <code>l</code> call uses. In case there is only one argument and it is
     * an iterator (vanilla expression specification has <code>range</code>, but Minecraft API implements
     * a bunch of them, like <code>diamond</code>), it will convert it to a proper list. Iterators can only be used in
     * high order functions, and are treated as empty lists, unless unrolled with <code>l</code></p>
     * <pre>
     * l(1,2,'foo') =&gt; [1, 2, foo]
     * l() =&gt; [] (empty list)
     * l(range(10)) =&gt; [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
     * l(1, 2) = l(3, 4) =&gt; Error: l is not a variable
     * l(foo, bar) = l(3,4); foo==3 &amp;&amp; bar==4 =&gt; 1
     * l(foo, bar, baz) = l(2, 4, 6); l(min(foo, bar), baz) = l(3, 5); l(foo, bar, baz)  =&gt; [3, 4, 5]
     * </pre>
     * <p>In the last example <code>l(min(foo, bar), baz)</code> creates a valid L-value, as min(foo, bar) finds the
     * lower of the variables (in this case <code>foo</code>) creating a valid assignable L-list of [foo, baz], and these
     * values will be assigned new values</p>
     *
     * <h3><code>join(delim, list), join(delim, values ...) </code></h3>
     * <p>Returns a string that contains joined elements of the list, iterator, or all values, concatenated with <code>delim</code> delimiter</p>
     * <pre>
     *     join('-',range(10))  =&gt; 0-1-2-3-4-5-6-7-8-9
     *     join('-','foo')  =&gt; foo
     *     join('-', 'foo', 'bar')  =&gt; foo-bar
     * </pre>
     *
     * <h3><code>split(delim, expr)</code></h3>
     * <p>Splits a string undr <code>expr</code> by <code>delim</code> which can be a regular expression</p>
     * <pre>
     *     split('',foo)  =&gt; [f, o, o]
     *     split('.','foo.bar')  =&gt; []
     *     split('\\.','foo.bar')  =&gt; [foo, bar]
     * </pre>
     *
     * <h3><code>slice(expr, from, to?)</code></h3>
     * <p>extracts a substring, or sublist (based on the type of the result of the expression under expr with starting index
     * of <code>from</code>, and ending at <code>to</code> if provided, or the end, if omitted</p>
     * <pre>
     *     slice(l(0,1,2,3,4,5), 1, 3)  =&gt; [1, 2, 3]
     *     slice('foobar', 0, 1)  =&gt; 'f'
     *     slice('foobar', 3)  =&gt; 'bar'
     *     slice(range(10), 3, 5)  =&gt; [3, 4, 5]
     *     slice(range(10), 5)  =&gt; [5, 6, 7, 8, 9]
     * </pre>
     *
     * <h3><code>sort(list), sort(values ...) </code></h3>
     * <p>Sorts in the default sortographical order either all arguments, or a list if its the only argument. It returns a new
     * sorted list, not affecting the list passed to the argument</p>
     * <pre>
     * sort(3,2,1)  =&gt; [1, 2, 3]
     * sort('a',3,11,1)  =&gt; [1, 3, 11, 'a']
     * list = l(4,3,2,1); sort(list)  =&gt; [1, 2, 3, 4]
     * </pre>
     *
     * <h3><code>sort_key(list, key_expr)</code></h3>
     * <p>Sorts a copy of the list in the order or keys as defined by the <code>key_expr</code> for each element</p>
     * <pre>
     *     sort_key([1,3,2],_)  =&gt; [1, 2, 3]
     *     sort_key([1,3,2],-_)  =&gt; [3, 2, 1]
     *     sort_key(l(range(10)),rand(1))  =&gt; [1, 0, 9, 6, 8, 2, 4, 5, 7, 3]
     *     sort_key(l(range(20)),str(_))  =&gt; [0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9]
     * </pre>
     *
     * <h3><code>range(to), range(from, to), range(from, to, step)</code></h3>
     * <p>Creates a range of numbers from <code>from</code>, no greater/larger than <code>to</code>.
     * The <code>step</code> parameter dictates not only the increment size, but also direction (can be negative).
     * The returned value is not a proper list, just the iterator
     * but if for whatever reason you need a proper list with all items evaluated, use <code>l(range(to))</code>.
     * Primarily to be used in higher order functions</p>
     * <pre>
     *     range(10)  =&gt; [...]
     *     l(range(10))  =&gt; [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
     *     map(range(10),_*_)  =&gt; [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
     *     reduce(range(10),_a+_, 0)  =&gt; 45
     *     range(5,10)  =&gt; [5, 6, 7, 8, 9]
     *     range(20, 10, -2)  =&gt; [20, 18, 16, 14, 12]
     * </pre>
     *
     * <h3><code>element(list, index)(deprecated)</code></h3>
     * <p>Legacy support for older method that worked only on lists. Please use <code>get(...)</code> for equivalent
     * support, or <code>.</code> operator. Also previous unique behaviours with <code>put</code> on lists has been
     * removed to support all type of container types.</p>
     *
     * <h2>Map operations</h2>
     * <p>Scarpet supports map structures, aka hashmaps, dicts etc. Map structure can also be used, with <code>null</code>
     * values as sets. Apart from container access functions, (<code>. , get, put, has, delete</code>),
     * the following functions:</p>
     * <h3><code>m(values ...), m(iterator), m(key_value_pairs)  </code></h3>
     * <p>creates and initializes a map with supplied keys, and values. If the arguments contains a flat list,
     * these are all treated as keys with no value, same goes with the iterator - creates a map that behaves like a set.
     * If the arguments is a list of lists, they have to have two elements each, and then first is a key, and second,
     * a value</p>
     * <pre>
     * m(1,2,'foo') =&gt; {1: null, 2: null, foo: null}
     * m() =&gt; {} (empty map)
     * m(range(10)) =&gt; {0: null, 1: null, 2: null, 3: null, 4: null, 5: null, 6: null, 7: null, 8: null, 9: null}
     * m(l(1, 2), l(3, 4)) =&gt; {1: 2, 3: 4}
     * reduce(range(10), put(_a, _, _*_); _a, m())
     *      =&gt; {0: 0, 1: 1, 2: 4, 3: 9, 4: 16, 5: 25, 6: 36, 7: 49, 8: 64, 9: 81}
     * </pre>
     * <h3><code>keys(map), values(map), pairs(map)  </code></h3>
     * <p>Returns full lists of keys, values and key-value pairs (2-element lists) for all the entries in the map</p>
     *
     * </div>
     */

    public void BasicDataStructures()
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

        addFunction("m", lv ->
        {
            if (lv.size() == 1 && lv.get(0) instanceof LazyListValue)
                return new MapValue(((LazyListValue) lv.get(0)).unroll());
            return new MapValue(lv);
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

        //Deprecated, use "get" instead, or . operator
        addBinaryFunction("element", (v1, v2) ->
        {
            if (!(v1 instanceof ListValue))
                throw new InternalExpressionException("First argument of 'get' should be a list");
            List<Value> items = ((ListValue)v1).getItems();
            long index = NumericValue.asNumber(v2).getLong();
            int numitems = items.size();
            long range = abs(index)/numitems;
            index += (range+2)*numitems;
            index = index % numitems;
            return items.get((int)index);
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

    /**
     *
     *
     * <h1>System functions</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <h2>Type conversion functions</h2>
     * <h3><code>copy(expr)</code></h3>
     * <p>Returns the deep copy of the expression. Can be used to copy mutable objects, like maps and lists</p>
     * <h3><code>type(expr)</code></h3>
     * <p>Returns the string value indicating type of the expression. Possible outcomes are
     * <code>null, number, string, list, iterator</code>, as well as minecraft related concepts like
     * <code>block, entity, nbt</code></p>
     * <h3><code>bool(expr)</code></h3>
     * <p>Returns a boolean context of the expression. Note that there are no true/false values in
     * scarpet. <code>true</code> is alias of 1, and <code>false</code> is 0. Bool is also interpreting
     * string values as boolean, which is different from other places where boolean context can be used.
     * This can be used in places where API functions return string values to represent binary values</p>
     * <pre>
     * bool(pi) =&gt; 1
     * bool(false) =&gt; 0
     * bool('') =&gt; 0
     * bool(l()) =&gt; 0
     * bool(l('')) =&gt; 1
     * bool('foo') =&gt; 1
     * bool('false') =&gt; 0
     * bool('nulL') =&gt; 0
     * if('false',1,0) =&gt; 1
     * </pre>
     *
     * <h3><code>number(expr)</code></h3>
     * <p>Returns a numeric context of the expression.
     * Can be used to read numbers from strings</p>
     * <pre>
     * number(null) =&gt; null
     * number(false) =&gt; 0
     * number('') =&gt; null
     * number('3.14') =&gt; 3.14
     * number(l()) =&gt; 0
     * number(l('')) =&gt; 1
     * number('foo') =&gt; null
     * number('3bar') =&gt; null
     * number('2')+number('2') =&gt; 4
     * </pre>
     *
     * <h3><code>str(expr, params? ... ), str(expr, param_list)</code></h3>
     *
     * <p>Returns a formatted string representing expression.
     * Accepts formatting style accepted by <code>String.format</code>.
     * Supported types (with <code>"%?"</code> syntax):</p>
     * <ul>
     *     <li>d, o, x: integers</li>
     *     <li>a, e, f, g: floats</li>
     *     <li>b: booleans</li>
     *     <li>s: strings</li>
     * </ul>
     *
     * <pre>
     * str(null) =&gt; null
     * str(false) =&gt; 0
     * str('') =&gt; null
     * str('3.14') =&gt; 3.14
     * str(l()) =&gt; 0
     * str(l('')) =&gt; 1
     * str('foo') =&gt; null
     * str('3bar') =&gt; null
     * str(2)+str(2) =&gt; 22
     * str('pi: %.2f',pi) =&gt; 'pi: 3.14'
     * str('player at: %d %d %d',pos(player())) =&gt; 'player at: 567, -2423, 124'
     * </pre>
     *
     * <hr>
     * <h2>Threading and Parallel Execution</h2>
     * <p>Scarpet allows to run threads of execution in parallel to the main script execution thread. In Minecraft, the main
     * thread scripts are executed on the main server thread. Since Minecraft is inherently NOT thread safe, it is not that
     * beneficial to parallel execution in order to access world resources faster. Both
     * <code>GetBlockState</code> and <code>setBlockState</code> are not thread safe and require the execution to park on the server thread,
     * where these requests can be executed in the off-tick time in between ticks that didn't take 50ms. There are however
     * benefits of running things in parallel, like fine time control not relying on the tick clock, or running things independent
     * on each other. You can still run your actions on tick-by-tick basis, either taking control of the execution using
     * <code>game_tick()</code> API function (nasty solution), or scheduling tick using <code>schedule()</code> function (much nicer solution),
     * but threading often gives the neatest solution to solve problems in parallel (see scarpet camera).
     * </p>
     * <p>Due to limitations with the game, there are some limits to the threading as well. You cannot for instance <code>join_task()</code> at all
     * from the main script and server thread, because any use of Minecraft specific function that require any world access, will
     * require to park and join on the main thread to get world access, meaning that calling join on that task would inevitably lead to a
     * typical deadlock. You can still join tasks from other threads, just because the only possibility of a deadlock in this case would
     * come explicitly from your bad code, not the internal world access behaviour. Some things tough like player or entity manipulation,
     * can be effectively parallelized.</p>
     * <h3><code>task(function, ?args...., ?executor)</code></h3>
     * <p>Creates and runs a parallel task, returning the handle to the task object. Task will return the return value of the function
     * when its completed, or will return <code>null</code> immediately if task is still in progress, so grabbing a value of
     * a task object is non-blocking. Function can be either function value, or function lambda, or a name of an existing defined function.
     * In case function needs arguments to be called with, they should be supplied after the function name, or value.
     * Optional <code>executor</code> identifier is to place the task in a specific queue identified by this value. The default
     * thread value is the <code>null</code> thread. There is no limits on number of parallel tasks for any executor, so using different queues is solely
     * for synchronization purposes. Also, since <code>task</code> function knows how many extra arguments it requires, the use of
     * tailing optional parameter for the custom executor thread pool is not ambiguous.
     * </p>
     * <pre>
     * task( _() -&gt; print('Hello Other World') )  =&gt; Runs print command on a separate thread
     * foo(a, b) -&gt; print(a+b); task('foo',2,2)  =&gt; Uses existing function definition to start a task
     * task('foo',3,5,'temp');  =&gt; runs function foo with a different thread executor, identified as 'temp'
     * a = 3; task( _(outer(a), b) -&gt; foo(a,b), 5, 'temp')  =&gt; Another example of running the same thing passing arguments using closure over anonymous function as well as passing a parameter.
     * </pre>
     * <h3><code>task_count(executor?)</code></h3>
     * <p>If no argument provided, returns total number of tasks being executed in parallel at this moment using scarpet threading system.
     * If the executor is provided, returns number of active tasks for that provider. Use <code>task_count(null)</code> to
     * get the task count of the default executor only.</p>
     *
     * <h3><code>task_value(task)</code></h3>
     * <p>Returns the task return value, or <code>null</code> if task hasn't finished yet. Its a non-blocking operation.
     * Unlike <code>join_task</code>, can be called on any task at any point</p>
     *
     * <h3><code>task_join(task)</code></h3>
     * <p>Waits for the task completion and returns its computed value. If the task has already finished returns it immediately.
     * Unless taking the task value directly, i.e. via <code>task_value</code>, this operation is blocking. Since Minecraft has
     * a limitation that all world access operations have to be performed on the main game thread in the off-tick time, joining any tasks that use
     * Minecraft API from the main thread would mean automatic lock, so joining from the main thread is not allowed.
     * Join tasks from other threads, if you really need to, or communicate asynchronously with the task via globals or
     * function data / arguments to monitor its progress, communicate, get partial results, or signal termination. </p>
     *
     * <h3><code>task_completed(task)</code></h3>
     * <p>Returns true if task has completed, or false otherwise.</p>
     *
     * <h3><code>synchronize(lock, expression)</code></h3>
     * <p>Evaluates <code>expression</code> synchronized with respect to the lock <code>lock</code>. Returns the value of the
     * expression.</p>
     *
     * <hr>
     * <h2>Auxiliary functions</h2>
     *
     * <h3><code>lower(expr), upper(expr), title(expr)</code></h3>
     * <p>Returns lowercase, uppercase or titlecase representation of a string representation of the passed expression</p>
     * <pre>
     * lower('aBc') =&gt; 'abc'
     * upper('aBc') =&gt; 'ABC'
     * title('aBc') =&gt; 'Abc'
     * </pre>
     *
     * <h3><code>replace(string, regex, repl?); replace_first(string, regex, repl?)</code></h3>
     * <p>Replaces all, or first occurence of a regular expression in the string with <code>repl</code> expression, or
     * nothing, if not specified</p>
     * <pre>
     * replace('abbccddebfg','b+','z')  // =&gt; azccddezfg
     * replace_first('abbccddebfg','b+','z')  // =&gt; azccddebfg
     * </pre>
     *
     * <h3><code>length(expr)</code></h3>
     * <p>Returns length of the expression, the length of the string,
     * the length of the integer part of the number, or length of the list</p>
     * <pre>
     * length(pi) =&gt; 1
     * length(pi*pi) =&gt; 1
     * length(pi^pi) =&gt; 2
     * length(l()) =&gt; 0
     * length(l(1,2,3)) =&gt; 3
     * length('') =&gt; 0
     * length('foo') =&gt; 3
     * </pre>
     *
     * <h3><code>rand(expr), rand(expr, seed)</code></h3>
     * <p>returns a random number from <code>0.0</code>
     * (inclusive) to <code>expr</code> (exclusive).
     * In boolean context (in conditions, boolean functions, or <code>bool</code>), returns
     * false if the randomly selected value is less than 1. This means that <code>rand(2)</code> returns true half
     * of the time and <code>rand(5)</code> returns true for 80% (4/5) of the time. If seed is not provided, uses a random seed.
     * If seed is provided, each consecutive call to rand() will act like 'next' call to the same random object.
     * Scarpet keeps track of up to 1024 custom random number generators, so if you exceed this number (per app), then your
     * random sequence will revert to the beginning.</p>
     * <pre>
     * map(range(10), floor(rand(10))) =&gt; [5, 8, 0, 6, 9, 3, 9, 9, 1, 8]
     * map(range(10), bool(rand(2))) =&gt; [1, 1, 1, 0, 0, 1, 1, 0, 0, 0]
     * map(range(10), str('%.1f',rand(_))) =&gt; [0.0, 0.4, 0.6, 1.9, 2.8, 3.8, 5.3, 2.2, 1.6, 5.6]
     * </pre>
     *
     * <h3><code>perlin(x), perlin(x, y), perlin(x, y, z), perlin(x, y, z, seed)</code></h3>
     * <p>returns a noise value from <code>0.0</code> to <code>1.0</code> (roughly) for 1, 2 or 3 dimensional coordinate.
     * The default seed it samples from is <code>0</code>, but seed can be
     * specified as a 4th argument as well. In case you need 1D or 2D noise values with custom seed, use <code>null</code>
     * for <code>z</code>, or <code>y</code> and <code>z</code> arguments respectively.</p>
     * <p> Perlin noise is based on a square grid and generates rougher maps comparing to Simplex, which is creamier.
     * Querying for lower-dimensional result, rather than
     * affixing unused dimensions to constants has a speed benefit,
     * </p>
     * <p>Thou shall not sample from noise changing seed frequently. Scarpet will keep track of the last 256 perlin seeds used for sampling
     * providing similar speed comparing to the default seed of <code>0</code>. In case the app engine uses more than 256 seeds at the same time,
     * switching between them can get much more expensive.</p>
     *
     * <h3><code>simplex(x, y), simplex(x, y, z), simplex(x, y, z, seed)</code></h3>
     * <p>returns a noise value from <code>0.0</code> to <code>1.0</code> (roughly) for 2 or 3 dimensional coordinate.
     * The default seed it samples from is <code>0</code>, but seed can be
     * specified as a 4th argument as well. In case you need 2D noise values with custom seed, use <code>null</code>
     * for <code>z</code> argument.</p>
     * <p> Simplex noise is based on a triangular grid and generates smoother maps comparing to Perlin.
     * To sample 1D simplex noise, affix other coordinate to a constant.
     * </p>
     * <p>Thou shall not sample from noise changing seed frequently. Scarpet will keep track of the last 256 simplex seeds used for sampling
     * providing similar speed comparing to the default seed of <code>0</code>. In case the app engine uses more than 256 seeds at the same time,
     * switching between them can get much more expensive.</p>
     *
     * <h3><code>print(expr)</code></h3>
     * <p>prints the value of the expression to chat.
     * Passes the result of the argument to the output unchanged, so <code>print</code>-statements can
     * be weaved in code to debug programming issues</p>
     * <pre>
     *     print('foo') =&gt; results in foo, prints: foo
     *     a = 1; print(a = 5) =&gt; results in 5, prints: 5
     *     a = 1; print(a) = 5 =&gt; results in 5, prints: 1
     *     print('pi = '+pi) =&gt; prints: pi = 3.141592653589793
     *     print(str('pi = %.2f',pi)) =&gt; prints: pi = 3.14
     * </pre>
     *
     * <h3><code>sleep(expr)</code></h3>
     * <p>Halts the execution of the program (and the game itself) for <code>expr</code> milliseconds.
     * All in all, its better to use <code>game_tick(expr)</code> to let the game do its job while the program waits</p>
     * <pre>sleep(50)</pre>
     * <h3><code>time()</code></h3>
     * <p>Returns the number of milliseconds since 'some point',
     * like Java's <code>System.nanoTime()</code>. It returns a float, which has 1 microsecond precision
     * (0.001 ms)</p>
     * <pre>
     *     start_time = time();
     *     flip_my_world_upside_down();
     *     print(str('this took %d milliseconds',time()-start_time))
     * </pre>
     *
     * <h3><code>profile_expr(expression)</code></h3>
     * <p>Returns number of times given expression can be run in 50ms time. Useful to profile and optimize your
     * code. Note that, even if its only a number, it WILL run these commands, so if they are destructive,
     * you need to be careful.</p>
     *
     * <hr>
     * <h2>Access to variables and stored functions (use with caution)</h2>
     *
     * <h3><code>var(expr)</code></h3>
     * <p>Returns the variable under the name of the string value of the expression. Allows to
     * manipulate variables in more programmatic manner, which allows to use local variable set with a
     * hash map type key-value access, can also be used with global variables</p>
     * <pre>
     *     a = 1; var('a') = 'foo'; a =&gt; a == 'foo'
     * </pre>
     *
     * <h3><code>undef(expr)</code></h3>
     * <p>Removes all bindings of a variable with a name of <code>expr</code>.
     * Removes also all function definitions with that name. It can affect global variable pool, and local variable set
     * for a particular function which will become invalid ........, </p>
     * <pre>
     *     inc(i) -&gt; i+1; foo = 5; inc(foo) =&gt; 6
     *     inc(i) -&gt; i+1; foo = 5; undef('foo'); inc(foo) =&gt; 1
     *     inc(i) -&gt; i+1; foo = 5; undef('inc'); undef('foo'); inc(foo) =&gt; Error: Function inc is not defined yet at pos 53
     *     undef('pi')  =&gt; bad idea - removes hidden variable holding the pi value
     *     undef('true')  =&gt; even worse idea, unbinds global true value, all references to true would now refer to the default 0
     * </pre>
     * <h3><code>vars(prefix)</code></h3>
     * <p>It returns all names of variables from local scope (if prefix does not start with 'global')
     * or global variables (otherwise).
     * Here is a larger example that uses combination of <code>vars</code> and <code>var</code> functions to
     * be used for object counting</p>
     * <pre>
     * /script run
     * $ count_blocks(ent) -&gt; (
     * $   l(cx, cy, cz) = query(ent, 'pos');
     * $   scan(cx, cy, cz, 16, 16, 16, var('count_'+_) += 1);
     * $   for ( sort_key( vars('count_'), -var(_)),
     * $     print(str( '%s: %d', slice(_,6), var(_) ))
     * $   )
     * $ )
     *
     * /script run count_blocks(player())
     * </pre>
     *
     * <hr>
     * <h2>System key-value storage</h2>
     * <p>Scarpet runs apps in isolation. The can share code via use of shared libraries, but each library that is imported
     * to each app is specific to that app. Apps can store and fetch state from disk, but its restricted to specific locations
     * meaning apps cannot interact via disk either. To facilitate communication for interappperability, scarpet hosts its own
     * key-value storage that is shared between all apps currently running on the host, providing methods for getting
     * an associated value with optional setting it if not present, and an operation of modifying a content of a system global
     * value.</p>
     * <h3><code>system_variable_get(key, default_value ?)</code></h3>
     * <p>Returns the variable from the system shared key-value storage keyed with a <code>key</code> value, optionally
     * if value is not present, and default expression is provided, sets a new value to be associated with that key</p>
     * <h3><code>system_variable_set(key, new_value)</code></h3>
     * <p>Returns the variable from the system shared key-value storage keyed with a <code>key</code> value, and sets
     * a new mapping for the key</p>
     * </div>
     */
    public void SystemFunctions()
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
            if (lv.get(1) instanceof ListValue)
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
                return new StringValue(String.format(format, args.toArray()));
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
            Value funcDesc = lv.get(0).evalValue(c);
            if (!(funcDesc instanceof FunctionValue))
            {
                String name = funcDesc.getString();
                funcDesc = c.host.getAssertFunction(module, name);
            }
            FunctionValue fun = (FunctionValue)funcDesc;
            int extraargs = lv.size() - fun.getArguments().size();
            if (extraargs != 1 && extraargs != 2)
            {
                throw new InternalExpressionException("Function takes "+fun.getArguments().size()+" arguments.");
            }
            List<LazyValue> lvargs = new ArrayList<>();
            for (int i=0; i< fun.getArguments().size(); i++)
            {
                lvargs.add(lv.get(i+1));
            }
            Value queue = Value.NULL;
            if (extraargs == 2) queue = lv.get(lv.size()-1).evalValue(c);
            ThreadValue thread = new ThreadValue(queue, fun, expr, tok, c, lvargs);
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

    private LazyValue getOrSetAnyVariable(Context c, String name)
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

    static final Expression none = new Expression("null");
    /**
     * @param expression .
     */
    public Expression(String expression)
    {
        this.expression = expression.trim().replaceAll("\\r\\n?", "\n").replaceAll("\\t","   ");
        VariablesAndConstants();
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
        List<Tokenizer.Token> cleanedTokens = tokenizer.fixSemicolons();

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

    Value eval(Context c)
    {
        return eval(c, Context.NONE);
    }
    private Value eval(Context c, Integer expectedType)
    {
        if (ast == null)
        {
            ast = getAST(c);
        }
        return evalValue(() -> ast, c, expectedType);
    }

    Value evalValue(Supplier<LazyValue> exprProvider, Context c, Integer expectedType)
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
                    String name = token.surface.toLowerCase(Locale.ROOT);
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
                        if (token.surface.equalsIgnoreCase(";"))
                        {
                            throw new ExpressionException(c, this, token, "Unnecessary semicolon");
                        }
                        throw new ExpressionException(c, this, token, "Missing parameter(s) for operator " + token);
                    }
                    // pop the operator's 2 parameters and add the result
                    stack.set(stack.size() - 1, stack.peek() - 2 + 1);
                    break;
                case FUNCTION:
                    ILazyFunction f = functions.get(token.surface.toLowerCase(Locale.ROOT));// don't validate global - userdef functions
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
