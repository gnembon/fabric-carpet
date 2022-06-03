package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.Tokenizer;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ReturnStatement;
import carpet.script.value.FunctionSignatureValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.FunctionAnnotationValue;
import carpet.script.value.ListValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Functions {
    public static void apply(Expression expression) // public just to get the javadoc right
    {
        // artificial construct to handle user defined functions and function definitions
        expression.addContextFunction("import", -1, (c, t, lv) ->
        {
            if (lv.size() < 1) throw new InternalExpressionException("'import' needs at least a module name to import, and list of values to import");
            String moduleName = lv.get(0).getString();
            c.host.importModule(c, moduleName);
            moduleName = moduleName.toLowerCase(Locale.ROOT);
            if (lv.size() > 1)
                c.host.importNames(c, expression.module, moduleName, lv.subList(1, lv.size()).stream().map(Value::getString).collect(Collectors.toList()));
            if (t == Context.VOID)
                return Value.NULL;
            return ListValue.wrap(c.host.availableImports(moduleName).map(StringValue::new).collect(Collectors.toList()));
        });


        // needs to be lazy because of custom context of execution of arguments as a signature
        expression.addCustomFunction("call", new Fluff.AbstractLazyFunction(-1, "call") {
            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression expr, Tokenizer.Token tok, List<LazyValue> lv)
            {
                if (lv.size() == 0)
                    throw new InternalExpressionException("'call' expects at least function name to call");
                //lv.remove(lv.size()-1); // aint gonna cut it // maybe it will because of the eager eval changes
                if (t != Context.SIGNATURE) // just call the function
                {
                    List<Value> args = Fluff.AbstractFunction.unpackLazy(lv, c, Context.NONE);
                    FunctionArgument functionArgument = FunctionArgument.findIn(c, expression.module, args, 0, false, true);
                    FunctionValue fun = functionArgument.function;
                    return fun.callInContext(c, t, functionArgument.args);
                }
                // gimme signature
                String name = lv.get(0).evalValue(c, Context.NONE).getString();
                List<String> args = new ArrayList<>();
                List<String> globals = new ArrayList<>();
                String varArgs = null;
                for (int i = 1; i < lv.size(); i++)
                {
                    if (!(lv.get(i) instanceof LazyValue.VariableLazyValue var)) {
                    	if (lv.get(i).evalValue(c, Context.LOCALIZATION).isBound()) {
                    		//throw new AssertionError();
                    		throw new InternalExpressionException("Varargs aren't supported in this build"); //TODO
                    	}
                    	throw new InternalExpressionException("Only variables can be used in function signature");
                    }
                    Value v = lv.get(i).evalValue(c, Context.LOCALIZATION);
                    if (v instanceof FunctionAnnotationValue) // TODO move this into a LazyValue type?
                    {
                        if (((FunctionAnnotationValue) v).type == FunctionAnnotationValue.Type.GLOBAL)
                        {
                            globals.add(var.name());
                        }
                        else
                        {
                            if (varArgs != null)
                                throw new InternalExpressionException("Variable argument identifier is already defined as "+varArgs+", trying to overwrite with "+var.name());
                            varArgs = var.name();
                        }
                    }
                    else
                    {
                        args.add(var.name());
                    }
                }
                Value retval = new FunctionSignatureValue(name, args, varArgs, globals);
                return (cc, tt) -> retval;
            }

            @Override
            public boolean pure()
            {
                return false; //true for sinature, but lets leave it for later
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public Context.Type staticType(Context.Type outerType)
            {
                return outerType==Context.SIGNATURE?Context.LOCALIZATION:Context.NONE;
            }
        });


        expression.addLazyFunction("outer", 1, (c, t, lv) ->
        {
            if (t != Context.LOCALIZATION)
                throw new InternalExpressionException("Outer scoping of variables is only possible in function signatures.");
            // TODO hack for outer(), possibly convert FuncAnnotation to a LV, making VariableLazyValue an interface (or extend an interface NamedLazyValue that is also extended by this)
            Value ret = new FunctionAnnotationValue(lv.get(0).evalValue(c), FunctionAnnotationValue.Type.GLOBAL);
            return new LazyValue.VariableLazyValue((cc, tt) -> ret, ((LazyValue.VariableLazyValue)lv.get(0)).name());
        });

        //assigns const procedure to the lhs, returning its previous value
        // must be lazy due to RHS being an expression to save to execute
        expression.addLazyBinaryOperatorWithDelegation("->", Operators.precedence.get("def->"), false, false, (c, type, e, t, lv1, lv2) ->
        {
            if (type == Context.MAPDEF)
            {
                Value result = ListValue.of(lv1.evalValue(c), lv2.evalValue(c));
                return (cc, tt) -> result;
            }
            Value v1 = lv1.evalValue(c, Context.SIGNATURE);
            if (!(v1 instanceof FunctionSignatureValue sign))
                throw new InternalExpressionException("'->' operator requires a function signature on the LHS");
            Value result = expression.createUserDefinedFunction(c, sign.getName(), e, t, sign.getArgs(), sign.getVarArgs(), sign.getGlobals(), lv2);
            return (cc, tt) -> result;
        });

        expression.addImpureFunction("return", (lv) -> { throw new ReturnStatement(lv.size()==0?Value.NULL:lv.get(0));} );
    }
}
