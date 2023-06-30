package carpet.helpers;

import carpet.CarpetServer;
import carpet.network.ServerNetworkHandler;
import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ServerTickRateManager extends TickRateManager
{
    private long remainingWarpTicks = 0;
    private long tickWarpStartTime = 0;
    private long scheduledCurrentWarpTicks = 0;
    private ServerPlayer warpResponsiblePlayer = null;
    private String tickWarpCallback = null;
    private CommandSourceStack warpResponsibleSource = null;

    private MinecraftServer server;

    public ServerTickRateManager(MinecraftServer server) {
        this.server = server;
    }

    public boolean isInWarpSpeed()
    {
        return tickWarpStartTime != 0;
    }


    @Override
    public boolean shouldEntityTick(Entity e)
    {
        return (runsNormally() || (e instanceof Player));
    }

    /**
     * Whether or not the game is deeply frozen.
     * This can be used for things that you may not normally want
     * to freeze, but may need to in some situations.
     * This should be checked with {@link #runGameElements} to make sure the
     * current tick is actually frozen, not only the game
     *
     * @return Whether or not the game is deeply frozen.
     */
    public boolean deeplyFrozen()
    {
        return deepFreeze;
    }

    /**
     * Used to update the frozen state of the game.
     * Handles connected clients as well.
     *
     * @param isPaused     Whether or not the game is paused
     * @param isDeepFreeze Whether or not the game is deeply frozen
     */
    @Override
    public void setFrozenState(boolean isPaused, boolean isDeepFreeze)
    {
        super.setFrozenState(isPaused, isDeepFreeze);
        ServerNetworkHandler.updateFrozenStateToConnectedPlayers(server);
    }


    public void resetPlayerActivity()
    {
        if (playerActivityTimeout < PLAYER_GRACE)
        {
            playerActivityTimeout = PLAYER_GRACE;
            ServerNetworkHandler.updateTickPlayerActiveTimeoutToConnectedPlayers(server);
        }
    }

    public void stepGameIfPaused(int ticks)
    {
        playerActivityTimeout = PLAYER_GRACE + ticks;
        ServerNetworkHandler.updateTickPlayerActiveTimeoutToConnectedPlayers(server);
    }

    public Component requestGameToWarpSpeed(ServerPlayer player, int advance, String callback, CommandSourceStack source)
    {
        if (0 == advance)
        {
            tickWarpCallback = null;
            if (source != warpResponsibleSource)
            {
                warpResponsibleSource = null;
            }
            if (remainingWarpTicks > 0)
            {
                finishTickWarp();
                warpResponsibleSource = null;
                return Messenger.c("gi Warp interrupted");
            }
            return Messenger.c("ri No warp in progress");
        }
        if (remainingWarpTicks > 0)
        {
            String who = "Another player";
            if (warpResponsiblePlayer != null)
            {
                who = warpResponsiblePlayer.getScoreboardName();
            }
            return Messenger.c("l " + who + " is already advancing time at the moment. Try later or ask them");
        }
        warpResponsiblePlayer = player;
        tickWarpStartTime = System.nanoTime();
        scheduledCurrentWarpTicks = advance;
        remainingWarpTicks = advance;
        tickWarpCallback = callback;
        warpResponsibleSource = source;
        return Messenger.c("gi Warp speed ....");
    }

    // should be private
    public void finishTickWarp()
    {

        long completed_ticks = scheduledCurrentWarpTicks - remainingWarpTicks;
        double milis_to_complete = System.nanoTime() - tickWarpStartTime;
        if (milis_to_complete == 0.0)
        {
            milis_to_complete = 1.0;
        }
        milis_to_complete /= 1000000.0;
        int tps = (int) (1000.0D * completed_ticks / milis_to_complete);
        double mspt = (1.0 * milis_to_complete) / completed_ticks;
        scheduledCurrentWarpTicks = 0;
        tickWarpStartTime = 0;
        if (tickWarpCallback != null)
        {
            Commands icommandmanager = warpResponsibleSource.getServer().getCommands();
            try
            {
                icommandmanager.performPrefixedCommand(warpResponsibleSource, tickWarpCallback);
            }
            catch (Throwable var23)
            {
                if (warpResponsiblePlayer != null)
                {
                    Messenger.m(warpResponsiblePlayer, "r Command Callback failed - unknown error: ", "rb /" + tickWarpCallback, "/" + tickWarpCallback);
                }
            }
            tickWarpCallback = null;
            warpResponsibleSource = null;
        }
        if (warpResponsiblePlayer != null)
        {
            Messenger.m(warpResponsiblePlayer, String.format("gi ... Time warp completed with %d tps, or %.2f mspt", tps, mspt));
            warpResponsiblePlayer = null;
        }
        else
        {
            Messenger.print_server_message(CarpetServer.minecraft_server, String.format("... Time warp completed with %d tps, or %.2f mspt", tps, mspt));
        }
        remainingWarpTicks = 0;

    }

    public boolean continueWarp()
    {
        if (!runGameElements)
        // Returning false so we don't have to run at max speed when doing nothing
        {
            return false;
        }
        if (remainingWarpTicks > 0)
        {
            if (remainingWarpTicks == scheduledCurrentWarpTicks) //first call after previous tick, adjust start time
            {
                tickWarpStartTime = System.nanoTime();
            }
            remainingWarpTicks -= 1;
            return true;
        }
        else
        {
            finishTickWarp();
            return false;
        }
    }

    /**
     * Functional interface that listens for tickrate changes. This is
     * implemented to allow tickrate compatibility with other mods etc.
     */
    private final Map<String, BiConsumer<String, Float>> tickrateListeners = new HashMap<>();
    private static final float MIN_TICKRATE = 0.01f;

    //unused - mod compat reasons
    @Override
    public void setTickRate(float rate)
    {
        setTickRate(rate, true);
    }

    public void setTickRate(float rate, boolean update)
    {
        super.setTickRate(rate);
        if (update)
        {
            notifyTickrateListeners("carpet");
        }
    }

    private void tickrateChanged(String modId, float rate)
    {
        // Other mods might change the tickrate in a slightly
        // different way. Also allow for tickrates that don't
        // divide into 1000 here.

        if (rate < MIN_TICKRATE)
        {
            rate = MIN_TICKRATE;
        }

        tickrate = rate;
        mspt = 1000.0f / tickrate;

        notifyTickrateListeners(modId);
    }

    private void notifyTickrateListeners(String originModId)
    {
        synchronized (tickrateListeners)
        {
            for (Map.Entry<String, BiConsumer<String, Float>> listenerEntry : tickrateListeners.entrySet())
            {
                if (originModId == null || !originModId.equals(listenerEntry.getKey()))
                {
                    listenerEntry.getValue().accept(originModId, tickrate);
                }
            }
        }
        ServerNetworkHandler.updateTickSpeedToConnectedPlayers(server);
    }

    public BiConsumer<String, Float> addTickrateListener(String modId, BiConsumer<String, Float> tickrateListener)
    {
        synchronized (tickrateListeners)
        {
            tickrateListeners.put(modId, tickrateListener);
        }
        return this::tickrateChanged;
    }
}
