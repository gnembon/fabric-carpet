package carpet.script;

import carpet.script.value.Value;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;

public class CarpetContext extends Context
{
    public CommandSourceStack s;
    public final BlockPos origin;

    public CarpetContext(CarpetScriptHost host, CommandSourceStack source) {
        this(host, source, BlockPos.ZERO);
    }

    public CarpetContext(ScriptHost host, CommandSourceStack source, BlockPos origin)
    {
        super(host);
        s = source;
        this.origin = origin;
    }

    @Override
    public CarpetContext duplicate()
    {
        return new CarpetContext(this.host, this.s, this.origin);
    }

    @Override
    protected void initialize()
    {
        super.initialize();
        variables.put("_x", (c, t) -> Value.ZERO);
        variables.put("_y", (c, t) -> Value.ZERO);
        variables.put("_z", (c, t) -> Value.ZERO);
    }
}
