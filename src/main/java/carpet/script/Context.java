package carpet.script;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Context
{
    public enum Type
    {
        NONE, VOID, BOOLEAN, NUMBER, STRING, LIST, ITERATOR, SIGNATURE, LOCALIZATION, LVALUE, MAPDEF
    }

    public static final Type NONE = Type.NONE;
    public static final Type VOID = Type.VOID;
    public static final Type BOOLEAN = Type.BOOLEAN;
    public static final Type NUMBER = Type.NUMBER;
    public static final Type STRING = Type.STRING;
    public static final Type LIST = Type.LIST;
    public static final Type ITERATOR = Type.ITERATOR;
    public static final Type SIGNATURE = Type.SIGNATURE;
    public static final Type LOCALIZATION = Type.LOCALIZATION;
    public static final Type LVALUE = Type.LVALUE;
    public static final Type MAPDEF = Type.MAPDEF;

    public Map<String, LazyValue> variables = new HashMap<>();

    public final ScriptHost host;

    public Context(final ScriptHost host)
    {
        this.host = host;
    }

    public LazyValue getVariable(final String name)
    {
        return variables.get(name);
    }

    public void setVariable(final String name, final LazyValue lv)
    {
        variables.put(name, lv);
    }

    public void delVariable(final String variable)
    {
        variables.remove(variable);
    }

    public void removeVariablesMatching(final String varname)
    {
        variables.entrySet().removeIf(e -> e.getKey().startsWith(varname));
    }

    public Context with(final String variable, final LazyValue lv)
    {
        variables.put(variable, lv);
        return this;
    }

    public Set<String> getAllVariableNames()
    {
        return variables.keySet();
    }

    public Context recreate()
    {
        final Context ctx = duplicate();
        ctx.initialize();
        return ctx;
    }

    protected void initialize()
    {
        //special variables for second order functions so we don't need to check them all the time
        variables.put("_", (c, t) -> Value.ZERO);
        variables.put("_i", (c, t) -> Value.ZERO);
        variables.put("_a", (c, t) -> Value.ZERO);
    }

    public Context duplicate()
    {
        return new Context(this.host);
    }

    public ScriptHost.ErrorSnooper getErrorSnooper()
    {
        return host.errorSnooper;
    }

    public ScriptServer scriptServer()
    {
        return host.scriptServer();
    }

    /**
     * immutable context only for reason on reporting access violations in evaluating expressions in optimizization
     * mode detecting any potential violations that may happen on the way
     */
    public static class ContextForErrorReporting extends Context
    {
        public ScriptHost.ErrorSnooper optmizerEerrorSnooper;

        public ContextForErrorReporting(final Context parent)
        {
            super(null);
            optmizerEerrorSnooper = parent.host.errorSnooper;
        }

        @Override
        public ScriptHost.ErrorSnooper getErrorSnooper()
        {
            return optmizerEerrorSnooper;
        }

        public void badProgrammer()
        {
            throw new InternalExpressionException("Attempting to access the execution context while optimizing the code;" +
                    " This is not the problem with your code, but the error cause by improper use of code compile optimizations" +
                    "of scarpet authors. Please report this issue directly to the scarpet issue tracker");

        }

        @Override
        public LazyValue getVariable(final String name)
        {
            badProgrammer();
            return null;
        }

        @Override
        public void setVariable(final String name, final LazyValue lv)
        {
            badProgrammer();
        }

        @Override
        public void delVariable(final String variable)
        {
            badProgrammer();
        }

        @Override
        public void removeVariablesMatching(final String varname)
        {
            badProgrammer();
        }

        @Override
        public Context with(final String variable, final LazyValue lv)
        {
            badProgrammer();
            return this;
        }

        @Override
        public Set<String> getAllVariableNames()
        {
            badProgrammer();
            return null;
        }

        @Override
        public Context recreate()
        {
            badProgrammer();
            return null;
        }

        @Override
        protected void initialize()
        {
            badProgrammer();
        }

        @Override
        public Context duplicate()
        {
            badProgrammer();
            return null;
        }
    }
}
