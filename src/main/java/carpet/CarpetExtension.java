package carpet;

import carpet.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public interface CarpetExtension
{
    /**
     * When game started before world is loaded
     */
    default void onGameStarted() {}

    /**
     * Runs once per loaded world once the World files / gamerules etc are fully loaded
     * Can be loaded multiple times in SinglePlayer
     */
    default void onServerLoaded(MinecraftServer server) {}

    /**
     * Runs once per game tick, as a first thing in the tick
     */
    default void onTick(MinecraftServer server) {}

    /**
     * Register your own commands right after vanilla commands are added
     * If that matters for you
     */
    default void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {}

    /**
     * Provide your own custom settings manager managed in the same way as base /carpet
     * command, but separated to its own command as defined in SettingsManager.
     */
    default SettingsManager customSettingsManager() {return null;}

    /**
     * todiddalidoo
     */
    default void onPlayerLoggedIn(ServerPlayerEntity player) {}

    /**
     * todiddalidoo
     */
    default void onPlayerLoggedOut(ServerPlayerEntity player) {}

    default void onServerClosed(MinecraftServer server) {}
}
