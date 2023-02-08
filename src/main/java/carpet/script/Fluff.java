package carpet.script;

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
    public interface TriFunction<A, B, C, R>
    {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    public interface QuadFunction<A, B, C, D, R>
    {
        R apply(A a, B b, C c, D d);
    }

    @FunctionalInterface
    public interface QuinnFunction<A, B, C, D, E, R>
    {
        R apply(A a, B b, C c, D d, E e);
    }

    @FunctionalInterface
    public interface SexFunction<A, B, C, D, E, F, R>
    {
        R apply(A a, B b, C c, D d, E e, F f);
    }

    public interface UsageProvider
    {
        String getUsage();
    }

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
        default Context.Type staticType(Context.Type outerType)
        {
            return transitive() ? outerType : Context.NONE;
        }

    }

    public interface ILazyFunction extends EvalNode
    {
        int getNumParams();

        boolean numParamsVaries();

        LazyValue lazyEval(Context c, Context.Type type, Expression expr, Tokenizer.Token token, List<LazyValue> lazyParams);

        static void checkInterrupts()
        {
            if (ScriptHost.mainThread != Thread.currentThread() && Thread.currentThread().isInterrupted())
            {
                throw new InternalExpressionException("Thread interrupted");
            }
        }
        // lazy function has a chance to change execution based on context
    }

    public interface IFunction extends ILazyFunction
    {
        Value eval(List<Value> parameters);
    }

    public interface ILazyOperator extends EvalNode
    {
        int getPrecedence();

        boolean isLeftAssoc();

        LazyValue lazyEval(Context c, Context.Type type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2);
    }

    public interface IOperator extends ILazyOperator
    {
        Value eval(Value v1, Value v2);
    }

    public abstract static class AbstractLazyFunction implements ILazyFunction
    {
        protected String name;
        int numParams;

        public AbstractLazyFunction(int numParams, String name)
        {
            super();
            this.numParams = numParams;
            this.name = name;
        }


        public String getName()
        {
            return name;
        }

        @Override
        public int getNumParams()
        {
            return numParams;
        }

        @Override
        public boolean numParamsVaries()
        {
            return numParams < 0;
        }

        public static List<Value> unpackLazy(List<LazyValue> lzargs, Context c, Context.Type contextType)
        {
            List<Value> args = new ArrayList<>();
            for (LazyValue lv : lzargs)
            {
                Value arg = lv.evalValue(c, contextType);
                if (arg instanceof FunctionUnpackedArgumentsValue)
                {
                    args.addAll(((ListValue) arg).getItems());
                }
                else
                {
                    args.add(arg);
                }
            }
            return args;
        }

        public List<Value> unpackArgs(List<LazyValue> lzargs, Context c, Context.Type contextType)
        {
            List<Value> args = unpackLazy(lzargs, c, contextType);
            if (!numParamsVaries() && getNumParams() != args.size())
            {
                throw new InternalExpressionException("Function " + getName() + " expected " + getNumParams() + " parameters, got " + args.size());
            }
            return args;
        }

        public static List<LazyValue> lazify(List<Value> args)
        {
            List<LazyValue> lzargs = new ArrayList<>(args.size());
            args.forEach(v -> lzargs.add((c, t) -> v));
            return lzargs;
        }
    }

    public abstract static class AbstractFunction extends AbstractLazyFunction implements IFunction
    {
        AbstractFunction(int numParams, String name)
        {
            super(numParams, name);
        }

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
        public LazyValue lazyEval(Context cc, Context.Type type, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
        {

            return new LazyValue()
            { // eager evaluation always ignores the required type and evals params by none default
                private List<Value> params;

                @Override
                public Value evalValue(Context c, Context.Type type)
                {
                    ILazyFunction.checkInterrupts();
                    try
                    {
                        return AbstractFunction.this.eval(getParams(c));
                    }
                    catch (RuntimeException exc)
                    {
                        throw Expression.handleCodeException(cc, exc, e, t);
                    }
                }

                private List<Value> getParams(Context c)
                {
                    if (params == null)
                    {
                        // very likely needs to be dynamic, so not static like here, or remember if it was.
                        params = unpackArgs(lazyParams, c, Context.Type.NONE);
                    }
                    else
                    {
                        CarpetScriptServer.LOG.error("How did we get here 1");
                    }
                    return params;
                }
            };
        }
    }

    public abstract static class AbstractLazyOperator implements ILazyOperator
    {
        int precedence;

        boolean leftAssoc;

        AbstractLazyOperator(int precedence, boolean leftAssoc)
        {
            super();
            this.precedence = precedence;
            this.leftAssoc = leftAssoc;
        }

        @Override
        public int getPrecedence()
        {
            return precedence;
        }

        @Override
        public boolean isLeftAssoc()
        {
            return leftAssoc;
        }

    }

    public abstract static class AbstractOperator extends AbstractLazyOperator implements IOperator
    {

        AbstractOperator(int precedence, boolean leftAssoc)
        {
            super(precedence, leftAssoc);
        }

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
        public LazyValue lazyEval(Context cc, Context.Type type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2)
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
        AbstractUnaryOperator(int precedence, boolean leftAssoc)
        {
            super(precedence, leftAssoc);
        }

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
        public LazyValue lazyEval(Context cc, Context.Type type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2)
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
