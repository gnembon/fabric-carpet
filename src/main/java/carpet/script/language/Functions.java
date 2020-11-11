package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
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
        expression.addLazyFunction("import", -1, (c, t, lv) ->
        {
            if (lv.size() < 1) throw new InternalExpressionException("'import' needs at least a module name to import, and list of values to import");
            String moduleName = lv.get(0).evalValue(c).getString();
            c.host.importModule(c, moduleName);
            moduleName = moduleName.toLowerCase(Locale.ROOT);
            if (lv.size() > 1)
                c.host.importNames(c, expression.module, moduleName, lv.subList(1, lv.size()).stream().map((l) -> l.evalValue(c).getString()).collect(Collectors.toList()));
            if (t == Context.VOID)
                return LazyValue.NULL;
            ListValue list = ListValue.wrap(c.host.availableImports(moduleName).map(StringValue::new).collect(Collectors.toList()));
            return (cc, tt) -> list;
        });

        expression.addLazyFunctionWithDelegation("call",-1, (c, t, expr, tok, lv) ->
        { // adjust based on c
            if (lv.size() == 0)
                throw new InternalExpressionException("'call' expects at least function name to call");
            //lv.remove(lv.size()-1); // aint gonna cut it // maybe it will because of the eager eval changes
            if (t != Context.SIGNATURE) // just call the function
            {
                FunctionArgument functionArgument = FunctionArgument.findIn(c, expression.module, lv, 0, false, true);
                FunctionValue fun = functionArgument.function;
                Value retval = fun.callInContext(expr, c, t, fun.getExpression(), fun.getToken(), functionArgument.args).evalValue(c);
                return (cc, tt) -> retval; ///!!!! dono might need to store expr and token in statics? (e? t?)
            }
            // gimme signature
            String name = lv.get(0).evalValue(c).getString();
            List<String> args = new ArrayList<>();
            List<String> globals = new ArrayList<>();
            String varArgs = null;
            for (int i = 1; i < lv.size(); i++)
            {
                Value v = lv.get(i).evalValue(c, Context.LOCALIZATION);
                if (!v.isBound())
                {
                    throw new InternalExpressionException("Only variables can be used in function signature, not  " + v.getString());
                }
                if (v instanceof FunctionAnnotationValue)
                {
                    if (((FunctionAnnotationValue) v).type == 0)
                    {
                        globals.add(v.boundVariable);
                    }
                    else
                    {
                        if (varArgs != null)
                            throw new InternalExpressionException("Variable argument identifier is already defined as "+varArgs+", trying to overwrite with "+v.boundVariable);
                        varArgs = v.boundVariable;
                    }
                }
                else
                {
                    args.add(v.boundVariable);
                }
            }
            Value retval = new FunctionSignatureValue(name, args, varArgs, globals);
            return (cc, tt) -> retval;
        });

        expression.addLazyFunction("outer", 1, (c, t, lv) ->
        {
            if (t != Context.LOCALIZATION)
                throw new InternalExpressionException("Outer scoping of variables is only possible in function signatures");
            return (cc, tt) -> new FunctionAnnotationValue(lv.get(0).evalValue(c), 0);
        });

        expression.addLazyUnaryOperator("...", Operators.precedence.get("def->..."), false, (c, t, lv) ->
        {
            if (t != Context.LOCALIZATION)
                throw new InternalExpressionException("Outer scoping of variables is only possible in function signatures");
            return (cc, tt) -> new FunctionAnnotationValue(lv.evalValue(c), 1);
        });

        //assigns const procedure to the lhs, returning its previous value
        expression.addLazyBinaryOperatorWithDelegation("->", Operators.precedence.get("def->..."), false, (c, type, e, t, lv1, lv2) ->
        {
            if (type == Context.MAPDEF)
            {
                Value result = ListValue.of(lv1.evalValue(c), lv2.evalValue(c));
                return (cc, tt) -> result;
            }
            Value v1 = lv1.evalValue(c, Context.SIGNATURE);
            if (!(v1 instanceof FunctionSignatureValue))
                throw new InternalExpressionException("'->' operator requires a function signature on the LHS");
            FunctionSignatureValue sign = (FunctionSignatureValue) v1;
            Value result = expression.createUserDefinedFunction(c, sign.getName(), e, t, sign.getArgs(), sign.getVarArgs(), sign.getGlobals(), lv2);
            return (cc, tt) -> result;
        });

        expression.addFunction("return", (lv) -> { throw new ReturnStatement(lv.size()==0?Value.NULL:lv.get(0));} );
    }
}
