package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletionException;

public class Threading
{
    public static void apply(Expression expression)
    {
        //"overidden" native call to cancel if on main thread
        expression.addLazyFunction("task_join", 1, (c, t, lv) -> {
            if (((CarpetContext)c).s.getMinecraftServer().isOnThread())
                throw new InternalExpressionException("'task_join' cannot be called from main thread to avoid deadlocks");
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof ThreadValue))
                throw new InternalExpressionException("'task_join' could only be used with a task value");
            Value ret =  ((ThreadValue) v).join();
            return (_c, _t) -> ret;
        });

        expression.addLazyFunctionWithDelegation("task_dock", 1, (c, t, expr, tok, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            MinecraftServer server = cc.s.getMinecraftServer();
            if (server.isOnThread()) return lv.get(0); // pass through for on thread tasks
            Value[] result = new Value[]{Value.NULL};
            RuntimeException[] internal = new RuntimeException[]{null};
            try
            {
                ((CarpetContext) c).s.getMinecraftServer().submitAndJoin(() ->
                {
                    try
                    {
                        result[0] = lv.get(0).evalValue(c, t);
                    }
                    catch (ExpressionException exc)
                    {
                        internal[0] = exc;
                    }
                    catch (InternalExpressionException exc)
                    {
                        internal[0] = new ExpressionException(c, expr, tok, exc.getMessage(), exc.stack);
                    }

                    catch (ArithmeticException exc)
                    {
                        internal[0] = new ExpressionException(c, expr, tok, "Your math is wrong, "+exc.getMessage());
                    }
                });
            }
            catch (CompletionException exc)
            {
                throw new InternalExpressionException("Error while executing docked task section, internal stack trace is gone");
            }
            if (internal[0] != null)
            {
                throw internal[0];
            }
            Value ret = result[0]; // preventing from lazy evaluating of the result in case a future completes later
            return (_c, _t) -> ret;
            // pass through placeholder
            // implmenetation should dock the task on the main thread.
        });
    }
}
