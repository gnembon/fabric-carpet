package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.ReferenceArray;
import carpet.script.Context.Type;
import carpet.script.Tokenizer.Token;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.AbstractListValue;
import carpet.script.value.BooleanValue;
import carpet.script.value.ContainerValueInterface;
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
            if (!v1.getBoolean()) return v1;
            return lv2.evalValue(c, Context.BOOLEAN);
        });

        expression.addPureLazyFunction("and", -1, t -> Context.Type.BOOLEAN, (c, t, lv) -> {
            int last = lv.size()-1;
            if (last == -1) return Value.TRUE;
            for (LazyValue l: lv.subList(0, last))
            {
                Value val = l.evalValue(c, Context.Type.BOOLEAN);
                if (val instanceof FunctionUnpackedArgumentsValue)
                {
                    for (Value it : (FunctionUnpackedArgumentsValue) val)
                        if (!it.getBoolean()) return it;
                }
                else
                {
                    if (!val.getBoolean()) return val;
                }
            }
            return lv.get(last).evalValue(c, Context.Type.BOOLEAN);
        });
        expression.addFunctionalEquivalence("&&", "and");

        // lazy cause RHS is only conditional
        expression.addLazyBinaryOperator("||", precedence.get("or||"), false, true, t -> Context.Type.BOOLEAN, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.BOOLEAN);
            if (v1.getBoolean()) return v1;
            return lv2.evalValue(c, Context.BOOLEAN);
        });

        expression.addPureLazyFunction("or", -1, t -> Context.Type.BOOLEAN, (c, t, lv) -> {
            int last = lv.size()-1;
            if (last == -1) return Value.FALSE;
            for (LazyValue l: lv.subList(0, last))
            {
                Value val = l.evalValue(c, Context.Type.BOOLEAN);
                if (val instanceof FunctionUnpackedArgumentsValue)
                {
                    for (Value it : (FunctionUnpackedArgumentsValue) val)
                        if (it.getBoolean()) return it;
                }
                else
                {
                    if (val.getBoolean()) return val;
                }
            }
            return lv.get(last).evalValue(c, Context.Type.BOOLEAN);
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
                    expression.setAnyVariable(c, variable, vval);
                }
                return Value.TRUE;
            }
            if (!(lv1 instanceof LazyValue.Assignable var)) {
                // Check if it's LContainer, inside the check to prevent querying the LHS value if not needed
                Value v1 = lv1.evalValue(c, Context.LVALUE);
                if (v1 instanceof LContainerValue)
                {
                    ContainerValueInterface container = ((LContainerValue) v1).getContainer();
                    if (container == null)
                        return Value.NULL;
                    Value address = ((LContainerValue) v1).getAddress();
                    if (!(container.put(address, v2))) return Value.NULL;
                    return v2;
                }
                if (v1.isBound()) throw trap("compiling operator =");
                throw new InternalExpressionException("Left hand side must be a variable");
            }
            Value copy = v2.reboundedTo(null); // assignable.set will set the name
            var.set(c, copy);
            return copy;
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
                    expression.setAnyVariable(c, lhs.variables()[i], result);
                }
                return Value.TRUE;
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
                    return value;
                }
                else
                {
                    Value res = value.add(v2);
                    cvi.put(key, res);
                    return res;
                }
            }
            if (!(lv1 instanceof LazyValue.Assignable assignable)) {
                if (v1.isBound()) throw trap("compiling operator +=");
                throw new InternalExpressionException("Left hand side must be a variable");
            }
            Value result;
            if (v1 instanceof ListValue || v1 instanceof MapValue)
            {
                ((AbstractListValue) v1).append(v2);
                result = v1;
            }
            else
            {
                result = v1.add(v2);
            }
            assignable.set(c, result);
            return result;
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
                    expression.setAnyVariable(c, lname, right);
                    expression.setAnyVariable(c, rname, left);
                }
                return Value.TRUE;
            }
            if (!(lv1 instanceof LazyValue.Assignable lhs)) {
                if (lv1.evalValue(c).isBound())
                    throw trap("compiling <> operator (left)");
                throw new InternalExpressionException("Left hand side is not a variable");
            }
            if (!(lv2 instanceof LazyValue.Assignable rhs)) {
                if (lv2.evalValue(c).isBound())
                    throw trap("compiling <> operator (right)");
                throw new InternalExpressionException("Right hand side is not a variable");
            }
            Value v1 = lv1.evalValue(c);
            Value v2 = lv2.evalValue(c);
            Value lval = v2.reboundedTo(null); // Assignable.set will bind them, we don't know the variable as it could be a var() call
            Value rval = v1.reboundedTo(null);
            lhs.set(c, lval);
            rhs.set(c, rval);
            return lval;
        });

        expression.addUnaryOperator("-",  false, v -> NumericValue.asNumber(v).opposite());

        expression.addUnaryOperator("+", false, NumericValue::asNumber);

        // could be non-lazy, but who cares - its a small one.
        expression.addLazyUnaryOperator("!", precedence.get("unary+-!..."), false, true, x -> Context.Type.BOOLEAN, (c, t, lv) ->
                lv.evalValue(c, Context.BOOLEAN).getBoolean() ? Value.FALSE : Value.TRUE
        ); // might need context boolean

        // custom because of necessity to encode variable name for varargs and typed evaluation of argument as unpacker, "u" suffix because unary
        expression.addCustomOperator("...u", new Fluff.AbstractLazyOperator(precedence.get("unary+-!..."), false) {
            @Override
            public boolean pure() {
                // This operator is pure
                return true;
            }
            @Override
            public boolean transitive() { return false; }

            @Override
            public LazyValue createExecutable(Context compileContext, Expression expr, Token token, LazyValue v1, LazyValue _null) {
                LazyValue executable = super.createExecutable(compileContext, expression, token, v1, _null);
                if (v1 instanceof LazyValue.Variable var) {
                    return new LazyValue.VarArgsOrUnpacker(var.name(), executable);
                }
                return executable;
            }

            @Override
            public Value lazyEval(Context c, Type type, Expression expr, Token token, LazyValue v1, LazyValue _null) {
                Value params = v1.evalValue(c, type);
                if (!(params instanceof AbstractListValue))
                    throw new InternalExpressionException("Unable to unpack a non-list");
                return new FunctionUnpackedArgumentsValue( ((AbstractListValue) params).unpack());
            }
        });
    }

    // TODO remove, this is for traps while the new system is being tested
    static InternalExpressionException trap(String doing) {
        return new InternalExpressionException("Unexpected error while " + doing + "! Please report this to Carpet!");
    }
}
