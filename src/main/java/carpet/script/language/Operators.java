package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.AbstractListValue;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.LContainerValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Operators {
    public static final Map<String, Integer> precedence = new HashMap<String,Integer>() {{
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
        put("def->...", 2);
        put("nextop;", 1);
    }};

    public static void apply(Expression expression)
    {
        expression.addBinaryOperator("+", precedence.get("addition+-"), true, Value::add);
        expression.addBinaryOperator("-", precedence.get("addition+-"), true, Value::subtract);
        expression.addBinaryOperator("*", precedence.get("multiplication*/%"), true, Value::multiply);
        expression.addBinaryOperator("/", precedence.get("multiplication*/%"), true, Value::divide);
        expression.addBinaryOperator("%", precedence.get("multiplication*/%"), true, (v1, v2) ->
                new NumericValue(NumericValue.asNumber(v1).getDouble() % NumericValue.asNumber(v2).getDouble()));
        expression.addBinaryOperator("^", precedence.get("exponent^"), false, (v1, v2) ->
                new NumericValue(java.lang.Math.pow(NumericValue.asNumber(v1).getDouble(), NumericValue.asNumber(v2).getDouble())));

        // lazy cause RHS is only conditional
        expression.addLazyBinaryOperator("&&", precedence.get("and&&"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.BOOLEAN);
            if (!v1.getBoolean()) return (cc, tt) -> v1;
            return lv2;
        });

        // lazy cause RHS is only conditional
        expression.addLazyBinaryOperator("||", precedence.get("or||"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.BOOLEAN);
            if (v1.getBoolean()) return (cc, tt) -> v1;
            return lv2;
        });

        expression.addBinaryOperator("~", precedence.get("attribute~:"), true, Value::in);

        expression.addBinaryOperator(">", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) > 0 ? Value.TRUE : Value.FALSE);
        expression.addBinaryOperator(">=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) >= 0 ? Value.TRUE : Value.FALSE);
        expression.addBinaryOperator("<", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) < 0 ? Value.TRUE : Value.FALSE);
        expression.addBinaryOperator("<=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) <= 0 ? Value.TRUE : Value.FALSE);
        expression.addBinaryOperator("==", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.TRUE : Value.FALSE);
        expression.addBinaryOperator("!=", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.FALSE : Value.TRUE);

        // lazy cause of assignment which is non-trivial
        expression.addLazyBinaryOperator("=", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
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
                    expression.setAnyVariable(c, lname, (cc, tt) -> vval);
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
            expression.setAnyVariable(c, varname, boundedLHS);
            return boundedLHS;
        });

        // lazy due to assignment
        expression.addLazyBinaryOperator("+=", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
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
                    expression.setAnyVariable(c, lname, (cc, tt) -> result);
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
            expression.setAnyVariable(c, varname, boundedLHS);
            return boundedLHS;
        });

        expression.addBinaryContextOperator("<>", precedence.get("assign=<>"), false, (c, t, v1, v2) ->
        {
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
                    expression.setAnyVariable(c, lname, (cc, tt) -> rval);
                    expression.setAnyVariable(c, rname, (cc, tt) -> lval);
                }
                return Value.TRUE;
            }
            v1.assertAssignable();
            v2.assertAssignable();
            String lvalvar = v1.getVariable();
            String rvalvar = v2.getVariable();
            Value lval = v2.reboundedTo(lvalvar);
            Value rval = v1.reboundedTo(rvalvar);
            expression.setAnyVariable(c, lvalvar, (cc, tt) -> lval);
            expression.setAnyVariable(c, rvalvar, (cc, tt) -> rval);
            return lval;
        });

        expression.addUnaryOperator("-",  false, v -> NumericValue.asNumber(v).opposite());

        expression.addUnaryOperator("+", false, NumericValue::asNumber);

        // could be non-lazy, but who cares - its a small one.
        expression.addLazyUnaryOperator("!", precedence.get("unary+-!"), false, (c, t, lv)-> lv.evalValue(c, Context.BOOLEAN).getBoolean() ? (cc, tt)-> Value.FALSE : (cc, tt) -> Value.TRUE); // might need context boolean

    }
}
