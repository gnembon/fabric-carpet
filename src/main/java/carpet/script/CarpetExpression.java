package carpet.script;

import carpet.CarpetServer;
import carpet.script.annotation.AnnotationParser;
import carpet.script.api.Auxiliary;
import carpet.script.api.BlockIterators;
import carpet.script.api.Entities;
import carpet.script.api.Inventories;
import carpet.script.api.Monitoring;
import carpet.script.api.Scoreboards;
import carpet.script.api.Threading;
import carpet.script.api.WorldAccess;
import carpet.script.bundled.Module;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.exception.ExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

public class CarpetExpression
{
    private final ServerCommandSource source;
    private final BlockPos origin;
    private final Expression expr;
    // these are for extensions
    public Expression getExpr() {return expr;}
    public ServerCommandSource getSource() {return source;}
    public BlockPos getOrigin() {return origin;}

    public CarpetExpression(Module module, String expression, ServerCommandSource source, BlockPos origin)
    {
        this.origin = origin;
        this.source = source;
        this.expr = new Expression(expression);
        this.expr.asAModule(module);

        WorldAccess.apply(this.expr);
        Entities.apply(this.expr);
        Inventories.apply(this.expr);
        BlockIterators.apply(this.expr);
        Auxiliary.apply(this.expr);
        Threading.apply(this.expr);
        Scoreboards.apply(this.expr);
        Monitoring.apply(this.expr);
        AnnotationParser.apply(this.expr);
        CarpetServer.extensions.forEach(e -> e.scarpetApi(this));
    }

    public boolean fillAndScanCommand(ScriptHost host, int x, int y, int z)
    {
        if (CarpetServer.scriptServer.stopAll)
            return false;
        try
        {
            Context context = new CarpetContext(host, source, origin).
                    with("x", (c, t) -> new NumericValue(x - origin.getX()).bindTo("x")).
                    with("y", (c, t) -> new NumericValue(y - origin.getY()).bindTo("y")).
                    with("z", (c, t) -> new NumericValue(z - origin.getZ()).bindTo("z")).
                    with("_", (c, t) -> new BlockValue(null, source.getWorld(), new BlockPos(x, y, z)).bindTo("_"));
            Entity e = source.getEntity();
            if (e==null)
            {
                Value nullPlayer = Value.NULL.reboundedTo("p");
                context.with("p", (cc, tt) -> nullPlayer );
            }
            else
            {
                Value playerValue = new EntityValue(e).bindTo("p");
                context.with("p", (cc, tt) -> playerValue);
            }
            return this.expr.eval(context).getBoolean();
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage(), e.stack);
        }
        catch (ArithmeticException ae)
        {
            throw new CarpetExpressionException("Math doesn't compute... "+ae.getMessage(), null);
        }
    }

    public Value scriptRunCommand(ScriptHost host, BlockPos pos)
    {
        if (CarpetServer.scriptServer.stopAll)
            throw new CarpetExpressionException("SCRIPTING PAUSED", null);
        try
        {
            Context context = new CarpetContext(host, source, origin).
                    with("x", (c, t) -> new NumericValue(pos.getX() - origin.getX()).bindTo("x")).
                    with("y", (c, t) -> new NumericValue(pos.getY() - origin.getY()).bindTo("y")).
                    with("z", (c, t) -> new NumericValue(pos.getZ() - origin.getZ()).bindTo("z"));
            Entity e = source.getEntity();
            if (e==null)
            {
                Value nullPlayer = Value.NULL.reboundedTo("p");
                context.with("p", (cc, tt) -> nullPlayer );
            }
            else
            {
                Value playerValue = new EntityValue(e).bindTo("p");
                context.with("p", (cc, tt) -> playerValue);
            }
            return this.expr.eval(context);
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage(), e.stack);
        }
        catch (ArithmeticException ae)
        {
            throw new CarpetExpressionException("Math doesn't compute... "+ae.getMessage(), null);
        }
    }
}
