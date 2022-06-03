package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.LazyValue.VariableLazyValue;
import carpet.script.ReferenceArray;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.AbstractListValue;
import carpet.script.value.BooleanValue;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.FunctionAnnotationValue;
import carpet.script.value.FunctionUnpackedArgumentsValue;
import carpet.script.value.LContainerValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Operators {
    public static final Map<String, Integer> precedence = new HashMap<String, Integer>() {{
        put("attribute~:", 80);
        put("unary+-!...", 60);
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

    public static void apply(Expression expression)
    {
        expression.addBinaryOperator("+", precedence.get("addition+-"), true, Value::add);
        expression.addFunction("sum", lv -> {
            int size = lv.size();
            if (size == 0) return Value.NULL;
            Value accumulator = lv.get(0);
            for (Value v: lv.subList(1, size)) accumulator = accumulator.add(v);
            return accumulator;
        });
        expression.addFunctionalEquivalence("+", "sum");

        expression.addBinaryOperator("-", precedence.get("addition+-"), true, Value::subtract);
        expression.addFunction("difference", lv -> {
            int size = lv.size();
            if (size == 0) return Value.NULL;
            Value accumulator = lv.get(0);
            for (Value v: lv.subList(1, size)) accumulator = accumulator.subtract(v);
            return accumulator;
        });
        expression.addFunctionalEquivalence("-", "difference");

        expression.addBinaryOperator("*", precedence.get("multiplication*/%"), true, Value::multiply);
        expression.addFunction("product", lv -> {
            int size = lv.size();
            if (size == 0) return Value.NULL;
            Value accumulator = lv.get(0);
            for (Value v: lv.subList(1, size)) accumulator = accumulator.multiply(v);
            return accumulator;
        });
        expression.addFunctionalEquivalence("*", "product");

        expression.addBinaryOperator("/", precedence.get("multiplication*/%"), true, Value::divide);
        expression.addFunction("quotient", lv -> {
            int size = lv.size();
            if (size == 0) return Value.NULL;
            Value accumulator = lv.get(0);
            for (Value v: lv.subList(1, size)) accumulator = accumulator.divide(v);
            return accumulator;
        });
        expression.addFunctionalEquivalence("/", "quotient");

        expression.addBinaryOperator("%", precedence.get("multiplication*/%"), true, (v1, v2) ->
                NumericValue.asNumber(v1).mod(NumericValue.asNumber(v2)));
        expression.addBinaryOperator("^", precedence.get("exponent^"), false, (v1, v2) ->
                new NumericValue(java.lang.Math.pow(NumericValue.asNumber(v1).getDouble(), NumericValue.asNumber(v2).getDouble())));

        expression.addFunction("bitwise_and", lv -> {
            int size = lv.size();
            if (size == 0) return Value.NULL;
            long accumulator = NumericValue.asNumber(lv.get(0)).getLong();
            for (Value v: lv.subList(1, size)) accumulator = accumulator & NumericValue.asNumber(v).getLong();
            return new NumericValue(accumulator);
        });

        expression.addFunction("bitwise_xor", lv -> {
            int size = lv.size();
            if (size == 0) return Value.NULL;
            long accumulator = NumericValue.asNumber(lv.get(0)).getLong();
            for (Value v: lv.subList(1, size)) accumulator = accumulator ^ NumericValue.asNumber(v).getLong();
            return new NumericValue(accumulator);
        });

        expression.addFunction("bitwise_or", lv -> {
            int size = lv.size();
            if (size == 0) return Value.NULL;
            long accumulator = NumericValue.asNumber(lv.get(0)).getLong();
            for (Value v: lv.subList(1, size)) accumulator = accumulator | NumericValue.asNumber(v).getLong();
            return new NumericValue(accumulator);
        });

        // lazy cause RHS is only conditional
        expression.addLazyBinaryOperator("&&", precedence.get("and&&"), false, true, t -> Context.Type.BOOLEAN, (c, t, lv1, lv2) ->
        { // todo check how is optimizations going
            Value v1 = lv1.evalValue(c, Context.BOOLEAN);
            if (!v1.getBoolean()) return (cc, tt) -> v1;
            return lv2;
        });

        expression.addPureLazyFunction("and", -1, t -> Context.Type.BOOLEAN, (c, t, lv) -> {
            int last = lv.size()-1;
            if (last == -1) return LazyValue.TRUE;
            for (LazyValue l: lv.subList(0, last))
            {
                Value val = l.evalValue(c, Context.Type.BOOLEAN);
                if (val instanceof FunctionUnpackedArgumentsValue)
                {
                    for (Value it : (FunctionUnpackedArgumentsValue) val)
                        if (!it.getBoolean()) return (cc, tt) -> it;
                }
                else
                {
                    if (!val.getBoolean()) return (cc, tt) -> val;
                }
            }
            return lv.get(last);
        });
        expression.addFunctionalEquivalence("&&", "and");

        // lazy cause RHS is only conditional
        expression.addLazyBinaryOperator("||", precedence.get("or||"), false, true, t -> Context.Type.BOOLEAN, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.BOOLEAN);
            if (v1.getBoolean()) return (cc, tt) -> v1;
            return lv2;
        });

        expression.addPureLazyFunction("or", -1, t -> Context.Type.BOOLEAN, (c, t, lv) -> {
            int last = lv.size()-1;
            if (last == -1) return LazyValue.FALSE;
            for (LazyValue l: lv.subList(0, last))
            {
                Value val = l.evalValue(c, Context.Type.BOOLEAN);
                if (val instanceof FunctionUnpackedArgumentsValue)
                {
                    for (Value it : (FunctionUnpackedArgumentsValue) val)
                        if (it.getBoolean()) return (cc, tt) -> it;
                }
                else
                {
                    if (val.getBoolean()) return (cc, tt) -> val;
                }
            }
            return lv.get(last);
        });
        expression.addFunctionalEquivalence("||", "or");

        expression.addBinaryOperator("~", precedence.get("attribute~:"), true, Value::in);

        expression.addBinaryOperator(">", precedence.get("compare>=><=<"), false, (v1, v2) ->
                BooleanValue.of(v1.compareTo(v2) > 0));
        expression.addFunction("decreasing", lv -> {
            int size = lv.size();
            if (size < 2) return Value.TRUE;
            Value prev = lv.get(0);
            for (Value next: lv.subList(1, size))
            {
                if (prev.compareTo(next) <= 0) return Value.FALSE;
                prev = next;
            }
            return Value.TRUE;
        });
        expression.addFunctionalEquivalence(">", "decreasing");

        expression.addBinaryOperator(">=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                BooleanValue.of(v1.compareTo(v2) >= 0));
        expression.addFunction("nonincreasing", lv -> {
            int size = lv.size();
            if (size < 2) return Value.TRUE;
            Value prev = lv.get(0);
            for (Value next: lv.subList(1, size))
            {
                if (prev.compareTo(next) < 0) return Value.FALSE;
                prev = next;
            }
            return Value.TRUE;
        });
        expression.addFunctionalEquivalence(">=", "nonincreasing");

        expression.addBinaryOperator("<", precedence.get("compare>=><=<"), false, (v1, v2) ->
                BooleanValue.of(v1.compareTo(v2) < 0));
        expression.addFunction("increasing", lv -> {
            int size = lv.size();
            if (size < 2) return Value.TRUE;
            Value prev = lv.get(0);
            for (Value next: lv.subList(1, size))
            {
                if (prev.compareTo(next) >= 0) return Value.FALSE;
                prev = next;
            }
            return Value.TRUE;
        });
        expression.addFunctionalEquivalence("<", "increasing");

        expression.addBinaryOperator("<=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                BooleanValue.of(v1.compareTo(v2) <= 0));
        expression.addFunction("nondecreasing", lv -> {
            int size = lv.size();
            if (size < 2) return Value.TRUE;
            Value prev = lv.get(0);
            for (Value next: lv.subList(1, size))
            {
                if (prev.compareTo(next) > 0) return Value.FALSE;
                prev = next;
            }
            return Value.TRUE;
        });
        expression.addFunctionalEquivalence("<=", "nondecreasing");

        expression.addMathematicalBinaryIntFunction("bitwise_shift_left", (num, amount) -> {
            return num << amount;
        });
        expression.addMathematicalBinaryIntFunction("bitwise_shift_right", (num, amount) -> {
            return num >> amount;
        });
        expression.addMathematicalBinaryIntFunction("bitwise_roll_left", (num, num2) -> {
            long amount = num2 % 64;

            long amountToRoll = 64 - amount;
            long rolledBits = ((-1L) >> amountToRoll) << amountToRoll;
            long rolledAmount = (num & rolledBits) >> amountToRoll;
            return num << amount | rolledAmount;
        });
        expression.addMathematicalBinaryIntFunction("bitwise_roll_right", (num, num2) -> {
            long amount = num2 % 64;

            long amountToRoll = 64 - amount;
            long rolledBits = ((-1L) << amountToRoll) >> amountToRoll;
            long rolledAmount = (num & rolledBits) << amountToRoll;
            return num >> amount | rolledAmount;
        });
        expression.addMathematicalUnaryIntFunction("bitwise_not", d -> {
            long num = d.longValue();
            return num ^ (-1L);
        });
        expression.addMathematicalUnaryIntFunction("bitwise_popcount", d -> Long.valueOf(Long.bitCount(d.longValue())));
		
        expression.addMathematicalUnaryIntFunction("double_to_long_bits", Double::doubleToLongBits);
        expression.addUnaryFunction("long_to_double_bits", v -> {
			return new NumericValue(Double.longBitsToDouble(NumericValue.asNumber(v).getLong()));
		});


        expression.addBinaryOperator("==", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.TRUE : Value.FALSE);
        expression.addFunction("equal", lv -> {
            int size = lv.size();
            if (size < 2) return Value.TRUE;
            Value prev = lv.get(0);
            for (Value next: lv.subList(1, size))
            {
                if (!prev.equals(next)) return Value.FALSE;
                prev = next;
            }
            return Value.TRUE;
        });
        expression.addFunctionalEquivalence("==", "equal");


        expression.addBinaryOperator("!=", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.FALSE : Value.TRUE);
        expression.addFunction("unique", lv -> {
            int size = lv.size();
            if (size < 2) return Value.TRUE;
            // need to order them so same obejects will be next to each other.
            lv.sort(Comparator.comparingInt(Value::hashCode));
            Value prev = lv.get(0);
            for (Value next: lv.subList(1, size))
            {
                if (prev.equals(next)) return Value.FALSE;
                prev = next;
            }
            return Value.TRUE;
        });
        expression.addFunctionalEquivalence("!=", "unique");

        // lazy cause of assignment which is non-trivial
        expression.addLazyBinaryOperator("=", precedence.get("assign=<>"), false, false, t -> Context.Type.LVALUE, (c, t, lv1, lv2) ->
        {
            Value v2 = lv2.evalValue(c);
            if (lv1 instanceof ReferenceArray lhs && v2 instanceof ListValue)
            {
                List<Value> rl = ((ListValue)v2).getItems();
                if (lhs.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                if (lhs.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                Iterator<Value> ri = rl.iterator();
                for (String variable : lhs.variables())
                {
                    Value vval = ri.next().reboundedTo(variable);
                    expression.setAnyVariable(c, variable, (cc, tt) -> vval);
                }
                return (cc, tt) -> Value.TRUE;
            }
            Value v1 = lv1.evalValue(c, Context.LVALUE);
            if (v1 instanceof LContainerValue)
            {
                ContainerValueInterface container = ((LContainerValue) v1).getContainer();
                if (container == null)
                    return (cc, tt) -> Value.NULL;
                Value address = ((LContainerValue) v1).getAddress();
                if (!(container.put(address, v2))) return (cc, tt) -> Value.NULL;
                return (cc, tt) -> v2;
            }
            if (!(lv1 instanceof VariableLazyValue var)) {
            	throw new InternalExpressionException("Left hand side must be a variable");
            }
            Value copy = v2.reboundedTo(var.name());
            LazyValue boundedLHS = (cc, tt) -> copy;
            expression.setAnyVariable(c, var.name(), boundedLHS);
            return boundedLHS;
        });

        // lazy due to assignment
        expression.addLazyBinaryOperator("+=", precedence.get("assign=<>"), false, false, t -> Context.Type.LVALUE, (c, t, lv1, lv2) ->
        {
            Value v2 = lv2.evalValue(c);
            if (lv1 instanceof ReferenceArray lhs && v2 instanceof ListValue)
            {
                List<Value> rl = ((ListValue)v2).getItems();
                if (lhs.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                if (lhs.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                Iterator<Value> ri = rl.iterator();
                for (int i = 0; i < lhs.size(); i++)
                {
                    Value result = lhs.getValue(c, i).add(ri.next()).bindTo(lhs.variables()[i]);
                    expression.setAnyVariable(c, lhs.variables()[i], (cc, tt) -> result);
                }
                return (cc, tt) -> Value.TRUE;
            }
            Value v1 = lv1.evalValue(c, Context.LVALUE);
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
            if (!(lv1 instanceof VariableLazyValue var)) {
            	throw new InternalExpressionException("Left hand side must be a variable");
            }
            LazyValue boundedLHS;
            if (v1 instanceof ListValue || v1 instanceof MapValue)
            {
                ((AbstractListValue) v1).append(v2);
                boundedLHS = (cc, tt)-> v1;
            }
            else
            {
                Value result = v1.add(v2).bindTo(var.name());
                boundedLHS = (cc, tt) -> result;
            }
            expression.setAnyVariable(c, var.name(), boundedLHS);
            return boundedLHS;
        });

        expression.addLazyBinaryOperator("<>", precedence.get("assign=<>"), false, false, t -> Context.NONE, (c, t, lv1, lv2) ->
        {
            if (lv1 instanceof ReferenceArray lhs && lv2 instanceof ReferenceArray rhs)
            {
                if (lhs.size() < rhs.size()) throw new InternalExpressionException("Too many values to unpack");
                if (lhs.size() > rhs.size()) throw new InternalExpressionException("Too few values to unpack");
                for (int i = 0; i < lhs.size(); i++)
                {
                    String lname = lhs.variables()[i];
                    String rname = rhs.variables()[i];;
                    Value left = lhs.getValue(c, i).reboundedTo(rname);
                    Value right = rhs.getValue(c, i).reboundedTo(lname);
                    expression.setAnyVariable(c, lhs.variables()[i], (cc, tt) -> right);
                    expression.setAnyVariable(c, rhs.variables()[i], (cc, tt) -> left);
                }
                return LazyValue.TRUE;
            }
            if (!(lv1 instanceof LazyValue.VariableLazyValue lhs)) {
            	throw new InternalExpressionException("Left hand side is not a variable");
            }
            if (!(lv1 instanceof LazyValue.VariableLazyValue rhs)) {
            	throw new InternalExpressionException("Right hand side is not a variable");
            }
            Value v1 = lv1.evalValue(c);
            Value v2 = lv2.evalValue(c);
            Value lval = v2.reboundedTo(lhs.name());
            Value rval = v1.reboundedTo(rhs.name());
            expression.setAnyVariable(c, lhs.name(), (cc, tt) -> lval);
            expression.setAnyVariable(c, rhs.name(), (cc, tt) -> rval);
            return (cc, tt) -> lval;
        });

        expression.addUnaryOperator("-",  false, v -> NumericValue.asNumber(v).opposite());

        expression.addUnaryOperator("+", false, NumericValue::asNumber);

        // could be non-lazy, but who cares - its a small one.
        expression.addLazyUnaryOperator("!", precedence.get("unary+-!..."), false, true, x -> Context.Type.BOOLEAN, (c, t, lv) ->
                lv.evalValue(c, Context.BOOLEAN).getBoolean() ? (cc, tt)-> Value.FALSE : (cc, tt) -> Value.TRUE
        ); // might need context boolean

        // lazy because of typed evaluation of the argument
        expression.addLazyUnaryOperator("...", Operators.precedence.get("unary+-!..."), false, true, t -> t== Context.Type.LOCALIZATION?Context.NONE:t, (c, t, lv) ->
        {
            if (t == Context.LOCALIZATION)
                return (cc, tt) -> new FunctionAnnotationValue(lv.evalValue(c), FunctionAnnotationValue.Type.VARARG);

            Value params = lv.evalValue(c, t);
            if (!(params instanceof AbstractListValue))
                throw new InternalExpressionException("Unable to unpack a non-list");
            FunctionUnpackedArgumentsValue fuaval = new FunctionUnpackedArgumentsValue( ((AbstractListValue) params).unpack());
            return (cc, tt) -> fuaval;
            //throw new InternalExpressionException("That functionality has not been implemented yet.");
        });

    }
}
