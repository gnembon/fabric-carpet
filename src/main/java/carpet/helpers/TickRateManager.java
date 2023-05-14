package carpet.helpers;

import carpet.CarpetServer;
import carpet.network.ServerNetworkHandler;
import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class TickRateManager
{
    public static final int PLAYER_GRACE = 2;
    public float tickrate = 20.0f;
    public float mspt = 50.0f;
    public long time_bias = 0;
    public long time_warp_start_time = 0;
    public long time_warp_scheduled_ticks = 0;
    public ServerPlayer time_advancerer = null;
    public String tick_warp_callback = null;
    public CommandSourceStack tick_warp_sender = null;
    public int player_active_timeout = 0;
    public boolean process_entities = true;
    private boolean deepFreeze = false;
    private boolean is_paused = false;
    public boolean is_superHot = false;

    private MinecraftServer server;

    public TickRateManager(MinecraftServer server) {
        this.server = server;
    }


    /**
     * @return Whether or not the game is in a frozen state.
     * You should normally use {@link #process_entities} instead,
     * since that one accounts for tick steps and superhot
     */
    public boolean isPaused()
    {
        return is_paused;
    }

    /**
     * Whether or not the game is deeply frozen.
     * This can be used for things that you may not normally want
     * to freeze, but may need to in some situations.
     * This should be checked with {@link #process_entities} to make sure the
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
    public void setFrozenState(boolean isPaused, boolean isDeepFreeze)
    {
        is_paused = isPaused;
        deepFreeze = isPaused ? isDeepFreeze : false;
        ServerNetworkHandler.updateFrozenStateToConnectedPlayers(server);
    }


    public void reset_player_active_timeout()
    {
        if (player_active_timeout < PLAYER_GRACE)
        {
            player_active_timeout = PLAYER_GRACE;
            ServerNetworkHandler.updateTickPlayerActiveTimeoutToConnectedPlayers(server);
        }
    }

    public void reset() // do we need reset?
    {
        tickrate = 20.0f;
        mspt = 50.0f;
        time_bias = 0;
        time_warp_start_time = 0;
        time_warp_scheduled_ticks = 0;
        time_advancerer = null;
        tick_warp_callback = null;
        tick_warp_sender = null;
        player_active_timeout = 0;
        process_entities = true;
        deepFreeze = false;
        is_paused = false;
        is_superHot = false;
        notifyTickrateListeners("carpet");
    }

    public void add_ticks_to_run_in_pause(int ticks)
    {
        player_active_timeout = PLAYER_GRACE + ticks;
        ServerNetworkHandler.updateTickPlayerActiveTimeoutToConnectedPlayers(server);
    }

    public Component tickrate_advance(ServerPlayer player, int advance, String callback, CommandSourceStack source)
    {
        if (0 == advance)
        {
            tick_warp_callback = null;
            if (source != tick_warp_sender)
            {
                tick_warp_sender = null;
            }
            if (time_bias > 0)
            {
                finish_time_warp();
                tick_warp_sender = null;
                return Messenger.c("gi Warp interrupted");
            }
            return Messenger.c("ri No warp in progress");
        }
        if (time_bias > 0)
        {
            String who = "Another player";
            if (time_advancerer != null)
            {
                who = time_advancerer.getScoreboardName();
            }
            return Messenger.c("l " + who + " is already advancing time at the moment. Try later or ask them");
        }
        time_advancerer = player;
        time_warp_start_time = System.nanoTime();
        time_warp_scheduled_ticks = advance;
        time_bias = advance;
        tick_warp_callback = callback;
        tick_warp_sender = source;
        return Messenger.c("gi Warp speed ....");
    }

    public void finish_time_warp()
    {

        long completed_ticks = time_warp_scheduled_ticks - time_bias;
        double milis_to_complete = System.nanoTime() - time_warp_start_time;
        if (milis_to_complete == 0.0)
        {
            milis_to_complete = 1.0;
        }
        milis_to_complete /= 1000000.0;
        int tps = (int) (1000.0D * completed_ticks / milis_to_complete);
        double mspt = (1.0 * milis_to_complete) / completed_ticks;
        time_warp_scheduled_ticks = 0;
        time_warp_start_time = 0;
        if (tick_warp_callback != null)
        {
            Commands icommandmanager = tick_warp_sender.getServer().getCommands();
            try
            {
                icommandmanager.performPrefixedCommand(tick_warp_sender, tick_warp_callback);
            }
            catch (Throwable var23)
            {
                if (time_advancerer != null)
                {
                    Messenger.m(time_advancerer, "r Command Callback failed - unknown error: ", "rb /" + tick_warp_callback, "/" + tick_warp_callback);
                }
            }
            tick_warp_callback = null;
            tick_warp_sender = null;
        }
        if (time_advancerer != null)
        {
            Messenger.m(time_advancerer, String.format("gi ... Time warp completed with %d tps, or %.2f mspt", tps, mspt));
            time_advancerer = null;
        }
        else
        {
            Messenger.print_server_message(CarpetServer.minecraft_server, String.format("... Time warp completed with %d tps, or %.2f mspt", tps, mspt));
        }
        time_bias = 0;

    }

    public boolean continueWarp()
    {
        if (!process_entities)
        // Returning false so we don't have to run at max speed when doing nothing
        {
            return false;
        }
        if (time_bias > 0)
        {
            if (time_bias == time_warp_scheduled_ticks) //first call after previous tick, adjust start time
            {
                time_warp_start_time = System.nanoTime();
            }
            time_bias -= 1;
            return true;
        }
        else
        {
            finish_time_warp();
            return false;
        }
    }

    public void tick()
    {
        if (player_active_timeout > 0)
        {
            player_active_timeout--;
        }
        if (is_paused)
        {
            process_entities = player_active_timeout >= PLAYER_GRACE;
        }
        else if (is_superHot)
        {
            process_entities = player_active_timeout > 0;
        }
        else
        {
            process_entities = true;
        }
    }

    /**
     * Functional interface that listens for tickrate changes. This is
     * implemented to allow tickrate compatibility with other mods etc.
     */
    private final Map<String, BiConsumer<String, Float>> tickrateListeners = new HashMap<>();
    private static final float MIN_TICKRATE = 0.01f;

    //unused - mod compat reasons
    public void tickrate(float rate)
    {
        tickrate(rate, true);
    }

    public void tickrate(float rate, boolean update)
    {
        tickrate = rate;
        long msptt = (long) (1000.0 / tickrate);
        if (msptt <= 0L)
        {
            msptt = 1L;
            tickrate = 1000.0f;
        }

        this.mspt = msptt;

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
                    listenerEntry.getValue().accept(originModId, Float.valueOf(tickrate));
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
