package carpet.script;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

public class CarpetContext extends Context
{
    public ServerCommandSource s;
    public BlockPos origin;
    public CarpetContext(ScriptHost host, ServerCommandSource source, BlockPos origin)
    {
        super(host);
        s = source;
        this.origin = origin;
    }

    @Override
    public Context recreate()
    {
        return new CarpetContext(this.host, this.s, this.origin);
    }

}
