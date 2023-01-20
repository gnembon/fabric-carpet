package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;

public class Arithmetic {
    public static final Value PI = new NumericValue(Math.PI);
    public static final Value euler = new NumericValue(Math.E);

    public static void apply(Expression expression)
    {
        expression.addTypedContextFunction("not", 1, Context.Type.BOOLEAN, (c, t, lv) -> BooleanValue.of(lv.get(0).getBoolean()) );
        expression.addUnaryFunction("fact", (v) ->
        {
            long number = NumericValue.asNumber(v).getLong();
            if (number < 21)
            {
                long factorial = 1;
                for (int i = 1; i <= number; i++)
                {
                    factorial = factorial * i;
                }
                return new NumericValue(factorial);
            }
            else if (number > 170)
            {
                return NumericValue.of(Double.MAX_VALUE);
            }
            // values over 21 will exceed long limits
            double factorial = 1.0;
            for (int i = 1; i <= number; i++)
            {
                factorial = factorial * i;
            }
            return new NumericValue(factorial);

        });
        expression.addMathematicalUnaryFunction("sin",    (d) -> Math.sin(Math.toRadians(d)));
        expression.addMathematicalUnaryFunction("cos",    (d) -> Math.cos(Math.toRadians(d)));
        expression.addMathematicalUnaryFunction("tan",    (d) -> Math.tan(Math.toRadians(d)));
        expression.addMathematicalUnaryFunction("asin",   (d) -> Math.toDegrees(Math.asin(d)));
        expression.addMathematicalUnaryFunction("acos",   (d) -> Math.toDegrees(Math.acos(d)));
        expression.addMathematicalUnaryFunction("atan",   (d) -> Math.toDegrees(Math.atan(d)));
        expression.addMathematicalBinaryFunction("atan2", (d, d2) -> Math.toDegrees(Math.atan2(d, d2)) );
        expression.addMathematicalUnaryFunction("sinh",   Math::sinh );
        expression.addMathematicalUnaryFunction("cosh",   Math::cosh  );
        expression.addMathematicalUnaryFunction("tanh",   Math::tanh );
        expression.addMathematicalUnaryFunction("sec",    (d) ->  1.0 / Math.cos(Math.toRadians(d)) ); // Formula: sec(x) = 1 / cos(x)
        expression.addMathematicalUnaryFunction("csc",    (d) ->  1.0 / Math.sin(Math.toRadians(d)) ); // Formula: csc(x) = 1 / sin(x)
        expression.addMathematicalUnaryFunction("sech",   (d) ->  1.0 / Math.cosh(d) );                // Formula: sech(x) = 1 / cosh(x)
        expression.addMathematicalUnaryFunction("csch",   (d) -> 1.0 / Math.sinh(d)  );                // Formula: csch(x) = 1 / sinh(x)
        expression.addMathematicalUnaryFunction("cot",    (d) -> 1.0 / Math.tan(Math.toRadians(d))  ); // Formula: cot(x) = cos(x) / sin(x) = 1 / tan(x)
        expression.addMathematicalUnaryFunction("acot",   (d) ->  Math.toDegrees(Math.atan(1.0 / d)) );// Formula: acot(x) = atan(1/x)
        expression.addMathematicalUnaryFunction("coth",   (d) ->  1.0 / Math.tanh(d) );                // Formula: coth(x) = 1 / tanh(x)
        expression.addMathematicalUnaryFunction("asinh",  (d) ->  Math.log(d + (Math.sqrt(Math.pow(d, 2) + 1))));  // Formula: asinh(x) = ln(x + sqrt(x^2 + 1))
        expression.addMathematicalUnaryFunction("acosh",  (d) ->  Math.log(d + (Math.sqrt(Math.pow(d, 2) - 1))));  // Formula: acosh(x) = ln(x + sqrt(x^2 - 1))
        expression.addMathematicalUnaryFunction("atanh",  (d) ->                                       // Formula: atanh(x) = 0.5*ln((1 + x)/(1 - x))
        {
            if (Math.abs(d) > 1 || Math.abs(d) == 1)
                throw new InternalExpressionException("Number must be |x| < 1");
            return 0.5 * Math.log((1 + d) / (1 - d));
        });
        expression.addMathematicalUnaryFunction("rad",  Math::toRadians);
        expression.addMathematicalUnaryFunction("deg", Math::toDegrees);
        expression.addMathematicalUnaryFunction("ln", Math::log);
        expression.addMathematicalUnaryFunction("ln1p", Math::log1p);
        expression.addMathematicalUnaryFunction("log10", Math::log10);
        expression.addMathematicalUnaryFunction("log", a -> Math.log(a)/Math.log(2));
        expression.addMathematicalUnaryFunction("log1p", x -> Math.log1p(x)/Math.log(2));
        expression.addMathematicalUnaryFunction("sqrt", Math::sqrt);
        expression.addMathematicalUnaryFunction("abs", Math::abs);
        expression.addMathematicalUnaryIntFunction("round", Math::round);
        expression.addMathematicalUnaryIntFunction("floor", n -> (long)Math.floor(n));
        expression.addMathematicalUnaryIntFunction("ceil",  n -> (long)Math.ceil(n));

        expression.addContextFunction("mandelbrot", 3, (c, t, lv) -> {
            double a0 = NumericValue.asNumber(lv.get(0)).getDouble();
            double b0 = NumericValue.asNumber(lv.get(1)).getDouble();
            long maxiter = NumericValue.asNumber(lv.get(2)).getLong();
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
            return new NumericValue(iFinal);
        });

        expression.addFunction("max", (lv) ->
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

        expression.addFunction("min", (lv) ->
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

        expression.addUnaryFunction("relu", (v) -> v.compareTo(Value.ZERO) < 0 ? Value.ZERO : v);
    }
}
