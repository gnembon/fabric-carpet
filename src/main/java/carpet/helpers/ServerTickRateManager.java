package carpet.helpers;


import net.minecraft.server.MinecraftServer;


public class ServerTickRateManager extends TickRateManager
{
    public ServerTickRateManager(MinecraftServer server) {
    }

    public boolean isInWarpSpeed()
    {
        return false;
    }

    public void requestGameToSprint(final int ticks)
    { // noop
    }
}
