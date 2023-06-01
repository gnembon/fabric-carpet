package carpet.settings;

import carpet.CarpetServer;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
import java.util.Collection;
import java.util.List;

/**
 * Manages and parses Carpet rules with their own command.
 * @deprecated Use {@link carpet.api.settings.SettingsManager} instead
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("removal") // Gradle needs the explicit suppression
public class SettingsManager extends carpet.api.settings.SettingsManager
{
    /**
     * Creates a new {@link SettingsManager} without a fancy name.
     * @see #SettingsManager(String, String, String)
     * 
     * @param version A {@link String} with the mod's version
     * @param identifier A {@link String} with the mod's id, will be the command name
     * @deprecated This type is deprecated, use {@link carpet.api.settings.SettingsManager#SettingsManager(String, String, String)} instead
     */
    @Deprecated(forRemoval = true)
    public SettingsManager(String version, String identifier)
    {
        this(version, identifier, identifier);
    }

    /**
     * Creates a new {@link SettingsManager} with a fancy name
     * 
     * @param version A {@link String} with the mod's version
     * @param identifier A {@link String} with the mod's id, will be the command name
     * @param fancyName A {@link String} being the mod's fancy name.
     * 
     * @deprecated This type is deprecated, use {@link carpet.api.settings.SettingsManager#SettingsManager(String, String, String)} instead
     */
    @Deprecated(forRemoval = true)
    public SettingsManager(String version, String identifier, String fancyName)
    {
        super(version, identifier, fancyName);
    }

    /**
     * @deprecated Use {@link #identifier()} instead
     */
    @Deprecated(forRemoval = true)
    public String getIdentifier() {
        return identifier();
    }

    /**
     * <p>Gets a registered {@link ParsedRule} in this {@link SettingsManager} by its name.</p>
     * @param name The rule name
     * @return The {@link ParsedRule} with that name
     * @deprecated Use {@link #getCarpetRule(String)} instead. This method is not able to return rules not implemented by {@link ParsedRule}
     */
    @Deprecated(forRemoval = true)
    public ParsedRule<?> getRule(String name)
    {
        return getCarpetRule(name) instanceof ParsedRule<?> pr ? pr : null;
    }

    /**
     * @return A {@link Collection} of the registered {@link ParsedRule}s in this {@link SettingsManager}.
     * @deprecated Use {@link #getCarpetRules()} instead. This method won't be able to return rules not implemented by {@link ParsedRule}
     */
    @Deprecated(forRemoval = true)
    public Collection<ParsedRule<?>> getRules()
    {
        return List.of(getCarpetRules().stream().filter(ParsedRule.class::isInstance).map(ParsedRule.class::cast).toArray(ParsedRule[]::new));
    }

    /**
     * @deprecated Use {@link #dumpAllRulesToStream(java.io.PrintStream, String)} instead
     */
    @Deprecated(forRemoval = true)
    public int printAllRulesToLog(String category) {
        return dumpAllRulesToStream(System.out, category);
    }

    /**
     * Notifies all players that the commands changed by resending the command tree.
     * @deprecated While there's not an API replacement for this (at least yet),
     *             you can use {@link CommandHelper#notifyPlayersCommandsChanged(MinecraftServer)} instead
     */
    @Deprecated(forRemoval = true)
    public void notifyPlayersCommandsChanged()
    {
        CommandHelper.notifyPlayersCommandsChanged(CarpetServer.minecraft_server);
    }

    /**
     * Returns whether the {@link CommandSourceStack} can execute
     * a command given the required permission level, according to
     * Carpet's standard for permissions.
     * @param source The origin {@link CommandSourceStack}
     * @param commandLevel The permission level
     * @return Whether or not the {@link CommandSourceStack} meets the required level
     * 
     * @deprecated While there's not an API replacement for this (at least yet),
     *             you can use {@link CommandHelper#canUseCommand(CommandSourceStack, Object)} instead
     */
    @Deprecated(forRemoval = true)
    public static boolean canUseCommand(CommandSourceStack source, Object commandLevel)
    {
        return CommandHelper.canUseCommand(source, commandLevel);
    }

    @Deprecated(forRemoval = true)
    public void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        final CommandBuildContext context = CommandBuildContext.simple(RegistryAccess.EMPTY, FeatureFlags.VANILLA_SET);
        registerCommand(dispatcher, context);
    }
}
