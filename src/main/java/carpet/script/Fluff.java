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

    public interface ILazyFunction
    {
        int getNumParams();

        boolean numParamsVaries();

        LazyValue lazyEval(Context c, Integer type, Expression expr, Tokenizer.Token token, List<LazyValue> lazyParams);
        // lazy function has a chance to change execution based on contxt
    }

    public interface IFunction extends ILazyFunction
    {
        Value eval(List<Value> parameters);
    }

    public interface ILazyOperator
    {
        int getPrecedence();

        boolean isLeftAssoc();

        LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2);
    }

    public interface IOperator extends ILazyOperator
    {
        Value eval(Value v1, Value v2);
    }

    public abstract static class AbstractLazyFunction implements ILazyFunction
    {
        protected String name;
        int numParams;

        AbstractLazyFunction(int numParams)
        {
            this.numParams = numParams;
        }


        public String getName() {
            return name;
        }

        public int getNumParams() {
            return numParams;
        }

        public boolean numParamsVaries() {
            return numParams < 0;
        }
    }

    public abstract static class AbstractFunction extends AbstractLazyFunction implements IFunction
    {
        AbstractFunction(int numParams) {
            super(numParams);
        }

        public static List<Value> unpackArgs(List<LazyValue> lzargs, Context c)
        {
            List<Value> args = new ArrayList<>();
            for (LazyValue lv : lzargs)
            {
                Value arg = lv.evalValue(c, Context.CALLARGS);
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

        public void checkArgs(int candidates)
        {
            if (!numParamsVaries() && getNumParams() !=candidates)
                throw new InternalExpressionException("Function " + getName() + " expected " + getNumParams() + " parameters, got " + candidates);
        }

        @Override
        public LazyValue lazyEval(Context cc, Integer type, Expression e, Tokenizer.Token t, final List<LazyValue> lazyParams)
        {
            try
            {
                return new LazyValue()
                { // eager evaluation always ignores the required type and evals params by none default
                    private List<Value> params;

                    public Value evalValue(Context c, Integer type) {
                        return AbstractFunction.this.eval(getParams(c));
                    }

                    private List<Value> getParams(Context c) {
                        if (params == null) {
                            // very likely needs to be dynamic, so not static like here, or remember if it was.
                            params = unpackArgs(lazyParams, c);
                            checkArgs(params.size());
                        }
                        return params;
                    }
                };
            }
            catch (RuntimeException exc)
            {
                throw Expression.handleCodeException(cc, exc, e, t);
            }
        }
    }

    public abstract static class AbstractLazyOperator implements ILazyOperator
    {
        int precedence;

        boolean leftAssoc;

        AbstractLazyOperator(int precedence, boolean leftAssoc) {
            this.precedence = precedence;
            this.leftAssoc = leftAssoc;
        }

        public int getPrecedence() {
            return precedence;
        }

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
        public LazyValue lazyEval(Context cc, Integer type, Expression e, Tokenizer.Token t, final LazyValue v1, final LazyValue v2)
        {
            try
            {
                return (c, type_ignored) -> AbstractOperator.this.eval(v1.evalValue(c, Context.NONE), v2.evalValue(c, Context.NONE));
            }
            catch (RuntimeException exc)
            {
                throw Expression.handleCodeException(cc, exc, e, t);
            }
        }
    }

    public abstract static class AbstractUnaryOperator extends AbstractOperator
    {
        AbstractUnaryOperator(int precedence, boolean leftAssoc) {
            super(precedence, leftAssoc);
        }

        @Override
        public LazyValue lazyEval(Context cc, Integer type, Expression e, Tokenizer.Token t, final LazyValue v1, final LazyValue v2)
        {
            try
            {
                if (v2 != null)
                {
                    throw new ExpressionException(cc, e, t, "Did not expect a second parameter for unary operator");
                }
                return (c, ignored_type) -> AbstractUnaryOperator.this.evalUnary(v1.evalValue(c));
            }
            catch (RuntimeException exc)
            {
                throw Expression.handleCodeException(cc, exc, e, t);
            }
        }

        @Override
        public Value eval(Value v1, Value v2)
        {
            throw new RuntimeException("Shouldn't end up here");
        }

        public abstract Value evalUnary(Value v1);
    }
}
