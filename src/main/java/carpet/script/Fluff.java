package carpet.script;

import carpet.CarpetSettings;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionUnpackedArgumentsValue;
import carpet.script.value.ListValue;
import carpet.script.value.Value;

import java.util.ArrayList;
import java.util.List;

public abstract class Fluff
{
    @FunctionalInterface
    public interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }

    @FunctionalInterface
    public interface TriConsumer<A, B, C> { void accept(A a, B b, C c); }

    @FunctionalInterface
    public interface QuadConsumer<A, B, C, D> { void accept(A a, B b, C c, D d); }

    @FunctionalInterface
    public interface QuadFunction<A, B, C, D, R> { R apply(A a, B b, C c, D d);}

    @FunctionalInterface
    public interface QuinnFunction<A, B, C, D, E, R> { R apply(A a, B b, C c, D d, E e);}

    @FunctionalInterface
    public interface SexFunction<A, B, C, D, E, F, R> { R apply(A a, B b, C c, D d, E e, F f);}

    public interface UsageProvider { String getUsage();}

    public interface EvalNode
    {
        /**
         * @return true if function has constant output if arguments are constant and can be evaluated
         * statically (without context)
         */
        boolean pure();

        /**
         * @return true if function has constant output if arguments are constant and can be evaluated
         * statically (without context)
         */
        boolean transitive();

        /**
         * @return required argument eval type in case its evaluated statically without context but with a given context type
         */
        default Context.Type staticType(Context.Type outerType) {return transitive()?outerType:Context.NONE;};
    }

    public interface ILazyFunction extends EvalNode
    {
        int getNumParams();

        boolean numParamsVaries();

        Value lazyEval(Context c, Context.Type type, Expression expr, Tokenizer.Token token, List<LazyValue> lazyParams);

        /**
         * Creates an executable {@link LazyValue} to be used for executing this function.<p>
         * Unlike {@link #lazyEval(Context, carpet.script.Context.Type, Expression, carpet.script.Tokenizer.Token, List) lazyEval},
         * this method is only ran once per function call during compilation, and the returned {@link LazyValue} will be exposed
         * to whatever that this function execution is in.
         * 
         * @param compilationContext The current compilation context. It's likely to be unusable other than for error reporting
         * @param expr The expression this call is in
         * @param token The token of this call
         * @param params A list with the parameters for this call, unevaluated and potentially with unpackers in it
         * @return The executable as defined above
         */
        default LazyValue createExecutable(Context compilationContext/* ? */, Expression expr, Tokenizer.Token token, List<LazyValue> params) {
            return (c, t) -> lazyEval(c, t, expr, token, params);
        }

        static void checkInterrupts()
        {
            if (ScriptHost.mainThread != Thread.currentThread() && Thread.currentThread().isInterrupted())
                throw new InternalExpressionException("Thread interrupted");
        }
        // lazy function has a chance to change execution based on contxt
    }

    public interface IFunction extends ILazyFunction
    {
        Value eval(List<Value> parameters);
    }

    public interface ILazyOperator extends EvalNode
    {
        int getPrecedence();

        boolean isLeftAssoc();

        LazyValue lazyEval(Context c, Context.Type type, Expression expr, Tokenizer.Token token, LazyValue v1, LazyValue v2);

        /**
         * @see ILazyFunction#createExecutable(Context, Expression, carpet.script.Tokenizer.Token, List)
         */
        default LazyValue createExecutable(Context compileContext, Expression expr, Tokenizer.Token token, LazyValue v1, LazyValue v2) {
            return (c, t) -> lazyEval(c, t, expr, token, v1, v2).evalValue(c, t);
        }
    }

    public interface IOperator extends ILazyOperator
    {
        Value eval(Value v1, Value v2);
    }

    public abstract static class AbstractLazyFunction implements ILazyFunction
    {
        protected final String name;
        final int numParams;

        public AbstractLazyFunction(int numParams, String name)
        {
            this.numParams = numParams;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public int getNumParams() {
            return numParams;
        }

        @Override
        public boolean numParamsVaries() {
            return numParams < 0;
        }

        public static List<Value> unpackLazy(List<LazyValue> lzargs, Context c, Context.Type contextType)
        {
            List<Value> args = new ArrayList<>();
            for (LazyValue lv : lzargs)
            {
                Value arg = lv.evalValue(c, contextType);
                if (arg instanceof FunctionUnpackedArgumentsValue)
                    args.addAll(((ListValue) arg).getItems());
                else
                    args.add(arg);
            }
            return args;
        }

        public List<Value> unpackArgs(List<LazyValue> lzargs, Context c, Context.Type contextType)
        {
            List<Value> args = unpackLazy(lzargs, c, contextType);
            if (!numParamsVaries() && getNumParams() != args.size())
                throw new InternalExpressionException("Function " + getName() + " expected " + getNumParams() + " parameters, got " + args.size());
            return args;
        }

        public static List<LazyValue> lazify(List<Value> args)
        {
            List<LazyValue> lzargs = new ArrayList<>(args.size());
            args.forEach( v -> lzargs.add( (c, t) -> v));
            return lzargs;
        }
    }

    public abstract static class AbstractFunction extends AbstractLazyFunction implements IFunction
    {
        public AbstractFunction(int numParams, String name) {
            super(numParams, name);
        }

        @Override
        public boolean pure() {
            return true;
        }

        @Override
        public boolean transitive() {
            return false;
        }

        @Override
        public Value lazyEval(Context cc, Context.Type type, Expression e, Tokenizer.Token t, final List<LazyValue> lazyParams)
        {
            // eager evaluation always ignores the required type and evals params by none default
            ILazyFunction.checkInterrupts();
            try
            {
                return AbstractFunction.this.eval(unpackArgs(lazyParams, cc, Context.Type.NONE));
            }
            catch (RuntimeException exc)
            {
                throw Expression.handleCodeException(cc, exc, e, t);
            }
        }
    }

    public abstract static class AbstractLazyOperator implements ILazyOperator
    {
        final int precedence;
        final boolean leftAssoc;

        public AbstractLazyOperator(int precedence, boolean leftAssoc) {
            this.precedence = precedence;
            this.leftAssoc = leftAssoc;
        }

        @Override
        public int getPrecedence() {
            return precedence;
        }

        @Override
        public boolean isLeftAssoc() {
            return leftAssoc;
        }

    }

    public abstract static class AbstractOperator extends AbstractLazyOperator implements IOperator
    {

        AbstractOperator(int precedence, boolean leftAssoc) {
            super(precedence, leftAssoc);
        }
        @Override
        public boolean pure() {
            return true;
        }

        @Override
        public boolean transitive() {
            return false;
        }

        @Override
        public LazyValue lazyEval(Context cc, Context.Type type, Expression e, Tokenizer.Token t, final LazyValue v1, final LazyValue v2)
        {
            return (c, typeIgnored) -> {
                try
                {
                    return AbstractOperator.this.eval(v1.evalValue(c, Context.Type.NONE), v2.evalValue(c, Context.Type.NONE));
                }
                catch (RuntimeException exc)
                {
                    throw Expression.handleCodeException(cc, exc, e, t);
                }
            };
        }
    }

    public abstract static class AbstractUnaryOperator extends AbstractOperator
    {
        AbstractUnaryOperator(int precedence, boolean leftAssoc) {
            super(precedence, leftAssoc);
        }
        @Override
        public boolean pure() {
            return true;
        }

        @Override
        public boolean transitive() {
            return false;
        }

        @Override
        public LazyValue lazyEval(Context cc, Context.Type type, Expression e, Tokenizer.Token t, final LazyValue v1, final LazyValue v2)
        {
            if (v2 != null)
            {
                throw new ExpressionException(cc, e, t, "Did not expect a second parameter for unary operator");
            }
            return (c, ignoredType) -> {
                try
                {
                    return AbstractUnaryOperator.this.evalUnary(v1.evalValue(c, Context.Type.NONE));
                }
                catch (RuntimeException exc)
                {
                    throw Expression.handleCodeException(cc, exc, e, t);
                }
            };
        }

        @Override
        public Value eval(Value v1, Value v2)
        {
            throw new IllegalStateException("Shouldn't end up here");
        }

        public abstract Value evalUnary(Value v1);
    }
}
