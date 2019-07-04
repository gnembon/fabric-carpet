package carpet;

import carpet.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

public abstract class CarpetExtension
{
    /**
     * When game started before world is loaded
     */
    public void onGameStarted() {}

    /**
     * Runs once per loaded world once the World files / gamerules etc are fully loaded
     * Can be loaded multiple times in SinglePlayer
     */
    public void onServerLoaded(MinecraftServer server) {}

    /**
     * Runs once per game tick, as a first thing in the tick
     */
    public void onTick(MinecraftServer server) {}

    /**
     * Register your own commands right after vanilla commands are added
     * If that matters for you
     */
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {}

    /**
     * Provide your own custom settings manager managed in the same way as base /carpet
     * command, but separated to its own command as defined in SettingsManager.
     */
    public SettingsManager customSettingsManager() {return null;}

    /**
     * todiddalidoo
     */
    public void onPlayerLoggedIn(PlayerEntity player) {}

    /**
     * todiddalidoo
     */
    public void onPlayerLoggedOut(PlayerEntity player) {}

}
