package carpet.script;

import carpet.script.value.Value;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class CarpetContext extends Context
{
    /**
     * @deprecated Use {@link #source()} or the new methods to access stuff in it instead
     */
    @Deprecated(forRemoval = true)
    public CommandSourceStack s;
    private final BlockPos origin;

    public CarpetContext(final CarpetScriptHost host, final CommandSourceStack source)
    {
        this(host, source, BlockPos.ZERO);
    }

    public CarpetContext(final ScriptHost host, final CommandSourceStack source, final BlockPos origin)
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

    public MinecraftServer server()
    {
        return s.getServer();
    }

    public ServerLevel level()
    {
        return s.getLevel();
    }

    public RegistryAccess registryAccess()
    {
        return s.getLevel().registryAccess();
    }

    public <T> Registry<T> registry(final ResourceKey<? extends Registry<? extends T>> resourceKey)
    {
        return registryAccess().registryOrThrow(resourceKey);
    }

    public CommandSourceStack source()
    {
        return s;
    }

    public BlockPos origin()
    {
        return origin;
    }

    public void swapSource(final CommandSourceStack source)
    {
        s = source;
    }
}
