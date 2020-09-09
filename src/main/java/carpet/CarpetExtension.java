package carpet;

import carpet.script.CarpetExpression;
import carpet.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

public interface CarpetExtension
{
    /**
     * When game started before world is loaded
     */
    default void onGameStarted() {}

    /**
     * Runs once per loaded world once the server / gamerules etc are fully loaded
     * but before worlds are loaded
     * Can be loaded multiple times in SinglePlayer
     */
    default void onServerLoaded(MinecraftServer server) {}

    /**
     * Runs once per loaded world once the World files are fully loaded
     * Can be loaded multiple times in SinglePlayer
     */
    default void onServerLoadedWorlds(MinecraftServer server) {}

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

    default void onReload(MinecraftServer server) {}

    default String version() {return null;}

    default void registerLoggers() {}

    default Map<String, String> canHasTranslations(String lang) { return null;}

    /**
     * Handles each call that creates / parses the scarpet expression.
     * Extensions can add their own built-in functions here.
     *
     * Events such as generic events or entity events, can be added statically
     * by creating new events as
     *
     * CarpetEventServer.Event class: to handle `__on_foo()` type of call definitions
     *
     * or
     *
     * EntityEventsGroup.Event class: to handle `entity_event('foo', ...)` type of events
     *
     * @param expression: Passed CarpetExpression to add built-in functions to
     */
    default void scarpetApi(CarpetExpression expression) {}

}
