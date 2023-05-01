package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.utils.PerlinNoiseSampler;
import carpet.script.utils.SimplexNoiseSampler;
import carpet.script.value.BooleanValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Sys
{
    public static final Random randomizer = new Random();
    // %[argument_index$][flags][width][.precision][t]conversion
    private static final Pattern formatPattern = Pattern.compile("%(\\d+\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

    public static void apply(Expression expression)
    {
        expression.addUnaryFunction("hash_code", v -> new NumericValue(v.hashCode()));

        expression.addImpureUnaryFunction("copy", Value::deepcopy);

        expression.addTypedContextFunction("bool", 1, Context.BOOLEAN, (c, t, lv) ->
        {
            Value v = lv.get(0);
            if (v instanceof StringValue)
            {
                String str = v.getString().toLowerCase(Locale.ROOT);
                if ("false".equals(str) || "null".equals(str))
                {
                    return Value.FALSE;
                }
            }
            return BooleanValue.of(v.getBoolean());
        });

        expression.addUnaryFunction("number", v ->
        {
            if (v instanceof final NumericValue num)
            {
                return num.clone();
            }
            if (v instanceof ListValue || v instanceof MapValue)
            {
                return new NumericValue(v.length());
            }
            try
            {
                return new NumericValue(v.getString());
            }
            catch (NumberFormatException format)
            {
                return Value.NULL;
            }
        });

        expression.addFunction("str", lv ->
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'str' requires at least one argument");
            }
            String format = lv.get(0).getString();
            if (lv.size() == 1)
            {
                return new StringValue(format);
            }
            int argIndex = 1;
            if (lv.get(1) instanceof final ListValue list && lv.size() == 2)
            {
                lv = list.getItems();
                argIndex = 0;
            }
            List<Object> args = new ArrayList<>();
            Matcher m = formatPattern.matcher(format);

            for (int i = 0, len = format.length(); i < len; )
            {
                if (m.find(i))
                {
                    // Anything between the start of the string and the beginning
                    // of the format specifier is either fixed text or contains
                    // an invalid format string.
                    // [[scarpet]] but we skip it and let the String.format fail
                    char fmt = m.group(6).toLowerCase().charAt(0);
                    if (fmt == 's')
                    {
                        if (argIndex >= lv.size())
                        {
                            throw new InternalExpressionException("Not enough arguments for " + m.group(0));
                        }
                        args.add(lv.get(argIndex).getString());
                        argIndex++;
                    }
                    else if (fmt == 'd' || fmt == 'o' || fmt == 'x')
                    {
                        if (argIndex >= lv.size())
                        {
                            throw new InternalExpressionException("Not enough arguments for " + m.group(0));
                        }
                        args.add(lv.get(argIndex).readInteger());
                        argIndex++;
                    }
                    else if (fmt == 'a' || fmt == 'e' || fmt == 'f' || fmt == 'g')
                    {
                        if (argIndex >= lv.size())
                        {
                            throw new InternalExpressionException("Not enough arguments for " + m.group(0));
                        }
                        args.add(lv.get(argIndex).readDoubleNumber());
                        argIndex++;
                    }
                    else if (fmt == 'b')
                    {
                        if (argIndex >= lv.size())
                        {
                            throw new InternalExpressionException("Not enough arguments for " + m.group(0));
                        }
                        args.add(lv.get(argIndex).getBoolean());
                        argIndex++;
                    }
                    else if (fmt != '%')
                    {
                        throw new InternalExpressionException("Format not supported: " + m.group(6));
                    }

                    i = m.end();
                }
                else
                {
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
                throw new InternalExpressionException("Illegal string format: " + ife.getMessage());
            }
        });

        expression.addUnaryFunction("lower", v -> new StringValue(v.getString().toLowerCase(Locale.ROOT)));

        expression.addUnaryFunction("upper", v -> new StringValue(v.getString().toUpperCase(Locale.ROOT)));

        expression.addUnaryFunction("title", v -> new StringValue(titleCase(v.getString())));

        expression.addFunction("replace", lv ->
        {
            if (lv.size() != 3 && lv.size() != 2)
            {
                throw new InternalExpressionException("'replace' expects string to read, pattern regex, and optional replacement string");
            }
            String data = lv.get(0).getString();
            String regex = lv.get(1).getString();
            String replacement = "";
            if (lv.size() == 3)
            {
                replacement = lv.get(2).getString();
            }
            try
            {
                return new StringValue(data.replaceAll(regex, replacement));
            }
            catch (PatternSyntaxException pse)
            {
                throw new InternalExpressionException("Incorrect pattern for 'replace': " + pse.getMessage());
            }
        });

        expression.addFunction("replace_first", lv ->
        {
            if (lv.size() != 3 && lv.size() != 2)
            {
                throw new InternalExpressionException("'replace_first' expects string to read, pattern regex, and optional replacement string");
            }
            String data = lv.get(0).getString();
            String regex = lv.get(1).getString();
            String replacement = "";
            if (lv.size() == 3)
            {
                replacement = lv.get(2).getString();
            }
            return new StringValue(data.replaceFirst(regex, replacement));
        });

        expression.addUnaryFunction("type", v -> new StringValue(v.getTypeString()));
        expression.addUnaryFunction("length", v -> new NumericValue(v.length()));
        expression.addContextFunction("rand", -1, (c, t, lv) ->
        {
            int argsize = lv.size();
            Random randomizer = Sys.randomizer;
            if (argsize != 1 && argsize != 2)
            {
                throw new InternalExpressionException("'rand' takes one (range) or two arguments (range and seed)");
            }
            if (argsize == 2)
            {
                randomizer = c.host.getRandom(NumericValue.asNumber(lv.get(1)).getLong());
            }
            Value argument = lv.get(0);
            if (argument instanceof final ListValue listValue)
            {
                List<Value> list = listValue.getItems();
                return list.get(randomizer.nextInt(list.size()));
            }
            double value = NumericValue.asNumber(argument).getDouble() * randomizer.nextDouble();
            return t == Context.BOOLEAN ? BooleanValue.of(value >= 1.0D) : new NumericValue(value);
        });
        expression.addContextFunction("reset_seed", 1, (c, t, lv) -> {
            boolean gotIt = c.host.resetRandom(NumericValue.asNumber(lv.get(0)).getLong());
            return BooleanValue.of(gotIt);
        });

        expression.addFunction("perlin", lv ->
        {
            PerlinNoiseSampler sampler;
            Value x;
            Value y;
            Value z;

            if (lv.size() >= 4)
            {
                x = lv.get(0);
                y = lv.get(1);
                z = lv.get(2);
                sampler = PerlinNoiseSampler.getPerlin(NumericValue.asNumber(lv.get(3)).getLong());
            }
            else
            {
                sampler = PerlinNoiseSampler.instance;
                y = Value.NULL;
                z = Value.NULL;
                if (lv.isEmpty())
                {
                    throw new InternalExpressionException("'perlin' requires at least one dimension to sample from");
                }
                x = NumericValue.asNumber(lv.get(0));
                if (lv.size() > 1)
                {
                    y = NumericValue.asNumber(lv.get(1));
                    if (lv.size() > 2)
                    {
                        z = NumericValue.asNumber(lv.get(2));
                    }
                }
            }

            double result;

            if (z.isNull())
            {
                result = y.isNull()
                        ? sampler.sample1d(NumericValue.asNumber(x).getDouble())
                        : sampler.sample2d(NumericValue.asNumber(x).getDouble(), NumericValue.asNumber(y).getDouble());
            }
            else
            {
                result = sampler.sample3d(
                        NumericValue.asNumber(x).getDouble(),
                        NumericValue.asNumber(y).getDouble(),
                        NumericValue.asNumber(z).getDouble());
            }
            return new NumericValue(result);
        });

        expression.addFunction("simplex", lv ->
        {
            SimplexNoiseSampler sampler;
            Value x;
            Value y;
            Value z;

            if (lv.size() >= 4)
            {
                x = lv.get(0);
                y = lv.get(1);
                z = lv.get(2);
                sampler = SimplexNoiseSampler.getSimplex(NumericValue.asNumber(lv.get(3)).getLong());
            }
            else
            {
                sampler = SimplexNoiseSampler.instance;
                z = Value.NULL;
                if (lv.size() < 2)
                {
                    throw new InternalExpressionException("'simplex' requires at least two dimensions to sample from");
                }
                x = NumericValue.asNumber(lv.get(0));
                y = NumericValue.asNumber(lv.get(1));
                if (lv.size() > 2)
                {
                    z = NumericValue.asNumber(lv.get(2));
                }
            }
            double result;

            if (z.isNull())
            {
                result = sampler.sample2d(NumericValue.asNumber(x).getDouble(), NumericValue.asNumber(y).getDouble());
            }
            else
            {
                result = sampler.sample3d(
                        NumericValue.asNumber(x).getDouble(),
                        NumericValue.asNumber(y).getDouble(),
                        NumericValue.asNumber(z).getDouble());
            }
            return new NumericValue(result);
        });

        expression.addUnaryFunction("print", v ->
        {
            System.out.println(v.getString());
            return v; // pass through for variables
        });

        expression.addContextFunction("time", 0, (c, t, lv) ->
                new NumericValue((System.nanoTime() / 1000.0) / 1000.0));

        expression.addContextFunction("unix_time", 0, (c, t, lv) ->
                new NumericValue(System.currentTimeMillis()));

        expression.addFunction("convert_date", lv ->
        {
            int argsize = lv.size();
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'convert_date' requires at least one parameter");
            }
            Value value = lv.get(0);
            if (argsize == 1 && !(value instanceof ListValue))
            {
                Calendar cal = new GregorianCalendar(Locale.ROOT);
                cal.setTimeInMillis(NumericValue.asNumber(value, "timestamp").getLong());
                int weekday = cal.get(Calendar.DAY_OF_WEEK) - 1;
                if (weekday == 0)
                {
                    weekday = 7;
                }
                return ListValue.ofNums(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND),
                        weekday,
                        cal.get(Calendar.DAY_OF_YEAR),
                        cal.get(Calendar.WEEK_OF_YEAR)
                );
            }
            else if (value instanceof final ListValue list)
            {
                lv = list.getItems();
                argsize = lv.size();
            }
            Calendar cal = new GregorianCalendar(0, Calendar.JANUARY, 1, 0, 0, 0);

            if (argsize == 3)
            {
                cal.set(
                        NumericValue.asNumber(lv.get(0)).getInt(),
                        NumericValue.asNumber(lv.get(1)).getInt() - 1,
                        NumericValue.asNumber(lv.get(2)).getInt()
                );
            }
            else if (argsize == 6)
            {
                cal.set(
                        NumericValue.asNumber(lv.get(0)).getInt(),
                        NumericValue.asNumber(lv.get(1)).getInt() - 1,
                        NumericValue.asNumber(lv.get(2)).getInt(),
                        NumericValue.asNumber(lv.get(3)).getInt(),
                        NumericValue.asNumber(lv.get(4)).getInt(),
                        NumericValue.asNumber(lv.get(5)).getInt()
                );
            }
            else
            {
                throw new InternalExpressionException("Date conversion requires 3 arguments for Dates or 6 arguments, for time");
            }
            return new NumericValue(cal.getTimeInMillis());
        });

        // lazy cause evaluates expression multiple times
        expression.addLazyFunction("profile_expr", 1, (c, t, lv) ->
        {
            LazyValue lazy = lv.get(0);
            long end = System.nanoTime() + 50000000L;
            long it = 0;
            while (System.nanoTime() < end)
            {
                lazy.evalValue(c);
                it++;
            }
            Value res = new NumericValue(it);
            return (cc, tt) -> res;
        });

        expression.addContextFunction("var", 1, (c, t, lv) ->
                expression.getOrSetAnyVariable(c, lv.get(0).getString()).evalValue(c));

        expression.addContextFunction("undef", 1, (c, t, lv) ->
        {
            Value remove = lv.get(0);
            if (remove instanceof FunctionValue)
            {
                c.host.delFunction(expression.module, remove.getString());
                return Value.NULL;
            }
            String varname = remove.getString();
            boolean isPrefix = varname.endsWith("*");
            if (isPrefix)
            {
                varname = varname.replaceAll("\\*+$", "");
            }
            if (isPrefix)
            {
                c.host.delFunctionWithPrefix(expression.module, varname);
                if (varname.startsWith("global_"))
                {
                    c.host.delGlobalVariableWithPrefix(expression.module, varname);
                }
                else if (!varname.startsWith("_"))
                {
                    c.removeVariablesMatching(varname);
                }
            }
            else
            {
                c.host.delFunction(expression.module, varname);
                if (varname.startsWith("global_"))
                {
                    c.host.delGlobalVariable(expression.module, varname);
                }
                else if (!varname.startsWith("_"))
                {
                    c.delVariable(varname);
                }
            }
            return Value.NULL;
        });

        //deprecate
        expression.addContextFunction("vars", 1, (c, t, lv) ->
        {
            String prefix = lv.get(0).getString();
            List<Value> values = new ArrayList<>();
            if (prefix.startsWith("global"))
            {
                c.host.globalVariableNames(expression.module, s -> s.startsWith(prefix)).forEach(s -> values.add(new StringValue(s)));
            }
            else
            {
                c.getAllVariableNames().stream().filter(s -> s.startsWith(prefix)).forEach(s -> values.add(new StringValue(s)));
            }
            return ListValue.wrap(values);
        });

        // lazy cause default expression may not be executed if not needed
        expression.addLazyFunction("system_variable_get", (c, t, lv) ->
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'system_variable_get' expects at least a key to be fetched");
            }
            Value key = lv.get(0).evalValue(c);
            if (lv.size() > 1)
            {
                c.host.scriptServer().systemGlobals.computeIfAbsent(key, k -> lv.get(1).evalValue(c));
            }
            Value res = c.host.scriptServer().systemGlobals.get(key);
            return res == null ? LazyValue.NULL : ((cc, tt) -> res);
        });

        expression.addContextFunction("system_variable_set", 2, (c, t, lv) ->
        {
            Value res = c.host.scriptServer().systemGlobals.put(lv.get(0), lv.get(1));
            return res == null ? Value.NULL : res;
        });
    }

    public static String titleCase(String str) {
        if (str.isEmpty()) {
            return str;
        }
        str = str.toLowerCase();
        char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            char ch = buffer[i];
            if (Character.isWhitespace(ch)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
    }

}
