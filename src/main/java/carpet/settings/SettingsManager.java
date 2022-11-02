package carpet.settings;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.api.settings.CarpetRule;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
import org.apache.logging.log4j.util.TriConsumer;
import java.util.Collection;
import java.util.List;

/**
 * Manages and parses Carpet rules with their own command.
 * @deprecated Use {@link carpet.api.settings.SettingsManager} instead
 */
@Deprecated(forRemoval = true)
public class SettingsManager extends carpet.api.settings.SettingsManager
{
    /**
     * @deprecated Use {@link #locked()} instead
     */
    @Deprecated(forRemoval = true) //to private (or protected?)
    public boolean locked;

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
     * <p>Adds a custom rule observer to changes in rules from 
     * <b>this specific</b> {@link SettingsManager} instance.</p>
     * 
     * <p>This <b>outdated</b> method may not be able to listen to all {@link CarpetRule} implementations</p>
     * 
     * @see SettingsManager#registerGlobalRuleObserver(RuleObserver)
     * 
     * @param observer A {@link TriConsumer} that will be called with
     *                 the used {@link CommandSourceStack}, the changed
     *                 {@link ParsedRule} and a {@link String} being the
     *                 value that the user typed.
     * @deprecated Use {@link SettingsManager#registerRuleObserver(RuleObserver)} instead.
     */
    @Deprecated(forRemoval = true) //to remove
    public void addRuleObserver(TriConsumer<CommandSourceStack, ParsedRule<?>, String> observer)
    {
        registerRuleObserver((source, rule, stringValue) -> {
            if (rule instanceof ParsedRule<?> pr)
                observer.accept(source, pr, stringValue);
            else
                CarpetSettings.LOG.warn("Failed to notify observer '" + observer.getClass().getName() + "' about rule change");
        });
        CarpetSettings.LOG.warn("""
                Extension added outdated rule observer, this is deprecated and will crash in later carpet versions \
                (way before the rest of the old settings api because of relying on log4j)!
                The observer class name is '%s'""".formatted(observer.getClass().getName()));
    }
    
    /**
     * Adds a custom rule observer to changes in rules from 
     * <b>any</b> registered {@link SettingsManager} instance.
     * @see SettingsManager#addRuleObserver(TriConsumer)
     * 
     * @param observer A {@link TriConsumer} that will be called with
     *                 the used {@link CommandSourceStack}, the changed
     *                 {@link ParsedRule} and a {@link String} being the
     *                 value that the user typed.
     * @deprecated Use {@link SettingsManager#registerRuleObserver(RuleObserver)} instead, given this one can't deal with {@link CarpetRule}
     */
    @Deprecated(forRemoval = true) // to remove. This isn't really used anywhere
    public static void addGlobalRuleObserver(TriConsumer<CommandSourceStack, ParsedRule<?>, String> observer)
    {
        registerGlobalRuleObserver((source, rule, stringValue) -> {
            if (rule instanceof ParsedRule<?> pr)
                observer.accept(source, pr, stringValue);
            else
                CarpetSettings.LOG.warn("Failed to notify observer '" + observer.getClass().getName() + "' about rule change");
        });
        CarpetSettings.LOG.warn("""
                Extension added outdated rule observer, this is deprecated and will crash in later carpet versions \
                (way before the rest of the old settings api because of relying on log4j)!
                The observer class name is '%s'""".formatted(observer.getClass().getName()));
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

    /**
     * @param commandLevel A {@link String} being a permission level according to 
     *                     Carpet's standard for permissions (either 0-4, a {@link boolean}, 
     *                     or "ops".
     * @return An {@link int} with the translated Vanilla permission level
     * 
     * @deprecated While there's not an API replacement for this (at least yet),
     *             you can use the similar {@link CommandHelper#canUseCommand(CommandSourceStack, Object)} instead
     * @apiNote Note that this method returns {@code 2} for {@code false} and {@code 0} for {@code true}.
     * You probably want to use {@link #canUseCommand(CommandSourceStack, Object)}e
     */
    @Deprecated(forRemoval = true)
    public static int getCommandLevel(String commandLevel)
    {
        switch (commandLevel)
        {
            case "true": return 2;
            case "false": return 0;
            case "ops": return 2; // typical for other cheaty commands
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
                return Integer.parseInt(commandLevel);
        }
        return 0;
    }
    @Deprecated(forRemoval = true)
    public void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        final CommandBuildContext context = CommandBuildContext.simple(RegistryAccess.EMPTY, FeatureFlags.VANILLA_SET);
        registerCommand(dispatcher, context);
    }
}
