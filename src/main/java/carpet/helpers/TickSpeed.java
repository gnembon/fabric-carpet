package carpet.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import carpet.CarpetServer;
import carpet.network.ServerNetworkHandler;
import carpet.utils.Messenger;

public class TickSpeed
{
    public static final int PLAYER_GRACE = 2;
    public static float tickrate = 20.0f;
    public static float mspt = 50.0f;
    public static long time_bias = 0;
    public static long time_warp_start_time = 0;
    public static long time_warp_scheduled_ticks = 0;
    public static ServerPlayer time_advancerer = null;
    public static String tick_warp_callback = null;
    public static CommandSourceStack tick_warp_sender = null;
    public static int player_active_timeout = 0;
    public static boolean process_entities = true;
    private static boolean deepFreeze = false;
    private static boolean is_paused = false;
    public static boolean is_superHot = false;

    /**
     * @return Whether or not the game is in a frozen state.
     *         You should normally use {@link #process_entities} instead,
     *         since that one accounts for tick steps and superhot
     */
    public static boolean isPaused() {
	    return is_paused;
    }

    /**
     * Whether or not the game is deeply frozen.
     * This can be used for things that you may not normally want
     * to freeze, but may need to in some situations.
     * This should be checked with {@link #process_entities} to make sure the 
     * current tick is actually frozen, not only the game
     * @return Whether or not the game is deeply frozen.
     */
    public static boolean deeplyFrozen() {
        return deepFreeze;
    }

    /**
     * Used to update the frozen state of the game.
     * Handles connected clients as well.
     * @param isPaused Whether or not the game is paused
     * @param isDeepFreeze Whether or not the game is deeply frozen
     */
    public static void setFrozenState(boolean isPaused, boolean isDeepFreeze) {
        is_paused = isPaused;
        deepFreeze = isPaused ? isDeepFreeze : false;
        ServerNetworkHandler.updateFrozenStateToConnectedPlayers();
    }

    /**
     * Functional interface that listens for tickrate changes. This is
     * implemented to allow tickrate compatibility with other mods etc.
     */
    private static final Map<String, BiConsumer<String, Float>> tickrateListeners = new HashMap<>();
    private static final float MIN_TICKRATE = 0.01f;
    
    public static void reset_player_active_timeout()
    {
        if (player_active_timeout < PLAYER_GRACE)
        {
            player_active_timeout = PLAYER_GRACE;
            ServerNetworkHandler.updateTickPlayerActiveTimeoutToConnectedPlayers();
        }
    }

    public static void reset()
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

    public static void add_ticks_to_run_in_pause(int ticks)
    {
        player_active_timeout = PLAYER_GRACE+ticks;
        ServerNetworkHandler.updateTickPlayerActiveTimeoutToConnectedPlayers();
    }

    public static Component tickrate_advance(ServerPlayer player, int advance, String callback, CommandSourceStack source)
    {
        if (0 == advance)
        {
            tick_warp_callback = null;
            if (source != tick_warp_sender) tick_warp_sender = null;
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
            if (time_advancerer != null) who = time_advancerer.getScoreboardName();
            return Messenger.c("l "+who+" is already advancing time at the moment. Try later or ask them");
        }
        time_advancerer = player;
        time_warp_start_time = System.nanoTime();
        time_warp_scheduled_ticks = advance;
        time_bias = advance;
        tick_warp_callback = callback;
        tick_warp_sender = source;
        return Messenger.c("gi Warp speed ....");
    }

    public static void finish_time_warp()
    {

        long completed_ticks = time_warp_scheduled_ticks - time_bias;
        double milis_to_complete = System.nanoTime()-time_warp_start_time;
        if (milis_to_complete == 0.0)
        {
            milis_to_complete = 1.0;
        }
        milis_to_complete /= 1000000.0;
        int tps = (int) (1000.0D*completed_ticks/milis_to_complete);
        double mspt = (1.0*milis_to_complete)/completed_ticks;
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
                    Messenger.m(time_advancerer, "r Command Callback failed - unknown error: ", "rb /"+tick_warp_callback,"/"+tick_warp_callback);
                }
            }
            tick_warp_callback = null;
            tick_warp_sender = null;
        }
        if (time_advancerer != null)
        {
            Messenger.m(time_advancerer, String.format("gi ... Time warp completed with %d tps, or %.2f mspt",tps, mspt ));
            time_advancerer = null;
        }
        else
        {
            Messenger.print_server_message(CarpetServer.minecraft_server, String.format("... Time warp completed with %d tps, or %.2f mspt",tps, mspt ));
        }
        time_bias = 0;

    }

    public static boolean continueWarp()
    {
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

    public static void tick()
    {
        process_entities = true;
        if (player_active_timeout > 0)
        {
            player_active_timeout--;
        }
        if (is_paused)
        {
            if (player_active_timeout < PLAYER_GRACE)
            {
                process_entities = false;
            }
        }
        else if (is_superHot)
        {
            if (player_active_timeout <= 0)
            {
                process_entities = false;

            }
        }
    }
    //unused - mod compat reasons
    public static void tickrate(float rate) {tickrate(rate, true);}
    public static void tickrate(float rate, boolean update)
    {
        tickrate = rate;
        long mspt = (long)(1000.0 / tickrate);
        if (mspt <= 0L)
        {
            mspt = 1L;
            tickrate = 1000.0f;
        }
        
        TickSpeed.mspt = (float)mspt;
        
        if (update) notifyTickrateListeners("carpet");
    }
    
    private static void tickrateChanged(String modId, float rate)
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
    
    private static void notifyTickrateListeners(String originModId)
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
        ServerNetworkHandler.updateTickSpeedToConnectedPlayers();
    }
    
    public static BiConsumer<String, Float> addTickrateListener(String modId, BiConsumer<String, Float> tickrateListener) 
    {
        synchronized (tickrateListeners)
        {
            tickrateListeners.put(modId, tickrateListener);
        }
        return TickSpeed::tickrateChanged;
    }
}

