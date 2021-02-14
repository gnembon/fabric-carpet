package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.LContainerValue;
import carpet.script.value.LazyListValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DataStructures {
    public static void apply(Expression expression)
    {
        expression.addFunction("l", lv ->
        {
            if (lv.size() == 1 && lv.get(0) instanceof LazyListValue)
                return ListValue.wrap(((LazyListValue) lv.get(0)).unroll());
            return new ListValue.ListConstructorValue(lv);
        });

        expression.addFunction("join", (lv) ->
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

        expression.addFunction("split", (lv) ->
        {
            Value delimiter;
            Value hwat;
            if (lv.size() == 1)
            {
                hwat = lv.get(0);
                delimiter = null;
            }
            else if (lv.size() == 2)
            {
                delimiter = lv.get(0);
                hwat = lv.get(1);
            }
            else
            {
                throw new InternalExpressionException("'split' takes 1 or 2 arguments");
            }
            return hwat.split(delimiter);
        });

        expression.addFunction("slice", (lv) ->
        {

            if (lv.size() != 2 && lv.size() != 3)
                throw new InternalExpressionException("'slice' takes 2 or 3 arguments");
            Value hwat = lv.get(0);
            long from = NumericValue.asNumber(lv.get(1)).getLong();
            Long to = null;
            if (lv.size()== 3)
                to = NumericValue.asNumber(lv.get(2)).getLong();
            return hwat.slice(from, to);
        });

        expression.addFunction("sort", (lv) ->
        {
            List<Value> toSort = lv;
            if (lv.size()==1 && lv.get(0) instanceof ListValue)
            {
                toSort = new ArrayList<>(((ListValue)lv.get(0)).getItems());
            }
            Collections.sort(toSort);
            return ListValue.wrap(toSort);
        });

        expression.addLazyFunction("sort_key", -1, (c, t, lv) ->  //get working with iterators
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

        expression.addFunction("range", (lv) ->
        {
            NumericValue from = Value.ZERO;
            NumericValue to;
            NumericValue step = Value.ONE;
            int argsize = lv.size();
            if (argsize == 0 || argsize > 3)
                throw new InternalExpressionException("'range' accepts from 1 to 3 arguments, not "+argsize);
            to = NumericValue.asNumber(lv.get(0));
            if (lv.size() > 1)
            {
                from = to;
                to = NumericValue.asNumber(lv.get(1));
                if (lv.size() > 2)
                {
                    step = NumericValue.asNumber(lv.get(2));
                }
            }
            return (from.isInteger() && to.isInteger() && step.isInteger())
                    ? LazyListValue.rangeLong(from.getLong(), to.getLong(), step.getLong())
                    : LazyListValue.rangeDouble(from.getDouble(), to.getDouble(), step.getDouble());
        });

        expression.addLazyFunction("m", -1, (c, t, llv) ->
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

        expression.addUnaryFunction("keys", v ->
        {
            if (v instanceof MapValue)
                return new ListValue(((MapValue) v).getMap().keySet());
            return Value.NULL;
        });

        expression.addUnaryFunction("values", v ->
        {
            if (v instanceof MapValue)
                return new ListValue(((MapValue) v).getMap().values());
            return Value.NULL;
        });

        expression.addUnaryFunction("pairs", v ->
        {
            if (v instanceof MapValue)
                return ListValue.wrap(((MapValue) v).getMap().entrySet().stream().map(
                        (p) -> ListValue.of(p.getKey(), p.getValue())
                ).collect(Collectors.toList()));
            return Value.NULL;
        });

        expression.addLazyBinaryOperator(":", Operators.precedence.get("attribute~:"),true, (c, t, container_lv, key_lv) ->
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

        expression.addLazyFunction("get", -1, (c, t, lv) ->
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

        expression.addLazyFunction("has", -1, (c, t, lv) ->
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

        expression.addLazyFunction("put", -1, (c, t, lv) ->
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

        expression.addLazyFunction("delete", -1, (c, t, lv) ->
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
}
