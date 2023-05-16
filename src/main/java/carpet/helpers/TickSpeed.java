package carpet.helpers;

import java.util.Optional;
import java.util.function.BiConsumer;

import carpet.fakes.MinecraftServerInterface;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import carpet.CarpetServer;
import carpet.utils.Messenger;

public class TickSpeed
{
    private static Optional<TickRateManager> gTRM() {
        if (CarpetServer.minecraft_server == null) return Optional.empty();
        return Optional.of(((MinecraftServerInterface)CarpetServer.minecraft_server).getTickRateManager());
    }

    public static boolean process_entities() {
        return gTRM().map(trm -> trm.process_entities).orElse(true);
    }

    public static float mspt() {
        return gTRM().map(trm -> trm.mspt).orElse(50.0f);
    }

    public static boolean isPaused() {
	    return gTRM().map(TickRateManager::isPaused).orElse(false);
    }

    public static boolean deeplyFrozen() {
        return gTRM().map(TickRateManager::deeplyFrozen).orElse(false);
    }

    public static void setFrozenState(boolean isPaused, boolean isDeepFreeze) {
        gTRM().ifPresent(trm -> trm.setFrozenState(isPaused, isDeepFreeze));
    }
    
    public static void reset_player_active_timeout()
    {
        gTRM().ifPresent(TickRateManager::reset_player_active_timeout);
    }

    public static void reset()
    {
        // noop - called on server to reset client
    }

    public static void add_ticks_to_run_in_pause(int ticks)
    {
        gTRM().ifPresent(trm -> trm.add_ticks_to_run_in_pause(ticks));
    }

    public static Component tickrate_advance(ServerPlayer player, int advance, String callback, CommandSourceStack source)
    {
        return gTRM().map(trm -> trm.tickrate_advance(player, advance, callback, source)).orElse(Messenger.c("ri Tickrate management not enabled"));
    }

    public static void finish_time_warp()
    {
        gTRM().ifPresent(TickRateManager::finish_time_warp);
    }

    public static boolean continueWarp()
    {
        return gTRM().map(TickRateManager::continueWarp).orElse(false);
    }

    public static void tick()
    {
        gTRM().ifPresent(TickRateManager::tick);
    }
    //unused - mod compat reasons
    public static void tickrate(float rate) {tickrate(rate, true);}
    public static void tickrate(float rate, boolean update)
    {
        gTRM().ifPresent(trm -> trm.tickrate(rate, update));
    }


    public static BiConsumer<String, Float> addTickrateListener(String modId, BiConsumer<String, Float> tickrateListener) 
    {
        return gTRM().map(trm -> trm.addTickrateListener(modId, tickrateListener)).orElse(null);
    }


    // client only

    private static float tickrate = 20.0f;
    private static float mspt = 50.0f;
    private static int player_active_timeout = 0;
    private static boolean process_entities = true;
    private static boolean deepFreeze = false;
    private static boolean is_paused = false;
    private static boolean is_superHot = false;

    public static void tickrateClient(float rate)
    {
        tickrate = rate;
        long msptt = (long) (1000.0 / tickrate);
        if (msptt <= 0L)
        {
            msptt = 1L;
            tickrate = 1000.0f;
        }

        mspt = msptt;
    }

    public static float msptClient() {
        return mspt;
    }

    public static boolean process_entitiesClient()
    {
        return process_entities;
    }

    public static boolean isIs_superHotClient()
    {
        return is_superHot;
    }

    public static void setPlayer_active_timeoutClient(int timeout)
    {
        player_active_timeout = timeout;
    }


    public static void setFrozenStateClient(boolean isPaused, boolean isDeepFreeze)
    {
        is_paused = isPaused;
        deepFreeze = isPaused ? isDeepFreeze : false;
    }

    public static void setSuperHotClient(boolean isSuperHot)
    {
        is_superHot = isSuperHot;
    }

    public static void tickClient()
    {
        if (player_active_timeout > 0)
        {
            player_active_timeout--;
        }
        if (is_paused)
        {
            process_entities = player_active_timeout >= TickRateManager.PLAYER_GRACE;
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

    public static void resetClient()
    {
        tickrate = 20.0f;
        mspt = 50.0f;
        player_active_timeout = 0;
        process_entities = true;
        deepFreeze = false;
        is_paused = false;
        is_superHot = false;
    }

}

